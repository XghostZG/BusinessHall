package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yyt.entity.AppointmentResource;
import com.yyt.mapper.AppointmentResourceMapper;
import com.yyt.service.AppointmentResourceService;
import com.yyt.service.AppointmentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;

@Service
public class AppointmentResourceServiceImpl extends ServiceImpl<AppointmentResourceMapper, AppointmentResource>
        implements AppointmentResourceService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Resource
    private AppointmentService appointmentService;

    @Override
    public List<AppointmentResource> getAllResources() {
        return list(new QueryWrapper<AppointmentResource>()
                .orderByAsc("day_of_week"));
    }

    @Override
    public List<AppointmentResource> getWorkdayResources() {
        return list(new QueryWrapper<AppointmentResource>()
                .eq("is_workday", 1)
                .eq("status", 1)
                .orderByAsc("day_of_week"));
    }

    @Override
    public AppointmentResource getResourceByDayOfWeek(Integer dayOfWeek) {
        return getOne(new QueryWrapper<AppointmentResource>()
                .eq("day_of_week", dayOfWeek));
    }

    @Override
    public boolean updateResource(AppointmentResource resource) {
        return updateById(resource);
    }

    @Override
    public Map<String, Object> getAvailableByDate(String dateStr) {
        Map<String, Object> result = new HashMap<>();
        LocalDate date = LocalDate.parse(dateStr);
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=周一, 7=周日

        AppointmentResource resource = getResourceByDayOfWeek(dayOfWeek);
        if (resource == null || resource.getIsWorkday() == 0) {
            result.put("available", false);
            result.put("message", "该日期不可预约");
            return result;
        }

        result.put("available", true);
        result.put("dayName", resource.getDayName());
        result.put("isWorkday", resource.getIsWorkday() == 1);

        // 解析时间段
        try {
            List<Map<String, Object>> timeSlots = objectMapper.readValue(
                    resource.getTimeSlots(),
                    new TypeReference<>() {}
            );
            
            // 获取已预约数量
            Map<String, Long> bookedCountMap = appointmentService.getBookedCountByDate(dateStr);
            
            // 为每个时段添加已预约数量
            for (Map<String, Object> slot : timeSlots) {
                String startTime = (String) slot.get("startTime");
                String endTime = (String) slot.get("endTime");
                String timeRange = startTime + "-" + endTime;
                Long bookedCount = bookedCountMap.getOrDefault(timeRange, 0L);
                slot.put("bookedCount", bookedCount);
            }
            
            result.put("timeSlots", timeSlots);
        } catch (Exception e) {
            result.put("timeSlots", new ArrayList<>());
        }

        // 解析可办理业务
        try {
            List<String> businessTypes = objectMapper.readValue(
                    resource.getAvailableBusinessTypes(),
                    new TypeReference<>() {}
            );
            result.put("availableBusinessTypes", businessTypes);
        } catch (Exception e) {
            result.put("availableBusinessTypes", new ArrayList<>());
        }

        return result;
    }

    @Override
    public List<String> getAvailableBusinessTypes() {
        List<AppointmentResource> resources = getWorkdayResources();
        Set<String> allTypes = new LinkedHashSet<>();
        for (AppointmentResource resource : resources) {
            try {
                List<String> types = objectMapper.readValue(
                        resource.getAvailableBusinessTypes(),
                        new TypeReference<>() {}
                );
                allTypes.addAll(types);
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(allTypes);
    }
}
