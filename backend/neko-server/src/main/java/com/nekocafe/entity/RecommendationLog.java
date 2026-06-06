package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 规则型推荐的计算日志（最小审计表）。写入失败不影响推荐主流程。
 */
@TableName("recommendation_logs")
public class RecommendationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long storeId;
    private String preferencesJson;
    private String resultSnapshotJson;

    public RecommendationLog() {
    }

    public RecommendationLog(Long userId, Long storeId, String preferencesJson, String resultSnapshotJson) {
        this.userId = userId;
        this.storeId = storeId;
        this.preferencesJson = preferencesJson;
        this.resultSnapshotJson = resultSnapshotJson;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getPreferencesJson() { return preferencesJson; }
    public void setPreferencesJson(String preferencesJson) { this.preferencesJson = preferencesJson; }

    public String getResultSnapshotJson() { return resultSnapshotJson; }
    public void setResultSnapshotJson(String resultSnapshotJson) { this.resultSnapshotJson = resultSnapshotJson; }
}
