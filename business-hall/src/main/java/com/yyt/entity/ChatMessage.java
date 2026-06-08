package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;           // 会话ID

    private String senderType;         // 发送者类型：user用户|staff客服|system系统
    private Long senderId;             // 发送者ID
    private String senderName;         // 发送者姓名

    private String content;            // 消息内容

    private String messageType;        // 消息类型：text文本|image图片|file文件

    private Integer isRead;            // 是否已读：0未读|1已读

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
