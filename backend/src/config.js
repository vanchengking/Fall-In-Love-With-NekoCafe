require("dotenv").config();

const config = {
  port: Number(process.env.PORT || 8080),
  databaseUrl:
    process.env.DATABASE_URL || "postgres://neko:neko@localhost:5432/neko_cafe",
  redisUrl: process.env.REDIS_URL || "redis://localhost:6379",
  autoSeed: String(process.env.AUTO_SEED || "false").toLowerCase() === "true",
  logLevel: process.env.LOG_LEVEL || "info",
};

module.exports = { config };
