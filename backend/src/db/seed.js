const { pool } = require("./pool");

async function runSeed() {
  const { rows } = await pool.query("SELECT COUNT(*)::int AS count FROM stores");
  return { alreadySeeded: rows[0].count > 0 };
}

if (require.main === module) {
  runSeed()
    .then(async (result) => {
      console.log(result.alreadySeeded ? "Seed data already exists." : "Seed skipped.");
      await pool.end();
    })
    .catch(async (error) => {
      console.error(error);
      await pool.end();
      process.exit(1);
    });
}

module.exports = { runSeed };
