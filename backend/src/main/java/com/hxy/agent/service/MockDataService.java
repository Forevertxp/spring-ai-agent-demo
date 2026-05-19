package com.hxy.agent.service;

import com.hxy.agent.entity.TimeRange;
import com.hxy.agent.entity.TimeUnit;
import com.hxy.agent.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MockDataService {

    private static final Map<String, Store> STORES = new LinkedHashMap<>();

    static {
        STORES.put("BJ001", new Store("BJ001", "北京王府井店", "北京"));
        STORES.put("SH001", new Store("SH001", "上海南京路店", "上海"));
        STORES.put("GZ001", new Store("GZ001", "广州天河店", "广州"));
        STORES.put("SZ001", new Store("SZ001", "深圳福田店", "深圳"));
    }

    public Store getStore(String code) {
        return STORES.get(code);
    }

    public List<Store> getAllStores() {
        return new ArrayList<>(STORES.values());
    }

    public List<Store> findStoresByKeyword(String keyword) {
        return STORES.values().stream()
                .filter(s -> s.getName().contains(keyword) ||
                        s.getCity().contains(keyword) ||
                        s.getCode().contains(keyword))
                .toList();
    }

    public FinancialData generateFinancialData(String storeCode, String startDate, String endDate) {
        Store store = getStore(storeCode);
        if (store == null) {
            return FinancialData.error("门店不存在: " + storeCode);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseRevenue = 500000 + random.nextDouble(300000);

        return FinancialData.builder()
                .storeCode(storeCode)
                .storeName(store.getName())
                .timeRange(startDate + " - " + endDate)
                .revenue(baseRevenue)
                .profit(baseRevenue * 0.15 + random.nextDouble(50000))
                .cost(baseRevenue * 0.85)
                .profitMargin(15 + random.nextDouble(5))
                .success(true)
                .build();
    }

    public List<FinancialData> generateFinancialTrend(String storeCode, String startDate, String endDate) {
        Store store = getStore(storeCode);
        if (store == null) {
            return Collections.emptyList();
        }

        List<FinancialData> trend = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double baseRevenue = 500000;

        LocalDate current = start;
        while (!current.isAfter(end)) {
            double dailyRevenue = baseRevenue / 30 + random.nextDouble(5000);
            trend.add(FinancialData.builder()
                    .storeCode(storeCode)
                    .storeName(store.getName())
                    .timeRange(current.toString())
                    .revenue(dailyRevenue)
                    .profit(dailyRevenue * 0.15)
                    .cost(dailyRevenue * 0.85)
                    .profitMargin(15 + random.nextDouble(3))
                    .success(true)
                    .build());
            current = current.plusDays(1);
        }

        return trend;
    }

    public CustomerFlowData generateCustomerFlowData(String storeCode, String startDate, String endDate) {
        Store store = getStore(storeCode);
        if (store == null) {
            return CustomerFlowData.error("门店不存在: " + storeCode);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int totalFlow = 10000 + random.nextInt(5000);
        int newCustomers = (int)(totalFlow * (0.3 + random.nextDouble(0.1)));

        return CustomerFlowData.builder()
                .storeCode(storeCode)
                .storeName(store.getName())
                .timeRange(startDate + " - " + endDate)
                .totalFlow(totalFlow)
                .conversionRate(20 + random.nextDouble(10))
                .newCustomers(newCustomers)
                .repeatCustomers(totalFlow - newCustomers)
                .newCustomerRatio(newCustomers * 100.0 / totalFlow)
                .success(true)
                .build();
    }

    public InventoryStatus generateInventoryStatus(String storeCode) {
        Store store = getStore(storeCode);
        if (store == null) {
            return InventoryStatus.error("门店不存在: " + storeCode);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int totalItems = 1000 + random.nextInt(500);

        List<StockAlert> alerts = new ArrayList<>();
        int alertCount = random.nextInt(5);
        for (int i = 0; i < alertCount; i++) {
            alerts.add(StockAlert.builder()
                    .productName("商品-" + (i + 1))
                    .currentStock(random.nextInt(20))
                    .minStock(50)
                    .alertType("LOW_STOCK")
                    .build());
        }

        return InventoryStatus.builder()
                .storeCode(storeCode)
                .storeName(store.getName())
                .turnoverRate(4 + random.nextDouble(2))
                .totalItems(totalItems)
                .lowStockItems(alertCount)
                .outOfStockItems(random.nextInt(3))
                .alerts(alerts)
                .success(true)
                .build();
    }

    public Map<String, Object> generateHourlyFlow(String storeCode, String date) {
        Store store = getStore(storeCode);
        if (store == null) {
            return Collections.emptyMap();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeCode", storeCode);
        result.put("storeName", store.getName());
        result.put("date", date);

        Map<Integer, Integer> hourlyData = new LinkedHashMap<>();
        for (int hour = 8; hour <= 22; hour++) {
            int baseFlow = hour >= 10 && hour <= 14 || hour >= 17 && hour <= 20 ? 200 : 50;
            hourlyData.put(hour, baseFlow + random.nextInt(100));
        }
        result.put("hourlyFlow", hourlyData);

        return result;
    }

    public String getStoreCodeByName(String name) {
        for (Store store : STORES.values()) {
            if (store.getName().contains(name) || store.getCity().contains(name)) {
                return store.getCode();
            }
        }
        return null;
    }
}