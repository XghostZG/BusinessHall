package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quick_reply")
public class QuickReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;             // 标题
    private String content;            // 回复内容
    private String category;            // 分类：问候|业务|致歉|结束
    private Integer sortOrder;         // 排序
    private Integer isActive;          // 是否启用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
