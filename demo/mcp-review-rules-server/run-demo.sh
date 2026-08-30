#!/usr/bin/env bash
# 一键复跑脚本：构建(可选) -> 启动 MCP Server -> 跑 Node 客户端 -> 留存证据 -> 清理进程
# 用法: bash run-demo.sh [--build]
# 前置：JDK 21+、Maven 3.9+、Node 18+ 已在 PATH 中（Windows 下用 Git Bash 运行）
set -u

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/jdk-21}"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")" || exit 1
MVN="${MVN:-mvn}"  # 如需阿里云镜像加速，可自备 settings.xml 后执行: MVN="mvn -s /path/to/settings.xml" bash run-demo.sh
JAR=target/mcp-review-rules-server-0.0.1.jar

if [ "${1:-}" = "--build" ] || [ ! -f "$JAR" ]; then
  echo ">>> mvn package ..."
  $MVN -q -DskipTests package || exit 1
fi

rm -f server.log client/demo-output.txt

java -jar "$JAR" > server.log 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null; sleep 1; taskkill //PID $PID //F 2>/dev/null' EXIT

echo ">>> 等待服务启动 (PID=$PID, 最多 90s) ..."
for i in $(seq 1 90); do
  grep -q "Started McpReviewRulesApplication" server.log 2>/dev/null && break
  sleep 1
done
if ! grep -q "Started McpReviewRulesApplication" server.log; then
  echo ">>> 启动失败，server.log 末尾:"
  tail -60 server.log
  exit 1
fi
echo ">>> 服务已启动"

{
  echo "===== 环境信息 ====="
  java -version 2>&1 | head -1
  echo "Spring Boot 4.1.1 + Spring AI 2.0.1, Maven 3.9.x"
  echo
  echo "===== 服务端启动日志摘录 ====="
  grep -E "Tomcat initialized|Apache Tomcat/|Registered tools|Started McpReviewRulesApplication" server.log
  echo
  echo "===== MCP 客户端运行输出 ====="
} | tee client/demo-output.txt

node client/client.mjs 2>&1 | tee -a client/demo-output.txt
CLIENT_EXIT=${PIPESTATUS[0]}

kill $PID 2>/dev/null; sleep 2; taskkill //PID $PID //F 2>/dev/null
trap - EXIT

echo ">>> 客户端退出码: $CLIENT_EXIT"
echo ">>> 证据已写入 client/demo-output.txt"
exit "$CLIENT_EXIT"
