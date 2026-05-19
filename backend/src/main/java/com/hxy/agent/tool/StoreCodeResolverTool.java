package com.hxy.agent.tool;

import com.hxy.agent.model.Store;
import com.hxy.agent.service.MockDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StoreCodeResolverTool {

    private final MockDataService mockDataService;

    public StoreCodeResolverTool(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    public String resolveStoreCode(String storeName) {
        log.info("解析门店名称: {}", storeName);

        List<Store> candidates = mockDataService.findStoresByKeyword(storeName);

        if (candidates.size() == 1) {
            return candidates.get(0).getCode();
        }

        if (candidates.isEmpty()) {
            return "未找到门店: " + storeName + "。可用门店: BJ001(北京), SH001(上海), GZ001(广州), SZ001(深圳)";
        }

        return candidates.stream()
                .map(s -> s.getCode() + "(" + s.getName() + ")")
                .collect(Collectors.joining(","));
    }

    public String getAllStores() {
        List<Store> stores = mockDataService.getAllStores();
        return stores.stream()
                .map(s -> s.getCode() + ":" + s.getName() + "(" + s.getCity() + ")")
                .collect(Collectors.joining("\n"));
    }

    public String getDescription() {
        return """
                门店编码解析工具：
                - resolveStoreCode(storeName): 将门店名称转换为编码
                - getAllStores(): 获取所有门店列表

                门店对照表：
                - 北京王府井店: BJ001
                - 上海南京路店: SH001
                - 广州天河店: GZ001
                - 深圳福田店: SZ001
                """;
    }
}