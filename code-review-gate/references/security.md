# 安全检查卡

## SQL 注入
搜 `$` 符号。凡是 `${}` 拼接前端参数的地方，必须改成白名单或改为 #{}。
```java
// 危险
@Select("select * from user order by ${sortField}")

// 正确：sortField 用白名单校验
private static final Set<String> ALLOWED_SORT = Set.of("id", "create_time");
if (!ALLOWED_SORT.contains(sortField)) throw new IllegalArgumentException();
```

## SSRF
看到 HttpClient / RestTemplate / OkHttp，先问：URL 是用户传的吗？
```java
// 危险
restTemplate.getForObject(url, byte[].class);

// 正确
URI uri = URI.create(url);
if (!ALLOWED_HOSTS.contains(uri.getHost())) throw new IllegalArgumentException();
if (isPrivateIp(uri.getHost())) throw new IllegalArgumentException();
```

## 日志脱敏
`log.info("user: {}", user)` 前确认 `toString()` 不会打印手机号、身份证、Token。
敏感字段加 @Mask 注解或覆盖 toString()。
```java
// 危险：Lombok @Data 自动生成的 toString() 原样打印所有字段
log.info("user: {}", user);

// 正确：覆盖 toString()，敏感字段脱敏后输出
@Override
public String toString() {
    return "User(id=" + id +
           ", mobile=" + MaskUtil.maskMobile(mobile) + ")";  // 138****5678
}
```

## 越权
看 userId 从哪来：是从 token / session 解出来的当前用户 ID，还是从请求参数里直接拿的？
```java
// 危险：任何登录用户都能查别人的订单
@GetMapping("/orders/{orderId}")
public Order getOrder(@PathVariable Long orderId) {
    return orderService.getById(orderId);
}

// 正确
@GetMapping("/orders/{orderId}")
public Order getOrder(@PathVariable Long orderId,
                      @AuthenticationPrincipal Long currentUserId) {
    Order order = orderService.getById(orderId);
    if (!order.getUserId().equals(currentUserId)) {
        throw new AccessDeniedException("无权访问");
    }
    return order;
}
```

## 管理后台下载 / 转发类接口
用户传入外部 URL 场景，必须同时满足：
1. 目标域名配置固定白名单
2. 拦截全部私有 IP 地址段（10.x、172.16-31.x、192.168.x、127.x）及 link-local 段（169.254.x）

如果业务无法提供域名白名单，直接拒绝该需求，禁止在代码层绕过安全限制。

## 规则清单（11 条，全部为复盘推演，零真实事故）
- R5-1 SQL 注入：凡是 `${}` 拼接前端参数的地方，必须改成白名单或改为 `#{}`
- R5-2 SSRF：用户可传入 URL 的接口，必须做 host 白名单校验
- R5-3 SSRF：白名单之外必须过滤私有 IP 段（10.x、172.16-31.x、192.168.x、127.x、localhost）
- R5-4 SSRF：必须额外拦截 link-local 段 169.254.0.0/16——云厂商元数据接口 169.254.169.254 是 SSRF 最经典的攻击目标（Capital One 泄露事故打的就是这个地址）
- R5-5 SSRF 高安全场景：用 DNS 解析后的 IP 做白名单校验，防 DNS 重绑定绕过
- R5-6 越权：userId 只能来自 token / session 解析，禁止从请求参数直接取
- R5-7 越权：按 ID 查数据必须校验资源归属当前用户
- R5-8 日志脱敏：打印对象前确认 toString() 不输出手机号、身份证、Token
- R5-9 管理后台下载 / 转发类接口：域名白名单 + 私有 IP 过滤双重校验，缺一不可
- R5-10 业务无法提供域名白名单时直接拒绝需求，禁止在代码层绕过安全限制
- R5-11 对外开放接口必须加签验签，并带时间戳防重放
