# 个人 Claude Code Skills 仓库

> 📖 配套电子书《Java 团队的 AI 代码审查实战》已上架：41 条规则逐条注释 + 11 个线上 bug 完整复盘（95 页）→ https://mbd.pub/o/bread/YZaVmphpZA==

## English

A curated set of Claude Code Skills for Java backend teams adopting AI-assisted coding:

- **code-review-gate** — a five-gate code review skill (concurrency / transaction / security / NPE / requirement review) built from 41 rules: 13 distilled from real production incidents, 28 from architecture review. Measured over 5 months on a 4-person Java team where AI writes the code: online bugs 11/month → 2/month, PR lead time 32h → 9h.
- **team-code-review** — team-level rules template; each rule records its origin (who added it, which incident). Fork and replace with your own scars.
- **demo/** — a practice MCP Server (Spring AI) with a one-command rerun script, for comparing Skill vs MCP overhead hands-on.

Companion ebook (Chinese): 《Java 团队的 AI 代码审查实战》 → https://mbd.pub/o/bread/YZaVmphpZA==

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

### `team-code-review`

`code-review-gate` 的**团队级规则扩展模板**。个人 Skill 放 `~/.claude/skills/` 跟着人走，团队规则放项目 `.claude/skills/` 跟着仓库走，Claude Code 两个位置都会扫描，天然同时生效。

每条规则标注来源（谁加的、踩了什么坑、什么时候加的），规则打架时找当事人聊。fork 之后替换成你们团队自己的踩坑记录即可。

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

## 配套 Demo

### `demo/mcp-review-rules-server`

把 41 条规则库包装成 MCP Server 的 Spring AI 实操 demo（Spring Boot 4.1.1 + Spring AI 2.0.1 + JDK 21），
内置 3 条完整示例规则，含一键复跑脚本和真实运行输出，是《Java 手写第一个 MCP Server》一文的配套工程。
详见其 [README](demo/mcp-review-rules-server/README.md)。

## 相关文章

规则库里每条规则背后的真实事故，都写成了复盘文章。

**AI 代码审查五关·实战系列（公众号「硅基书斋」，已收官）**

- [五个月，11 个线上 bug，41 条规则：我把团队的 AI 审查 Skill 开源了](https://mp.weixin.qq.com/s/Sz8a5S9ln2Ff_rGYDzYldg)
- 并发关：[7 个并发 bug 其实是同一个 bug：我把堵住它的 9 条规则开源了](https://mp.weixin.qq.com/s/MtEcmKxIeFKETnG6XZ5cbw)
- 事务关：[事务里调 RPC 的两种死法：一种我们真炸过，一种没炸但规则先立了](https://mp.weixin.qq.com/s/Od_N34fFl9Gz2K3qiYVy3g)
- 安全关：[一次没炸过，却立了最多的 11 条规则：AI 代码的安检卡](https://mp.weixin.qq.com/s/3SXMaQHKKB7OqCVjHZv2tA)
- 团队关：[3 条真规则，好过 41 条抄来的：团队 AI 审查规则库的搭法和活法](https://mp.weixin.qq.com/s/0rZ-kiJzZWjqjE8MVbDeEQ)

**Skill × MCP 实战系列（更新中）**

- [Skill 和 MCP 到底什么区别？Java 后端一篇讲透](https://blog.csdn.net/u010567709/article/details/164043833)
- Java 手写第一个 MCP Server：Spring AI MCP 半小时跑通（demo 即本仓库 `demo/mcp-review-rules-server`）——[知乎专栏版](https://zhuanlan.zhihu.com/p/2078763930764174335)｜[CSDN 版见作者主页「实战skill」专栏](https://blog.csdn.net/u010567709)
- 知乎 4.3 万阅读回答：[vibe coding 如果长期使用，会不会有一天出现无法解决的 bug？](https://www.zhihu.com/question/2004911938891715573/answer/2078128129848112627)

更早的「AI 协作实验室」6 篇复盘（bug 从月均 11 降到 2 的五个月全程）见 [CSDN 作者主页](https://blog.csdn.net/u010567709)。

## License

MIT
