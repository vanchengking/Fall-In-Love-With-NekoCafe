const fs = require("fs");
const path = require("path");
const { pool } = require("./pool");

const schemaPath = path.join(__dirname, "schema.sql");

async function runMigrations() {
  const sql = fs.readFileSync(schemaPath, "utf8");
  await pool.query(sql);
}

if (require.main === module) {
  runMigrations()
    .then(async () => {
      console.log("Database migration completed.");
      await pool.end();
    })
    .catch(async (error) => {
      console.error(error);
      await pool.end();
      process.exit(1);
    });
}

module.exports = { runMigrations };
