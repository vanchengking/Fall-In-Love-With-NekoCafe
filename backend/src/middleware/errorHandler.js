function notFound(req, res, next) {
  next({ status: 404, message: `Route not found: ${req.method} ${req.originalUrl}` });
}

function errorHandler(error, req, res, next) {
  const status = error.status || 500;
  const message = status >= 500 ? "Internal server error" : error.message;

  if (status >= 500) {
    req.log?.error({ error }, "request failed");
  }

  res.status(status).json({
    error: {
      message,
      status,
    },
  });
}

module.exports = { errorHandler, notFound };
