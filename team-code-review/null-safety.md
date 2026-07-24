# 空指针：团队规则

> 本文件是 `code-review-gate/references/null-safety.md` 的团队扩展。

## MQ心跳包字段判空
- 规则: 消费平台MQ消息时，eventType 字段必须按可空处理（Optional 或显式判空），心跳包不带业务字段，不得假设 eventType 一定有值
- 来源: 小何, 2026-06-12
- 触发: 平台偶尔发心跳包，eventType 为空，线上因此炸过两次
