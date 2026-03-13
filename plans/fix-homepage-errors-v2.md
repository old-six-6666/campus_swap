# 首页报错修复方案（V2 - 基于实际错误日志）

## 问题分析

根据后端启动日志，真正的根因是：

### 核心问题：数据库连接失败

**错误日志**：
```
java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: YES)
```

**配置**：[`application.properties`](../src/main/resources/application.properties:7)
```
spring.datasource.url=jdbc:mysql://localhost:3306/campus_swap
spring.datasource.username=root
spring.datasource.password=He2004080922
```

**原因**：MySQL 密码不正确或 root 用户没有远程访问权限。

---

### 次要问题：Element Plus 导入冲突

前端 `v[w] is not a function` 错误，原因是 [`main.js`](../campus_swap_front/src/main.js) 全量导入与 [`vite.config.js`](../campus_swap_front/vite.config.js) 按需导入冲突。

---

### 次要问题：Promise 未捕获

[`HomeView.vue`](../campus_swap_front/src/views/home/HomeView.vue) 的 `fetchItems` 缺少 `catch` 块，导致 rejected promise 未被捕获。

---

## 修复步骤

### 步骤 1：修复数据库连接

**方案 A（推荐）**：更新 MySQL root 密码为 `He2004080922`

```sql
-- 在 MySQL 命令行中执行
ALTER USER 'root'@'localhost' IDENTIFIED BY 'He2004080922';
FLUSH PRIVILEGES;
```

**方案 B**：修改 [`application.properties`](../src/main/resources/application.properties) 使用正确的密码

```properties
spring.datasource.password=你的实际密码
```

**方案 C**：创建新用户并授权

```sql
CREATE USER 'campus_swap'@'localhost' IDENTIFIED BY 'CampusSwap123';
GRANT ALL PRIVILEGES ON campus_swap.* TO 'campus_swap'@'localhost';
FLUSH PRIVILEGES;
```

然后更新配置：
```properties
spring.datasource.username=campus_swap
spring.datasource.password=CampusSwap123
```

---

### 步骤 2：修复 Element Plus 导入冲突

修改 [`main.js`](../campus_swap_front/src/main.js)：

```javascript
// 删除以下三行：
// import ElementPlus from 'element-plus'
// import 'element-plus/dist/index.css'
// app.use(ElementPlus)

// 保持以下内容：
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

---

### 步骤 3：修复 Promise 未捕获

修改 [`HomeView.vue`](../campus_swap_front/src/views/home/HomeView.vue) 的 `fetchItems` 函数：

```javascript
async function fetchItems() {
  loading.value = true
  try {
    const data = await itemApi.getList({ keyword: keyword.value, page: 1, size: 20 })
    items.value = data.records || []
  } catch (error) {
    // 错误已由响应拦截器统一提示，此处静默处理
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}
```

---

## 验证步骤

1. **重启后端**：`mvn spring-boot:run`
2. **检查数据库连接**：确认没有 `Access denied` 错误
3. **测试 API**：浏览器访问 `http://localhost:8080/api/item/list` 应返回 JSON 数据
4. **启动前端**：`cd campus_swap_front && npm run dev`
5. **访问首页**：`http://localhost:5173` 应正常显示商品列表

---

## 故障排除

如果仍然有问题：

1. **检查 MySQL 服务是否运行**：
   ```bash
   # Windows
   net start mysql
   # 或
   services.msc 查看 MySQL 服务状态
   ```

2. **检查数据库是否存在**：
   ```sql
   SHOW DATABASES LIKE 'campus_swap';
   ```

3. **检查 t_item 表结构**：
   ```sql
   DESCRIBE t_item;
   -- 确认有 audit_status, audit_remark 字段
   ```

4. **检查是否有数据**：
   ```sql
   SELECT COUNT(*) FROM t_item WHERE audit_status = 1;
   -- 如果没有数据，首页会显示"暂无商品"
   ```
