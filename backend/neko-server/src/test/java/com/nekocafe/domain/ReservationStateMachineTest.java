package com.nekocafe.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.nekocafe.domain.ReservationStatus.BOOKED;
import static com.nekocafe.domain.ReservationStatus.CANCELLED;
import static com.nekocafe.domain.ReservationStatus.CREATED;
import static com.nekocafe.domain.ReservationStatus.DINING;
import static com.nekocafe.domain.ReservationStatus.FINISHED;
import static com.nekocafe.domain.ReservationStatus.NO_SHOW;
import static com.nekocafe.domain.ReservationStatus.SEATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预约状态机单元测试（D-01 非功能要求："状态机严格按预定义路径流转，禁止非法跳转"）。
 * 以矩阵方式覆盖全部 7 个状态两两组合，并严格断言完桌必须经过 dining。
 */
class ReservationStateMachineTest {

    /** 期望的合法跳转表（与开题报告口径一致：created->booked->seated->dining->finished/...）。 */
    private static final Map<ReservationStatus, Set<ReservationStatus>> EXPECTED = Map.of(
            CREATED, Set.of(BOOKED, CANCELLED),
            BOOKED, Set.of(SEATED, CANCELLED, NO_SHOW),
            SEATED, Set.of(DINING, CANCELLED),
            DINING, Set.of(FINISHED),
            FINISHED, Set.of(),
            CANCELLED, Set.of(),
            NO_SHOW, Set.of()
    );

    @Test
    @DisplayName("7×7 矩阵：所有状态两两组合的合法/非法跳转")
    void fullTransitionMatrix() {
        for (ReservationStatus from : ReservationStatus.values()) {
            Set<ReservationStatus> legal = EXPECTED.get(from);
            for (ReservationStatus to : ReservationStatus.values()) {
                boolean expected = legal.contains(to);
                assertEquals(expected, ReservationStateMachine.canTransition(from, to),
                        () -> from.value() + " -> " + to.value() + " 期望 " + (expected ? "合法" : "非法"));
            }
        }
    }

    @Test
    @DisplayName("seated -> finished 必须非法：完桌必须先经过 dining")
    void seatedToFinishedIsIllegal() {
        assertFalse(ReservationStateMachine.canTransition(SEATED, FINISHED));
        assertTrue(ReservationStateMachine.canTransition(SEATED, DINING));
        assertTrue(ReservationStateMachine.canTransition(DINING, FINISHED));
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"FINISHED", "CANCELLED", "NO_SHOW"})
    @DisplayName("终态没有任何后继状态")
    void terminalStatesHaveNoNext(ReservationStatus terminal) {
        assertTrue(ReservationStateMachine.nextStates(terminal).isEmpty());
        for (ReservationStatus to : ReservationStatus.values()) {
            assertFalse(ReservationStateMachine.canTransition(terminal, to),
                    () -> "终态 " + terminal.value() + " 不应能流转到 " + to.value());
        }
    }

    @Test
    @DisplayName("null 起点/终点一律非法")
    void nullIsIllegal() {
        assertFalse(ReservationStateMachine.canTransition(null, BOOKED));
        assertFalse(ReservationStateMachine.canTransition(BOOKED, null));
        assertFalse(ReservationStateMachine.canTransition(null, null));
    }

    @Test
    @DisplayName("ReservationStatus.from 大小写不敏感且空值容错")
    void statusParsing() {
        assertEquals(BOOKED, ReservationStatus.from("BOOKED").orElseThrow());
        assertEquals(NO_SHOW, ReservationStatus.from("  no_show ").orElseThrow());
        assertTrue(ReservationStatus.from("flying").isEmpty());
        assertTrue(ReservationStatus.from(null).isEmpty());
        assertTrue(ReservationStatus.from("").isEmpty());
    }

    @Test
    @DisplayName("所有枚举都有非空且唯一的中文标签")
    void labelsComplete() {
        Set<String> labels = new HashSet<>();
        for (ReservationStatus s : ReservationStatus.values()) {
            assertNotNull(s.label(), s.value() + " 缺少中文标签");
            assertFalse(s.label().isBlank(), s.value() + " 中文标签为空");
            assertTrue(labels.add(s.label()), "中文标签重复：" + s.label());
        }
        assertEquals("待确认", CREATED.label());
        assertEquals("用餐中", DINING.label());
        assertEquals("未到店", NO_SHOW.label());
        assertEquals("未到店", ReservationStatus.labelOf("no_show"));
        assertEquals("unknown", ReservationStatus.labelOf("unknown"));
    }
}
