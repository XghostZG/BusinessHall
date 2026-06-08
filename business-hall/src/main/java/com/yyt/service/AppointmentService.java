package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.Appointment;

import java.util.List;
import java.util.Map;

public interface AppointmentService extends IService<Appointment> {
    Long createAppointment(Appointment appointment);
    boolean cancelAppointment(Long appointmentId);
    boolean rescheduleAppointment(Long appointmentId, String newDate, String newStartTime, String newEndTime);
    List<Map<String, Object>> getTodayAppointments();
    List<Map<String, Object>> getTodayAppointmentsByClerk(Long clerkId);
    List<Map<String, Object>> getUserAppointments(Long userId);
    boolean updateAppointmentStatus(Long appointmentId, String status);
    List<Map<String, Object>> enrichAppointments(List<Appointment> appointments);
    
    /**
     * 核验码验证 + 开始办理
     */
    boolean startWithVerification(Long appointmentId, Long clerkId, String verificationCode);
    
    /**
     * 输入处理结果 + 完成办理
     */
    boolean completeWithResult(Long appointmentId, Long clerkId, String result);
    
    /**
     * 获取指定日期各时段的已预约数量
     * @param date 预约日期
     * @return Map，key为时段标识(startTime-endTime)，value为已预约数量
     */
    Map<String, Long> getBookedCountByDate(String date);
}