package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String role; // 客户、营业员、管理员
    private String realName;
    private String phone;
    private String email;
    private Integer status; // 0: 禁用, 1: 启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}