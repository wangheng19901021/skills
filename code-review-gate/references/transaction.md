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
