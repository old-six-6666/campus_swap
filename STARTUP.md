# Campus Swap 配置与启动指南

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Elasticsearch | 8.x（可选，搜索功能） |

---

## 1. 数据库配置

创建数据库：

```sql
CREATE DATABASE campus_swap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改后端配置文件 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/campus_swap?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root        # 改为你的 MySQL 用户名
spring.datasource.password=yourpassword  # 改为你的 MySQL 密码
```

---

## 2. 邮件配置（可选，用于验证码）

在 `application.properties` 中修改：

```properties
spring.mail.username=your_qq@qq.com   # 你的 QQ 邮箱
spring.mail.password=your_auth_code   # QQ 邮箱授权码（非登录密码）
```

> 获取授权码：QQ邮箱 → 设置 → 账户 → SMTP服务 → 生成授权码

---

## 3. 启动后端

```bash
# 项目根目录下
mvn spring-boot:run

# 或使用 IDE 直接运行 CampusSwapApplication.java
```

后端启动后访问：`http://localhost:8080`

---

## 4. 启动前端

```bash
cd campus_swap_front

# 首次运行，安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：`http://localhost:5173`

---

## 5. 端口说明

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 | http://localhost:8080 |
| MySQL | localhost:3306 |
| Elasticsearch | http://localhost:9200 |

> 前端开发时 `/api` 请求会自动代理到后端 8080 端口，无需手动处理跨域。

---

## 常见问题

**Q: 前端页面空白或报网络错误？**
确认后端已启动，且 MySQL 连接配置正确。

**Q: 不需要 Elasticsearch？**
暂时注释掉 `application.properties` 中的 `spring.elasticsearch.uris` 一行即可。

**Q: 修改前端端口？**
编辑 `campus_swap_front/vite.config.js` 中的 `server.port`。

**Q: 修改后端端口？**
编辑 `application.properties` 中的 `server.port`，同时更新 `vite.config.js` 代理中的目标地址。
