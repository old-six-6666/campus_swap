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
          @keyup.enter="fetchItems"
        >
          <template #append>
            <el-button :icon="Search" @click="fetchItems" />
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
        <el-empty v-if="!loading && items.length === 0" description="暂无商品" />
      </el-row>
    </div>
  </div>
</template>

<style scoped lang="scss">
.category-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 32px;
    color: #333;
    margin-bottom: 8px;
  }

  .subtitle {
    font-size: 16px;
    color: #666;
  }
}

.search-bar {
  margin-bottom: 32px;
}

.category-filter {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      font-size: 18px;
      color: #333;
      margin: 0;
    }

    .filter-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .category-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 20px;

    .category-tag {
      cursor: pointer;
      transition: all 0.3s ease;
      padding: 8px 20px;
      font-size: 15px;
      border-radius: 20px;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      &.selected {
        background: linear-gradient(135deg, #409eff, #66b1ff);
        border-color: #409eff;
      }

      .check-icon {
        margin-left: 6px;
        font-size: 14px;
      }
    }
  }

  .selected-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 16px;
    border-top: 1px solid #e4e7ed;

    span {
      color: #666;
      font-size: 14px;
    }

    .count-text {
      margin-left: auto;
      color: #409eff;
      font-weight: 500;
    }
  }
}

.items-section {
  h3 {
    font-size: 20px;
    color: #333;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 2px solid #f0f2f5;
  }
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
  transition: transform 0.3s ease;

  &:hover {
    transform: translateY(-4px);
  }

  .item-image {
    width: 100%;
    height: 160px;
    border-radius: 4px;
  }

  .image-placeholder {
    width: 100%;
    height: 160px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #c0c4cc;
    border-radius: 4px;
  }

  .item-info {
    padding: 12px 0 0;

    .item-title {
      font-size: 14px;
      color: #303133;
      margin: 0 0 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.4;
    }

    .item-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .item-price {
        font-size: 16px;
        font-weight: 600;
        color: #f56c6c;
        margin: 0;
      }

      .item-category {
        font-size: 12px;
        background: #f0f9ff;
        color: #409eff;
        border-color: #d9ecff;
      }
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .category-view {
    padding: 16px;
  }

  .page-header {
    h1 {
      font-size: 24px;
    }

    .subtitle {
      font-size: 14px;
    }
  }

  .category-filter {
    padding: 16px;

    .filter-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;

      .filter-actions {
        width: 100%;
        justify-content: space-between;
      }
    }

    .category-tags {
      .category-tag {
        padding: 6px 16px;
        font-size: 14px;
      }
    }
  }

  .items-section {
    h3 {
      font-size: 18px;
    }
  }
}
</style>