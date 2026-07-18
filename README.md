# 个人 Claude Code Skills 仓库

这里收集我日常工作中沉淀的 Claude Code Skills。

## 当前 Skill

### `code-review-gate`

面向 Java 后端项目的**五关代码审查 Skill**，把线上踩坑经验固化成 AI 编码规则。

**五关清单：**
1. 需求评审（requirements-review.md）
2. 并发安全（concurrency.md）
3. 事务边界（transaction.md）
4. 空指针防护（null-safety.md）
5. 安全检查卡（security.md）

**补充规则：**
- 手动忽略决策（ignore-decision.md）：紧急场景确需忽略 AI 审查警告时，必须先回答的四个决策问题，附决策速查表和可直接复制的 PR 评论模板

## 使用方法

1. 复制 `code-review-gate/` 目录到你的 Java 项目根目录：
   ```bash
   cp -r code-review-gate /path/to/your-project/.claude/skills/
   ```
2. 确保 Claude Code 已启用 Skill 自动加载
3. 在项目中让 AI 生成或审查 Java 代码时，会自动应用五关规则

## 维护原则

**踩一个坑，加一条规则。**

这个 Skill 不是一次性写完的静态文档，而是持续迭代的项目故障知识库。每遇到一次线上问题，就把对应的检查点补充到 `references/` 下的相应文件中。

## 相关文章

- [编码时间降了 65%，但我反而更焦虑了]
- [AI 30 秒写代码，我审 5 分钟：这 5 关不过，不敢合]
- [五关清单吃灰 3 周后，我让 AI 自己按我的规则写代码]
- [3个同事装上我的审查Skill后，PR里一半评论消失了：CR从找bug变成了做决策]
- [深夜11点，AI标了3条警告我全点了忽略：不是胆大，是答完了这4个问题]

## License

MIT
