package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.ApprovalApplication;
import com.yyt.entity.Attendance;
import com.yyt.entity.User;
import com.yyt.mapper.ApprovalApplicationMapper;
import com.yyt.mapper.AttendanceMapper;
import com.yyt.service.AttendanceService;
import com.yyt.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;

@Service
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance>
        implements AttendanceService {

    @Autowired
    private UserService userService;

    @Autowired
    private ApprovalApplicationMapper approvalApplicationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 固定上下班时间
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);
    private static final int LATE_THRESHOLD = 0;       // 迟到阈值(分钟)
    private static final int EARLY_LEAVE_THRESHOLD = 0; // 早退阈值(分钟)

    @Override
    public Map<String, Object> checkIn(Long userId, String type, String remark) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        Attendance attendance = getOne(new QueryWrapper<Attendance>()
                .eq("user_id", userId)
                .eq("date", today));

        LocalDateTime now = LocalDateTime.now();

        if ("checkin".equals(type)) {
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setUserId(userId);
                attendance.setDate(today);
                attendance.setCheckInTime(now);
                attendance.setCheckInRemark(remark);

                LocalTime checkIn = now.toLocalTime();
                long lateMinutes = java.time.Duration.between(WORK_START_TIME, checkIn).toMinutes();

                if (lateMinutes > LATE_THRESHOLD) {
                    attendance.setStatus("迟到");
                } else {
                    attendance.setStatus("正常");
                }

                save(attendance);
                result.put("message", lateMinutes > LATE_THRESHOLD ? "打卡成功，已迟到" + lateMinutes + "分钟" : "打卡成功");
            } else {
                result.put("success", false);
                result.put("message", "今日已打过上班卡");
                return result;
            }
        } else {
            if (attendance == null) {
                result.put("success", false);
                result.put("message", "请先打上班卡");
                return result;
            }
            if (attendance.getCheckOutTime() != null) {
                result.put("success", false);
                result.put("message", "今日已打过下班卡");
                return result;
            }

            attendance.setCheckOutTime(now);
            attendance.setCheckOutRemark(remark);

            LocalTime checkOut = now.toLocalTime();
            long earlyMinutes = java.time.Duration.between(checkOut, WORK_END_TIME).toMinutes();

            if (earlyMinutes > EARLY_LEAVE_THRESHOLD && "正常".equals(attendance.getStatus())) {
                attendance.setStatus("早退");
            }

            updateById(attendance);
            result.put("message", "下班打卡成功");
        }

        result.put("success", true);
        result.put("data", attendance);
        return result;
    }

    @Override
    public List<Attendance> getMyAttendance(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return list(new QueryWrapper<Attendance>()
                .eq("user_id", userId)
                .ge("date", startDate)
                .le("date", endDate)
                .orderByAsc("date"));
    }

    @Override
    public Map<String, Object> getStats(Long userId, String month) {
        Map<String, Object> stats = new HashMap<>();
        List<Attendance> records = getMyAttendance(userId, month);

        long normal = records.stream().filter(a -> "正常".equals(a.getStatus())).count();
        long late = records.stream().filter(a -> "迟到".equals(a.getStatus())).count();
        long early = records.stream().filter(a -> "早退".equals(a.getStatus())).count();
        long absent = records.stream().filter(a -> "缺勤".equals(a.getStatus())).count();
        long leave = records.stream().filter(a -> "请假".equals(a.getStatus())).count();

        long checkedIn = records.stream()
                .filter(a -> a.getCheckInTime() != null)
                .count();

        stats.put("total", records.size());
        stats.put("normal", normal);
        stats.put("late", late);
        stats.put("early", early);
        stats.put("absent", absent);
        stats.put("leave", leave);
        stats.put("checkedIn", checkedIn);

        return stats;
    }

    @Override
    public Attendance getTodayAttendance(Long userId) {
        return getOne(new QueryWrapper<Attendance>()
                .eq("user_id", userId)
                .eq("date", LocalDate.now()));
    }

    @Override
    public List<Map<String, Object>> getAllAttendance(String month) {
        List<User> clerks = userService.list(new QueryWrapper<User>()
                .eq("role", "clerk"));

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        // 计算当月实际应到天数（工作日，到今天为止）
        int shouldAttendDays = 0;
        LocalDate effectiveEnd = endDate.isAfter(today) ? today : endDate;
        LocalDate countDate = startDate;
        while (!countDate.isAfter(effectiveEnd)) {
            if (countDate.getDayOfWeek().getValue() <= 5) {
                shouldAttendDays++;
            }
            countDate = countDate.plusDays(1);
        }

        List<Attendance> attendances = list(new QueryWrapper<Attendance>()
                .ge("date", startDate)
                .le("date", effectiveEnd));

        // 查询当月已批准的请假申请（从approval_application表）
        List<ApprovalApplication> approvedLeaves = approvalApplicationMapper.selectList(
                new QueryWrapper<ApprovalApplication>()
                        .eq("application_type", "leave")
                        .eq("status", "approved")
        );

        // 按用户分组请假日期
        Map<Long, Set<LocalDate>> leaveDatesMap = new HashMap<>();
        for (ApprovalApplication leave : approvedLeaves) {
            try {
                String extraData = leave.getExtraData();
                if (extraData != null && !extraData.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> data = objectMapper.readValue(extraData, Map.class);
                    String startStr = data.get("startDate");
                    String endStr = data.get("endDate");

                    if (startStr != null && endStr != null) {
                        LocalDate leaveStart = LocalDate.parse(startStr);
                        LocalDate leaveEnd = LocalDate.parse(endStr);

                        // 只处理当月范围内的请假
                        if (leaveEnd.isBefore(startDate) || leaveStart.isAfter(effectiveEnd)) {
                            continue;
                        }

                        // 裁剪到查询月份范围内
                        LocalDate effectiveStart = leaveStart.isBefore(startDate) ? startDate : leaveStart;
                        LocalDate effectiveEndLeave = leaveEnd.isAfter(effectiveEnd) ? effectiveEnd : leaveEnd;

                        Set<LocalDate> dates = leaveDatesMap.computeIfAbsent(leave.getApplicantId(), k -> new HashSet<>());
                        LocalDate current = effectiveStart;
                        while (!current.isAfter(effectiveEndLeave)) {
                            dates.add(current);
                            current = current.plusDays(1);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing leave extra data: " + leave.getExtraData(), e);
            }
        }

        // 统计当月所有工作日（用于计算缺勤）
        Set<LocalDate> allWorkDays = new HashSet<>();
        LocalDate wd = startDate;
        while (!wd.isAfter(effectiveEnd)) {
            if (wd.getDayOfWeek().getValue() <= 5) {
                allWorkDays.add(wd);
            }
            wd = wd.plusDays(1);
        }

        Map<Long, List<Attendance>> attendanceMap = attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (User clerk : clerks) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", clerk.getId());
            item.put("username", clerk.getUsername());
            item.put("realName", clerk.getRealName());
            item.put("month", month);

            List<Attendance> userRecords = attendanceMap.getOrDefault(clerk.getId(), new ArrayList<>());
            Set<LocalDate> leaveDates = leaveDatesMap.getOrDefault(clerk.getId(), new HashSet<>());

            // 统计各项数据
            int normalDays = 0;      // 出勤（正常）
            int lateDays = 0;         // 迟到
            int earlyDays = 0;        // 早退
            int leaveDays = 0;        // 请假
            int absentDays = 0;       // 缺勤

            // 统计已有打卡记录
            Set<LocalDate> checkedDates = new HashSet<>();
            for (Attendance att : userRecords) {
                checkedDates.add(att.getDate());
                String status = att.getStatus();
                if ("正常".equals(status)) {
                    normalDays++;
                } else if ("迟到".equals(status)) {
                    lateDays++;
                } else if ("早退".equals(status)) {
                    earlyDays++;
                }
            }

            // 统计请假天数（工作日且有请假）
            for (LocalDate date : leaveDates) {
                if (date.getDayOfWeek().getValue() <= 5 && !date.isAfter(effectiveEnd)) {
                    leaveDays++;
                }
            }

            // 计算缺勤（工作日未打卡且未请假）
            Set<LocalDate> checkedOrLeave = new HashSet<>(checkedDates);
            checkedOrLeave.addAll(leaveDates);
            for (LocalDate workDay : allWorkDays) {
                if (!checkedOrLeave.contains(workDay)) {
                    absentDays++;
                }
            }

            item.put("shouldAttend", shouldAttendDays);  // 应到
            item.put("normal", normalDays);               // 出勤
            item.put("late", lateDays);                   // 迟到
            item.put("early", earlyDays);                 // 早退
            item.put("leave", leaveDays);                 // 请假
            item.put("absent", absentDays);               // 缺勤

            result.add(item);
        }

        return result;
    }



}
