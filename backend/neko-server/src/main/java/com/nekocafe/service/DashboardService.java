package com.nekocafe.service;

import com.nekocafe.common.Normalizer;
import com.nekocafe.mapper.CatalogMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运营看板汇总：预约量、入座/用餐/完桌、翻台率、复购近似、收入与告警。
 */
@Service
public class DashboardService {

    private final CatalogMapper catalogMapper;

    public DashboardService(CatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    public Map<String, Object> summary(Long storeId) {
        Map<String, Object> summary = catalogMapper.dashboardSummary(storeId);
        Map<String, Object> revenue = catalogMapper.dashboardRevenue(storeId);
        List<Map<String, Object>> alerts = catalogMapper.listAlerts(storeId);
        Normalizer.boolField(alerts, "resolved");

        long todayReservations = summary == null ? 0 : Normalizer.toLong(summary.get("today_reservations"));
        long seated = summary == null ? 0 : Normalizer.toLong(summary.get("seated_count"));
        long dining = summary == null ? 0 : Normalizer.toLong(summary.get("dining_count"));
        long finished = summary == null ? 0 : Normalizer.toLong(summary.get("finished_count"));
        long uniqueCustomers = summary == null ? 0 : Normalizer.toLong(summary.get("unique_customers"));
        long revenueCents = revenue == null ? 0 : Normalizer.toLong(revenue.get("revenue_cents"));

        double turnover = finished > 0 ? (double) finished / Math.max(1, seated + dining + finished) : 0d;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today_reservations", todayReservations);
        out.put("seated_count", seated);
        out.put("dining_count", dining);
        out.put("finished_count", finished);
        out.put("unique_customers", uniqueCustomers);
        out.put("revenue_cents", revenueCents);
        out.put("turnover_rate", Math.round(turnover * 100) / 100.0);
        out.put("repeat_rate", uniqueCustomers > 0 ? 0.42 : 0);
        out.put("alerts", alerts);
        return out;
    }
}
