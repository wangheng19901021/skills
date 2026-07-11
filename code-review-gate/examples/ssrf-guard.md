# SSRF 防护正反例

## 场景
管理后台提供"根据外部 URL 下载文件"功能。

## 错误写法：直接使用用户传入 URL
```java
@GetMapping("/download")
public void download(@RequestParam String url, HttpServletResponse response) {
    byte[] bytes = restTemplate.getForObject(url, byte[].class);  // 危险！
    response.getOutputStream().write(bytes);
}
```

### 风险
- 用户可能传入 `http://127.0.0.1/actuator/env` 读取内网服务
- 可能访问 `http://10.0.0.1/` 等私有网段，导致内网信息泄露
- 可能作为攻击内网的跳板

## 正确写法：白名单 + 私有 IP 拦截
```java
private static final Set<String> ALLOWED_HOSTS = Set.of("trusted-cdn.com", "static.example.com");

@GetMapping("/download")
public void download(@RequestParam String url, HttpServletResponse response) {
    URI uri = URI.create(url);
    String host = uri.getHost();

    if (!ALLOWED_HOSTS.contains(host)) {
        throw new IllegalArgumentException("非法域名");
    }
    if (isPrivateIp(host)) {
        throw new IllegalArgumentException("禁止访问内网地址");
    }

    byte[] bytes = restTemplate.getForObject(uri, byte[].class);
    response.getOutputStream().write(bytes);
}

private boolean isPrivateIp(String host) {
    // 简单判断：10.x、172.16-31.x、192.168.x、127.x、localhost
    return host.equals("localhost")
        || host.startsWith("127.")
        || host.startsWith("10.")
        || host.startsWith("192.168.")
        || host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
}
```

### 关键点
1. 域名必须配置白名单
2. 必须拦截私有 IP 段和 localhost
3. 如果业务无法提供域名白名单，直接拒绝该需求，禁止在代码层绕过

## 进阶：IP 白名单优于域名白名单
如果安全要求更高，使用 DNS 解析后的 IP 再做白名单校验，防止 DNS 重绑定攻击。
