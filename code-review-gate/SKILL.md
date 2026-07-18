---
name: code-review-gate
description: Use when generating or reviewing Java backend code (Service/Controller/Mapper) that touches a database, Redis, MQ, RPC, SMS/email, or file writes. Applies a 5-gate checklist (requirements review, concurrency safety, transaction boundaries, null-safety, security) distilled from real production incidents, so AI-generated code doesn't repeat known bugs.
---

# Code Review Gate

为 Java 后端项目提供基于"五关审查清单"的代码生成与审查能力。

## 适用场景
- 用户要求生成 Java 后端业务代码（Service、Controller、Mapper 等）
- 用户要求审查已有 Java 代码
- 代码涉及数据库、Redis、MQ、RPC、短信、邮件、文件写入等外部依赖

## 使用方式
1. 在生成或审查代码前，先加载本 Skill 的 `references/` 目录
2. 逐一对照以下五关检查代码：
   - 第一关：需求评审（references/requirements-review.md）
   - 第二关：并发安全（references/concurrency.md）
   - 第三关：事务边界（references/transaction.md）
   - 第四关：空指针防护（references/null-safety.md）
   - 第五关：安全检查卡（references/security.md）
   - 补充规则：手动忽略决策（references/ignore-decision.md）——紧急场景确需忽略 AI 警告时，必须先回答四个问题并回填 PR 评论模板
3. 每发现一处违反规则的地方，必须给出修改后的代码
4. 如果不确定项目中的"外部调用"范围或并发量级，必须向用户提问，禁止猜测

## 输出要求
- 每个业务方法必须附带简短注释，说明它守住了哪几关
- 所有外部调用必须明确标注 `afterCommit` 或 `非事务内`
- 所有查询集合禁止返回 null，统一返回空集合
- 绝对禁止使用裸的 `SELECT + UPDATE` 更新余额、库存、计数器类字段
