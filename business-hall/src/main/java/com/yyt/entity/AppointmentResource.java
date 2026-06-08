package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可预约资源配置表
 * 按星期几配置每天的可预约时间段和业务类型
 */
@Data
@TableName("appointment_resource")
public class AppointmentResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 星期几: 1=周一 2=周二 3=周三 4=周四 5=周五 6=周六 7=周日
     */
    private Integer dayOfWeek;

    /**
     * 星期名称，如"周一"、"周二"
     */
    private String dayName;

    /**
     * 是否工作日: 1工作日 0休息日
     */
    private Integer isWorkday;

    /**
     * 时间段配置JSON，格式：[{"startTime":"09:00","endTime":"12:00","maxQuota":20}]
     */
    private String timeSlots;

    /**
     * 可办理业务类型JSON，格式：["开户","销户","缴费"]
     */
    private String availableBusinessTypes;

    /**
     * 备注描述
     */
    private String description;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
