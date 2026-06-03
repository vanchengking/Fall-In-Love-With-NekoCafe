# 第三方组件清单

本项目使用以下开源组件，特此声明：

| 名称 | 版本 | 协议 | 用途 | 出处 |
|------|------|------|------|------|
| Node.js | 20+ | MIT | 后端运行时 | https://nodejs.org |
| Express | 5.x | MIT | 后端 HTTP API | https://expressjs.com |
| pg | 8.x | MIT | PostgreSQL 客户端 | https://node-postgres.com |
| Vue | 3.x | MIT | 前端界面 | https://vuejs.org |
| Vite | 5.x | MIT | 前端构建 | https://vitejs.dev |
| lucide-vue-next | 0.x | ISC | 前端图标 | https://lucide.dev |
| PostgreSQL | 16 | PostgreSQL License | 关系数据库 | https://www.postgresql.org |
| Redis | 7 | BSD-3-Clause | 缓存预留 | https://redis.io |

## 参考资料

- 参考仓库：`https://github.com/JonOdyssey/starter-restaurant-reservation`
- 参考范围：预约列表、预约创建、桌位入座、完桌、手机号搜索等业务流和测试故事。
- 处理方式：未直接复制其核心源码；本仓库按照 NekoCafe 选题要求重新设计数据表、API、前端页面和状态机。

## 参考仓库依赖老化记录

参考仓库使用 React 17、react-scripts 4、Jest 26、Knex 0.21、dotenv 8 等旧依赖。当前实现改为 Vue 3 + Vite、Node.js 20+、Express 5、PostgreSQL 16、Redis 7，避免继续依赖 Create React App 和旧 Knex 版本。

## AI 辅助记录

本轮由 Codex 辅助完成课程资料梳理、参考仓库阅读、第一版代码框架、数据库迁移和前端横幅位图生成。后续总结报告需按课程要求补充 AI 使用申报表。
