package com.nekocafe.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * D-01 预约订单状态：created -> booked -> seated -> dining -> finished / cancelled / no_show。
 *
 * <p>数据库与 API 中统一存英文 {@link #value()}；前端展示使用 {@link #label()} 给出的中文标签，
 * 切勿把中文写回数据库枚举列。</p>
 */
public enum ReservationStatus {
    CREATED("created", "待确认"),
    BOOKED("booked", "已预约"),
    SEATED("seated", "已入座"),
    DINING("dining", "用餐中"),
    FINISHED("finished", "已完成"),
    CANCELLED("cancelled", "已取消"),
    NO_SHOW("no_show", "未到店");

    private final String value;
    private final String label;

    ReservationStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    /** 中文展示标签（仅用于返回 DTO/Map，不写回数据库）。 */
    public String label() {
        return label;
    }

    /** 占用桌位 / 阻塞时段的活跃状态。 */
    public boolean isActive() {
        return this == CREATED || this == BOOKED || this == SEATED || this == DINING;
    }

    public static Optional<ReservationStatus> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(raw.trim()))
                .findFirst();
    }

    /**
     * 把原始状态字符串映射为中文标签；无法识别时回退为原值，便于演示历史脏数据时仍可读。
     */
    public static String labelOf(String raw) {
        return from(raw).map(ReservationStatus::label).orElse(raw);
    }
}
