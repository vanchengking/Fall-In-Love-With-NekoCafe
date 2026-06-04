package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("reservation_events")
public class ReservationEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reservationId;
    private String fromStatus;
    private String toStatus;
    private String actorRole;
    private Long actorUserId;
    private String note;

    public ReservationEvent() {
    }

    public ReservationEvent(Long reservationId, String fromStatus, String toStatus,
                            String actorRole, Long actorUserId, String note) {
        this.reservationId = reservationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorRole = actorRole;
        this.actorUserId = actorUserId;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
