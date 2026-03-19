<script setup>
import { ref, onMounted, watch } from 'vue'
import { Search, Picture, Check } from '@element-plus/icons-vue'
import { itemApi } from '@/api/modules/item'

const items = ref([])
const loading = ref(false)
const keyword = ref('')
const selectedCategories = ref([])

// 分类选项
const categories = ['数码', '书籍', '服饰', '生活用品', '其他']

// 获取物品列表
async function fetchItems() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      category: selectedCategories.value.length > 0 ? selectedCategories.value.join(',') : undefined,
      page: 1,
      size: 20
    }
    const data = await itemApi.getList(params)
    items.value = data.records || []
  } catch (error) {
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 切换分类选择
function toggleCategory(category) {
  const index = selectedCategories.value.indexOf(category)
  if (index === -1) {
    selectedCategories.value.push(category)
  } else {
    selectedCategories.value.splice(index, 1)
  }
  fetchItems()
}

// 清空所有选择
function clearSelection() {
  selectedCategories.value = []
  fetchItems()
}

// 单选模式（点击时清空其他选择）
function selectSingle(category) {
  selectedCategories.value = [category]
  fetchItems()
}

onMounted(fetchItems)

// 监听搜索关键词变化
watch(keyword, () => {
  fetchItems()
})
</script>

<template>
  <div class="category-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>物品分类</h1>
      <p class="subtitle">浏览不同分类的闲置物品，支持多选或单选筛选</p>
    </div>

    <!-- 搜索栏 -->
    <el-row justify="center" class="search-bar">
      <el-col :span="16">
        <el-input
          v-model="keyword"
          placeholder="搜索闲置物品..."
          size="large"
          clearable
          class="search-input"
          @keyup.enter="fetchItems"
        >
          <template #suffix>
            <button class="search-icon-btn" @click="fetchItems">
              <el-icon><Search /></el-icon>
            </button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 分类筛选 -->
    <div class="category-filter">
      <div class="filter-header">
        <h3>分类筛选</h3>
        <div class="filter-actions">
          <el-button size="small" @click="clearSelection">清空选择</el-button>
          <el-tooltip content="按住Ctrl键可多选" placement="top">
            <el-tag type="info" size="small">提示：点击标签进行单选，按住Ctrl键可多选</el-tag>
          </el-tooltip>
        </div>
      </div>
      
      <div class="category-tags">
        <el-tag
          v-for="category in categories"
          :key="category"
          :type="selectedCategories.includes(category) ? 'primary' : 'info'"
          class="category-tag"
          :class="{ 'selected': selectedCategories.includes(category) }"
          @click="toggleCategory(category)"
          @click.ctrl="selectSingle(category)"
          size="large"
          effect="dark"
        >
          {{ category }}
          <el-icon v-if="selectedCategories.includes(category)" class="check-icon">
            <Check />
          </el-icon>
        </el-tag>
      </div>

      <!-- 已选分类提示 -->
      <div v-if="selectedCategories.length > 0" class="selected-info">
        <span>已选择：</span>
        <el-tag
          v-for="cat in selectedCategories"
          :key="cat"
          type="primary"
          size="small"
          closable
          @close="toggleCategory(cat)"
        >
          {{ cat }}
        </el-tag>
        <span class="count-text">共 {{ items.length }} 个物品</span>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="items-section">
      <h3 v-if="selectedCategories.length > 0">
        {{ selectedCategories.join('、') }} 分类的物品
      </h3>
      <h3 v-else>所有物品</h3>

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
              <el-image
                :src="item.coverImage"
                fit="cover"
                class="item-image"
                lazy
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon size="32"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="item-info">
                <p class="item-title">{{ item.title }}</p>
                <div class="item-meta">
                  <p class="item-price">¥ {{ item.price }}</p>
                  <el-tag size="small" class="item-category">{{ item.category }}</el-tag>
                </div>
              </div>
            </el-card>
          </RouterLink>
        </el-col>
        <div v-if="!loading && items.length === 0" class="empty-wrap">
          <el-empty description="暂无商品，快来发布第一件吧~" />
        </div>
      </el-row>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.category-view {
  max-width: $max-content-width;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 28px;
  padding: 32px 20px 28px;
  background: linear-gradient(135deg, rgba(27,153,170,0.06) 0%, rgba(158,208,204,0.1) 100%);
  border-radius: $border-radius-lg;

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: $primary;
    margin-bottom: 8px;
    letter-spacing: $letter-spacing-wide;
  }

  .subtitle {
    font-size: 14px;
    color: $text-secondary;
    letter-spacing: $letter-spacing-base;
  }
}

.search-bar { margin-bottom: 24px; }

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 50px !important;
    padding-right: 6px;
    box-shadow: 0 2px 12px rgba(27,153,170,0.12) !important;
    border: 1.5px solid rgba(27,153,170,0.25) !important;
    transition: box-shadow 0.2s, border-color 0.2s;

    &:hover, &.is-focus {
      border-color: $primary !important;
      box-shadow: 0 4px 18px rgba(27,153,170,0.22) !important;
    }
  }
}

.search-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: $primary;
  border: none;
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  transition: background 0.2s, transform 0.15s;
  flex-shrink: 0;

  &:hover {
    background: darken(#1B99AA, 8%);
    transform: scale(1.08);
  }

  &:active { transform: scale(0.96); }
}

.category-filter {
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 24px 28px;
  margin-bottom: 28px;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;

  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 10px;

    h3 {
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
      letter-spacing: $letter-spacing-base;
    }

    .filter-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }

  .category-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 18px;

    .category-tag {
      cursor: pointer;
      transition: $transition-fast;
      padding: 8px 22px;
      font-size: 14px;
      border-radius: 50px !important;
      border: 1.5px solid $border-color !important;
      background: $bg-subtle !important;
      color: $text-regular !important;
      letter-spacing: $letter-spacing-base;

      &:hover {
        transform: translateY(-2px);
        box-shadow: $shadow-sm;
        border-color: $primary !important;
        color: $primary !important;
      }

      &.selected {
        background: $primary !important;
        border-color: $primary !important;
        color: #fff !important;
        box-shadow: $shadow-sm;
      }

      .check-icon { margin-left: 6px; font-size: 12px; }
    }
  }

  .selected-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 14px;
    border-top: 1px solid $border-color;

    span { color: $text-secondary; font-size: 13px; }
    .count-text { margin-left: auto; color: $primary; font-weight: 500; }
  }
}

.items-section {
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 18px;
    padding-bottom: 12px;
    border-bottom: 2px solid rgba(27, 153, 170, 0.15);
    letter-spacing: $letter-spacing-base;
  }
}

.item-list { min-height: 200px; }

.empty-wrap {
  width: 100%;
  padding: 60px 0;
  text-align: center;
}

.item-card-link { text-decoration: none; }

.item-card {
  margin-bottom: 16px;
  cursor: pointer;
  border-radius: $border-radius !important;
  overflow: hidden;
  transition: $transition-base;

  &:hover { transform: translateY(-4px); box-shadow: $shadow-md !important; border-color: $primary-light !important; }

  .item-image {
    width: 100%;
    height: 160px;
  }

  .image-placeholder {
    width: 100%;
    height: 160px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $bg-subtle;
    color: $text-secondary;
  }

  .item-info {
    padding: 12px 2px 4px;

    .item-title {
      font-size: 13px;
      color: $text-primary;
      font-weight: 500;
      margin-bottom: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      letter-spacing: $letter-spacing-base;
    }

    .item-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;

      .item-price {
        font-size: 15px;
        font-weight: 700;
        color: $warning;
        margin: 0;
      }
    }
  }
}

@media (max-width: 768px) {
  .page-header { padding: 24px 16px 20px; h1 { font-size: 22px; } }
  .category-filter { padding: 16px; .filter-header { flex-direction: column; align-items: flex-start; } }
}
</style>