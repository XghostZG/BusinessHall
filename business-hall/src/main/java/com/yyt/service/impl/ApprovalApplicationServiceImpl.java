package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yyt.entity.ApprovalApplication;
import com.yyt.entity.User;
import com.yyt.entity.Appointment;
import com.yyt.mapper.ApprovalApplicationMapper;
import com.yyt.service.ApprovalApplicationService;
import com.yyt.service.UserService;
import com.yyt.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApprovalApplicationServiceImpl extends ServiceImpl<ApprovalApplicationMapper, ApprovalApplication>
        implements ApprovalApplicationService {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public boolean submitApplication(ApprovalApplication application) {
        // 生成申请编号
        application.setApplicationNo("AP" + System.currentTimeMillis());

        // 设置初始状态
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());

        return save(application);
    }

    @Override
    public List<ApprovalApplication> getMyApplications(Long userId, String type) {
        LambdaQueryWrapper<ApprovalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalApplication::getApplicantId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ApprovalApplication::getApplicationType, type);
        }
        wrapper.orderByDesc(ApprovalApplication::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public ResponseEntity<?> approve(Long id, String result, String remark, Long approverId) {
        ApprovalApplication application = getById(id);
        if (application == null) {
            return ResponseEntity.badRequest().body("审批申请不存在");
        }

        if (!"pending".equals(application.getStatus())) {
            return ResponseEntity.badRequest().body("该申请已审批");
        }

        // 更新审批申请状态
        application.setStatus(result);
        application.setApproveResult(result.equals("approved") ? "已通过" : "已拒绝");
        application.setApproveRemark(remark);
        application.setApproverId(approverId);
        application.setApproveTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        updateById(application);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "审批完成");
        response.put("data", application);
        return ResponseEntity.ok(response);
    }

    @Override
    public List<Map<String, Object>> getApplicationList(String type, String status) {
        LambdaQueryWrapper<ApprovalApplication> wrapper = new LambdaQueryWrapper<>();
        
        // 动态添加查询条件
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ApprovalApplication::getApplicationType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ApprovalApplication::getStatus, status);
        }
        wrapper.orderByDesc(ApprovalApplication::getCreateTime);
        
        List<ApprovalApplication> applications = list(wrapper);
        
        // 转换为 Map 列表并补充申请人信息
        List<Long> userIds = applications.stream()
                .map(ApprovalApplication::getApplicantId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, User> userMap = userIds.isEmpty() ? new HashMap<>() : 
                userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        
        return applications.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("applicationNo", app.getApplicationNo());
            map.put("applicationType", app.getApplicationType());
            map.put("title", app.getTitle());
            map.put("content", app.getContent());
            map.put("status", app.getStatus());
            map.put("applicantId", app.getApplicantId());
            map.put("createTime", app.getCreateTime());
            map.put("updateTime", app.getUpdateTime());
            map.put("approveResult", app.getApproveResult());
            map.put("approveRemark", app.getApproveRemark());
            map.put("approveTime", app.getApproveTime());
            map.put("approverId", app.getApproverId());
            map.put("extraData", resolveExtraData(app.getExtraData()));
            
            User user = userMap.get(app.getApplicantId());
            if (user != null) {
                map.put("applicantName", user.getRealName());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPendingList() {
        return getApplicationList(null, "pending");
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 统计各状态数量
        long pending = count(new LambdaQueryWrapper<ApprovalApplication>()
                .eq(ApprovalApplication::getStatus, "pending"));
        long approved = count(new LambdaQueryWrapper<ApprovalApplication>()
                .eq(ApprovalApplication::getStatus, "approved"));
        long rejected = count(new LambdaQueryWrapper<ApprovalApplication>()
                .eq(ApprovalApplication::getStatus, "rejected"));
        long total = count();

        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("total", total);

        return stats;
    }

    /**
     * 解析extraData JSON，将ID替换为可读信息
     */
    private Map<String, Object> resolveExtraData(String extraDataJson) {
        if (extraDataJson == null || extraDataJson.isEmpty()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(extraDataJson, Map.class);
            Map<String, Object> resolved = new HashMap<>();

            // 解析关联预约
            Object appointmentId = data.get("appointmentId");
            if (appointmentId != null) {
                try {
                    Appointment apt = appointmentService.getById(Long.valueOf(appointmentId.toString()));
                    if (apt != null) {
                        resolved.put("关联预约", apt.getAppointmentNo() + " - " + apt.getBusinessType());
                    } else {
                        resolved.put("关联预约", "预约已删除");
                    }
                } catch (Exception e) {
                    resolved.put("关联预约", appointmentId.toString());
                }
            } else {
                resolved.put("关联预约", null);
            }

            // 解析被投诉营业员
            Object complainedUserId = data.get("complainedUserId");
            if (complainedUserId != null) {
                try {
                    User complainedUser = userService.getById(Long.valueOf(complainedUserId.toString()));
                    if (complainedUser != null) {
                        resolved.put("被投诉营业员", complainedUser.getRealName());
                    } else {
                        resolved.put("被投诉营业员", "用户已删除");
                    }
                } catch (Exception e) {
                    resolved.put("被投诉营业员", complainedUserId.toString());
                }
            } else {
                resolved.put("被投诉营业员", null);
            }

            // 证据描述
            Object evidence = data.get("evidence");
            if (evidence != null && !evidence.toString().isEmpty()) {
                resolved.put("evidence", evidence.toString());
            } else {
                resolved.put("evidence", null);
            }

            return resolved;
        } catch (Exception e) {
            return null;
        }
    }
}
