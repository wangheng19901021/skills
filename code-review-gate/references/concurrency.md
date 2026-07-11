# 并发安全规则

## 适用条件
当代码中出现以下模式时，必须进行并发安全检查：
- 从数据库 / Redis / 内存读取一个值
- 对该值做加减乘除或状态变更
- 将新值写回数据源

## 检查决策树
1. 不同用户操作不同数据 → 无数据竞争，不需要锁，但需防重复提交
2. 同一份数据、并发量低 → 用 SELECT FOR UPDATE 行锁
3. 同一份数据、并发量高（秒杀场景）→ 用 Redis DECR / Lua 原子操作，异步落库

## 禁止写法
- 不允许使用裸的 SELECT + UPDATE（非原子）更新余额、库存、计数器类字段
- 不明确并发量的场景，先上行锁，加监控看重试率再调整

## 行锁示例（SELECT FOR UPDATE）
注意：必须在 @Transactional 方法内调用，否则 FOR UPDATE 不会持有到事务结束。

```java
// Mapper 层：加锁查询
@Select("SELECT * FROM account WHERE id = #{id} FOR UPDATE")
Account lockAndSelect(@Param("id") Long id);

// Service 层：事务内使用
@Transactional
public void deductPoints(Long userId, Integer amount) {
    Account account = accountMapper.lockAndSelect(userId);
    if (account.getPoints() < amount) {
        throw new BusinessException("积分不足");
    }
    accountMapper.deductPoints(userId, amount);
}
```

## 乐观锁示例（version 字段）
```java
// Mapper 层：带版本号的 CAS 更新
@Update("UPDATE account SET points = points - #{amount}, version = version + 1 " +
        "WHERE id = #{id} AND version = #{version}")
int deductWithVersion(@Param("id") Long id,
                      @Param("amount") Integer amount,
                      @Param("version") Integer version);

// Service 层：重试兜底
@Transactional
public void deductPoints(Long userId, Integer amount) {
    for (int i = 0; i < 3; i++) {
        Account account = accountMapper.selectById(userId);
        if (account.getPoints() < amount) {
            throw new BusinessException("积分不足");
        }
        int affected = accountMapper.deductWithVersion(
            userId, amount, account.getVersion());
        if (affected == 1) return;
    }
    throw new BusinessException("扣减冲突，请重试");
}
```
