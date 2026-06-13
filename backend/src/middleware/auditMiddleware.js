const repository = require("../routes/repository");

// 从请求路径推断目标表
function inferTargetTable(path) {
  const map = {
    "/reservations": "reservations",
    "/orders": "orders",
    "/stores": "stores",
    "/tables": "dining_tables",
    "/cats": "cats",
    "/menu-items": "menu_items",
    "/users": "users",
    "/cat-health-records": "cat_health_records",
  };
  for (const [prefix, table] of Object.entries(map)) {
    if (path.startsWith(prefix)) return table;
  }
  return null;
}

// 从请求路径提取目标 ID
function inferTargetId(path) {
  const match = path.match(/\/(\d+)(?:\/|$)/);
  return match ? Number(match[1]) : null;
}

// 从请求方法推断操作类型
function inferAction(method) {
  const map = { POST: "CREATE", PATCH: "UPDATE", PUT: "UPDATE", DELETE: "DELETE" };
  return map[method] || null;
}

// 跳过不需要审计的路径
function shouldSkip(path) {
  const skipPaths = ["/auth/login", "/audit-logs", "/recommendations"];
  return skipPaths.some((p) => path.startsWith(p));
}

function auditMiddleware(req, res, next) {
  // 只审计写操作
  const action = inferAction(req.method);
  if (!action) return next();

  // 跳过特定路径
  const subPath = req.baseUrl.replace(/^\/api/, "") + req.path;
  if (shouldSkip(subPath)) return next();

  const targetTable = inferTargetTable(subPath);
  if (!targetTable) return next();

  const targetId = inferTargetId(subPath);

  // 捕获响应完成后写入审计日志（不阻塞请求）
  const originalJson = res.json.bind(res);
  res.json = function (body) {
    // 仅在成功响应时记录（2xx）
    if (res.statusCode >= 200 && res.statusCode < 300) {
      const user = req.headers["x-user"] ? JSON.parse(req.headers["x-user"]) : null;
      repository
        .insertAuditLog({
          userId: user?.id || null,
          userName: user?.name || null,
          action,
          targetTable,
          targetId,
          detail: {
            method: req.method,
            path: subPath,
            body: req.body,
            status: res.statusCode,
          },
          ipAddress: req.ip,
        })
        .catch(() => {}); // 审计失败不影响主流程
    }
    return originalJson(body);
  };

  next();
}

module.exports = { auditMiddleware };
