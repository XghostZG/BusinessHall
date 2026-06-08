# 业务大厅预约管理系统

基于 Spring Boot + Vue 3 构建的业务大厅预约管理系统，支持在线预约、实时客服、投诉处理等功能。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.15
- **语言**: Java 17
- **ORM**: MyBatis-Plus 3.5.6
- **数据库**: MySQL 8.0+
- **安全**: Spring Security + JWT
- **实时通信**: WebSocket (STOMP)

### 前端
- **框架**: Vue 3.3.4
- **构建工具**: Vite 4.4.5
- **UI组件**: Element Plus 2.3.12
- **状态管理**: Pinia 2.1.6
- **路由**: Vue Router 4.2.4
- **图表**: ECharts 5.4.3

## 项目结构

```
BusinessHall/
├── business-hall/                    # 后端Spring Boot项目
│   ├── src/main/java/com/yyt/
│   │   ├── controller/               # REST API控制器
│   │   ├── service/                  # 业务逻辑层
│   │   ├── mapper/                   # MyBatis映射接口
│   │   ├── entity/                   # 数据库实体
│   │   ├── config/                   # 配置类
│   │   └── websocket/                # WebSocket处理
│   ├── src/main/resources/
│   │   └── application.yml           # 应用配置
│   └── pom.xml                       # Maven依赖
├── business-hall-frontend/           # 前端Vue项目
│   ├── src/
│   │   ├── views/                    # 页面组件
│   │   ├── components/               # 公共组件
│   │   ├── api/                      # API封装
│   │   ├── store/                    # Pinia状态管理
│   │   ├── router/                   # 路由配置
│   │   └── utils/                    # 工具函数
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
└── business_hall.sql                 # 数据库初始化脚本
```

## 核心功能

### 用户角色

| 角色 | 描述 | 权限 |
|------|------|------|
| **admin** | 系统管理员 | 完整的系统管理权限 |
| **clerk** | 营业厅工作人员 | 处理预约、回复咨询、考勤打卡 |
| **user** | 普通用户 | 预约服务、发起咨询、提交投诉 |

### 功能模块

#### 1. 预约管理
- 在线预约业务服务
- 时段选择与预约确认
- 预约改签与取消
- 核验码签到
- 预约状态流转（待办理→已签到→办理中→已完成）

#### 2. 实时客服
- WebSocket实时消息推送
- 客服会话管理
- 快捷回复模板
- 消息状态追踪

#### 3. 投诉处理
- 投诉提交与证据上传
- 多级审批流程
- 用户评价反馈

#### 4. 咨询服务
- 在线咨询提交
- 客服回复处理

#### 5. 考勤管理
- 上下班打卡
- 考勤状态统计
- 请假申请与审批

#### 6. 系统管理
- 用户管理（增删改查）
- 时段资源配置
- 预约统计报表
- 数据归档管理

## 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| `user` | 用户表（管理员/营业员/普通用户） |
| `appointment` | 预约表 |
| `appointment_resource` | 可预约资源配置表 |
| `approval_application` | 审批申请表 |
| `chat_session` | 聊天会话表 |
| `chat_message` | 聊天消息表 |
| `complaint` | 投诉记录表 |
| `consultation` | 咨询记录表 |
| `attendance` | 考勤记录表 |
| `attendance_config` | 考勤配置表 |
| `leave_record` | 请假记录表 |
| `notification` | 通知表 |
| `quick_reply` | 快捷回复模板 |

> **说明**: `appointment_archive` 表在代码中定义但暂未在SQL文件中创建DDL，归档功能需要手动创建该表。

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Node.js 16+
- Maven 3.6+

### 步骤1：数据库初始化

```sql
-- 执行SQL脚本创建数据库和表结构
mysql -u root -p < business_hall.sql
```

### 步骤2：配置后端

修改 `business-hall/src/main/resources/application.yml`：

```yaml
spring:
datasource:
    url: jdbc:mysql://localhost:3306/business_hall?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 步骤3：启动后端服务

```bash
cd business-hall
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

### 步骤4：启动前端服务

```bash
cd business-hall-frontend
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动。

## 访问系统

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 营业员 | clerk | clerk123 |
| 普通用户 | xzg | xzg123 |

## API接口

### 预约接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/appointment/create` | POST | 创建预约 |
| `/api/appointment/cancel` | POST | 取消预约 |
| `/api/appointment/user` | GET | 获取用户预约列表 |
| `/api/appointment/today` | GET | 获取今日预约 |
| `/api/appointment/start` | POST | 核验签到开始办理 |
| `/api/appointment/complete` | POST | 完成办理 |

### 用户接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/login` | POST | 用户登录 |
| `/api/user/register` | POST | 用户注册 |
| `/api/user/update` | POST | 更新用户信息 |

### 聊天接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/session/create` | POST | 创建会话 |
| `/api/chat/message/list` | GET | 获取消息列表 |

### WebSocket端点
- `ws://localhost:8080/ws` - WebSocket连接端点

## 开发说明

### 后端开发

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包构建
mvn clean package
```

### 前端开发

```bash
# 开发模式
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

## 项目特点

- ✅ 前后端分离架构
- ✅ 角色权限控制
- ✅ 实时消息推送
- ✅ 预约状态流转
- ✅ 数据统计报表
- ✅ 响应式界面设计

## 许可证

MIT License