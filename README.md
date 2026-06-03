# Fall-In-Love-With-NekoCafe

NekoCafe 智慧餐饮预约平台，面向猫咪主题餐厅的门店浏览、桌位预约、点单履约、会员运营、猫咪档案和经营看板系统。

本仓库保留课程模板目录结构，在 `backend/`、`frontend/`、`db/`、`tests/`、`infra/` 内补充实现内容。参考项目 `JonOdyssey/starter-restaurant-reservation` 的预约/桌位状态机思路已重写为 NekoCafe 业务模型，没有直接搬运其旧代码。

## 前置依赖

- Docker Desktop 24+
- Node.js 20+
- npm 10+

## 一键启动

```bash
make up
```

服务地址：

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/healthz
- 后端 API：http://localhost:8080/api

## 本地开发

```bash
make install
make dev-backend
make dev-frontend
```

## 验证

```bash
make test
curl http://localhost:8080/healthz
curl http://localhost:8080/api/stores
```

## 已覆盖模块

- M1 用户与会员：演示登录、会员等级、积分、偏好标签
- M2 门店与桌位：门店列表、桌位容量、时段可用性
- M3 预约与点单：预约创建、可用桌位分配、沙箱点单
- M4 订单与履约：订单状态、支付状态、预约状态机
- M5 店员后台：今日预约、入座、完桌、取消
- M6 数据看板：预约量、翻台、收入、复购近似指标
- 选做：基于偏好标签和猫咪性格标签的规则推荐

## 团队

- 组长：gaovancheng@gmail.com
