package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionNo;          // 会话编号

    private Long userId;               // 用户ID
    private String userName;           // 用户姓名

    private Long staffId;              // 分配的客服ID
    private String staffName;          // 客服姓名

    private String status;             // 会话状态：waiting等待中|chatting进行中|closed已关闭

    private Integer unreadUser;         // 用户未读消息数
    private Integer unreadStaff;       // 客服未读消息数

    private LocalDateTime startTime;   // 会话开始时间
    private LocalDateTime endTime;     // 会话结束时间

    private String lastMessage;        // 最后一条消息
    private LocalDateTime lastMessageTime; // 最后消息时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
