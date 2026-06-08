package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appointmentNo; // 预约编号: AP+时间戳
    private Long userId;
    private String appointmentDate; // 预约日期
    private String timeSlotJson; // 时间段JSON {"startTime":"09:00","endTime":"12:00"}
    private String appointmentTimeRange; // 预约时间段显示: 09:00-12:00
    private String businessType; // 业务类型
    private String customerName;
    private String customerPhone;
    private String remark;
    private String status; // 待办理/办理中/已完成/已取消/爽约
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime checkinTime; // 签到时间
    private LocalDateTime completeTime; // 完成时间
    private LocalDateTime cancelTime; // 取消时间
    private String verificationCode; // 核验码
    private Long clerkId; // 营业员ID
    private String result; // 处理结果
}