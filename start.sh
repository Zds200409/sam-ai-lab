#!/bin/bash
echo "========================================"
echo " 山姆会员管理系统 - 一键启动"
echo "========================================"
echo ""

export JAVA_HOME="/c/Users/赵/.jdks/ms-17.0.18"
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "[1/2] 启动后端 (Spring Boot :8080)..."
(cd "$SCRIPT_DIR" && ./mvnw spring-boot:run -DskipTests) &
BACKEND_PID=$!

echo "[2/2] 启动前端 (Vite :5173)..."
(cd "$SCRIPT_DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "等待服务启动..."
sleep 8

echo "打开浏览器 http://localhost:5173"
start http://localhost:5173 2>/dev/null || xdg-open http://localhost:5173 2>/dev/null

echo ""
echo "========================================"
echo " 启动完成！"
echo " 前端: http://localhost:5173"
echo " 后端: http://localhost:8080"
echo " 登录: admin / admin123"
echo "========================================"
echo ""
echo "按 Ctrl+C 停止所有服务"
wait
