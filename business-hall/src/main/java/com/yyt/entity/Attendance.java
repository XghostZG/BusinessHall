package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录表
 */
@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;                    // 营业员ID
    private LocalDate date;                // 考勤日期
    private LocalDateTime checkInTime;     // 上班打卡时间
    private LocalDateTime checkOutTime;    // 下班打卡时间
    private String status;                  // 状态: 正常/迟到/早退/缺勤/请假
    private String checkInRemark;          // 上班打卡备注
    private String checkOutRemark;         // 下班打卡备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
