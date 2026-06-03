# ADR-0001 技术栈与参考项目处理

## 状态

Accepted

## 背景

课程选题 T-01 要求实现 NekoCafe 智慧餐饮预约平台，覆盖预约、桌位、点单、会员、猫咪档案、数据看板和至少一项选做能力。参考仓库 `JonOdyssey/starter-restaurant-reservation` 只覆盖传统餐厅预约和桌位入座，且依赖版本偏旧。

## 决策

第一版代码框架采用：

- 前端：Vue 3 + Vite
- 后端：Node.js 20 + Express 5
- 数据库：MySQL 8
- 缓存预留：Redis 7
- 部署：Docker Compose
- 测试：Node 内置 test、k6 压测脚本

参考仓库仅作为业务流样例，不直接复制源码。预约状态机、桌位可用性、手机号检索等能力在本项目中重新实现，并扩展为 NekoCafe 所需的数据模型。

## 后果

- 避免继续使用 react-scripts 4、React 17、Knex 0.21、Jest 26 等旧依赖。
- 前后端目录仍符合老师给定提交框架。
- 若后续教师严格要求 Spring Boot，可保留当前 API 契约和数据库设计，逐步替换后端实现。
