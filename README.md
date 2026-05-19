# 门店数据分析智能体

基于阿里云DashScope SDK + Vue构建的门店数据分析智能体系统。

## 架构特点

### 智能体编排流程（工具调用机制）

```
用户输入 → IntentRouter(意图识别) → AI决策工具调用 → ToolRegistry执行工具 → 结果给AI分析 → 输出
```

核心流程：
1. **意图识别**：`IntentRouter.analyzeIntent()` 分析用户意图类型
2. **工具决策**：AI决定需要调用哪些工具获取数据
3. **工具执行**：`ToolRegistry.executeTool()` 调用相应工具
4. **分析生成**：将工具结果给AI进行最终分析

### 注册的工具

| 工具名称 | 功能 | 参数 |
|----------|------|------|
| `get_financial_data` | 查询单门店财务数据 | storeCode, startDate, endDate |
| `get_multi_store_financial` | 查询多门店财务数据(对比) | storeCodes, startDate, endDate |
| `get_financial_trend` | 查询财务趋势数据 | storeCode, startDate, endDate |
| `get_inventory_status` | 查询库存状态 | storeCode |
| `parse_time` | 解析相对时间 | timeExpression |
| `resolve_store_code` | 门店名称转编码 | storeName |

### 意图类型

| 意图类型 | 示例问题 | 调用的工具 |
|----------|----------|------------|
| DATA_QUERY | "北京门店上个月营收多少" | get_financial_data |
| COMPARISON | "对比北京和上海门店业绩" | get_multi_store_financial |
| TREND_ANALYSIS | "分析北京门店销售趋势" | get_financial_trend |
| ANOMALY_DIAG | "为什么客流突然下降" | get_financial_data + get_inventory_status |
| RECOMMENDATION | "如何提升业绩" | get_financial_data + get_inventory_status |

## 项目结构

```
hxy-agent/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/.../
│   │   ├── config/             # 配置类
│   │   ├── controller/         # API控制器
│   │   ├── entity/             # 实体类
│   │   │   ├── IntentType.java # 意图类型枚举
│   │   │   ├── IntentResult.java
│   │   │   ├── TimeRange.java
│   │   │   └── TimeUnit.java
│   │   ├── model/              # 数据模型
│   │   │   ├── FinancialData.java
│   │   │   ├── CustomerFlowData.java
│   │   │   ├── InventoryStatus.java
│   │   │   ├── ToolCallRequest.java
│   │   │   └── ToolCallResult.java
│   │   ├── service/            # 核心服务
│   │   │   ├── StoreAnalysisAgent.java  # 主智能体(编排核心)
│   │   │   ├── IntentRouter.java        # 意图路由器
│   │   │   ├── ToolRegistry.java        # 工具注册与执行
│   │   │   └── MockDataService.java     # 模拟数据(在Tool中调用)
│   │   └── tool/               # 工具类
│   │       ├── FinancialDataTool.java   # 财务数据工具
│   │       ├── InventoryTool.java       # 库存数据工具
│   │       ├── TimeParserTool.java      # 时间解析工具
│   │       └── StoreCodeResolverTool.java # 门店编码工具
│   └── src/main/resources/
│       └── application.yml
├── frontend/                   # Vue前端
│   ├── src/
│   │   ├── App.vue
│   │   ├── components/ChartRenderer.vue
│   │   └── api/agentApi.js
│   └── package.json
└── README.md
```

## 启动步骤

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务地址：http://localhost:8080

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务地址：http://localhost:3000

## API测试示例

```bash
# 测试数据查询(调用get_financial_data工具)
curl -X POST http://localhost:8080/api/agent/analyze \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-001" \
  -d '{"query": "北京门店上个月的财务情况怎么样"}'

# 测试对比分析(调用get_multi_store_financial工具)
curl -X POST http://localhost:8080/api/agent/analyze \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-002" \
  -d '{"query": "对比北京和上海门店上个月的业绩"}'

# 测试趋势分析(调用get_financial_trend工具)
curl -X POST http://localhost:8080/api/agent/analyze \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-003" \
  -d '{"query": "分析北京门店最近三个月的销售趋势"}'
```

## 响应结构

```json
{
  "analysis": "Markdown格式的分析报告",
  "charts": [
    {
      "type": "line/bar",
      "title": "图表标题",
      "xAxis": [...],
      "series": [...]
    }
  ],
  "data": {
    "get_financial_data": {...},    // 工具返回的原始数据
    "get_multi_store_financial": [...]
  },
  "suggestions": [...]
}
```

## 技术栈

**后端:**
- Spring Boot 3.3.5
- 阿里云DashScope SDK 2.12.0
- Lombok

**前端:**
- Vue 3.4
- Vite 5.0
- ECharts 5.5