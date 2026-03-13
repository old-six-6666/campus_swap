<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { itemApi } from '@/api/modules/item'

const route = useRoute()
const item = ref(null)
const loading = ref(false)

// 有图列表：优先用 images，无则用 coverImage
const images = computed(() => {
  if (!item.value) return []
  if (item.value.images && item.value.images.length > 0) return item.value.images
  if (item.value.coverImage) return [item.value.coverImage]
  return []
})

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
    <template v-if="item">
      <el-row :gutter="24">
        <!-- 左栏：图片展示 -->
        <el-col :span="10">
          <!-- 多图时使用轮播 -->
          <el-carousel
            v-if="images.length > 1"
            height="360px"
            indicator-position="outside"
            class="image-carousel"
          >
            <el-carousel-item v-for="(src, idx) in images" :key="idx">
              <el-image :src="src" fit="contain" class="carousel-image" />
            </el-carousel-item>
          </el-carousel>
          <!-- 单图直接展示 -->
          <el-image
            v-else-if="images.length === 1"
            :src="images[0]"
            fit="contain"
            class="main-image"
          />
          <div v-else class="no-image">暂无图片</div>
        </el-col>

        <!-- 右栏：商品信息 -->
        <el-col :span="14">
          <h2 class="item-title">{{ item.title }}</h2>
          <p class="item-price">¥ {{ item.price }}</p>
          <el-tag class="item-category" size="small">{{ item.category }}</el-tag>
          <el-divider />
          <p class="item-desc">{{ item.description }}</p>
          <el-divider />
          <!-- 卖家信息 -->
          <div class="seller-info">
            <el-avatar :size="36" :src="item.sellerAvatar" />
            <span class="seller-name">{{ item.sellerNickname }}</span>
          </div>
          <el-button type="primary" size="large" class="contact-btn">联系卖家</el-button>
        </el-col>
      </el-row>

      <!-- 底部缩略图导航（3 张以上时展示） -->
      <div v-if="images.length > 2" class="thumb-row">
        <el-image
          v-for="(src, idx) in images"
          :key="idx"
          :src="src"
          fit="cover"
          class="thumb"
        />
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.item-detail {
  max-width: 1000px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.image-carousel {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 360px;
}

.main-image {
  width: 100%;
  max-height: 400px;
  border-radius: 8px;
}

.no-image {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  border-radius: 8px;
}

.item-title {
  font-size: 20px;
  margin-bottom: 8px;
}

.item-price {
  font-size: 24px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 8px;
}

.item-category {
  margin-bottom: 4px;
}

.item-desc {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .seller-name {
    font-size: 14px;
    color: #303133;
  }
}

.contact-btn {
  width: 160px;
}

.thumb-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;

  .thumb {
    width: 80px;
    height: 80px;
    border-radius: 4px;
    cursor: pointer;
    border: 2px solid transparent;

    &:hover {
      border-color: #409eff;
    }
  }
}
</style>
