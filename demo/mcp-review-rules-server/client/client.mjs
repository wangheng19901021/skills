import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { SSEClientTransport } from "@modelcontextprotocol/sdk/client/sse.js";

const SSE_URL = "http://localhost:8080/sse";

console.log("[client] 连接 MCP Server: " + SSE_URL + " ...");

const transport = new SSEClientTransport(new URL(SSE_URL));
const client = new Client(
  { name: "review-rules-demo-client", version: "0.0.1" },
  { capabilities: {} }
);

await client.connect(transport);
console.log("[client] SSE 连接成功\n");

// 1) tools/list
const { tools } = await client.listTools();
console.log("[client] tools/list 结果：共 " + tools.length + " 个工具");
for (const t of tools) {
  console.log("  - " + t.name + " : " + (t.description ?? ""));
}
console.log();

// 2) callTool getReviewRule ssrf-metadata
const result = await client.callTool({
  name: "getReviewRule",
  arguments: { ruleId: "ssrf-metadata" }
});
const text = (result.content ?? [])
  .filter((c) => c.type === "text")
  .map((c) => c.text)
  .join("\n");
console.log('[client] callTool getReviewRule({ ruleId: "ssrf-metadata" }) 返回：');
console.log("------------------------------------------------------------");
console.log(text);
console.log("------------------------------------------------------------");

await client.close();
console.log("[client] 连接已关闭，demo 结束");
