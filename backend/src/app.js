const express = require("express");
const cors = require("cors");
const pinoHttp = require("pino-http");
const { config } = require("./config");
const { apiRouter } = require("./routes/api");
const { errorHandler, notFound } = require("./middleware/errorHandler");

function createApp() {
  const app = express();

  app.use(
    pinoHttp({
      level: config.logLevel,
      transport: process.env.NODE_ENV === "production" ? undefined : { target: "pino-pretty" },
    })
  );
  app.use(cors());
  app.use(express.json({ limit: "1mb" }));

  app.get("/healthz", (req, res) => {
    res.json({ ok: true, service: "neko-cafe-backend" });
  });

  app.use("/api", apiRouter);
  app.use(notFound);
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
