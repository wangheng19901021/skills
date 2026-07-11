# 空指针防护规则

## 只盯外部来源
重点检查从以下来源进入的字段：
- MQ 消息体
- HTTP 请求参数
- RPC 返回值
- 数据库查询结果里的关联字段

## 禁止写法
```java
// AI 容易写的
String userId = message.getUserId();
userService.process(userId);  // NPE：测试消息可能没有 userId

List<Order> orders = orderService.queryByUser(userId);
orders.stream().filter(...).collect(...);  // NPE：异常分支可能返回 null
```

## 正确写法
```java
// 外部字段用 Optional 编码可空语义
Optional.ofNullable(message.getUserId())
        .ifPresent(userService::process);

// Service 返回 List 不允许返回 null
List<Order> orders = orderService.queryByUser(userId);
orders = orders == null ? Collections.emptyList() : orders;

// 或者用 Optional 包装
return Optional.ofNullable(orderMapper.selectById(orderId));
```

## 校验策略
- 必填字段：用 @NotNull / @NotBlank
- 非必填字段：用 Optional 或显式默认值
- 不要给所有字段无脑判 null
