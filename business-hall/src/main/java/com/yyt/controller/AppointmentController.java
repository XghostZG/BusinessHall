package com.yyt.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yyt.entity.Appointment;
import com.yyt.entity.AppointmentArchive;
import com.yyt.service.AppointmentService;
import com.yyt.service.AppointmentArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentArchiveService appointmentArchiveService;

    public AppointmentController(AppointmentService appointmentService, AppointmentArchiveService appointmentArchiveService) {
        this.appointmentService = appointmentService;
        this.appointmentArchiveService = appointmentArchiveService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAppointment(@RequestBody Appointment appointment) {
        try {
            Long appointmentId = appointmentService.createAppointment(appointment);
            if (appointmentId != null) {
                // 查询完整预约信息以获取核验码
                Appointment created = appointmentService.getById(appointmentId);
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("appointmentId", appointmentId);
                result.put("appointmentNo", created.getAppointmentNo());
                result.put("verificationCode", created.getVerificationCode());
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body("预约失败，时段已满");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("预约失败，系统异常");
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAppointment(@RequestParam Long appointmentId) {
        if (appointmentService.cancelAppointment(appointmentId)) {
            return ResponseEntity.ok("取消成功");
        } else {
            return ResponseEntity.badRequest().body("取消失败，预约状态已变更");
        }
    }

    @PostMapping("/reschedule")
    public ResponseEntity<?> rescheduleAppointment(
            @RequestParam Long appointmentId, 
            @RequestParam String newDate,
            @RequestParam String newStartTime,
            @RequestParam String newEndTime) {
        if (appointmentService.rescheduleAppointment(appointmentId, newDate, newStartTime, newEndTime)) {
            return ResponseEntity.ok("改签成功");
        } else {
            return ResponseEntity.badRequest().body("改签失败，预约状态已变更或不存在");
        }
    }

    @GetMapping("/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayAppointments(@RequestParam(required = false) Long clerkId) {
        if (clerkId != null) {
            return ResponseEntity.ok(appointmentService.getTodayAppointmentsByClerk(clerkId));
        }
        return ResponseEntity.ok(appointmentService.getTodayAppointments());
    }

    @GetMapping("/user")
    public ResponseEntity<List<Map<String, Object>>> getUserAppointments(@RequestParam Long userId) {
        return ResponseEntity.ok(appointmentService.getUserAppointments(userId));
    }

    @PutMapping("/status")
    public ResponseEntity<?> updateAppointmentStatus(@RequestParam Long appointmentId, @RequestParam String status) {
        // 仅允许取消和爽约状态变更
        if (!"已取消".equals(status) && !"爽约".equals(status)) {
            return ResponseEntity.badRequest().body("不支持的状态变更，请使用专用接口");
        }
        if (appointmentService.updateAppointmentStatus(appointmentId, status)) {
            return ResponseEntity.ok("状态更新成功");
        } else {
            return ResponseEntity.badRequest().body("状态更新失败");
        }
    }

    /**
     * 核验码验证 + 开始办理
     */
    @PostMapping("/start")
    public ResponseEntity<?> startWithVerification(@RequestBody Map<String, Object> params) {
        try {
            Long appointmentId = Long.valueOf(params.get("appointmentId").toString());
            Long clerkId = Long.valueOf(params.get("clerkId").toString());
            String verificationCode = (String) params.get("verificationCode");
            if (verificationCode == null || verificationCode.isEmpty()) {
                return ResponseEntity.badRequest().body("请输入核验码");
            }
            if (appointmentService.startWithVerification(appointmentId, clerkId, verificationCode)) {
                return ResponseEntity.ok("核验通过，开始办理");
            } else {
                return ResponseEntity.badRequest().body("开始办理失败");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("操作失败，系统异常");
        }
    }

    /**
     * 输入处理结果 + 完成办理
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeWithResult(@RequestBody Map<String, Object> params) {
        try {
            Long appointmentId = Long.valueOf(params.get("appointmentId").toString());
            Long clerkId = Long.valueOf(params.get("clerkId").toString());
            String result = (String) params.get("result");
            if (result == null || result.isEmpty()) {
                return ResponseEntity.badRequest().body("请输入处理结果");
            }
            if (appointmentService.completeWithResult(appointmentId, clerkId, result)) {
                return ResponseEntity.ok("办理完成");
            } else {
                return ResponseEntity.badRequest().body("完成办理失败");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("操作失败，系统异常");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllAppointments() {
        List<Appointment> appointments = appointmentService.list();
        return ResponseEntity.ok(appointmentService.enrichAppointments(appointments));
    }

    @GetMapping("/statistics/today")
    public ResponseEntity<?> getTodayStatistics() {
        LocalDate today = LocalDate.now();
        List<Appointment> todayAppointments = appointmentService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay()));

        long total = todayAppointments.size();
        long completed = todayAppointments.stream().filter(a -> "已完成".equals(a.getStatus())).count();
        long canceled = todayAppointments.stream().filter(a -> "已取消".equals(a.getStatus())).count();
        long noShow = todayAppointments.stream().filter(a -> "爽约".equals(a.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("canceled", canceled);
        stats.put("noShow", noShow);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/trend")
    public ResponseEntity<?> getAppointmentTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            long count = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                    .ge(Appointment::getCreateTime, date.atStartOfDay())
                    .le(Appointment::getCreateTime, date.plusDays(1).atStartOfDay()));
            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", date.toString());
            dayStats.put("count", count);
            trend.add(dayStats);
        }

        return ResponseEntity.ok(trend);
    }

    @GetMapping("/statistics/business-type")
    public ResponseEntity<?> getBusinessTypeStatistics() {
        List<Appointment> appointments = appointmentService.list();
        // 按业务类型分组统计（businessType 是字符串）
        Map<String, Long> businessTypeCount = appointments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Appointment::getBusinessType,
                        java.util.stream.Collectors.counting()));

        List<Map<String, Object>> stats = businessTypeCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("name", entry.getKey() != null ? entry.getKey() : "未知");
                    stat.put("value", entry.getValue());
                    return stat;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/week")
    public ResponseEntity<?> getWeekAppointments() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        long count = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, startDate.atStartOfDay())
                .le(Appointment::getCreateTime, endDate.plusDays(1).atStartOfDay()));

        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics/monthly")
    public ResponseEntity<?> getMonthlyStatistics() {
        LocalDate now = LocalDate.now();
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        // 获取最近6个月的数据
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            long appointmentCount = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                    .ge(Appointment::getCreateTime, monthStart.atStartOfDay())
                    .le(Appointment::getCreateTime, monthEnd.plusDays(1).atStartOfDay()));

            long completedCount = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                    .ge(Appointment::getCreateTime, monthStart.atStartOfDay())
                    .le(Appointment::getCreateTime, monthEnd.plusDays(1).atStartOfDay())
                    .eq(Appointment::getStatus, "已完成"));

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonth().getValue() + "月");
            monthData.put("appointmentCount", appointmentCount);
            monthData.put("completedCount", completedCount);
            monthlyData.add(monthData);
        }

        // 整理数据格式
        Map<String, Object> result = new HashMap<>();
        result.put("months", monthlyData.stream().map(item -> item.get("month")).collect(Collectors.toList()));
        result.put("appointmentCounts", monthlyData.stream().map(item -> item.get("appointmentCount")).collect(Collectors.toList()));
        result.put("completedCounts", monthlyData.stream().map(item -> item.get("completedCount")).collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/trends")
    public ResponseEntity<?> getTrends() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastWeek = today.minusWeeks(1);

        Map<String, Object> trends = new HashMap<>();

        // 今日预约总量趋势
        long todayTotal = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay()));
        long yesterdayTotal = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, yesterday.atStartOfDay())
                .le(Appointment::getCreateTime, yesterday.plusDays(1).atStartOfDay()));
        trends.put("todayTotal", calculateTrend(todayTotal, yesterdayTotal));

        // 已完成预约趋势
        long todayCompleted = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "已完成"));
        long yesterdayCompleted = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, yesterday.atStartOfDay())
                .le(Appointment::getCreateTime, yesterday.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "已完成"));
        trends.put("todayCompleted", calculateTrend(todayCompleted, yesterdayCompleted));

        // 已取消预约趋势
        long todayCanceled = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "已取消"));
        long yesterdayCanceled = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, yesterday.atStartOfDay())
                .le(Appointment::getCreateTime, yesterday.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "已取消"));
        trends.put("todayCanceled", calculateTrend(todayCanceled, yesterdayCanceled));

        // 爽约趋势
        long todayNoShow = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, today.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "爽约"));
        long yesterdayNoShow = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, yesterday.atStartOfDay())
                .le(Appointment::getCreateTime, yesterday.plusDays(1).atStartOfDay())
                .eq(Appointment::getStatus, "爽约"));
        trends.put("todayNoShow", calculateTrend(todayNoShow, yesterdayNoShow));

        // 本周预约趋势
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate lastWeekStart = lastWeek.minusDays(lastWeek.getDayOfWeek().getValue() - 1);
        long thisWeekAppointments = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, weekStart.atStartOfDay())
                .le(Appointment::getCreateTime, today.plusDays(1).atStartOfDay()));
        long lastWeekAppointments = appointmentService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreateTime, lastWeekStart.atStartOfDay())
                .le(Appointment::getCreateTime, lastWeek.plusDays(1).atStartOfDay()));
        trends.put("weekAppointments", calculateTrend(thisWeekAppointments, lastWeekAppointments));

        return ResponseEntity.ok(trends);
    }

    private Map<String, Object> calculateTrend(long current, long previous) {
        Map<String, Object> trend = new HashMap<>();
        if (previous == 0) {
            trend.put("direction", current > 0 ? "positive" : "neutral");
            trend.put("value", current > 0 ? "+100%" : "0%");
        } else {
            double percentage = ((double) (current - previous) / previous) * 100;
            trend.put("direction", percentage > 0 ? "positive" : percentage < 0 ? "negative" : "neutral");
            trend.put("value", String.format("%.1f%%", percentage).replace("-0.0%", "0%"));
        }
        return trend;
    }

    /**
     * 批量删除预约（管理员）
     */
    @PostMapping("/batch-delete")
    public ResponseEntity<?> batchDeleteAppointments(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择要删除的预约");
        }
        boolean success = appointmentService.removeByIds(ids);
        if (success) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "成功删除 " + ids.size() + " 条预约记录");
            result.put("count", ids.size());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("删除失败");
        }
    }

    /**
     * 预览可归档的预约数量
     */
    @PostMapping("/archive/preview")
    public ResponseEntity<?> previewArchive(@RequestBody Map<String, Object> params) {
        String endDateStr = (String) params.get("endDate");
        @SuppressWarnings("unchecked")
        List<String> statuses = (List<String>) params.get("statuses");

        if (endDateStr == null || endDateStr.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择截止日期");
        }

        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.le(Appointment::getAppointmentDate, endDate.toString());

        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(Appointment::getStatus, statuses);
        }

        long count = appointmentService.count(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("endDate", endDateStr);
        result.put("statuses", statuses);
        return ResponseEntity.ok(result);
    }

    /**
     * 执行预约归档
     */
    @PostMapping("/archive")
    @Transactional
    public ResponseEntity<?> archiveAppointments(@RequestBody Map<String, Object> params) {
        String endDateStr = (String) params.get("endDate");
        @SuppressWarnings("unchecked")
        List<String> statuses = (List<String>) params.get("statuses");

        if (endDateStr == null || endDateStr.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择截止日期");
        }

        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE);

        // 查询符合条件的预约
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Appointment::getAppointmentDate, endDate.toString());

        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(Appointment::getStatus, statuses);
        }

        List<Appointment> appointmentsToArchive = appointmentService.list(wrapper);

        if (appointmentsToArchive.isEmpty()) {
            return ResponseEntity.badRequest().body("没有符合条件的预约记录");
        }

        // 转换为归档记录并保存
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentArchive> archives = appointmentsToArchive.stream().map(appointment -> {
            AppointmentArchive archive = new AppointmentArchive();
            BeanUtils.copyProperties(appointment, archive);
            archive.setArchivedAt(now);
            return archive;
        }).collect(Collectors.toList());

        // 批量插入归档表
        boolean archiveSuccess = appointmentArchiveService.saveBatch(archives);

        if (!archiveSuccess) {
            return ResponseEntity.internalServerError().body("归档失败");
        }

        // 删除原表记录
        List<Long> idsToDelete = appointmentsToArchive.stream()
                .map(Appointment::getId)
                .collect(Collectors.toList());
        appointmentService.removeByIds(idsToDelete);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "成功归档 " + archives.size() + " 条预约记录");
        result.put("count", archives.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取归档记录列表
     */
    @GetMapping("/archive")
    public ResponseEntity<?> getArchiveList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerName) {

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppointmentArchive> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(AppointmentArchive::getArchivedAt);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(AppointmentArchive::getAppointmentDate, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(AppointmentArchive::getAppointmentDate, endDate);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AppointmentArchive::getStatus, status);
        }
        if (customerName != null && !customerName.isEmpty()) {
            wrapper.like(AppointmentArchive::getCustomerName, customerName);
        }

        List<AppointmentArchive> archives = appointmentArchiveService.list(wrapper);
        return ResponseEntity.ok(archives);
    }

    /**
     * 恢复归档记录到主表（可选功能）
     */
    @PostMapping("/archive/restore")
    @Transactional
    public ResponseEntity<?> restoreArchive(@RequestBody List<Long> archiveIds) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择要恢复的归档记录");
        }

        List<AppointmentArchive> archives = appointmentArchiveService.listByIds(archiveIds);

        if (archives.isEmpty()) {
            return ResponseEntity.badRequest().body("未找到指定的归档记录");
        }

        // 转换回主表记录
        List<Appointment> appointments = archives.stream().map(archive -> {
            Appointment appointment = new Appointment();
            BeanUtils.copyProperties(archive, appointment);
            appointment.setId(null); // 重新生成ID
            return appointment;
        }).collect(Collectors.toList());

        // 保存到主表
        boolean saveSuccess = appointmentService.saveBatch(appointments);

        if (!saveSuccess) {
            return ResponseEntity.internalServerError().body("恢复失败");
        }

        // 删除归档记录
        appointmentArchiveService.removeByIds(archiveIds);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "成功恢复 " + appointments.size() + " 条预约记录");
        result.put("count", appointments.size());
        return ResponseEntity.ok(result);
    }
}