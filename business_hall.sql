/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : business_hall

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 05/05/2026 21:06:23
*/

CREATE DATABASE IF NOT EXISTS `business_hall` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `business_hall`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `appointment_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预约编号: AP+时间戳',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `time_slot_json` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时间段JSON',
  `appointment_time_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预约时间段显示: 09:00-12:00',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型',
  `customer_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户姓名',
  `customer_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户电话',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待办理' COMMENT '状态: 待办理/已签到/办理中/已完成/已取消/爽约',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `checkin_time` datetime NULL DEFAULT NULL COMMENT '签到时间',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `appointment_no`(`appointment_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_appointment_date`(`appointment_date` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES (1, 'AP202603280001', 3, '2026-04-20', '{\"startTime\":\"09:00\",\"endTime\":\"12:00\"}', '09:00-12:00', '开户', '夏', '15926718512', NULL, '已取消', '2026-04-18 20:01:49', '2026-04-29 22:23:54', NULL, NULL, '2026-04-29 22:23:54');
INSERT INTO `appointment` VALUES (2, 'AP1777472650035', 3, '2026-04-30', '{\"startTime\":\"09:00\",\"endTime\":\"12:00\"}', '09:00-12:00', '开户', '张三', '16782637652', '', '已完成', '2026-04-29 22:24:10', '2026-04-30 19:27:42', '2026-04-30 02:15:39', '2026-04-30 19:27:42', '2026-04-29 22:49:45');
INSERT INTO `appointment` VALUES (3, 'AP1777474204532', 3, '2026-04-29', '{\"startTime\":\"09:00\",\"endTime\":\"12:00\"}', '09:00-12:00', '开户', '李四', '17656537676', '', '待办理', '2026-04-30 22:50:05', '2026-04-30 02:14:20', '2026-04-29 23:02:49', '2026-04-29 23:02:51', NULL);

-- ----------------------------
-- Table structure for appointment_resource
-- ----------------------------
DROP TABLE IF EXISTS `appointment_resource`;
CREATE TABLE `appointment_resource`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `day_of_week` int NOT NULL COMMENT '星期几: 1=周一 2=周二 3=周三 4=周四 5=周五 6=周六 7=周日',
  `day_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '星期名称',
  `is_workday` int NULL DEFAULT 1 COMMENT '是否工作日: 1工作日 0休息日',
  `time_slots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '时间段配置JSON',
  `available_business_types` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可办理业务类型JSON',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注描述',
  `status` int NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_day_of_week`(`day_of_week` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '可预约资源配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appointment_resource
-- ----------------------------
INSERT INTO `appointment_resource` VALUES (1, 1, '周一', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":20},{\"startTime\":\"14:00\",\"endTime\":\"18:00\",\"maxQuota\":20}]', '[\"开户\",\"销户\",\"缴费\",\"咨询\",\"变更\"]', '', 1, '2026-04-18 20:01:49', '2026-04-30 01:02:51');
INSERT INTO `appointment_resource` VALUES (2, 2, '周二', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":20},{\"startTime\":\"14:00\",\"endTime\":\"18:00\",\"maxQuota\":20}]', '[\"开户\",\"销户\",\"缴费\",\"咨询\",\"变更\"]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `appointment_resource` VALUES (3, 3, '周三', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":20},{\"startTime\":\"14:00\",\"endTime\":\"18:00\",\"maxQuota\":20}]', '[\"开户\",\"销户\",\"缴费\",\"咨询\",\"变更\"]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `appointment_resource` VALUES (4, 4, '周四', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":20},{\"startTime\":\"14:00\",\"endTime\":\"18:00\",\"maxQuota\":20}]', '[\"开户\",\"销户\",\"缴费\",\"咨询\",\"变更\"]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `appointment_resource` VALUES (5, 5, '周五', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":20},{\"startTime\":\"14:00\",\"endTime\":\"18:00\",\"maxQuota\":20}]', '[\"开户\",\"销户\",\"缴费\",\"咨询\",\"变更\"]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `appointment_resource` VALUES (6, 6, '周六', 1, '[{\"startTime\":\"09:00\",\"endTime\":\"12:00\",\"maxQuota\":10}]', '[\"缴费\",\"咨询\"]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `appointment_resource` VALUES (7, 7, '周日', 0, '[]', '[]', NULL, 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');

-- ----------------------------
-- Table structure for attendance
-- ----------------------------
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE `attendance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `user_id` bigint NOT NULL COMMENT '营业员ID',
  `date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime NULL DEFAULT NULL COMMENT '上班打卡时间',
  `check_out_time` datetime NULL DEFAULT NULL COMMENT '下班打卡时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '正常' COMMENT '状态: 正常/迟到/早退/缺勤/请假',
  `check_in_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上班打卡备注',
  `check_out_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '下班打卡备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `date` ASC) USING BTREE,
  INDEX `idx_date`(`date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance
-- ----------------------------
INSERT INTO `attendance` VALUES (2, 2, '2026-04-30', '2026-04-30 02:16:55', '2026-04-30 17:28:38', '早退', NULL, NULL, '2026-04-30 02:16:55', '2026-04-30 02:16:55');

-- ----------------------------
-- Table structure for attendance_config
-- ----------------------------
DROP TABLE IF EXISTS `attendance_config`;
CREATE TABLE `attendance_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `work_start_time` time NULL DEFAULT '09:00:00' COMMENT '上班时间',
  `work_end_time` time NULL DEFAULT '18:00:00' COMMENT '下班时间',
  `late_threshold` int NULL DEFAULT 15 COMMENT '迟到阈值(分钟)',
  `early_leave_threshold` int NULL DEFAULT 15 COMMENT '早退阈值(分钟)',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_config
-- ----------------------------
INSERT INTO `attendance_config` VALUES (1, '默认配置', '09:00:00', '18:00:00', 15, 15, 1);

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送者类型: user/staff/system',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者姓名',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `message_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'text' COMMENT '消息类型: text/image/file',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  CONSTRAINT `chat_message_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (1, 3, 'user', 3, '', 'nihk', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (2, 5, 'user', 3, '', 'nihk', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (3, 6, 'user', 3, '', 'aaa', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (4, 1, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (5, 2, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (6, 12, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (7, 1, 'staff', 2, 'clerk', '请稍等，我为您查询一下...', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (8, 1, 'staff', 2, 'clerk', 'nihk', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (9, 14, 'system', 0, '系统', '会话已结束', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (10, 15, 'user', 3, '', 'nihk', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (11, 17, 'user', 3, '', 'dajijdiowa', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (12, 17, 'user', 3, '', 'nihk', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (13, 16, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (14, 18, 'staff', 2, 'clerk', '非常抱歉给您带来不便，我们会尽快处理。', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (15, 21, 'user', 3, '', '你好', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (16, 22, 'user', 3, '', '你好', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (17, 18, 'staff', 2, 'clerk', '您好，请问有什么可以帮助您的？', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (18, 18, 'staff', 2, 'clerk', '你好', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (19, 18, 'staff', 2, 'clerk', '您好，请问有什么可以帮助您的？', 'text', 0, NULL);
INSERT INTO `chat_message` VALUES (20, 18, 'staff', 2, 'clerk', '您好，请问有什么可以帮助您的？', 'text', 0, '2026-04-19 04:15:41');
INSERT INTO `chat_message` VALUES (21, 18, 'staff', 2, 'clerk', '请稍等，我为您查询一下...', 'text', 0, '2026-04-19 04:36:04');
INSERT INTO `chat_message` VALUES (22, 18, 'staff', 2, 'clerk', '非常抱歉给您带来不便，我们会尽快处理。', 'text', 0, '2026-04-19 04:36:29');
INSERT INTO `chat_message` VALUES (23, 27, 'user', 3, '', 'nihk', 'text', 0, '2026-04-19 04:36:42');
INSERT INTO `chat_message` VALUES (24, 18, 'system', 0, '系统', '会话已结束', 'text', 0, '2026-04-19 04:37:01');
INSERT INTO `chat_message` VALUES (25, 29, 'user', 3, '', 'nihk', 'text', 0, '2026-04-19 04:38:05');
INSERT INTO `chat_message` VALUES (26, 28, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 04:39:21');
INSERT INTO `chat_message` VALUES (27, 41, 'user', 3, '', 'nihk', 'text', 1, '2026-04-19 05:21:24');
INSERT INTO `chat_message` VALUES (28, 32, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 05:21:28');
INSERT INTO `chat_message` VALUES (29, 33, 'system', 0, '系统', '客服 clerk1 已接入', 'text', 0, '2026-04-19 05:21:28');
INSERT INTO `chat_message` VALUES (30, 41, 'clerk', 2, 'clerk', '您好，请问有什么可以帮助您的？', 'text', 0, '2026-04-19 05:21:34');
INSERT INTO `chat_message` VALUES (31, 34, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 05:21:51');
INSERT INTO `chat_message` VALUES (32, 35, 'system', 0, '系统', '客服 clerk1 已接入', 'text', 0, '2026-04-19 05:21:51');
INSERT INTO `chat_message` VALUES (33, 41, 'clerk', 2, 'clerk', '感谢您的咨询，如有其他问题随时联系我们！', 'text', 0, '2026-04-19 05:26:32');
INSERT INTO `chat_message` VALUES (34, 33, 'user', 3, '', 'nihk', 'text', 0, '2026-04-19 05:26:45');
INSERT INTO `chat_message` VALUES (35, 44, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 06:52:51');
INSERT INTO `chat_message` VALUES (36, 45, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 06:54:03');
INSERT INTO `chat_message` VALUES (37, 45, 'system', 0, '系统', '会话已结束', 'text', 0, '2026-04-19 06:54:17');
INSERT INTO `chat_message` VALUES (38, 46, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-19 06:54:23');
INSERT INTO `chat_message` VALUES (39, 55, 'user', 3, '', 'nihk', 'text', 0, '2026-04-19 07:01:38');
INSERT INTO `chat_message` VALUES (40, 55, 'clerk', 2, 'clerk', 'wobuvidk', 'text', 0, '2026-04-19 07:01:45');
INSERT INTO `chat_message` VALUES (41, 55, 'user', 3, '', 'okok', 'text', 0, '2026-04-19 07:01:49');
INSERT INTO `chat_message` VALUES (42, 55, 'clerk', 2, 'clerk', '感谢您的咨询，如有其他问题随时联系我们！', 'text', 0, '2026-04-19 07:02:13');
INSERT INTO `chat_message` VALUES (43, 56, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-29 20:48:43');
INSERT INTO `chat_message` VALUES (44, 56, 'clerk', 2, 'clerk', '您好，请问有什么可以帮助您的？', 'text', 0, '2026-04-30 17:28:00');
INSERT INTO `chat_message` VALUES (45, 57, 'system', 0, '系统', '客服 clerk 已接入', 'text', 0, '2026-04-30 19:27:24');

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户姓名',
  `staff_id` bigint NULL DEFAULT NULL COMMENT '分配的客服ID',
  `staff_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服姓名',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'waiting' COMMENT '状态: waiting等待中/chatting进行中/closed已关闭',
  `unread_user` int NULL DEFAULT 0 COMMENT '用户未读消息数',
  `unread_staff` int NULL DEFAULT 0 COMMENT '客服未读消息数',
  `start_time` datetime NULL DEFAULT NULL COMMENT '会话开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '会话结束时间',
  `last_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最后一条消息',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后消息时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `session_no`(`session_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_staff_id`(`staff_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_session
-- ----------------------------
INSERT INTO `chat_session` VALUES (1, 'LX1776513763693', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:02:44', '2026-04-18 21:00:48', 'nihk', '2026-04-18 21:01:30', NULL, NULL);
INSERT INTO `chat_session` VALUES (2, 'LX1776513960753', 2, 'clerk', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:06:01', '2026-04-18 21:00:49', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (3, 'LX1776514976406', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:22:56', '2026-04-18 20:23:05', 'nihk', '2026-04-18 20:23:05', NULL, NULL);
INSERT INTO `chat_session` VALUES (4, 'LX1776515105998', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:25:06', '2026-04-18 20:25:26', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (5, 'LX1776515126095', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:25:26', '2026-04-18 20:25:35', 'nihk', '2026-04-18 20:25:35', NULL, NULL);
INSERT INTO `chat_session` VALUES (6, 'LX1776515156616', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:25:57', '2026-04-18 20:26:06', 'aaa', '2026-04-18 20:26:06', NULL, NULL);
INSERT INTO `chat_session` VALUES (7, 'LX1776515357796', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:29:18', '2026-04-18 20:29:25', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (8, 'LX1776515364638', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:29:25', '2026-04-18 20:29:45', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (9, 'LX1776515529693', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:32:10', '2026-04-18 20:32:13', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (10, 'LX1776515566022', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:32:46', '2026-04-18 20:33:05', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (11, 'LX1776515591148', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:33:11', '2026-04-18 20:34:02', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (12, 'LX1776515630682', 1, 'admin', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:33:51', '2026-04-18 21:00:51', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (13, 'LX1776515630706', 1, 'admin', 2, 'clerk', 'closed', 0, 0, '2026-04-18 20:33:51', '2026-04-18 20:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (14, 'LX1776517233736', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 21:00:34', '2026-04-18 21:01:42', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (15, 'LX1776517261015', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:01:01', '2026-04-18 21:01:55', 'nihk', '2026-04-18 21:01:48', NULL, NULL);
INSERT INTO `chat_session` VALUES (16, 'LX1776517316522', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 21:01:57', '2026-04-18 21:03:19', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (17, 'LX1776517316544', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:01:57', '2026-04-18 21:02:48', 'nihk', '2026-04-18 21:02:40', NULL, NULL);
INSERT INTO `chat_session` VALUES (18, 'LX1776517380795', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-18 21:03:01', '2026-04-19 04:37:01', '非常抱歉给您带来不便，我们会尽快处理。', '2026-04-19 04:36:29', NULL, NULL);
INSERT INTO `chat_session` VALUES (19, 'LX1776517476091', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:04:36', '2026-04-18 21:04:41', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (20, 'LX1776517481225', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:04:41', '2026-04-18 21:04:53', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (21, 'LX1776517493353', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:04:53', '2026-04-18 21:07:56', '你好', '2026-04-18 21:05:27', NULL, NULL);
INSERT INTO `chat_session` VALUES (22, 'LX1776517676590', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:07:57', '2026-04-18 21:08:44', '你好', '2026-04-18 21:08:10', NULL, NULL);
INSERT INTO `chat_session` VALUES (23, 'LX1776517726464', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:08:46', '2026-04-18 21:10:40', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (24, 'LX1776517840344', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:10:40', '2026-04-18 21:11:14', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (25, 'LX1776517874298', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-18 21:11:14', '2026-04-18 22:57:04', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (26, 'LX1776544536187', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 04:35:36', '2026-04-19 04:35:58', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (27, 'LX1776544573815', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 04:36:14', '2026-04-19 04:37:11', 'nihk', '2026-04-19 04:36:42', NULL, NULL);
INSERT INTO `chat_session` VALUES (28, 'LX1776544678312', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 04:37:58', '2026-04-19 04:39:25', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (29, 'LX1776544678342', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 04:37:58', '2026-04-19 04:38:38', 'nihk', '2026-04-19 04:38:05', NULL, NULL);
INSERT INTO `chat_session` VALUES (30, 'LX1776544717681', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 04:38:38', '2026-04-19 05:26:24', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (31, 'LX1776545229121', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 04:47:09', '2026-04-19 04:47:40', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (32, 'LX1776546670835', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:11:11', '2026-04-19 05:21:51', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (33, 'LX1776546887574', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 05:14:48', '2026-04-19 05:28:15', 'nihk', '2026-04-19 05:26:45', NULL, NULL);
INSERT INTO `chat_session` VALUES (34, 'LX1776547119407', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:18:39', '2026-04-19 05:21:52', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (35, 'LX1776547155907', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 05:19:16', '2026-04-19 05:25:19', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (36, 'LX1776547168906', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:19:29', '2026-04-19 05:20:02', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (37, 'LX1776547205906', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:20:06', '2026-04-19 05:20:11', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (38, 'LX1776547214905', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:20:15', '2026-04-19 05:20:20', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (39, 'LX1776547223904', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:20:24', '2026-04-19 05:20:29', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (40, 'LX1776547231906', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:20:32', '2026-04-19 05:20:37', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (41, 'LX1776547239905', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:20:40', '2026-04-19 05:28:59', '感谢您的咨询，如有其他问题随时联系我们！', '2026-04-19 05:26:33', NULL, NULL);
INSERT INTO `chat_session` VALUES (42, 'LX1776547741904', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:29:02', '2026-04-19 05:29:13', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (43, 'LX1776547763903', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:29:24', '2026-04-19 05:30:14', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (44, 'LX1776547814561', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 05:30:15', '2026-04-19 06:53:07', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (45, 'LX1776552838636', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:53:59', '2026-04-19 06:54:17', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (46, 'LX1776552858030', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:54:18', '2026-04-19 06:54:28', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (47, 'LX1776553107562', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 06:58:28', '2026-04-19 06:58:37', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (48, 'LX1776553121571', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 06:58:42', '2026-04-19 06:58:58', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (49, 'LX1776553142568', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:59:03', '2026-04-19 06:59:14', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (50, 'LX1776553159496', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:59:19', '2026-04-19 06:59:22', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (51, 'LX1776553172406', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:59:32', '2026-04-19 06:59:35', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (52, 'LX1776553180222', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 06:59:40', '2026-04-19 06:59:56', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (53, 'LX1776553201170', 3, 'xzg', 4, 'clerk1', 'closed', 0, 0, '2026-04-19 07:00:01', '2026-04-19 07:00:12', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (54, 'LX1776553217109', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 07:00:17', '2026-04-19 07:00:32', NULL, NULL, NULL, NULL);
INSERT INTO `chat_session` VALUES (55, 'LX1776553237137', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-19 07:00:37', '2026-04-19 07:02:28', '感谢您的咨询，如有其他问题随时联系我们！', '2026-04-19 07:02:13', NULL, NULL);
INSERT INTO `chat_session` VALUES (56, 'LX1777460115370', 3, 'xzg', 2, 'clerk', 'closed', 0, 0, '2026-04-29 18:55:15', '2026-04-30 19:02:57', '您好，请问有什么可以帮助您的？', '2026-04-30 17:28:00', NULL, NULL);
INSERT INTO `chat_session` VALUES (57, 'LX1777546978160', 3, 'xzg', 2, 'clerk', 'chatting', 0, 0, '2026-04-30 19:02:58', NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for complaint
-- ----------------------------
DROP TABLE IF EXISTS `complaint`;
CREATE TABLE `complaint`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '投诉ID',
  `complaint_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '投诉编号',
  `user_id` bigint NOT NULL COMMENT '投诉人ID',
  `complained_user_id` bigint NULL DEFAULT NULL COMMENT '被投诉营业员ID',
  `appointment_id` bigint NULL DEFAULT NULL COMMENT '关联预约ID',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '投诉原因',
  `evidence` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '证据图片URL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待处理' COMMENT '状态: 待处理/处理中/已处理/已反馈/已驳回',
  `handle_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理结果描述',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `handler_staff_id` bigint NULL DEFAULT NULL COMMENT '处理营业员ID',
  `user_rating` int NULL DEFAULT NULL COMMENT '用户评价1-5星',
  `evaluation_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户评价内容',
  `evaluated` tinyint NULL DEFAULT 0 COMMENT '是否已评价',
  `evaluation_time` datetime NULL DEFAULT NULL COMMENT '评价时间',
  `final_decision` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最终决策',
  `decision_time` datetime NULL DEFAULT NULL COMMENT '决策时间',
  `decision_by` bigint NULL DEFAULT NULL COMMENT '决策人ID',
  `feedback_to_user` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '反馈给用户的内容',
  `feedback_time` datetime NULL DEFAULT NULL COMMENT '反馈时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `complaint_no`(`complaint_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_complained_user_id`(`complained_user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '投诉记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of complaint
-- ----------------------------
INSERT INTO `complaint` VALUES (1, 'TS1777463540387', 3, 2, 1, 'aaaaaaaaaa', '', '已反馈', 'ddd', '2026-04-29 21:19:07', '', NULL, NULL, NULL, 0, NULL, '重新处理', '2026-04-29 21:54:14', 1, '已联系客服重新处理，请等待', '2026-04-29 21:54:14', '2026-04-29 19:52:20', '2026-04-29 21:54:14');
INSERT INTO `complaint` VALUES (2, 'TS1777481258966', 3, NULL, NULL, 'aaaaaaaaaa', '', '已处理', 'a', '2026-04-30 01:49:25', '', NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-30 00:47:39', '2026-04-30 01:49:25');

-- ----------------------------
-- Table structure for consultation
-- ----------------------------
DROP TABLE IF EXISTS `consultation`;
CREATE TABLE `consultation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询ID',
  `consultation_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '咨询编号',
  `user_id` bigint NOT NULL COMMENT '提问用户ID',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID(营业员)',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '咨询标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '咨询内容',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待回复' COMMENT '状态: 待回复/处理中/已回复',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `consultation_no`(`consultation_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_handler_id`(`handler_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '咨询记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consultation
-- ----------------------------

-- ----------------------------
-- Table structure for leave_record
-- ----------------------------
DROP TABLE IF EXISTS `leave_record`;
CREATE TABLE `leave_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `leave_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请假类型: 事假/病假/年假/其他',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `total_days` decimal(4, 1) NULL DEFAULT NULL COMMENT '请假天数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请假原因',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审批' COMMENT '审批状态',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `approve_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请假记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of leave_record
-- ----------------------------
INSERT INTO `leave_record` VALUES (1, 2, '事假', '2026-04-30', '2026-05-01', NULL, 'a', '待审批', NULL, NULL, NULL, '2026-04-30 01:31:01', '2026-04-30 01:31:01');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '接收用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通知内容',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类型: system/consultation/complaint/attendance',
  `related_id` bigint NULL DEFAULT NULL COMMENT '关联ID(咨询/投诉等)',
  `is_read` int NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 2, '新投诉', '用户提交了新投诉，请及时处理', 'complaint', 1, 0, '2026-04-29 19:52:20');
INSERT INTO `notification` VALUES (2, 4, '新投诉', '用户提交了新投诉，请及时处理', 'complaint', 1, 0, '2026-04-29 19:52:20');
INSERT INTO `notification` VALUES (3, 1, '新投诉', '用户提交了新投诉，需要最终审核', 'complaint', 1, 1, '2026-04-29 19:52:20');
INSERT INTO `notification` VALUES (4, 1, '投诉待审核', '投诉TS1777463540387已处理，请审核', 'complaint', 1, 1, '2026-04-29 21:19:07');
INSERT INTO `notification` VALUES (5, 3, '投诉已处理', '您提交的投诉TS1777463540387已有处理结果，请查看', 'complaint', 1, 1, '2026-04-29 21:54:14');
INSERT INTO `notification` VALUES (6, 2, '新预约创建', '客户 张三 创建了新预约', 'system', 2, 0, '2026-04-29 22:24:10');
INSERT INTO `notification` VALUES (7, 4, '新预约创建', '客户 张三 创建了新预约', 'system', 2, 0, '2026-04-29 22:24:10');
INSERT INTO `notification` VALUES (8, 1, '新预约创建', '客户 张三 创建了新预约', 'system', 2, 1, '2026-04-29 22:24:10');
INSERT INTO `notification` VALUES (9, 2, '新预约创建', '客户 李四 创建了新预约', 'system', 3, 0, '2026-04-29 22:50:05');
INSERT INTO `notification` VALUES (10, 4, '新预约创建', '客户 李四 创建了新预约', 'system', 3, 0, '2026-04-29 22:50:05');
INSERT INTO `notification` VALUES (11, 1, '新预约创建', '客户 李四 创建了新预约', 'system', 3, 1, '2026-04-29 22:50:05');
INSERT INTO `notification` VALUES (12, 2, '预约状态更新', '客户 李四 的预约状态已更新为 已签到', 'system', 3, 0, '2026-04-29 23:02:49');
INSERT INTO `notification` VALUES (13, 4, '预约状态更新', '客户 李四 的预约状态已更新为 已签到', 'system', 3, 0, '2026-04-29 23:02:49');
INSERT INTO `notification` VALUES (14, 1, '预约状态更新', '客户 李四 的预约状态已更新为 已签到', 'system', 3, 1, '2026-04-29 23:02:49');
INSERT INTO `notification` VALUES (15, 2, '预约状态更新', '客户 李四 的预约状态已更新为 办理中', 'system', 3, 0, '2026-04-29 23:02:50');
INSERT INTO `notification` VALUES (16, 4, '预约状态更新', '客户 李四 的预约状态已更新为 办理中', 'system', 3, 0, '2026-04-29 23:02:50');
INSERT INTO `notification` VALUES (17, 1, '预约状态更新', '客户 李四 的预约状态已更新为 办理中', 'system', 3, 1, '2026-04-29 23:02:50');
INSERT INTO `notification` VALUES (18, 2, '预约状态更新', '客户 李四 的预约状态已更新为 已完成', 'system', 3, 0, '2026-04-29 23:02:51');
INSERT INTO `notification` VALUES (19, 4, '预约状态更新', '客户 李四 的预约状态已更新为 已完成', 'system', 3, 0, '2026-04-29 23:02:51');
INSERT INTO `notification` VALUES (20, 1, '预约状态更新', '客户 李四 的预约状态已更新为 已完成', 'system', 3, 1, '2026-04-29 23:02:51');
INSERT INTO `notification` VALUES (21, 2, '新投诉', '用户提交了新投诉，请及时处理', 'complaint', 2, 0, '2026-04-30 00:47:39');
INSERT INTO `notification` VALUES (22, 4, '新投诉', '用户提交了新投诉，请及时处理', 'complaint', 2, 0, '2026-04-30 00:47:39');
INSERT INTO `notification` VALUES (23, 1, '新投诉', '用户提交了新投诉，需要最终审核', 'complaint', 2, 1, '2026-04-30 00:47:39');
INSERT INTO `notification` VALUES (24, 1, '投诉待审核', '投诉TS1777481258966已处理，请审核', 'complaint', 2, 1, '2026-04-30 01:49:25');
INSERT INTO `notification` VALUES (25, 2, '预约状态更新', '客户 张三 的预约状态已更新为 已签到', 'system', 2, 0, '2026-04-30 02:15:39');
INSERT INTO `notification` VALUES (26, 4, '预约状态更新', '客户 张三 的预约状态已更新为 已签到', 'system', 2, 0, '2026-04-30 02:15:39');
INSERT INTO `notification` VALUES (27, 1, '预约状态更新', '客户 张三 的预约状态已更新为 已签到', 'system', 2, 1, '2026-04-30 02:15:39');
INSERT INTO `notification` VALUES (28, 2, '预约状态更新', '客户 张三 的预约状态已更新为 办理中', 'system', 2, 0, '2026-04-30 02:15:44');
INSERT INTO `notification` VALUES (29, 4, '预约状态更新', '客户 张三 的预约状态已更新为 办理中', 'system', 2, 0, '2026-04-30 02:15:44');
INSERT INTO `notification` VALUES (30, 1, '预约状态更新', '客户 张三 的预约状态已更新为 办理中', 'system', 2, 1, '2026-04-30 02:15:44');
INSERT INTO `notification` VALUES (31, 2, '预约状态更新', '客户 张三 的预约状态已更新为 已完成', 'system', 2, 0, '2026-04-30 19:27:42');
INSERT INTO `notification` VALUES (32, 4, '预约状态更新', '客户 张三 的预约状态已更新为 已完成', 'system', 2, 0, '2026-04-30 19:27:42');
INSERT INTO `notification` VALUES (33, 1, '预约状态更新', '客户 张三 的预约状态已更新为 已完成', 'system', 2, 0, '2026-04-30 19:27:42');

-- ----------------------------
-- Table structure for quick_reply
-- ----------------------------
DROP TABLE IF EXISTS `quick_reply`;
CREATE TABLE `quick_reply`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回复内容',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类: 问候/业务/致歉/结束',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '快捷回复模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of quick_reply
-- ----------------------------
INSERT INTO `quick_reply` VALUES (1, '您好', '您好，请问有什么可以帮助您的？', '问候', 1, 1, '2026-04-18 20:01:49');
INSERT INTO `quick_reply` VALUES (2, '感谢咨询', '感谢您的咨询，如有其他问题随时联系我们！', '结束', 2, 1, '2026-04-18 20:01:49');
INSERT INTO `quick_reply` VALUES (3, '请稍等', '请稍等，我为您查询一下...', '业务', 3, 1, '2026-04-18 20:01:49');
INSERT INTO `quick_reply` VALUES (4, '抱歉', '非常抱歉给您带来不便，我们会尽快处理。', '致歉', 4, 1, '2026-04-18 20:01:49');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'user' COMMENT '角色: admin/clerk/user',
  `status` int NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  CONSTRAINT `role` CHECK (`role` in (_utf8mb4'admin',_utf8mb4'clerk',_utf8mb4'user'))
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$.SIB7gVZ3PTvT63woLKJYObS/etHIwgalUkC5wyFmV.zhUKcAMW8.', '系统管理员', '13800138000', NULL, 'admin', 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `user` VALUES (2, 'clerk', '$2a$10$5MdrOyJJh75DPR/gNZ83p.GMVAXDktXpTtEuK/afWD1lQZrBKjjJu', '营业员1', '13900139000', NULL, 'clerk', 1, '2026-04-18 20:01:49', '2026-04-19 05:03:11');
INSERT INTO `user` VALUES (3, 'xzg', '$2a$10$VhR1RfDbcNANySm.DCm93OfwtdsNXc5sJtOd5Q3SXP4ZX.hKs5mje', '夏', '123', NULL, 'user', 1, '2026-04-18 20:01:49', '2026-04-18 20:01:49');
INSERT INTO `user` VALUES (4, 'clerk1', '$2a$10$htpEgGupdqXcB/HUhP717Og9SoWcBgMkrUbJyOb8nYjBodG46YkKm', '李四', '15846798652', NULL, 'clerk', 1, '2026-04-18 20:01:49', '2026-04-19 05:03:17');

SET FOREIGN_KEY_CHECKS = 1;
