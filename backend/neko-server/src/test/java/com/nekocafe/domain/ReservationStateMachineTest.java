package com.nekocafe.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预约状态机单元测试（D-01 非功能要求："状态机严格按预定义路径流转，禁止非法跳转"）。
 */
class ReservationStateMachineTest {

    @Test
    @DisplayName("合法跳转：created->booked->seated->dining->finished")
    void legalHappyPath() {
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.CREATED, ReservationStatus.BOOKED));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.BOOKED, ReservationStatus.SEATED));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.SEATED, ReservationStatus.DINING));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.DINING, ReservationStatus.FINISHED));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.SEATED, ReservationStatus.FINISHED));
    }

    @Test
    @DisplayName("合法分支：取消与未到店")
    void legalBranches() {
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.CREATED, ReservationStatus.CANCELLED));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.BOOKED, ReservationStatus.CANCELLED));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.BOOKED, ReservationStatus.NO_SHOW));
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.SEATED, ReservationStatus.CANCELLED));
    }

    @Test
    @DisplayName("非法跳转被禁止")
    void illegalTransitions() {
        assertFalse(ReservationStateMachine.canTransition(ReservationStatus.BOOKED, ReservationStatus.FINISHED));
        assertFalse(ReservationStateMachine.canTransition(ReservationStatus.BOOKED, ReservationStatus.DINING));
        assertFalse(ReservationStateMachine.canTransition(ReservationStatus.CREATED, ReservationStatus.SEATED));
        assertFalse(ReservationStateMachine.canTransition(ReservationStatus.DINING, ReservationStatus.SEATED));
    }

    @Test
    @DisplayName("终态不可再流转")
    void terminalStatesAreFinal() {
        for (ReservationStatus terminal : new ReservationStatus[]{
                ReservationStatus.FINISHED, ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW}) {
            assertTrue(ReservationStateMachine.nextStates(terminal).isEmpty());
            assertFalse(ReservationStateMachine.canTransition(terminal, ReservationStatus.BOOKED));
        }
    }

    @Test
    @DisplayName("状态枚举解析大小写不敏感且容错")
    void statusParsing() {
        assertTrue(ReservationStatus.from("BOOKED").isPresent());
        assertTrue(ReservationStatus.from("no_show").isPresent());
        assertFalse(ReservationStatus.from("flying").isPresent());
        assertFalse(ReservationStatus.from(null).isPresent());
    }
}
