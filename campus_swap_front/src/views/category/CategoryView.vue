<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { CATEGORIES, CATEGORY_TAGS, COMMON_TAGS } from '@/constants/itemTags'
import { Search, Picture, Check, Switch } from '@element-plus/icons-vue'
import { itemApi, exchangeApi } from '@/api/modules/item'
import { useUserStore } from '@/stores/useUserStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const items = ref([])
const loading = ref(false)
const keyword = ref('')
const selectedCategories = ref([])

// ===== 以物换物匹配 =====
const matchDialogVisible = ref(false)
const matchLoading = ref(false)
const matchResults = ref([])
const matchForm = ref({
  category: '',
  expectCategory: [],
  descriptionKeyword: '',
  minNewDegree: 1,
  maxNewDegree: 5,
})

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

function toggleCategory(category) {
  const index = selectedCategories.value.indexOf(category)
  if (index === -1) {
    selectedCategories.value.push(category)
  } else {
    selectedCategories.value.splice(index, 1)
  }
  fetchItems()
}

function clearSelection() {
  selectedCategories.value = []
  fetchItems()
}

function selectSingle(category) {
  selectedCategories.value = [category]
  fetchItems()
}

// 打开匹配对话框
function openMatchDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用匹配功能')
    router.push({ name: 'Login' })
    return
  }
  matchResults.value = []
  matchDialogVisible.value = true
}

// 执行匹配
async function doMatch() {
  if (!matchForm.value.category) {
    ElMessage.warning('请选择您的物品分类')
    return
  }
  if (!matchForm.value.expectCategory.length) {
    ElMessage.warning('请选择期望换取的分类')
    return
  }
  matchLoading.value = true
  try {
    const result = await exchangeApi.match({
      category: matchForm.value.category,
      expectCategory: matchForm.value.expectCategory,
      descriptionKeyword: matchForm.value.descriptionKeyword || undefined,
      minNewDegree: matchForm.value.minNewDegree,
      maxNewDegree: matchForm.value.maxNewDegree,
      pageNum: 1,
      pageSize: 20,
    })
    matchResults.value = result || []
    if (!matchResults.value.length) {
      ElMessage.info('暂无匹配结果，换个条件试试')
    }
  } catch (e) {
    ElMessage.error('匹配服务暂时不可用')
  } finally {
    matchLoading.value = false
  }
}

onMounted(fetchItems)
watch(keyword, () => fetchItems())
</script>

<template>
  <div class="category-view">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>物品分类</h1>
      <p class="subtitle">浏览不同分类的闲置物品，支持多选或单选筛选</p>
    </div>

    <!-- 搜索栏 + 匹配入口 -->
    <el-row justify="center" class="search-bar" :gutter="12">
      <el-col :span="14">
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
      <el-col :span="4">
        <el-button
          type="warning"
          size="large"
          :icon="Switch"
          style="width:100%"
          @click="openMatchDialog"
        >
          以物换物匹配
        </el-button>
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
          v-for="category in CATEGORIES"
          :key="category"
          :type="selectedCategories.includes(category) ? 'primary' : 'info'"
          class="category-tag"
          :class="{ selected: selectedCategories.includes(category) }"
          size="large"
          effect="dark"
          @click="toggleCategory(category)"
          @click.ctrl="selectSingle(category)"
        >
          {{ category }}
          <el-icon v-if="selectedCategories.includes(category)" class="check-icon">
            <Check />
          </el-icon>
        </el-tag>
      </div>

      <div v-if="selectedCategories.length > 0" class="selected-info">
        <span>已选择：</span>
        <el-tag
          v-for="cat in selectedCategories"
          :key="cat"
          type="primary"
          size="small"
          closable
          @close="toggleCategory(cat)"
        >{{ cat }}</el-tag>
        <span class="count-text">共 {{ items.length }} 个物品</span>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="items-section">
      <h3 v-if="selectedCategories.length > 0">{{ selectedCategories.join('、') }} 分类的物品</h3>
      <h3 v-else>所有物品</h3>

      <el-row v-loading="loading" :gutter="16" class="item-list">
        <el-col
          v-for="item in items"
          :key="item.id"
          :xs="12" :sm="8" :md="6" :lg="4"
        >
          <RouterLink :to="`/item/${item.id}`" class="item-card-link">
            <el-card shadow="hover" class="item-card">
              <el-image :src="item.coverImage" fit="cover" class="item-image" lazy>
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
                </div>
                <div class="item-tags">
                  <el-tag
                    v-for="tag in item.tags"
                    :key="tag"
                    size="small"
                    :type="tag === item.category ? '' : 'info'"
                  >{{ tag }}</el-tag>
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

    <!-- ===== 以物换物匹配对话框 ===== -->
    <el-dialog
      v-model="matchDialogVisible"
      title="🔄 以物换物智能匹配"
      width="640px"
      :close-on-click-modal="false"
    >
      <div class="match-form">
        <el-form label-width="110px">
          <el-form-item label="我的物品分类">
            <el-select v-model="matchForm.category" placeholder="选择您要换出的物品分类" style="width:100%">
              <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>

          <el-form-item label="期望换取分类">
            <el-select
              v-model="matchForm.expectCategory"
              multiple
              placeholder="选择您想换取的物品分类（可多选）"
              style="width:100%"
            >
              <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>

          <el-form-item label="物品描述关键词">
            <el-input
              v-model="matchForm.descriptionKeyword"
              placeholder="输入关键词提升匹配精度（可选）"
            />
          </el-form-item>

          <el-form-item label="新旧程度范围">
            <el-slider
              v-model="matchForm.minNewDegree"
              :min="1" :max="5"
              style="width:45%"
            />
            <span style="margin:0 8px;color:#909399">~</span>
            <el-slider
              v-model="matchForm.maxNewDegree"
              :min="1" :max="5"
              style="width:45%"
            />
            <div class="degree-hint">1=全新 &nbsp; 5=较旧 &nbsp; 当前：{{ matchForm.minNewDegree }} ~ {{ matchForm.maxNewDegree }}</div>
          </el-form-item>
        </el-form>

        <el-button
          type="warning"
          :loading="matchLoading"
          style="width:100%;margin-bottom:16px"
          @click="doMatch"
        >开始匹配</el-button>

        <!-- 匹配结果 -->
        <div v-if="matchResults.length" class="match-results">
          <div class="results-title">匹配到 {{ matchResults.length }} 个物品</div>
          <div
            v-for="r in matchResults"
            :key="r.id"
            class="match-item"
            @click="router.push(`/item/${r.id}`); matchDialogVisible = false"
          >
            <el-image :src="r.coverImage" fit="cover" class="match-img" />
            <div class="match-info">
              <div class="match-name">{{ r.title }}</div>
              <div class="match-meta">
                <el-tag size="small">{{ r.category }}</el-tag>
                <span class="match-price">¥ {{ r.price }}</span>
              </div>
              <div class="match-desc">{{ r.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="matchDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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

// 匹配对话框
.match-form {
  .degree-hint { font-size: 12px; color: #909399; margin-top: 4px; }
}

.match-results {
  .results-title { font-size: 14px; font-weight: 600; color: $primary; margin-bottom: 12px; }
}

.match-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid $border-color;
  border-radius: $border-radius-sm;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: $primary; background: rgba(27,153,170,0.05); }

  .match-img { width: 72px; height: 72px; border-radius: 6px; flex-shrink: 0; }
  .match-info { flex: 1; min-width: 0; }
  .match-name { font-size: 14px; font-weight: 500; color: $text-primary; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .match-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  .match-price { font-size: 14px; font-weight: 600; color: $warning; }
  .match-desc { font-size: 12px; color: $text-secondary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

@media (max-width: 768px) {
  .page-header { padding: 24px 16px 20px; h1 { font-size: 22px; } }
  .category-filter { padding: 16px; .filter-header { flex-direction: column; align-items: flex-start; } }
}
</style>
