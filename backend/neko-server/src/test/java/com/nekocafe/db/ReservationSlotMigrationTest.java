package com.nekocafe.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * FR-TABLE-002 数据库层契约校验：静态解析迁移脚本，固化验收口径——
 * V002 重建的生成列 active_slot_key 必须覆盖 created/booked/seated/dining 四个活跃状态
 * （配唯一约束 uq_reservations_active_slot 防同桌同时段重复），其余状态
 * （cancelled/finished/no_show）走 ELSE NULL 释放时段；user_active_slot_key 同口径防同人重复预约；
 * 且 V002 之后的任何迁移不得回退/重建这两组生成列与唯一约束。
 * 迁移文件不在编译上下文中时跳过（如 Docker 镜像内仅 COPY src 的构建场景）。
 */
class ReservationSlotMigrationTest {

    private static final String ACTIVE_STATUS_SET = "status IN ('created', 'booked', 'seated', 'dining')";

    private static Path migrationsDir;
    private static String v002;
    private static String v003;

    @BeforeAll
    static void loadMigrations() throws IOException {
        migrationsDir = locateMigrationsDir();
        assumeTrue(migrationsDir != null, "db/migrations 不在当前构建上下文，跳过迁移契约校验");
        v002 = Files.readString(migrationsDir.resolve("V002__d01_upgrade.sql"), StandardCharsets.UTF_8);
        v003 = Files.readString(migrationsDir.resolve("V003__reservation_backend_completion.sql"),
                StandardCharsets.UTF_8);
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
    @DisplayName("active_slot_key 覆盖 created/booked/seated/dining，终态 ELSE NULL 释放时段")
    void activeSlotKeyCoversAllActiveStatusesAndReleasesTerminal() {
        String columnDef = generatedColumnDef(v002, "active_slot_key");

        assertTrue(columnDef.contains(ACTIVE_STATUS_SET),
                "active_slot_key 必须覆盖 created/booked/seated/dining 四个活跃状态");
        assertTrue(columnDef.contains("ELSE NULL"),
                "非活跃状态必须生成 NULL（MySQL 唯一索引允许多个 NULL）以释放时段");
        assertFalse(columnDef.contains("cancelled") || columnDef.contains("finished")
                        || columnDef.contains("no_show"),
                "cancelled/finished/no_show 不得出现在占用键中——它们通过 ELSE NULL 释放时段");
        assertTrue(columnDef.contains("table_id IS NOT NULL"),
                "未分配桌位的预约不应参与桌位时段唯一性");

        assertTrue(v002.contains("DROP COLUMN active_slot_key"),
                "V002 必须先移除 V001 仅含 booked/seated 的旧口径生成列");
        assertTrue(v002.contains("ADD UNIQUE KEY uq_reservations_active_slot (active_slot_key)"),
                "同桌同时段唯一约束 uq_reservations_active_slot 必须存在");
    }

    @Test
    @DisplayName("user_active_slot_key 同口径防同人同店同时段重复预约，V003 兜底补建唯一约束")
    void userSlotKeySharesTheSameStatusContract() {
        String columnDef = generatedColumnDef(v002, "user_active_slot_key");

        assertTrue(columnDef.contains(ACTIVE_STATUS_SET));
        assertTrue(columnDef.contains("ELSE NULL"));
        assertTrue(v002.contains("ADD UNIQUE KEY uq_reservations_user_slot (user_active_slot_key)"),
                "V002 必须建立同人同店同时段唯一约束");
        assertTrue(v003.contains("uq_reservations_user_slot"),
                "V003 必须保留对 uq_reservations_user_slot 的幂等兜底检查");
    }

    @Test
    @DisplayName("V002 之后的迁移不得回退活跃时段生成列与唯一约束")
    void laterMigrationsNeverRegressSlotConstraints() throws IOException {
        for (Path file : migrationsAfterV002()) {
            String sql = Files.readString(file, StandardCharsets.UTF_8);
            String name = file.getFileName().toString();
            assertFalse(sql.contains("DROP COLUMN active_slot_key"),
                    name + " 不得删除 active_slot_key 生成列");
            assertFalse(sql.contains("ADD COLUMN active_slot_key"),
                    name + " 不得重建 active_slot_key（口径以 V002 为准）");
            assertFalse(sql.contains("DROP INDEX uq_reservations_active_slot")
                            || sql.contains("DROP KEY uq_reservations_active_slot"),
                    name + " 不得删除唯一约束 uq_reservations_active_slot");
            assertFalse(sql.contains("DROP COLUMN user_active_slot_key")
                            || sql.contains("ADD COLUMN user_active_slot_key"),
                    name + " 不得改动 user_active_slot_key（口径以 V002 为准）");
        }
    }

    /** 截取 V002 中指定生成列的 ADD COLUMN ... STORED 定义段。 */
    private static String generatedColumnDef(String sql, String column) {
        int start = sql.indexOf("ADD COLUMN " + column);
        assertTrue(start >= 0, "V002 中应存在 ADD COLUMN " + column);
        int end = sql.indexOf("STORED", start);
        assertTrue(end > start, column + " 应定义为 STORED 生成列");
        return sql.substring(start, end);
    }

    /** 版本号大于 2 的全部迁移文件（含未来新增，防止后续回退）。 */
    private static List<Path> migrationsAfterV002() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(migrationsDir)) {
            stream.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .filter(p -> versionOf(p) > 2)
                    .sorted()
                    .forEach(files::add);
        }
        assertTrue(files.size() >= 6, "应至少扫描到 V003~V008 共 6 个后续迁移");
        return files;
    }

    private static int versionOf(Path file) {
        String name = file.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }
}
