# 校园闲置交换平台 (Campus Swap)

> 前后端分离项目 | 前端：Vue3 + Vite | 后端：Spring Boot 3

---

## 目录

- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [新功能开发指南](#新功能开发指南)
  - [前端：新增一个页面](#前端新增一个页面)
  - [前端：新增一个 API 模块](#前端新增一个-api-模块)
  - [前端：新增一个全局状态](#前端新增一个全局状态)
  - [后端：新增一个接口](#后端新增一个接口)

---

## 项目结构

```
campus_swap/                         ← 项目根目录
│
├── campus_swap_front/               ← 【前端】Vue3 项目
│   ├── index.html                   ← 应用入口 HTML（一般不用改）
│   ├── vite.config.js               ← 构建/代理配置（改端口、代理在这里）
│   ├── package.json                 ← 前端依赖清单
│   │
│   └── src/                         ← 【所有前端代码写在这里】
│       ├── main.js                  ← 应用启动入口（注册插件在这里）
│       ├── App.vue                  ← 根组件（一般只放 <RouterView />）
│       │
│       ├── api/                     ← 【第一步】所有后端接口调用写这里
│       │   ├── index.js             ← axios 实例（Token/错误已统一处理，不用改）
│       │   └── modules/             ← 按业务模块拆分
│       │       ├── user.js          ← 用户相关接口
│       │       └── item.js          ← 商品相关接口
│       │           ↑ 新模块照着这两个文件的格式复制一份
│       │
│       ├── views/                   ← 【第二步】页面组件写这里（一个路由 = 一个文件）
│       │   ├── home/
│       │   │   └── HomeView.vue     ← 首页
│       │   ├── auth/
│       │   │   ├── LoginView.vue    ← 登录页
│       │   │   └── RegisterView.vue ← 注册页
│       │   ├── item/
│       │   │   ├── ItemDetailView.vue ← 商品详情
│       │   │   └── PublishView.vue    ← 发布商品
│       │   ├── user/
│       │   │   └── ProfileView.vue  ← 个人主页
│       │   └── NotFoundView.vue     ← 404 页面
│       │       ↑ 新页面按业务分子文件夹，命名规范：XxxView.vue
│       │
│       ├── router/
│       │   └── index.js             ← 【第三步】在这里注册新页面的路由
│       │
│       ├── stores/                  ← 全局状态（跨页面共享的数据放这里）
│       │   └── useUserStore.js      ← 用户登录状态、Token
│       │       ↑ 新 Store 命名规范：use[模块名]Store.js
│       │
│       ├── components/              ← 可复用的小组件（多个页面共用的零件）
│       │   └── （暂无，按需创建）
│       │
│       ├── composables/             ← 可复用的逻辑函数（use 开头）
│       │   └── usePagination.js     ← 分页逻辑封装
│       │
│       ├── utils/                   ← 纯工具函数（不含 Vue 的 JS 函数）
│       │   └── format.js            ← 日期、价格格式化
│       │
│       └── assets/
│           └── styles/
│               └── variables.scss   ← 全局 SCSS 变量（颜色、间距等）
│
│
└── src/                             ← 【后端】Spring Boot 项目
    └── main/
        ├── java/com/itcodai/campus_swap/
        │   │
        │   ├── CampusSwapApplication.java   ← 后端启动入口（不用改）
        │   │
        │   ├── controller/          ← 【第一步】接收前端请求，写接口在这里
        │   │   └── （新建 XxxController.java）
        │   │
        │   ├── service/             ← 【第二步】业务逻辑写这里
        │   │   ├── IXxxService.java ← 接口（定义方法签名）
        │   │   └── impl/
        │   │       └── XxxServiceImpl.java ← 实现类（写具体逻辑）
        │   │
        │   ├── mapper/              ← 【第三步】数据库操作写这里（MyBatis-Plus）
        │   │   └── XxxMapper.java
        │   │
        │   ├── entity/              ← 数据库表对应的 Java 类
        │   │   └── Xxx.java
        │   │
        │   ├── dto/                 ← 前端传给后端的数据格式（接收参数用）
        │   │   └── XxxDTO.java
        │   │
        │   ├── vo/                  ← 后端返回给前端的数据格式（响应数据用）
        │   │   └── XxxVO.java
        │   │
        │   ├── common/              ← 公共基础代码（已写好，一般不用改）
        │   │   ├── result/
        │   │   │   ├── Result.java      ← 统一响应格式 {code, message, data}
        │   │   │   └── ResultCode.java  ← 响应状态码枚举
        │   │   └── exception/
        │   │       ├── BusinessException.java       ← 业务异常（主动抛错用）
        │   │       └── GlobalExceptionHandler.java  ← 全局捕获异常（不用改）
        │   │
        │   ├── config/              ← 配置类（已写好，一般不用改）
        │   │   ├── CorsConfig.java        ← 跨域配置
        │   │   ├── MybatisPlusConfig.java ← 分页插件
        │   │   └── WebConfig.java         ← 拦截器注册
        │   │
        │   ├── interceptor/
        │   │   └── JwtInterceptor.java    ← JWT 登录验证拦截器（不用改）
        │   │
        │   └── utils/
        │       └── JwtUtils.java          ← JWT 工具类（不用改）
        │
        └── resources/
            ├── application.properties ← 【配置文件】数据库密码、端口在这里改
            └── mapper/               ← 复杂 SQL 的 XML 文件写这里（简单查询不需要）
```

---

## 快速启动

### 前端
```bash
cd campus_swap_front
npm install          # 第一次运行需要安装依赖
npm run dev          # 启动开发服务器，访问 http://localhost:5173
```

### 后端
1. 修改 `src/main/resources/application.properties`，填入数据库密码
2. 在 MySQL 中创建数据库 `campus_swap`
3. 用 IntelliJ IDEA 运行 `CampusSwapApplication.java`，或：
```bash
./mvnw spring-boot:run
```

> 前端的 `/api` 请求会自动代理到后端 `http://localhost:8080`，两个服务都启动后才能联调。

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

// 使用分页 composable，传入查询函数
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
      <el-table-column prop="price" label="金额" />
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
// 在 routes 数组的 MainLayout children 里添加：
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
  // 状态
  const unreadCount = ref(0)

  // 动作
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
// src/main/java/com/itcodai/campus_swap/entity/Order.java

package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buyerId;
    private Long itemId;
    private BigDecimal price;

    /** 状态：0-待确认 1-交易中 2-已完成 3-已取消 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除字段（MyBatis-Plus 自动处理） */
    @TableLogic
    private Integer deleted;
}
```

**第二步：** 创建 Mapper `mapper/OrderMapper.java`

```java
// src/main/java/com/itcodai/campus_swap/mapper/OrderMapper.java

package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 简单的增删改查 MyBatis-Plus 已提供，这里只写复杂 SQL
}
```

**第三步：** 创建 Service 接口和实现

```java
// src/main/java/com/itcodai/campus_swap/service/IOrderService.java

package com.itcodai.campus_swap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcodai.campus_swap.entity.Order;

public interface IOrderService extends IService<Order> {
    void createOrder(Long buyerId, Long itemId);
}
```

```java
// src/main/java/com/itcodai/campus_swap/service/impl/OrderServiceImpl.java

package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.entity.Order;
import com.itcodai.campus_swap.mapper.OrderMapper;
import com.itcodai.campus_swap.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Override
    public void createOrder(Long buyerId, Long itemId) {
        // 1. 校验商品是否存在
        // 2. 校验买家不能是自己
        // 3. 创建订单
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
// src/main/java/com/itcodai/campus_swap/controller/OrderController.java

package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.service.IOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    /** 创建订单（需要登录，拦截器会自动验证 Token） */
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
#   c a m p u s _ s w a p  
 