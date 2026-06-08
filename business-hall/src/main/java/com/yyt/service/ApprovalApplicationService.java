package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.ApprovalApplication;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface ApprovalApplicationService extends IService<ApprovalApplication> {

    /**
     * 统一提交申请（请假/投诉等）
     * @param application 申请信息
     * @return 是否成功
     */
    boolean submitApplication(ApprovalApplication application);

    /**
     * 获取我的申请列表
     * @param userId 用户ID
     * @param type 申请类型(可选)
     * @return 申请列表
     */
    List<ApprovalApplication> getMyApplications(Long userId, String type);

    /**
     * 审批申请
     * @param id 审批申请ID
     * @param result 审批结果: approved/rejected
     * @param remark 审批备注
     * @param approverId 审批人ID
     * @return 审批结果
     */
    ResponseEntity<?> approve(Long id, String result, String remark, Long approverId);

    /**
     * 获取审批申请列表
     * @param type 申请类型(可选)
     * @param status 状态(可选)
     * @return 申请列表
     */
    List<Map<String, Object>> getApplicationList(String type, String status);

    /**
     * 获取待审批列表
     * @return 待审批列表
     */
    List<Map<String, Object>> getPendingList();

    /**
     * 获取统计数据
     * @return 统计数据
     */
    Map<String, Object> getStatistics();
}
