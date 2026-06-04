package com.nekocafe.domain;

import java.util.Map;
import java.util.Set;

/**
 * 预约状态机：严格定义合法跳转，禁止非法跳转（D-01 非功能要求"状态机单元测试"）。
 *
 * <pre>
 * created  -> booked, cancelled
 * booked   -> seated, cancelled, no_show
 * seated   -> dining, finished, cancelled
 * dining   -> finished
 * finished -> (终态)
 * cancelled-> (终态)
 * no_show  -> (终态)
 * </pre>
 */
public final class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED = Map.of(
            ReservationStatus.CREATED, Set.of(ReservationStatus.BOOKED, ReservationStatus.CANCELLED),
            ReservationStatus.BOOKED, Set.of(ReservationStatus.SEATED, ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW),
            ReservationStatus.SEATED, Set.of(ReservationStatus.DINING, ReservationStatus.FINISHED, ReservationStatus.CANCELLED),
            ReservationStatus.DINING, Set.of(ReservationStatus.FINISHED),
            ReservationStatus.FINISHED, Set.of(),
            ReservationStatus.CANCELLED, Set.of(),
            ReservationStatus.NO_SHOW, Set.of()
    );

    private ReservationStateMachine() {
    }

    public static boolean canTransition(ReservationStatus from, ReservationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<ReservationStatus> nextStates(ReservationStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }
}
