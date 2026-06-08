package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.AppointmentResource;

import java.util.List;
import java.util.Map;

public interface AppointmentResourceService extends IService<AppointmentResource> {

    /**
     * 获取所有预约资源配置
     */
    List<AppointmentResource> getAllResources();

    /**
     * 获取工作日配置
     */
    List<AppointmentResource> getWorkdayResources();

    /**
     * 根据星期获取配置
     */
    AppointmentResource getResourceByDayOfWeek(Integer dayOfWeek);

    /**
     * 更新资源配置
     */
    boolean updateResource(AppointmentResource resource);

    /**
     * 获取指定日期的可预约信息
     * @param date 日期，格式：yyyy-MM-dd
     */
    Map<String, Object> getAvailableByDate(String date);

    /**
     * 获取可办理业务类型列表
     */
    List<String> getAvailableBusinessTypes();
}
