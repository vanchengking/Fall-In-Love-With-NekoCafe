package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("cat_health_records")
public class CatHealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long catId;
    private BigDecimal weightKg;
    private String vaccineNote;
    private String interactionNote;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCatId() { return catId; }
    public void setCatId(Long catId) { this.catId = catId; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getVaccineNote() { return vaccineNote; }
    public void setVaccineNote(String vaccineNote) { this.vaccineNote = vaccineNote; }

    public String getInteractionNote() { return interactionNote; }
    public void setInteractionNote(String interactionNote) { this.interactionNote = interactionNote; }
}
