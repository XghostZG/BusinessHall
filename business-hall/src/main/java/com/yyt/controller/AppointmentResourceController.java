package com.yyt.controller;

import com.yyt.entity.AppointmentResource;
import com.yyt.service.AppointmentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource")
@CrossOrigin
public class AppointmentResourceController {

    @Autowired
    private AppointmentResourceService resourceService;

    /**
     * 获取所有预约资源配置
     */
    @GetMapping("/list")
    public Map<String, Object> getAllResources() {
        Map<String, Object> result = new HashMap<>();
        List<AppointmentResource> list = resourceService.getAllResources();
        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    /**
     * 获取工作日配置
     */
    @GetMapping("/workday")
    public Map<String, Object> getWorkdayResources() {
        Map<String, Object> result = new HashMap<>();
        List<AppointmentResource> list = resourceService.getWorkdayResources();
        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    /**
     * 根据星期获取配置
     */
    @GetMapping("/day/{dayOfWeek}")
    public Map<String, Object> getResourceByDay(@PathVariable Integer dayOfWeek) {
        Map<String, Object> result = new HashMap<>();
        AppointmentResource resource = resourceService.getResourceByDayOfWeek(dayOfWeek);
        result.put("code", 200);
        result.put("data", resource);
        return result;
    }

    /**
     * 更新资源配置
     */
    @PutMapping("/update")
    public Map<String, Object> updateResource(@RequestBody AppointmentResource resource) {
        Map<String, Object> result = new HashMap<>();
        boolean success = resourceService.updateResource(resource);
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }

    /**
     * 获取指定日期的可预约信息
     */
    @GetMapping("/date/{date}")
    public Map<String, Object> getAvailableByDate(@PathVariable String date) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = resourceService.getAvailableByDate(date);
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    /**
     * 获取所有可办理业务类型
     */
    @GetMapping("/business-types")
    public Map<String, Object> getBusinessTypes() {
        Map<String, Object> result = new HashMap<>();
        List<String> types = resourceService.getAvailableBusinessTypes();
        result.put("code", 200);
        result.put("data", types);
        return result;
    }

    /**
     * 切换资源配置状态
     */
    @PutMapping("/toggle/{id}")
    public Map<String, Object> toggleStatus(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        AppointmentResource resource = resourceService.getById(id);
        if (resource != null) {
            resource.setStatus(resource.getStatus() == 1 ? 0 : 1);
            boolean success = resourceService.updateById(resource);
            result.put("code", success ? 200 : 500);
            result.put("message", success ? "状态切换成功" : "操作失败");
        } else {
            result.put("code", 404);
            result.put("message", "资源不存在");
        }
        return result;
    }
}
