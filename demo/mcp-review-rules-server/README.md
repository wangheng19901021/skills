# mcp-review-rules-server

把团队的 AI 代码审查规则库包装成 MCP Server 对外提供查询工具的 demo。
本工程是系列三第 2 篇《Java 手写第一个 MCP Server》的配套 demo。

规则库全貌：五关共 41 条规则（并发安全关 9 条、事务关 8 条、安全关 11 条、空指针关 7 条、需求评审关 6 条），
其中 13 条由线上事故炸出、28 条由架构推演先立。本 demo 内置 3 条完整示例规则，
完整 41 条见本仓库根目录的 `code-review-gate/` Skill。

## 版本组合

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | Spring AI 2.0 要求 Java 17+，推荐 21 |
| Spring Boot | 4.1.1 | Spring AI 2.0.x 官方兼容 Boot 4.0.x / 4.1.x |
| Spring AI | 2.0.1 | 最新 GA（`spring-ai-bom` 导入） |
| MCP starter | `spring-ai-starter-mcp-server-webmvc` | 版本由 BOM 管理 |
| MCP JavaScript SDK | 1.30.0 | `@modelcontextprotocol/sdk` |
| Maven | 3.9.x | 国内建议在 settings.xml 配阿里云镜像 |

> 注：Spring AI 2.0 起 SSE 传输已标记 deprecated（官方推荐 STREAMABLE，端点 `POST /mcp`）。
> 本 demo 为配合经典的 `SSEClientTransport` 客户端写法，显式设置
> `spring.ai.mcp.server.protocol: SSE`，端点仍为 `http://localhost:8080/sse`。

## 目录结构

```
mcp-review-rules-server/
├── pom.xml
├── run-demo.sh                    # 一键复跑脚本（启动+验证+清理）
├── src/main/java/com/example/reviewrules/
│   ├── McpReviewRulesApplication.java   # 主类
│   ├── ReviewRulesService.java          # 3 个 @Tool 工具方法 + 内置规则数据
│   └── McpServerConfig.java             # ToolCallbackProvider bean
├── src/main/resources/application.yml   # 端口/协议/服务名配置
└── client/
    ├── package.json
    ├── client.mjs                 # SSEClientTransport 客户端
    └── demo-output.txt            # 真实运行输出（截图素材）
```

## 复跑步骤（Windows + Git Bash）

前置：JDK 21+、Maven 3.9+、Node 18+ 已正常安装（Maven 建议配阿里云镜像，npm 可用 npmmirror 源）。

```bash
# 1. 构建
mvn -q -DskipTests package

# 2. 安装客户端依赖
cd client && npm install @modelcontextprotocol/sdk && cd ..

# 3. 一键启动 + 验证 + 清理（也可手动分步：java -jar target/*.jar 另开窗口跑 node client/client.mjs）
bash run-demo.sh
```

## 三个工具及示例输出

### listReviewRules()
```
团队 AI 代码审查规则库 · 目录总览
================================
一、并发安全关   9 条
二、事务关       8 条
三、安全关      11 条
四、空指针关     7 条
五、需求评审关   6 条
--------------------------------
合计 41 条：13 条线上事故炸出，28 条架构推演先立。
```

### getReviewRule("ssrf-metadata")（节选）
```
规则 ssrf-metadata（安全关 · SSRF 白名单必须拦内网段与云元数据接口）
来源：推演立规
……10.0.0.0/8、172.16.0.0/12、192.168.0.0/16、
169.254.169.254（云元数据接口！Capital One 泄露打的就是它）……
```

### searchReviewRules("事务")
```
关键词 [事务] 命中：
· tx-rpc-after-commit（事务关）事务里调 RPC 不可回滚 —— 来源：推演立规，未真炸，后拦回一次
```

完整真实运行输出见 [client/demo-output.txt](client/demo-output.txt)。

## 踩坑记录（文章素材）

1. **版本线已换代**：Spring AI 最新 GA 为 2.0.1（2026-06 发布），官方兼容 Spring Boot 4.0.x/4.1.x +
   Spring Framework 7 + Jackson 3，不再支持 Boot 3.x。本 demo 用 Boot 4.1.1 + Spring AI 2.0.1 + JDK 21。
2. **SSE 协议已 deprecated**：Spring AI 2.0 默认推荐 STREAMABLE（端点 `POST /mcp`），SSE 需显式配置
   `spring.ai.mcp.server.protocol: SSE`，`/sse` + `/mcp/message` 端点仍然可用。
3. **`Started ...` 日志"消失"之谜**：Boot 的启动完成日志 logger 名是**主类全名**
   （`LogFactory.getLog(mainApplicationClass)`，见 `SpringApplication.getApplicationLog()`）。
   把 `logging.level.root` 调成 `warn` 后若不放行自己的包，这行 INFO 日志会被吞掉——但服务其实
   已正常启动（jstack 可见 `DestroyJavaVM` 线程，`/sse` 返回 200）。修复：放行 `com.example.reviewrules: info`。
4. **String 返回值被 JSON 序列化**：Spring AI 2.0 的 `DefaultToolCallResultConverter` 对 String 也走
   `JsonHelper.toJson()`，客户端收到带引号和 `\n` 转义的文本。用 `@Tool(resultConverter = ...)` 自定义
   `PlainTextResultConverter` 原样返回（见 `PlainTextResultConverter.java`）。
5. **工具注册的双路径**：日志里 `No tool methods found in the provided tool objects: []` 的 WARN 来自
   2.0 新增的 `@McpTool` 注解扫描器（没扫到注解方法，属正常）；我们的 `@Tool` 方法经
   `ToolCallbackProvider` bean 路径注册，`Registered tools: 3` 为证。
6. **Windows + Git Bash 后台化陷阱**：`cd ... && java -jar ... &` 会把整条 `&&` 链后台化，
   `kill $PID` 杀的是子 shell 而非 java，导致 8080 端口残留。`run-demo.sh` 中 java 单独一行 `&` 解决。
