const mysql = require("mysql2/promise");
const { config } = require("../config");

function databaseOptions(databaseUrl) {
  const url = new URL(databaseUrl);
  return {
    host: url.hostname,
    port: Number(url.port || 3306),
    user: decodeURIComponent(url.username),
    password: decodeURIComponent(url.password),
    database: url.pathname.replace(/^\//, ""),
  };
}

const pool = mysql.createPool({
  ...databaseOptions(config.databaseUrl),
  waitForConnections: true,
  connectionLimit: 10,
  idleTimeout: 30_000,
  multipleStatements: true,
  charset: "utf8mb4",
  dateStrings: true,
});

function toResult([rows, fields]) {
  return {
    rows: Array.isArray(rows) ? rows : [],
    result: rows,
    fields,
  };
}

async function query(text, params = []) {
  return toResult(await pool.query(text, params));
}

async function withTransaction(work) {
  const connection = await pool.getConnection();
  const client = {
    query: async (text, params = []) => toResult(await connection.query(text, params)),
  };

  try {
    await connection.beginTransaction();
    const result = await work(client);
    await connection.commit();
    return result;
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

module.exports = {
  pool,
  query,
  withTransaction,
};
