package com.yyt.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * WebSocket 消息控制器
 * 注意：STOMP消息代理在 WebSocketConfig 中配置
 * 此控制器用于处理来自客户端的STOMP消息
 */
@RestController
public class WebSocketController {
    
    @Resource
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 处理预约更新消息
     * 客户端通过 @MessageMapping("/appointments") 发送消息
     * 消息会被广播到 /topic/appointments
     */
    @MessageMapping("/appointments")
    @SendTo("/topic/appointments")
    public AppointmentMessage handleAppointmentUpdate(AppointmentMessage message) {
        return message;
    }
    
    /**
     * 主动推送预约状态更新（供其他Service调用）
     */
    public void sendAppointmentStatusUpdate(String message) {
        messagingTemplate.convertAndSend("/topic/appointments", new AppointmentMessage(message));
    }
    
    /**
     * 预约消息
     */
    public static class AppointmentMessage {
        private String content;
        
        public AppointmentMessage() {
        }
        
        public AppointmentMessage(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}