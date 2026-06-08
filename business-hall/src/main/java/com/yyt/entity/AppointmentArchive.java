package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_archive")
public class AppointmentArchive {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appointmentNo;
    private Long userId;
    private String appointmentDate;
    private String timeSlotJson;
    private String appointmentTimeRange;
    private String businessType;
    private String customerName;
    private String customerPhone;
    private String remark;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime checkinTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String result; // 处理结果
    private LocalDateTime archivedAt; // 归档时间
}
