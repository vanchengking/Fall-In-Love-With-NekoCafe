package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 预约。生成列 active_slot_key / user_active_slot_key 由数据库维护，故不在实体中声明。
 * 日期/时间使用 String，避免 JDBC 时间类型映射歧义。
 */
@TableName("reservations")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long storeId;
    private Long tableId;
    private Long recommendedCatId;
    private String reservationDate;
    private String reservationTime;
    private Integer partySize;
    private String status;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public Long getRecommendedCatId() { return recommendedCatId; }
    public void setRecommendedCatId(Long recommendedCatId) { this.recommendedCatId = recommendedCatId; }

    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getReservationTime() { return reservationTime; }
    public void setReservationTime(String reservationTime) { this.reservationTime = reservationTime; }

    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
