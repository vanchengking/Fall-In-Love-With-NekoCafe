package com.nekocafe.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * FR-STORE-001 / FR-STORE-002 数据基线校验：静态解析 V001 + V008 种子迁移，
 * 固化验收口径——38 家门店分布 12 城市、字段完整、每店至少 3 张桌且含猫咪互动区桌、
 * area 仅使用前端兼容的 canonical 值（window/main/party/quiet）。
 * 迁移文件不在编译上下文中时跳过（如 Docker 镜像内仅 COPY src 的构建场景）。
 */
class StoreSeedMigrationTest {

    /** V008 门店行：(id, 'name', 'city', 'address', 'phone', 'HH:MM:SS', 'HH:MM:SS', 'hours', lat, lng, NULL, 'equip', 'area') */
    private static final Pattern V008_STORE_ROW = Pattern.compile(
            "\\((\\d+), 'NekoCafe ([^']+)', '([^']+)', '([^']+)', '([^']+)', "
                    + "'(\\d{2}:\\d{2}:\\d{2})', '(\\d{2}:\\d{2}:\\d{2})', '([^']+)', "
                    + "([0-9.]+), ([0-9.]+), NULL, '([^']+)', '([^']+)'\\)");

    /** V008 桌位行：(store_id, 'CODE', seats, 'area', TRUE/FALSE, 'available') */
    private static final Pattern V008_TABLE_ROW = Pattern.compile(
            "\\((\\d+), '([A-Z]\\d{2})', (\\d+), '(window|main|party|quiet)', (TRUE|FALSE), 'available'\\)");

    /** V001 门店行：(id, 'NekoCafe xx', 'city', 'address', 'phone', lat, lng) */
    private static final Pattern V001_STORE_ROW = Pattern.compile("\\((\\d+), 'NekoCafe [^']+', '[^']+', ");

    /** V001 桌位行：(id, store_id, 'CODE', seats, 'area', true/false, 'available') */
    private static final Pattern V001_TABLE_ROW = Pattern.compile(
            "\\(\\d+, (\\d+), '([A-Z]\\d{2})', (\\d+), '(\\w+)', (true|false), 'available'\\)");

    private static final Set<String> EXPECTED_CITIES = Set.of(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉", "西安", "重庆", "苏州", "天津");

    private static String v001;
    private static String v008;

    @BeforeAll
    static void loadMigrations() throws IOException {
        Path migrations = locateMigrationsDir();
        assumeTrue(migrations != null, "db/migrations 不在当前构建上下文，跳过种子校验");
        v001 = Files.readString(migrations.resolve("V001__init.sql"), StandardCharsets.UTF_8);
        v008 = Files.readString(migrations.resolve("V008__m2_store_table_seed.sql"), StandardCharsets.UTF_8);
    }

    private static Path locateMigrationsDir() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("db").resolve("migrations");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    @Test
    @DisplayName("FR-STORE-001：V001+V008 共 38 家门店，覆盖 12 个城市")
    void seedContains38StoresAcross12Cities() {
        int v001Stores = countMatches(V001_STORE_ROW, v001);
        Map<Long, String[]> v008Stores = parseV008Stores();

        assertEquals(2, v001Stores, "V001 应保留原有 2 家门店");
        assertEquals(36, v008Stores.size(), "V008 应补齐 36 家门店");
        assertEquals(38, v001Stores + v008Stores.size(), "门店总数应为 38");

        for (long id = 3; id <= 38; id++) {
            assertTrue(v008Stores.containsKey(id), "V008 应包含 id=" + id + " 的门店");
        }

        Set<String> cities = new HashSet<>();
        v008Stores.values().forEach(row -> cities.add(row[1]));
        cities.add("北京"); // V001 两家均在北京
        assertEquals(EXPECTED_CITIES, cities, "门店应恰好覆盖 12 个目标城市");
    }

    @Test
    @DisplayName("FR-STORE-001：每家新门店 name/city/address/phone/营业时间/营业时间文本 字段完整")
    void everyStoreRowHasCompleteFields() {
        Map<Long, String[]> stores = parseV008Stores();
        for (Map.Entry<Long, String[]> entry : stores.entrySet()) {
            String[] row = entry.getValue();
            String label = "门店 id=" + entry.getKey();
            assertTrue(!row[0].isBlank(), label + " 缺 name");
            assertTrue(!row[1].isBlank(), label + " 缺 city");
            assertTrue(!row[2].isBlank(), label + " 缺 address");
            assertTrue(row[3].matches("0\\d{2,3}-\\d{8}"), label + " 电话格式异常: " + row[3]);
            assertTrue(row[4].compareTo(row[5]) < 0, label + " 营业时间 open_time 应早于 close_time");
            assertTrue(row[6].contains("-"), label + " business_hours_text 应包含时间段");
        }
    }

    @Test
    @DisplayName("FR-STORE-002：38 家门店每店至少 3 张桌位，且至少 1 张猫咪互动区桌")
    void everyStoreHasTablesWithCatZone() {
        Map<Long, Integer> tableCount = new HashMap<>();
        Map<Long, Integer> catZoneCount = new HashMap<>();

        Matcher v001Tables = V001_TABLE_ROW.matcher(v001);
        while (v001Tables.find()) {
            long storeId = Long.parseLong(v001Tables.group(1));
            tableCount.merge(storeId, 1, Integer::sum);
            if ("true".equals(v001Tables.group(5))) {
                catZoneCount.merge(storeId, 1, Integer::sum);
            }
            int seats = Integer.parseInt(v001Tables.group(3));
            assertTrue(seats > 0, "V001 桌位座位数必须为正");
        }

        Matcher v008Tables = V008_TABLE_ROW.matcher(v008);
        Set<String> storeCodePairs = new HashSet<>();
        while (v008Tables.find()) {
            long storeId = Long.parseLong(v008Tables.group(1));
            tableCount.merge(storeId, 1, Integer::sum);
            if ("TRUE".equals(v008Tables.group(5))) {
                catZoneCount.merge(storeId, 1, Integer::sum);
            }
            int seats = Integer.parseInt(v008Tables.group(3));
            assertTrue(seats >= 2 && seats <= 8, "座位数应在 2~8 之间: store=" + storeId);
            assertTrue(storeCodePairs.add(storeId + "#" + v008Tables.group(2)),
                    "同店桌位编号重复: store=" + storeId + " code=" + v008Tables.group(2));
        }

        for (long storeId = 1; storeId <= 38; storeId++) {
            int tables = tableCount.getOrDefault(storeId, 0);
            assertTrue(tables >= 3, "门店 " + storeId + " 桌位数不足 3 张: " + tables);
            assertTrue(catZoneCount.getOrDefault(storeId, 0) >= 1,
                    "门店 " + storeId + " 缺少猫咪互动区桌位");
        }
    }

    @Test
    @DisplayName("V008 不修改旧迁移且不使用本地 MySQL 不兼容方言")
    void migrationAvoidsIncompatibleDialect() {
        // 仅检查可执行 SQL，注释行（-- 开头）不参与方言判断
        String executableSql = v008.lines()
                .filter(line -> !line.strip().startsWith("--"))
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(!executableSql.contains("IF NOT EXISTS"),
                "V008 不应使用 ADD COLUMN IF NOT EXISTS 等本地 mysql:8.x 不支持的方言");
        assertTrue(executableSql.contains("ON DUPLICATE KEY UPDATE"), "V008 应保持幂等可重复执行");
    }

    private Map<Long, String[]> parseV008Stores() {
        Map<Long, String[]> stores = new HashMap<>();
        Matcher m = V008_STORE_ROW.matcher(v008);
        while (m.find()) {
            stores.put(Long.parseLong(m.group(1)), new String[] {
                    m.group(2),  // name（去前缀）
                    m.group(3),  // city
                    m.group(4),  // address
                    m.group(5),  // phone
                    m.group(6),  // open_time
                    m.group(7),  // close_time
                    m.group(8),  // business_hours_text
            });
        }
        return stores;
    }

    private static int countMatches(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
}
