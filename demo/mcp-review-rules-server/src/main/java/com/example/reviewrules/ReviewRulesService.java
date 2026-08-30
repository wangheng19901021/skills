package com.example.reviewrules;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 团队 AI 代码审查规则库。
 * 五关 41 条规则的 demo 版：内置 3 条完整示例，其余以目录形式呈现。
 */
@Service
public class ReviewRulesService {

    private final Map<String, Rule> rules = new LinkedHashMap<>();

    public ReviewRulesService() {
        rules.put("concurrent-stock-deduct", new Rule(
                "concurrent-stock-deduct", "并发安全关", "库存扣减并发丢失更新",
                "2026-03 线上事故，超卖 11 单，排查三小时",
                """
                【适用条件】
                任何"读-算-写"三段式更新共享数据的代码：先 SELECT 出余额/库存/计数，
                在内存里加减，再 UPDATE 回去。典型场景：库存扣减、账户余额、优惠券核销。

                【决策树】
                1. 这条 UPDATE 依赖之前 SELECT 出来的值吗？
                   ├─ 不依赖 → 直接原子 UPDATE，收工
                   └─ 依赖 → 2
                2. 并发冲突概率高吗（同 sku 每秒 >1 次写）？
                   ├─ 高 → 原子 UPDATE ... WHERE stock >= ?，影响行数为 0 即失败
                   └─ 低 → 乐观锁 version 字段 + 失败重试 3 次，仍失败则报警

                【禁止写法】
                禁止裸"读-算-写"：SELECT stock → stock - 1 → UPDATE stock，
                中间没有任何并发控制。两个线程同时读到 5，都写回 4，就超卖了。

                【正反例】
                ✗ 反例：
                  int stock = skuDao.selectStock(skuId);
                  if (stock > 0) { skuDao.updateStock(skuId, stock - 1); }
                ✓ 正例（原子 UPDATE）：
                  int rows = skuDao.deductStock(skuId, 1);
                  // UPDATE sku SET stock = stock - 1 WHERE id = ? AND stock >= ?
                  if (rows == 0) { throw new OutOfStockException(); }
                ✓ 正例（乐观锁）：
                  for (int i = 0; i < 3; i++) {
                      Sku sku = skuDao.select(skuId);
                      if (skuDao.updateWithVersion(sku, sku.getStock() - 1) == 1) return;
                  }
                  throw new ConcurrentModifyException();
                """));

        rules.put("tx-rpc-after-commit", new Rule(
                "tx-rpc-after-commit", "事务关", "事务里调 RPC 不可回滚",
                "推演立规，未真炸，后拦回一次",
                """
                【适用条件】
                @Transactional 方法体内出现任何跨进程调用：HTTP 接口、Dubbo、
                消息发送、第三方 SDK。数据库事务回滚不了别的系统的状态。

                【决策树】
                1. 这个调用必须在事务内拿结果才能继续吗？
                   ├─ 是 → 它只读吗？只读可留，写操作必须挪走
                   └─ 否 → 2
                2. 调用失败时业务允许"先提交后发通知失败"吗？
                   ├─ 允许 → afterCommit 阶段调用，失败记日志 + 补偿任务
                   └─ 不允许 → 先发可靠消息（本地消息表/事务消息），消费者再调 RPC

                【禁止写法】
                禁止在 @Transactional 方法内直接调用外部写接口。
                RPC 已发出、对方已扣款，本地事务回滚，两边状态永久不一致。

                【正反例】
                ✗ 反例：
                  @Transactional
                  public void createOrder(Order order) {
                      orderDao.insert(order);
                      couponClient.useCoupon(order.getCouponId()); // RPC 不可回滚！
                  }
                ✓ 正例（afterCommit）：
                  TransactionSynchronizationManager.registerSynchronization(
                      new TransactionSynchronization() {
                          @Override public void afterCommit() {
                              couponClient.useCoupon(order.getCouponId());
                          }
                      });
                ✓ 正例（可靠消息）：
                  orderDao.insert(order);
                  outboxDao.insert(new UseCouponMessage(...)); // 同事务落库，异步投递
                """));

        rules.put("ssrf-metadata", new Rule(
                "ssrf-metadata", "安全关", "SSRF 白名单必须拦内网段与云元数据接口",
                "推演立规",
                """
                【适用条件】
                任何由用户传入 URL、由服务端发起请求的代码：头像抓取、网页摘要、
                webhook 回调、图片转存。攻击面是服务端代替攻击者访问内网。

                【决策树】
                1. URL 是用户可控的吗？
                   ├─ 否 → 走普通 HTTP 审查
                   └─ 是 → 2
                2. 白名单校验覆盖了哪些目标？
                   ├─ 只拦 127.0.0.1 → 不合格，必须全量拦截：
                   │   10.0.0.0/8、172.16.0.0/12、192.168.0.0/16、
                   │   169.254.169.254（云元数据接口！Capital One 泄露打的就是它）、
                   │   [::1]、0.0.0.0，以及 DNS 重绑定（解析后再校验一次）
                   └─ 域名白名单 + 解析后 IP 二次校验 → 合格

                【禁止写法】
                禁止只拦 127.0.0.1。云上 SSRF 的头号目标是 169.254.169.254，
                拿到临时凭证等于拿到整台机器的权限。也禁止只校验域名不校验解析结果。

                【正反例】
                ✗ 反例：
                  if (url.contains("127.0.0.1")) { throw new IllegalArgumentException(); }
                  httpGet(url); // 169.254.169.254/latest/meta-data/iam/ 畅通无阻
                ✓ 正例：
                  InetAddress addr = InetAddress.getByName(host); // 先解析
                  if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                          || addr.isLinkLocalAddress()           // 169.254.0.0/16
                          || !domainWhitelist.contains(host)) {
                      throw new SecurityException("SSRF blocked: " + host);
                  }
                """));
    }

    @Tool(name = "listReviewRules", description = "列出团队 AI 代码审查规则库的完整目录：五关各多少条、规则总量、来源构成", resultConverter = PlainTextResultConverter.class)
    public String listReviewRules() {
        return """
                团队 AI 代码审查规则库 · 目录总览
                ================================
                一、并发安全关   9 条
                二、事务关       8 条
                三、安全关      11 条
                四、空指针关     7 条
                五、需求评审关   6 条
                --------------------------------
                合计 41 条：13 条线上事故炸出，28 条架构推演先立。

                说明：本 demo 内置 3 条示例规则（concurrent-stock-deduct /
                tx-rpc-after-commit / ssrf-metadata），可用 getReviewRule 按 id 查询全文，
                或用 searchReviewRules 按关键词搜索。完整 41 条见 github.com/wangheng19901021/skills
                """;
    }

    @Tool(name = "getReviewRule", description = "按规则 id 获取单条审查规则全文，四段式：适用条件、决策树、禁止写法、正反例", resultConverter = PlainTextResultConverter.class)
    public String getReviewRule(
            @ToolParam(description = "规则 id，如 concurrent-stock-deduct、tx-rpc-after-commit、ssrf-metadata") String ruleId) {
        Rule rule = rules.get(ruleId);
        if (rule == null) {
            return "未找到规则 [" + ruleId + "]。本 demo 可用 id：" +
                    rules.keySet().stream().collect(Collectors.joining(", ")) +
                    "。完整 41 条见 github.com/wangheng19901021/skills";
        }
        return "规则 " + rule.id() + "（" + rule.gate() + " · " + rule.title() + "）\n"
                + "来源：" + rule.origin() + "\n\n" + rule.body();
    }

    @Tool(name = "searchReviewRules", description = "按关键词模糊搜索审查规则，返回命中规则的 id、所属关卡和摘要", resultConverter = PlainTextResultConverter.class)
    public String searchReviewRules(
            @ToolParam(description = "搜索关键词，如：库存、事务、SSRF、RPC") String keyword) {
        String kw = keyword.toLowerCase();
        String hits = rules.values().stream()
                .filter(r -> (r.id() + r.gate() + r.title() + r.body()).toLowerCase().contains(kw))
                .map(r -> "· " + r.id() + "（" + r.gate() + "）" + r.title() + " —— 来源：" + r.origin())
                .collect(Collectors.joining("\n"));
        if (hits.isEmpty()) {
            return "关键词 [" + keyword + "] 在 demo 内置的 3 条示例规则中无命中。"
                    + "完整 41 条见 github.com/wangheng19901021/skills";
        }
        return "关键词 [" + keyword + "] 命中：\n" + hits + "\n\n用 getReviewRule(id) 查看全文。";
    }

    private record Rule(String id, String gate, String title, String origin, String body) {
    }
}
