# 并发安全正反例

## 场景
用户积分扣减：读取当前积分，判断余额，更新积分。

## 错误写法：裸 SELECT + UPDATE
```java
@Transactional
public void deductPoints(Long userId, Integer amount) {
    User user = userMapper.selectById(userId);        // 读取
    if (user.getPoints() < amount) {                  // 计算/判断
        throw new BusinessException("积分不足");
    }
    user.setPoints(user.getPoints() - amount);        // 修改
    userMapper.updateById(user);                      // 写回
}
```

### 风险
两个并发请求同时读到 `points = 100`，各扣 10 分，都写回 `points = 90`，实际应扣到 80。MySQL 默认 RR 隔离级别下，普通 SELECT + UPDATE 存在读写窗口。

## 正确写法一：SELECT FOR UPDATE 行锁
```java
@Select("SELECT * FROM account WHERE id = #{id} FOR UPDATE")
Account lockAndSelect(@Param("id") Long id);

@Transactional
public void deductPoints(Long userId, Integer amount) {
    Account account = accountMapper.lockAndSelect(userId);
    if (account.getPoints() < amount) {
        throw new BusinessException("积分不足");
    }
    accountMapper.deductPoints(userId, amount);
}
```

### 关键点
- 行锁必须在 @Transactional 方法内获取
- 锁会持有到事务提交或回滚才释放

## 正确写法二：乐观锁（version 字段）
```java
@Update("UPDATE account SET points = points - #{amount}, version = version + 1 " +
        "WHERE id = #{id} AND version = #{version}")
int deductWithVersion(@Param("id") Long id,
                      @Param("amount") Integer amount,
                      @Param("version") Integer version);

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

### 选择建议
- 并发量低、冲突少 → 乐观锁
- 并发量不确定 → 先上行锁，加监控看重试率再调整
- 高并发秒杀场景 → Redis 原子预扣，异步落库
