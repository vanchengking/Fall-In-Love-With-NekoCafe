const { createApp } = require("./app");
const { config } = require("./config");
const { runMigrations } = require("./db/migrate");
const { runSeed } = require("./db/seed");

async function main() {
  await runMigrations();

  if (config.autoSeed) {
    await runSeed();
  }

  const app = createApp();
  app.listen(config.port, () => {
    console.log(`NekoCafe backend listening on http://localhost:${config.port}`);
  });
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
