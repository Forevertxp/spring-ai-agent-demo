package com.hxy.agent.tool;

import com.hxy.agent.model.FinancialData;
import com.hxy.agent.service.MockDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class FinancialDataTool {

    private final MockDataService mockDataService;

    public FinancialDataTool(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    public FinancialData getFinancialData(String storeCode, String startDate, String endDate, String metric) {
        log.info("财务数据查询: store={}, range={}, metric={}", storeCode, startDate + "-" + endDate, metric);
        return mockDataService.generateFinancialData(storeCode, startDate, endDate);
    }

    public List<FinancialData> getMultiStoreFinancial(String storeCodes, String startDate, String endDate, String metric) {
        String[] codes = storeCodes.split(",");
        return Arrays.stream(codes)
                .map(code -> getFinancialData(code.trim(), startDate, endDate, metric))
                .toList();
    }

    public List<FinancialData> getFinancialTrend(String storeCode, String startDate, String endDate, String metric) {
        log.info("财务趋势查询: store={}, range={}", storeCode, startDate + "-" + endDate);
        return mockDataService.generateFinancialTrend(storeCode, startDate, endDate);
    }

    public String getDescription() {
        return """
                财务数据工具，用于查询门店财务数据：
                - getFinancialData(storeCode, startDate, endDate, metric): 查询单个门店财务数据
                - getMultiStoreFinancial(storeCodes, startDate, endDate, metric): 查询多个门店财务数据
                - getFinancialTrend(storeCode, startDate, endDate, metric): 查询财务趋势数据

                参数说明：
                - storeCode: 门店编码(BJ001/SH001/GZ001/SZ001)
                - startDate: 开始日期(yyyy-MM-dd)
                - endDate: 结束日期(yyyy-MM-dd)
                - metric: 指标类型(revenue/profit/cost/margin)
                """;
    }
}