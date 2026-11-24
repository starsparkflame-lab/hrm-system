# 人力资源管理系统（HRM System）

基于 **Spring Boot + Vue** 的全栈人力资源管理系统，包含部门管理、员工管理、操作日志、数据统计等模块。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot、MyBatis-Plus、MySQL、Redis、HikariCP |
| 前端 | Vue 2、Element UI、ECharts、Axios |
| 构建 | Maven 3.8+、Vue CLI、Node.js |

---

## 目录结构

```
├── springboot/                  后端服务
│   ├── src/main/java/com/cow/   业务代码
│   │   ├── controller/          接口层
│   │   ├── service/             业务层
│   │   ├── mapper/              数据访问层
│   │   ├── entity/              实体类
│   │   └── aspect/              操作日志切面
│   └── src/main/resources/      配置文件
├── vue/                         前端工程
│   ├── src/                     页面与组件
│   │   ├── views/               页面
│   │   ├── components/          公共组件
│   │   └── api/                 接口封装
│   └── package.json             依赖清单
├── files/                       上传文件存储目录
├── 数据库/db_hrm.sql             数据库建表与初始数据脚本
└── .gitignore
```

---

## 快速启动

### 1. 准备数据库

```sql
CREATE DATABASE db_hrm DEFAULT CHARSET utf8mb4;
-- 然后导入 数据库/db_hrm.sql
```

### 2. 启动后端

```bash
cd springboot
```

数据库密码通过环境变量传入（不设置则默认 `123456`）：

```bash
# Windows PowerShell
$env:DB_PASSWORD="你的MySQL密码"

# Windows CMD
set DB_PASSWORD=你的MySQL密码

# Linux / macOS
export DB_PASSWORD=你的MySQL密码
```

启动：

```bash
mvn spring-boot:run
```

后端地址：<http://localhost:8888>

### 3. 启动前端

```bash
cd vue
npm install
npm run serve
```

前端地址：<http://localhost:8082>

> **Redis 说明**：Redis 未启动时项目仍可正常运行，缓存会自动降级为进程内缓存，不影响业务功能。

---

## 功能模块

- 用户登录与权限控制
- 部门管理
- 员工管理
- 操作日志记录与查询
- 数据统计图表（ECharts）
- 文件上传

---

## 注意事项

- `application.yml` 中的数据库密码使用 `${DB_PASSWORD:123456}` 占位，**请勿把真实密码提交到仓库**
- `node_modules/`、`target/`、日志文件已通过 `.gitignore` 排除
- 上传的文件保存在 `files/` 目录，操作日志输出到 `操作日志/` 目录

---

## 开发环境

- JDK 17
- MySQL 8.0
- Node.js 16+
- Maven 3.8.8

---

## 许可证

本项目为课程实训项目，仅供学习参考。
