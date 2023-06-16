package com.example.streaming.controller;

import com.example.streaming.service.RTSPService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StreamingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private boolean connect;

    private RTSPService rtspService;

    @GetMapping("/video")
    public String video(){
        return "video";
    }

    @MessageMapping("/streaming")
    //싱글톤이라 나중에 요청하는 클라이언트 id 받아서 처리해야 할 수도 있음
    public void streaming(@Payload String addr){
        rtspService = new RTSPService(addr);
        
        try {
            System.out.println("클라이언트에서 왔습니다. : " + addr);
            rtspService.startStreaming();

            while (rtspService.isConnect()) {
                byte[] streamData = rtspService.getStreamData();
                if (streamData != null) {
                        System.out.println(streamData.length);
                        String base64Image = Base64.encodeBase64String(streamData);
                        messagingTemplate.convertAndSend("/topic/streaming", base64Image);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
