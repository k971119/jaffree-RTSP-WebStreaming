package com.example.streaming.controller;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Controller
public class videoController {

    String URI = "";

    @RequestMapping("/streaming")
    public String streaming(String uri){
        URI = uri;
        return "video";
    }

    @RequestMapping("/live")
    public String live(){
        return "live";
    }

    @GetMapping(value = "/live.mp4")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> liveStream() {
        String url = "rtsp://user:1qaz5tgb!@192.168.0.128:554/profile2/media.smp";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(outputStream ->{
                    FFmpeg.atPath()         //ffmpeg 커맨드 명령어를 이용함으로 사전에 ffmpeg 파일 설치와 환경변수 세팅이 필요함
                            .addArgument("-re")
                            .addArguments("-acodec", "pcm_s16le")
                            .addArguments("-rtsp_transport", "tcp")
                            .addArguments("-i", url)
                            .addArguments("-vcodec", "copy")
                            .addArguments("-af", "asetrate=22050")
                            .addArguments("-acodec", "aac")
                            .addArguments("-b:a", "96k" )
                            .addOutput(PipeOutput.pumpTo(outputStream)
                                    //.disableStream(StreamType.AUDIO)
                                    //.disableStream(StreamType.SUBTITLE)
                                    .disableStream(StreamType.DATA)
                                    .setFrameCount(StreamType.VIDEO, 30l)
                                    //1 frame every 10 seconds
                                    .setFrameRate(0.1)
                                    .setDuration(1, TimeUnit.HOURS)
                                    .setFormat("ismv"))
                            .addArgument("-nostdin")
                            .execute();
                });
    }

    @RequestMapping(value = "/stream", produces = "application/x-mpegURL")
    @ResponseBody
    public ResponseEntity<byte[]> stream() throws IOException {
        // RTSP URL 설정
        String rtspUrl = "rtsp://user:1qaz5tgb!@192.168.0.128:554/profile2/media.smp";

        // FFmpeg 명령어 설정
        String ffmpegCommand = String.format("ffmpeg -i %s -c:v copy -c:a copy -f hls -hls_time 10 -hls_list_size 6 -hls_flags delete_segments -hls_segment_filename segment%%03d.ts classes/static/output.m3u8", rtspUrl);

        // FFmpeg 명령어 실행
        Process process = Runtime.getRuntime().exec(ffmpegCommand);

        // FFmpeg의 출력을 읽어오기 위한 InputStream 생성
        InputStream inputStream = process.getErrorStream();

        // FFmpeg 출력을 로그로 출력하기 위한 Thread 생성
        Thread logThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        logThread.start();

        // FFmpeg가 HLS 스트림을 생성할 때까지 대기 (일반적으로 몇 초 정도 소요됨)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 생성된 HLS 스트림 파일을 읽어서 바이트 배열로 변환
        Path hlsStreamPath = Path.of("output.m3u8");
        byte[] hlsStreamBytes = Files.readAllBytes(hlsStreamPath);

        // 생성된 파일 삭제
        Files.delete(hlsStreamPath);
        Files.deleteIfExists(Path.of("segment%03d.ts"));

        // HTTP 응답으로 HLS 스트림 전송
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-mpegURL"));
        return new ResponseEntity<>(hlsStreamBytes, headers, HttpStatus.OK);
    }
}
