package com.yyt.controller;

import com.yyt.entity.ApprovalApplication;
import com.yyt.entity.User;
import com.yyt.mapper.UserMapper;
import com.yyt.service.ApprovalApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
@CrossOrigin
public class ApprovalController {

    @Autowired
    private ApprovalApplicationService approvalService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统一提交申请（请假/投诉等）
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody ApprovalApplication application) {
        Long applicantId = getCurrentUserId();
        if (applicantId == null) {
            return ResponseEntity.badRequest().body("无法获取用户信息");
        }

        application.setApplicantId(applicantId);
        boolean saved = approvalService.submitApplication(application);

        if (saved) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "申请已提交");
            result.put("data", application);
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body("提交失败");
        }
    }

    /**
     * 获取我的申请列表
     */
    @GetMapping("/my")
    public ResponseEntity<List<ApprovalApplication>> getMyList(
            @RequestParam(required = false) String type) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(approvalService.getMyApplications(userId, type));
    }

    /**
     * 获取审批申请列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(approvalService.getApplicationList(type, status));
    }

    /**
     * 获取待审批列表
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPending() {
        return ResponseEntity.ok(approvalService.getPendingList());
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(approvalService.getStatistics());
    }

    /**
     * 审批申请
     */
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestParam String result,
            @RequestParam(required = false) String remark) {
        Long approverId = getCurrentUserId();
        if (approverId == null) {
            return ResponseEntity.badRequest().body("无法获取用户信息");
        }
        return approvalService.approve(id, result, remark, approverId);
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        ApprovalApplication application = approvalService.getById(id);
        if (application == null) {
            return ResponseEntity.notFound().build();
        }

        // 获取申请人信息
        User applicant = userMapper.selectById(application.getApplicantId());
        if (applicant != null) {
            application.setApplicantName(applicant.getRealName());
        }

        return ResponseEntity.ok(application);
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
