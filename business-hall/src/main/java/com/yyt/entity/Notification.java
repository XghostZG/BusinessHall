package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 接收用户ID
    private String title;
    private String content;
    private String type; // system/consultation/complaint/attendance
    private Long relatedId; // 关联ID(咨询/投诉等)
    private Integer isRead; // 是否已读: 0未读 1已读
    private LocalDateTime createTime;
}