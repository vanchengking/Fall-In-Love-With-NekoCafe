# 后端新增文件架构影响分析

## 新增文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| Coupon.java | `entity/Coupon.java` | 优惠券实体，遵循 `@TableName` 注解规范 |
| CouponMapper.java | `mapper/CouponMapper.java` | `extends BaseMapper<Coupon>`，标准 MyBatis-Plus Mapper |
| CouponController.java | `web/CouponController.java` | `GET/POST/PATCH /api/coupons`，遵循 `@RestController + ApiResponse` 模式 |
| UserController.java | `web/UserController.java` | `GET/PATCH /api/users?role=`，同上 |

## 架构影响：无

4 个文件全部是**新增**，不修改任何已有文件，完全遵循现有三层架构：

```
backend/neko-server/src/main/java/com/nekocafe/
├── entity/
│   ├── ...（已有）
│   └── Coupon.java          ← 新增
├── mapper/
│   ├── ...（已有）
│   └── CouponMapper.java    ← 新增
└── web/
    ├── ...（已有）
    ├── CouponController.java ← 新增
    └── UserController.java   ← 新增
```

## 检查项

| 检查项 | 结论 |
|--------|------|
| 是否修改了已有文件？ | **否**，4 个全是新文件 |
| SecurityConfig 需要改吗？ | **不需要**，现有规则 `GET /api/**` permitAll 已公开 GET，`anyRequest().authenticated()` 已覆盖 POST/PATCH |
| MyBatis-Plus 能自动扫描吗？ | **能**，`@Mapper` 注解 + `@RestController` 会被 Spring Boot 自动发现 |
| 会影响高范铖的预约模块吗？ | **不会**，完全独立的表和 API |
| 会影响成睿轩的门店/看板模块吗？ | **不会**，Coupon 和 User 是独立实体 |
| 会影响葛宇晨的会员/订单模块吗？ | **不会**，UserController 只读 users 表，不改 AuthService 流程 |

## 文件归属对比（零重叠）

```
你（卢欣怡）改的文件：          后端同学可能改的文件：
├── frontend/src/...            ├── entity/Reservation.java
├── web/UserController.java     ├── service/ReservationService.java
├── web/CouponController.java   ├── service/OrderService.java
├── entity/Coupon.java          ├── mapper/ReservationMapper.java
├── mapper/CouponMapper.java    ├── web/ReservationController.java
└── styles/*.css                └── web/OrderController.java
```

**没有一个文件重叠，git merge 不会产生冲突。**

## 本地测试

```bash
docker compose down -v
docker compose up --build
```

验证新 API：

```bash
# 用户列表（公开 GET）
curl http://localhost:8080/api/users?role=staff,manager

# 优惠券列表（公开 GET）
curl http://localhost:8080/api/coupons

# 创建优惠券（需要 JWT token）
curl -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"TEST10","title":"测试券","discount_cents":1000,"min_spend_cents":5000}'
```

## 唯一风险点

如果后端同学修改了 `NekoServerApplication.java`（比如加了新的 `@MapperScan`），可能和自动扫描重复，但即使重复也不会报错，影响极小。

## 结论

**放心提交，不会影响任何人，不会产生合并冲突。**
