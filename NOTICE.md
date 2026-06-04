# 第三方组件清单

本项目使用以下开源组件，特此声明。正式实现以 Spring Boot 后端为准；Express 一栏为早期原型保留依赖。

## 正式实现（Spring Boot 后端 + Vue 前端）

| 名称 | 版本 | 协议 | 用途 | 出处 |
|------|------|------|------|------|
| Spring Boot | 3.3.x | Apache-2.0 | 后端框架 | https://spring.io/projects/spring-boot |
| Spring Security | 6.x | Apache-2.0 | 鉴权与 RBAC | https://spring.io/projects/spring-security |
| MyBatis-Plus | 3.5.x | Apache-2.0 | ORM / Mapper | https://baomidou.com |
| jjwt (io.jsonwebtoken) | 0.12.x | Apache-2.0 | JWT 签发与校验 | https://github.com/jwtk/jjwt |
| springdoc-openapi | 2.6.x | Apache-2.0 | OpenAPI/Swagger 文档 | https://springdoc.org |
| MySQL Connector/J | 8.x | GPLv2+FOSS 例外 | MySQL JDBC 驱动 | https://dev.mysql.com/downloads/connector/j/ |
| Lettuce（Spring Data Redis） | 随 Boot | Apache-2.0/MIT | Redis 客户端 | https://lettuce.io |
| OpenJDK / Temurin | 21 | GPLv2+CE | Java 运行时 | https://adoptium.net |
| Maven | 3.9 | Apache-2.0 | 构建工具 | https://maven.apache.org |
| Vue | 3.x | MIT | 前端界面 | https://vuejs.org |
| Vite | 8.x | MIT | 前端构建 | https://vitejs.dev |
| lucide (@lucide/vue) | 1.x | ISC | 前端图标 | https://lucide.dev |
| MySQL | 8.x | GPLv2 / Commercial | 关系数据库 | https://www.mysql.com |
| Redis | 7 | BSD-3-Clause | 缓存 / 验证码 / 限流 | https://redis.io |
| Nginx | 1.27 | BSD-2-Clause | 前端静态资源与反向代理 | https://nginx.org |
| Docker / Docker Compose | 最新稳定版 | Apache-2.0 | 容器编排 | https://www.docker.com |

## 原型实现（保留参考，非启动路径）

| 名称 | 版本 | 协议 | 用途 | 出处 |
|------|------|------|------|------|
| Node.js | 20+ | MIT | 原型后端运行时 | https://nodejs.org |
| Express | 5.x | MIT | 原型后端 HTTP API | https://expressjs.com |
| mysql2 | 3.x | MIT | 原型 MySQL 客户端 | https://github.com/sidorares/node-mysql2 |

## 参考资料

- 参考仓库：`https://github.com/JonOdyssey/starter-restaurant-reservation`
- 参考范围：预约列表、预约创建、桌位入座、完桌、手机号搜索等业务流和测试故事。
- 处理方式：未直接复制其核心源码；本仓库按照 NekoCafe 选题要求重新设计数据表、API、前端页面和状态机，并按 D-01 技术路线用 Spring Boot 重写后端。

## 参考仓库依赖老化记录

参考仓库使用 React 17、react-scripts 4、Jest 26、Knex 0.21、dotenv 8 等旧依赖。当前实现改为 Vue 3 + Vite、Spring Boot 3 + MyBatis-Plus、MySQL 8、Redis 7，避免继续依赖 Create React App 和旧 Knex 版本。

## AI 辅助记录

课程资料梳理、参考仓库阅读、第一版代码框架与前端横幅位图由 Codex 辅助完成；后端 Spring Boot 正式化（数据迁移 V002、JWT/RBAC、预约状态机、Redis 沙箱、OpenAPI、单元测试与 Docker 编排）由 Claude Code 辅助完成。后续总结报告需按课程要求补充 AI 使用申报表（D-07）。
