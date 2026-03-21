# 校园闲置交换平台 (Campus Swap)

> 面向校园用户的闲置物品交换平台 | 前端：Vue 3 + Vite | 后端：Spring Boot 3

---

## 目录

- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [端口说明](#端口说明)
- [主要功能](#主要功能)
- [项目结构](#项目结构)
- [新功能开发指南](#新功能开发指南)
- [常见问题](#常见问题)

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.3 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0+ |
| 认证 | JWT（24小时有效期） |
| 邮件服务 | QQ SMTP |
| AI 集成 | DeepSeek API（广场问答功能） |
| 前端框架 | Vue 3.4 + Vite 5.1 |
| UI 组件库 | Element Plus 2.6 |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios |
| 搜索（可选） | Elasticsearch 8.x |

---

## 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Maven | 3.6+（或使用项目自带 `mvnw`） |
| Elasticsearch | 8.x（可选，用于搜索功能） |

---

## 快速启动

### 1. 数据库初始化

创建数据库：

```sql
CREATE DATABASE campus_swap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行项目根目录下的 SQL 文件导入表结构和初始数据：

```bash
mysql -u root -p campus_swap < database.sql
```

### 2. 配置后端

编辑 `src/main/resources/application.properties`，修改以下配置：

```properties
# 数据库连接（必填）
spring.datasource.username=root
spring.datasource.password=你的MySQL密码

# QQ邮箱授权码（可选，用于注册验证码）
spring.mail.username=你的QQ邮箱
spring.mail.password=QQ邮箱授权码

# DeepSeek AI（可选，用于广场 @问一问 功能）
deepseek.api.key=你的DeepSeek API Key
```

> 获取 QQ 邮箱授权码：QQ邮箱 → 设置 → 账户 → SMTP服务 → 生成授权码

### 3. 启动后端

```bash
# 方式一：Maven 命令
mvn spring-boot:run

# 方式二：项目自带 Maven Wrapper
./mvnw spring-boot:run

# 方式三：IDE 运行
# 打开 src/main/java/com/itcodai/campus_swap/CampusSwapApplication.java，运行 main 方法
```

后端启动后访问：`http://localhost:8080`

### 4. 启动前端

```bash
cd campus_swap_front

# 首次运行，安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：`http://localhost:5173`

> 前端的 `/api` 请求会自动代理到后端 8080 端口，两个服务都启动后才能联调。

---

## 端口说明

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| MySQL | localhost:3306 |
| Elasticsearch（可选） | http://localhost:9200 |

---

## 主要功能

- **用户系统**：注册/登录（邮箱验证码）、个人主页、JWT 认证
- **商品管理**：发布闲置物品、图片上传、分类浏览、关键词搜索
- **交换交易**：发起交换请求、交易状态跟踪、双方确认流程
- **广场社区**：发布帖子、多级评论回复、AI 问答（`@问一问`）
- **即时聊天**：用户间私信聊天
- **通知系统**：交易、评论、回复等消息通知
- **管理后台**：用户管理、内容审核、数据统计

---

## 构建生产包

```bash
# 后端打包
mvn clean package -DskipTests
# 输出：target/campus-swap-server-1.0.0.jar

# 前端打包
cd campus_swap_front
npm run build
# 输出：campus_swap_front/dist/
```

---

## 项目结构

```
campus_swap/                         ← 项目根目录
│
├── campus_swap_front/               ← 【前端】Vue3 项目
│   ├── index.html                   ← 应用入口 HTML
│   ├── vite.config.js               ← 构建/代理配置（改端口、代理在这里）
│   ├── package.json                 ← 前端依赖清单
│   │
│   └── src/                         ← 【所有前端代码写在这里】
│       ├── main.js                  ← 应用启动入口
│       ├── App.vue                  ← 根组件
│       │
│       ├── api/                     ← 所有后端接口调用写这里
│       │   ├── index.js             ← axios 实例（Token/错误已统一处理）
│       │   └── modules/             ← 按业务模块拆分
│       │       ├── user.js          ← 用户相关接口
│       │       └── item.js          ← 商品相关接口
│       │
│       ├── views/                   ← 页面组件（一个路由 = 一个文件）
│       │   ├── home/HomeView.vue
│       │   ├── auth/LoginView.vue
│       │   ├── item/ItemDetailView.vue
│       │   └── user/ProfileView.vue
│       │
│       ├── router/index.js          ← Vue Router 路由注册
│       ├── stores/                  ← Pinia 全局状态
│       ├── components/              ← 可复用的公共组件
│       ├── composables/             ← 可复用的逻辑函数
│       ├── utils/                   ← 纯工具函数
│       └── assets/styles/           ← 全局 SCSS 变量
│
└── src/                             ← 【后端】Spring Boot 项目
    └── main/
        ├── java/com/itcodai/campus_swap/
        │   ├── CampusSwapApplication.java   ← 后端启动入口
        │   ├── controller/          ← REST 接口层
        │   ├── service/             ← 业务逻辑层
        │   ├── mapper/              ← 数据库访问层（MyBatis-Plus）
        │   ├── entity/              ← 数据库表实体类
        │   ├── dto/                 ← 前端传入的数据格式
        │   ├── vo/                  ← 返回给前端的数据格式
        │   ├── common/              ← 统一响应、全局异常处理
        │   ├── config/              ← Spring 配置（跨域、拦截器等）
        │   └── utils/               ← 工具类（JWT 等）
        │
        └── resources/
            ├── application.properties  ← 应用配置（数据库、端口等）
            └── mapper/              ← MyBatis XML 映射文件
```

---

## 新功能开发指南

> 以"订单模块"为例，演示完整的新功能开发流程。

---

### 前端：新增一个 API 模块

**文件位置：** `campus_swap_front/src/api/modules/` 下新建文件

```js
// src/api/modules/order.js

import request from '@/api/index'

export const orderApi = {
  /** 创建订单 */
  create: (data) => request.post('/order/create', data),

  /** 查询我的订单列表 */
  getMyList: (params) => request.get('/order/my', { params }),

  /** 获取订单详情 */
  getDetail: (id) => request.get(`/order/${id}`),

  /** 取消订单 */
  cancel: (id) => request.post(`/order/${id}/cancel`),
}
```

---

### 前端：新增一个页面

**第一步：** 在 `src/views/` 下按业务分类新建 Vue 文件

```vue
<!-- src/views/order/OrderListView.vue -->

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api/modules/order'
import { usePagination } from '@/composables/usePagination'

const { loading, list, pagination, fetchData, onPageChange } = usePagination(
  (params) => orderApi.getMyList(params)
)

onMounted(fetchData)
</script>

<template>
  <div class="order-list-view">
    <h2>我的订单</h2>

    <el-table v-loading="loading" :data="list">
      <el-table-column prop="id" label="订单号" />
      <el-table-column prop="itemTitle" label="商品" />
      <el-table-column prop="status" label="状态" />
    </el-table>

    <el-pagination
      :total="pagination.total"
      :page-size="pagination.size"
      :current-page="pagination.page"
      @current-change="onPageChange"
    />
  </div>
</template>

<style scoped lang="scss">
.order-list-view {
  max-width: 900px;
  margin: 0 auto;
}
</style>
```

**第二步：** 在 `src/router/index.js` 中注册路由

```js
{
  path: 'orders',
  name: 'OrderList',
  component: () => import('@/views/order/OrderListView.vue'),
  meta: { requiresAuth: true },   // 需要登录才能访问
},
```

---

### 前端：新增一个全局状态

**文件位置：** `campus_swap_front/src/stores/` 下新建文件

```js
// src/stores/useOrderStore.js

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { orderApi } from '@/api/modules/order'

export const useOrderStore = defineStore('order', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    // unreadCount.value = await orderApi.getUnreadCount()
  }

  return { unreadCount, fetchUnreadCount }
})
```

---

### 后端：新增一个接口

按照 **entity → mapper → service → controller** 顺序逐层创建。

**第一步：** 创建数据库实体类 `entity/Order.java`

```java
package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buyerId;
    private Long itemId;

    /** 状态：0-待确认 1-交易中 2-已完成 3-已取消 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
```

**第二步：** 创建 Mapper `mapper/OrderMapper.java`

```java
package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 简单增删改查 MyBatis-Plus 已提供，这里只写复杂 SQL
}
```

**第三步：** 创建 Service 接口和实现

```java
// service/IOrderService.java
public interface IOrderService extends IService<Order> {
    void createOrder(Long buyerId, Long itemId);
}

// service/impl/OrderServiceImpl.java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Override
    public void createOrder(Long buyerId, Long itemId) {
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setItemId(itemId);
        order.setStatus(0);
        save(order);
    }
}
```

**第四步：** 创建 Controller `controller/OrderController.java`

```java
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping("/create")
    public Result<Void> create(@RequestParam Long itemId, HttpServletRequest request) {
        Long buyerId = (Long) request.getAttribute("userId"); // 从 Token 中取出用户 ID
        orderService.createOrder(buyerId, itemId);
        return Result.success();
    }
}
```

---

## 数据流说明

```
用户操作
  ↓
views/（页面）  调用 →  api/modules/（接口函数）
  ↓                         ↓
stores/（全局状态）      HTTP 请求（由 api/index.js 统一处理 Token 和错误）
                              ↓
                    后端 Controller  →  Service  →  Mapper  →  数据库
                              ↓
                    统一响应 Result{ code, message, data }
                              ↓
                    api/index.js 解包，只返回 data 给页面
```

---

## 常见问题

**Q: 前端页面空白或报网络错误？**
确认后端已启动，且 MySQL 连接配置正确。

**Q: 不需要 Elasticsearch？**
注释掉 `application.properties` 中的 `spring.elasticsearch.uris` 一行即可。

**Q: 邮件发送失败？**
检查 QQ 邮箱是否已开启 SMTP 服务，授权码是否正确（非 QQ 登录密码）。

**Q: 修改端口？**
- 后端端口：修改 `application.properties` 中的 `server.port`
- 前端端口：修改 `campus_swap_front/vite.config.js` 中的 `server.port`，同时更新代理目标地址

**Q: 文件上传大小限制？**
默认单文件最大 10MB，可在 `application.properties` 中调整 `spring.servlet.multipart` 配置。
