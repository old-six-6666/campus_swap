<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { itemApi } from '@/api/modules/item'

const route = useRoute()
const item = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    item.value = await itemApi.getDetail(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="item-detail">
    <el-empty v-if="!loading && !item" description="商品不存在" />
    <el-row v-if="item" :gutter="24">
      <el-col :span="10">
        <el-image :src="item.coverImage" fit="contain" class="main-image" />
      </el-col>
      <el-col :span="14">
        <h2 class="item-title">{{ item.title }}</h2>
        <p class="item-price">¥ {{ item.price }}</p>
        <el-divider />
        <p class="item-desc">{{ item.description }}</p>
        <el-button type="primary" size="large">联系卖家</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.item-detail {
  max-width: 1000px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;

  .main-image {
    width: 100%;
    max-height: 400px;
  }

  .item-title {
    font-size: 20px;
    margin-bottom: 8px;
  }

  .item-price {
    font-size: 24px;
    font-weight: 600;
    color: #f56c6c;
  }

  .item-desc {
    color: #606266;
    line-height: 1.8;
  }
}
</style>
