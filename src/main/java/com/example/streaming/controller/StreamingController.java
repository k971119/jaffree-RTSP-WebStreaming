package com.example.streaming.controller;

import com.example.streaming.service.StreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StreamingController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    StreamingService streamingService;

    @GetMapping("/video")
    public String video(){
        return "video";
    }

    @MessageMapping("/streaming")
    public void streaming(@Payload String addr){
        System.out.println("클라이언트에서 왔습니다. : " + addr);
        this.simpMessagingTemplate.convertAndSend("/topic/streaming", "서버에서 왔습니다. : " + addr);
    }
}
