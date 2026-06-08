package com.yyt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批申请表 - 统一管理请假和投诉等需要审批的申请
 */
@Data
@TableName("approval_application")
public class ApprovalApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;      // 申请编号: AP+时间戳
    private String applicationType;    // 申请类型: leave-请假, complaint-投诉

    private Long applicantId;          // 申请人ID
    @TableField(exist = false)
    private String applicantName;      // 申请人姓名(非数据库字段)

    private String title;              // 申请标题/摘要
    private String content;            // 申请内容描述
    private String extraData;          // 额外数据(JSON格式，存储类型特定的字段)

    // 状态: pending-待审批, approved-已通过, rejected-已拒绝
    private String status;

    private Long approverId;           // 审批人ID
    private LocalDateTime approveTime; // 审批时间
    private String approveResult;      // 审批结果
    private String approveRemark;      // 审批备注

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
