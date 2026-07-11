# 缓存一致性正反例

## 场景
更新用户资料后，需要删除 Redis 中的用户缓存。

## 错误写法：在 @Transactional 方法体内直接删缓存
```java
@Transactional
public void updateUser(UserDTO dto) {
    userMapper.updateById(dto);                       // 数据库操作，可回滚
    redisTemplate.delete("user:" + dto.getId());      // 缓存删除，不可回滚！危险
}
```

### 风险
事务尚未提交时，缓存已被删除。其他请求可能在此期间读到旧数据并重新写入缓存，导致缓存永久脏读。

## 正确写法：事务提交后删除缓存
```java
@Transactional
public void updateUser(UserDTO dto) {
    userMapper.updateById(dto);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.delete("user:" + dto.getId());
            }
        }
    );
}
```

### 关键点
- `afterCommit()` 只在事务成功提交后执行
- 事务回滚时不会触发，避免缓存与数据库状态不一致
- 不存在同类自调用导致代理失效的问题

## 更复杂场景
如果缓存删除失败不能丢，使用本地事件表：
1. 事务内写入事件表
2. 后台任务扫描事件表并投递删除缓存消息
3. 数据库提交与缓存删除生命周期彻底隔离
