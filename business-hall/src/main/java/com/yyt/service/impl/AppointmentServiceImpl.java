package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.Appointment;
import com.yyt.entity.User;
import com.yyt.mapper.AppointmentMapper;
import com.yyt.mapper.UserMapper;
import com.yyt.service.AppointmentService;
import com.yyt.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserMapper userMapper;

    /**
     * 生成6位数字核验码，同一天内唯一
     */
    private String generateVerificationCode() {
        String date = LocalDate.now().toString();
        String code;
        int maxRetries = 10;
        Random random = new Random();
        do {
            code = String.valueOf(100000 + random.nextInt(900000));
            maxRetries--;
        } while (baseMapper.countByVerificationCodeAndDate(code, date) > 0 && maxRetries > 0);
        return code;
    }

    /**
     * 分配营业员：按当日已分配预约数最少的原则负载均衡
     */
    private Long assignClerk() {
        // 查询所有启用的营业员
        List<User> clerks = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "clerk")
                .eq(User::getStatus, 1));
        if (clerks.isEmpty()) {
            return null;
        }

        // 查找当日分配预约最少的营业员
        Long bestClerkId = null;
        int minCount = Integer.MAX_VALUE;
        for (User clerk : clerks) {
            int count = (int) count(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getClerkId, clerk.getId())
                    .eq(Appointment::getAppointmentDate, LocalDate.now().toString())
                    .ne(Appointment::getStatus, "已取消"));
            if (count < minCount) {
                minCount = count;
                bestClerkId = clerk.getId();
            }
        }
        return bestClerkId;
    }

    @Override
    @Transactional
    public Long createAppointment(Appointment appointment) {
        try {
            // 生成预约编号
            if (appointment.getAppointmentNo() == null || appointment.getAppointmentNo().isEmpty()) {
                String appointmentNo = "AP" + System.currentTimeMillis();
                appointment.setAppointmentNo(appointmentNo);
            }

            // 检查用户是否已经在同一日期同一时段有待办理的预约
            String timeRange = appointment.getAppointmentTimeRange();
            List<Appointment> existingAppointments = list(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getUserId, appointment.getUserId())
                    .eq(Appointment::getAppointmentDate, appointment.getAppointmentDate())
                    .eq(Appointment::getAppointmentTimeRange, timeRange)
                    .eq(Appointment::getStatus, "待办理"));
            if (!existingAppointments.isEmpty()) {
                throw new IllegalArgumentException("您在该日期该时段已有待办理的预约");
            }

            // 生成核验码
            appointment.setVerificationCode(generateVerificationCode());

            // 分配营业员
            Long clerkId = assignClerk();
            appointment.setClerkId(clerkId);

            appointment.setStatus("待办理");
            appointment.setCreateTime(LocalDateTime.now());
            appointment.setUpdateTime(LocalDateTime.now());

            if (save(appointment)) {
                // 发送通知给分配的营业员
                if (clerkId != null) {
                    notificationService.sendNotificationToRole(
                        "新预约创建",
                        "客户 " + appointment.getCustomerName() + " 创建了新预约，已分配给您处理",
                        "system",
                        appointment.getId(),
                        "clerk"
                    );
                }
                notificationService.sendNotificationToRole(
                    "新预约创建",
                    "客户 " + appointment.getCustomerName() + " 创建了新预约",
                    "system",
                    appointment.getId(),
                    "admin"
                );
                return appointment.getId();
            } else {
                throw new RuntimeException("保存预约失败");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("预约失败，系统异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean startWithVerification(Long appointmentId, Long clerkId, String verificationCode) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"待办理".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不允许开始办理");
        }
        if (appointment.getClerkId() != null && !appointment.getClerkId().equals(clerkId)) {
            throw new IllegalArgumentException("该预约未分配给您处理");
        }
        if (!appointment.getVerificationCode().equals(verificationCode)) {
            throw new IllegalArgumentException("核验码错误，请重新输入");
        }
        appointment.setStatus("办理中");
        appointment.setCheckinTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        return updateById(appointment);
    }

    @Override
    @Transactional
    public boolean completeWithResult(Long appointmentId, Long clerkId, String result) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"办理中".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("当前预约状态不允许完成办理");
        }
        if (appointment.getClerkId() != null && !appointment.getClerkId().equals(clerkId)) {
            throw new IllegalArgumentException("该预约未分配给您处理");
        }
        appointment.setStatus("已完成");
        appointment.setResult(result);
        appointment.setCompleteTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());

        boolean updated = updateById(appointment);
        if (updated) {
            notificationService.sendNotificationToRole(
                "预约已完成",
                "客户 " + appointment.getCustomerName() + " 的预约已办理完成",
                "system",
                appointment.getId(),
                "admin"
            );
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean cancelAppointment(Long appointmentId) {
        Appointment appointment = getById(appointmentId);
        if (appointment != null && "待办理".equals(appointment.getStatus())) {
            appointment.setStatus("已取消");
            appointment.setCancelTime(LocalDateTime.now());
            appointment.setUpdateTime(LocalDateTime.now());
            return updateById(appointment);
        }
        return false;
    }

    @Override
    @Transactional
    public boolean rescheduleAppointment(Long appointmentId, String newDate, String newStartTime, String newEndTime) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            return false;
        }
        // 只有待办理状态的预约才能改签
        if (!"待办理".equals(appointment.getStatus())) {
            return false;
        }
        
        // 更新预约日期和时段信息
        appointment.setAppointmentDate(newDate);
        appointment.setTimeSlotJson("{\"startTime\":\"" + newStartTime + "\",\"endTime\":\"" + newEndTime + "\"}");
        appointment.setAppointmentTimeRange(newStartTime + "-" + newEndTime);
        appointment.setUpdateTime(LocalDateTime.now());
        
        // 重新分配营业员
        Long clerkId = assignClerk();
        appointment.setClerkId(clerkId);
        
        return updateById(appointment);
    }

    @Override
    public List<Map<String, Object>> getTodayAppointments() {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getAppointmentDate, today.toString())
                .orderByAsc(Appointment::getAppointmentTimeRange));
        return enrichAppointments(appointments);
    }

    @Override
    public List<Map<String, Object>> getTodayAppointmentsByClerk(Long clerkId) {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getAppointmentDate, today.toString())
                .eq(Appointment::getClerkId, clerkId)
                .orderByAsc(Appointment::getAppointmentTimeRange));
        return enrichAppointments(appointments);
    }

    @Override
    public List<Map<String, Object>> getUserAppointments(Long userId) {
        List<Appointment> appointments = list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId)
                .orderByDesc(Appointment::getCreateTime));
        return enrichAppointments(appointments, true);
    }

    public List<Map<String, Object>> enrichAppointments(List<Appointment> appointments) {
        return enrichAppointments(appointments, false);
    }

    public List<Map<String, Object>> enrichAppointments(List<Appointment> appointments, boolean includeVerificationCode) {
        if (appointments == null || appointments.isEmpty()) {
            return new ArrayList<>();
        }

        // 丰富预约数据
        return appointments.stream().map(appointment -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", appointment.getId());
            map.put("appointmentNo", appointment.getAppointmentNo());
            map.put("userId", appointment.getUserId());
            map.put("appointmentDate", appointment.getAppointmentDate());
            map.put("timeSlotJson", appointment.getTimeSlotJson());
            map.put("appointmentTimeRange", appointment.getAppointmentTimeRange());
            map.put("businessType", appointment.getBusinessType());
            map.put("customerName", appointment.getCustomerName());
            map.put("customerPhone", appointment.getCustomerPhone());
            map.put("remark", appointment.getRemark());
            if (includeVerificationCode) {
                map.put("verificationCode", appointment.getVerificationCode());
            }
            map.put("clerkId", appointment.getClerkId());
            map.put("result", appointment.getResult());
            map.put("status", appointment.getStatus());
            map.put("createTime", appointment.getCreateTime());
            map.put("updateTime", appointment.getUpdateTime());
            map.put("checkinTime", appointment.getCheckinTime());
            map.put("completeTime", appointment.getCompleteTime());
            map.put("cancelTime", appointment.getCancelTime());

            // 查询营业员姓名
            if (appointment.getClerkId() != null) {
                User clerk = userMapper.selectById(appointment.getClerkId());
                map.put("clerkName", clerk != null ? clerk.getRealName() : "未分配");
            } else {
                map.put("clerkName", "未分配");
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean updateAppointmentStatus(Long appointmentId, String status) {
        Appointment appointment = getById(appointmentId);
        if (appointment != null) {
            appointment.setStatus(status);
            appointment.setUpdateTime(LocalDateTime.now());

            // 仅记录取消和爽约时间
            if ("已取消".equals(status)) {
                appointment.setCancelTime(LocalDateTime.now());
            }

            boolean updated = updateById(appointment);
            if (updated) {
                // 发送通知
                notificationService.sendNotificationToRole(
                    "预约状态更新",
                    "客户 " + appointment.getCustomerName() + " 的预约状态已更新为 " + status,
                    "system",
                    appointment.getId(),
                    "clerk"
                );
                notificationService.sendNotificationToRole(
                    "预约状态更新",
                    "客户 " + appointment.getCustomerName() + " 的预约状态已更新为 " + status,
                    "system",
                    appointment.getId(),
                    "admin"
                );
            }
            return updated;
        }
        return false;
    }

    @Override
    public Map<String, Long> getBookedCountByDate(String date) {
        List<Appointment> appointments = list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getAppointmentDate, date)
                .ne(Appointment::getStatus, "已取消"));
        
        Map<String, Long> bookedCountMap = new java.util.HashMap<>();
        for (Appointment appointment : appointments) {
            String timeRange = appointment.getAppointmentTimeRange();
            if (timeRange != null && !timeRange.isEmpty()) {
                bookedCountMap.put(timeRange, bookedCountMap.getOrDefault(timeRange, 0L) + 1);
            }
        }
        return bookedCountMap;
    }
}
