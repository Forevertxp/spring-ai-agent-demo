#!/bin/bash

echo "启动门店数据分析智能体..."

# 启动后端
echo "1. 启动后端服务..."
cd backend
mvn spring-boot:run &

# 等待后端启动
sleep 10

# 启动前端
echo "2. 启动前端服务..."
cd ../frontend
npm install
npm run dev &

echo ""
echo "========================================"
echo "服务启动完成！"
echo "后端地址: http://localhost:8080"
echo "前端地址: http://localhost:3000"
echo "========================================"
echo ""
echo "测试问题示例:"
echo "- 北京门店上个月的财务情况怎么样"
echo "- 对比北京和上海门店Q1业绩"
echo "- 分析北京门店近半年销售趋势"