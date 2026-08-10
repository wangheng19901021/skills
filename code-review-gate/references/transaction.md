# 事务边界规则

## 核心原则
不要在事务提交之前，做那些事务回滚时撤不回来的事。

## 哪些操作属于"外部调用"
- RPC / HTTP 调用
- MQ 发送
- Redis 写入 / 删除
- 短信、邮件、推送
- 文件写入（NAS、OSS、本地磁盘）

## 禁止写法
```java
@Transactional
public void updateUserPhone(UserDTO dto) {
    userMapper.updateById(dto);                       // 可回滚
    smsService.sendVerifyCode(dto.getNewPhone());     // 不可回滚！危险
}
```

## 正确写法：afterCommit 回调
```java
@Transactional
public void updateUserPhone(UserDTO dto) {
    userMapper.updateById(dto);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                smsService.sendVerifyCode(dto.getNewPhone());
            }
        }
    );
}
```

## 复杂场景：本地事件表
如果消息不能丢，先把事件落本地事件表，事务提交后由后台任务扫表投递。
不要在同一段代码里同时处理"数据库提交"和"消息投递"两个生命周期。

## 规则清单（8 条）
- R3-1 事务提交前，禁止做任何回滚时撤不回来的动作（核心原则）（炸出——缓存一致性 bug）
- R3-2 不可回滚清单：RPC / HTTP 调用、MQ 发送、Redis 写入删除、短信邮件推送、文件写入（推演）
- R3-3 不可回滚动作必须挂 afterCommit，只在事务真正提交成功后执行（炸出）
- R3-4 禁止同类自调用：绕过 Spring 代理，@Transactional 直接失效（炸出——排查三小时）
- R3-5 事务内禁止 RPC / HTTP 调用：不只因为撤不回来，还因为它会把事务拉长到不可控（推演）
- R3-6 必须做的外部调用（风控、校验），挪到事务开始前做完，或改成提交后异步（推演）
- R3-7 消息丢不起的场景用本地事件表：事件随事务落库，提交后由后台任务扫表投递（推演）
- R3-8 锁是事务的一部分：FOR UPDATE 行锁必须握到事务结束才有效（与并发关 R2-4 联动）（推演）
