<script setup>
import { ref, onMounted } from 'vue'
import { Search, Picture } from '@element-plus/icons-vue'
import { itemApi } from '@/api/modules/item'

const items = ref([])
const loading = ref(false)
const keyword = ref('')

async function fetchItems() {
  loading.value = true
  try {
    const data = await itemApi.getList({ keyword: keyword.value, page: 1, size: 20 })
    items.value = data.records || []
  } catch (error) {
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchItems)
</script>

<template>
  <div class="home-view">
    <!-- Hero 搜索区 -->
    <div class="hero-section">
      <div class="hero-text">
        <h1 class="hero-title">发现校园好物</h1>
        <p class="hero-sub">轻松交换，让闲置物品找到新主人</p>
      </div>
      <div class="search-wrap">
        <el-input
          v-model="keyword"
          placeholder="搜索闲置物品..."
          size="large"
          clearable
          class="search-input"
          @keyup.enter="fetchItems"
          @clear="fetchItems"
        >
          <template #append>
            <el-button :icon="Search" class="search-btn" @click="fetchItems" />
          </template>
        </el-input>
      </div>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="item-grid">
      <template v-if="items.length > 0">
        <RouterLink
          v-for="item in items"
          :key="item.id"
          :to="`/item/${item.id}`"
          class="item-card"
        >
          <div class="card-image-wrap">
            <el-image
              :src="item.coverImage"
              fit="cover"
              class="card-image"
              lazy
            >
              <template #error>
                <div class="image-placeholder">
                  <el-icon size="28"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div class="card-info">
            <p class="card-title">{{ item.title }}</p>
            <p class="card-price">¥ {{ item.price }}</p>
          </div>
        </RouterLink>
      </template>
      <div v-if="!loading && items.length === 0" class="empty-wrap">
        <el-empty description="暂无商品，快来发布第一件吧~" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.home-view {
  max-width: $max-content-width;
  margin: 0 auto;
}

/* ── Hero ─────────────────────────────── */
.hero-section {
  text-align: center;
  padding: 40px 20px 36px;
  background: linear-gradient(135deg, rgba(27,153,170,0.06) 0%, rgba(158,208,204,0.1) 100%);
  border-radius: $border-radius-lg;
  margin-bottom: 28px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: $primary;
  letter-spacing: $letter-spacing-wide;
  margin-bottom: 8px;
}

.hero-sub {
  font-size: 15px;
  color: $text-secondary;
  margin-bottom: 28px;
  letter-spacing: $letter-spacing-base;
}

.search-wrap {
  max-width: 560px;
  margin: 0 auto;
}

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 50px 0 0 50px !important;
    padding-left: 20px;
  }
  :deep(.el-input-group__append) {
    border-radius: 0 50px 50px 0 !important;
    background: $primary;
    border-color: $primary;
    .el-button {
      color: #fff;
      border-radius: 0 50px 50px 0 !important;
      padding: 0 20px;
      &:hover { background: $primary-dark; }
    }
  }
}

/* ── 商品网格 ─────────────────────────── */
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 18px;
  min-height: 200px;
}

.item-card {
  text-decoration: none;
  display: block;
  background: $bg-card;
  border-radius: $border-radius;
  overflow: hidden;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
  transition: $transition-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-md;
    border-color: $primary-light;
  }
}

.card-image-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  display: block;
  transition: transform 0.4s ease;

  .item-card:hover & { transform: scale(1.04); }
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-subtle;
  color: $text-secondary;
}

.card-info {
  padding: 12px 14px;
}

.card-title {
  font-size: 14px;
  color: $text-primary;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  letter-spacing: $letter-spacing-base;
}

.card-price {
  font-size: 16px;
  font-weight: 700;
  color: $warning;
  letter-spacing: 0.02em;
}

.empty-wrap {
  grid-column: 1 / -1;
  padding: 60px 0;
  text-align: center;
}

/* ── 响应式 ───────────────────────────── */
@media (max-width: 768px) {
  .hero-title { font-size: 24px; }
  .hero-sub   { font-size: 14px; }
  .item-grid  { grid-template-columns: repeat(2, 1fr); gap: 12px; }
}

@media (max-width: 480px) {
  .hero-section { padding: 28px 16px 24px; }
  .item-grid { gap: 10px; }
}
</style>