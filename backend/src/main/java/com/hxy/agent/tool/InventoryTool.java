package com.hxy.agent.tool;

import com.hxy.agent.model.InventoryStatus;
import com.hxy.agent.service.MockDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryTool {

    private final MockDataService mockDataService;

    public InventoryTool(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    public InventoryStatus getInventoryStatus(String storeCode) {
        log.info("库存状态查询: store={}", storeCode);
        return mockDataService.generateInventoryStatus(storeCode);
    }

    public InventoryStatus getInventoryTurnover(String storeCode, int months) {
        return getInventoryStatus(storeCode);
    }

    public InventoryStatus getStockAlerts(String storeCode) {
        return getInventoryStatus(storeCode);
    }

    public String getDescription() {
        return """
                库存数据工具，用于查询门店库存状态：
                - getInventoryStatus(storeCode): 查询库存状态
                - getInventoryTurnover(storeCode, months): 查询库存周转率

                参数说明：
                - storeCode: 门店编码
                - months: 时间范围月数
                """;
    }
}