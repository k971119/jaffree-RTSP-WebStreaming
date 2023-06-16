package com.example.streaming.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class RTSPService {

    private final FFmpegFrameGrabber grabber;

    private boolean connect;

    public boolean isConnect() {
        return connect;
    }

    public RTSPService(String URI){
        this.grabber = new FFmpegFrameGrabber(URI);
        grabber.setImageWidth(550);
        grabber.setImageHeight(340);
        grabber.setFrameRate(20);
        grabber.setVideoCodecName("h264");
        grabber.setVideoBitrate(2560000);
    }

    public void startStreaming() throws Exception{
        grabber.start();
        connect = true;
    }

    public byte[] getStreamData() throws Exception {
        Frame frame = grabber.grabFrame();
        if (frame.image != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.getBufferedImage(frame);

            // 이미지를 압축하여 JPEG 포맷으로 인코딩
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(1.0f); // 압축 품질 설정 (0.0f ~ 1.0f)
            writer.setOutput(new MemoryCacheImageOutputStream(byteArrayOutputStream));
            writer.write(null, new IIOImage(image, null, null), params);
            writer.dispose();

            byte[] byteImage = byteArrayOutputStream.toByteArray();

            return byteImage;
        }
        return null;
    }


    public void stopStreaming() throws Exception{
        connect = false;
        grabber.stop();
    }

}
