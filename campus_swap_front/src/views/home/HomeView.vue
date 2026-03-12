<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { itemApi } from '@/api/modules/item'

const items = ref([])
const loading = ref(false)
const keyword = ref('')

async function fetchItems() {
  loading.value = true
  try {
    const data = await itemApi.getList({ keyword: keyword.value, page: 1, size: 20 })
    items.value = data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchItems)
</script>

<template>
  <div class="home-view">
    <!-- 搜索栏 -->
    <el-row justify="center" class="search-bar">
      <el-col :span="16">
        <el-input
          v-model="keyword"
          placeholder="搜索闲置物品..."
          size="large"
          clearable
          @keyup.enter="fetchItems"
        >
          <template #append>
            <el-button :icon="Search" @click="fetchItems" />
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 商品列表 -->
    <el-row v-loading="loading" :gutter="16" class="item-list">
      <el-col
        v-for="item in items"
        :key="item.id"
        :xs="12"
        :sm="8"
        :md="6"
        :lg="4"
      >
        <RouterLink :to="`/item/${item.id}`" class="item-card-link">
          <el-card shadow="hover" class="item-card">
            <el-image :src="item.coverImage" fit="cover" class="item-image" />
            <div class="item-info">
              <p class="item-title">{{ item.title }}</p>
              <p class="item-price">¥ {{ item.price }}</p>
            </div>
          </el-card>
        </RouterLink>
      </el-col>
      <el-empty v-if="!loading && items.length === 0" description="暂无商品" />
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.home-view {
  max-width: 1200px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 24px;
}

.item-list {
  min-height: 200px;
}

.item-card-link {
  text-decoration: none;
}

.item-card {
  margin-bottom: 16px;
  cursor: pointer;

  .item-image {
    width: 100%;
    height: 160px;
    border-radius: 4px;
  }

  .item-info {
    padding: 8px 0 0;

    .item-title {
      font-size: 14px;
      color: #303133;
      margin: 0 0 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-price {
      font-size: 16px;
      font-weight: 600;
      color: #f56c6c;
      margin: 0;
    }
  }
}
</style>
