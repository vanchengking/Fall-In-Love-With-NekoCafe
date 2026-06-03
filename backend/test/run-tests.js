const fs = require("node:fs");
const path = require("node:path");

const testDir = __dirname;
const files = fs
  .readdirSync(testDir)
  .filter((file) => file.endsWith(".test.js"))
  .sort();

for (const file of files) {
  require(path.join(testDir, file));
}
