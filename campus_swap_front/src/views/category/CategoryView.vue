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
          @keyup.enter="fetchItems"
        >
          <template #append>
            <el-button :icon="Search" @click="fetchItems" />
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
        <el-empty v-if="!loading && items.length === 0" description="暂无商品" />
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
.category-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
  h1 { font-size: 32px; color: #333; margin-bottom: 8px; }
  .subtitle { font-size: 16px; color: #666; }
}

.search-bar { margin-bottom: 32px; }

.category-filter {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);

  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h3 { font-size: 18px; color: #333; margin: 0; }
    .filter-actions { display: flex; align-items: center; gap: 12px; }
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
      &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
      &.selected { background: linear-gradient(135deg, #409eff, #66b1ff); border-color: #409eff; }
      .check-icon { margin-left: 6px; font-size: 14px; }
    }
  }

  .selected-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 16px;
    border-top: 1px solid #e4e7ed;
    span { color: #666; font-size: 14px; }
    .count-text { margin-left: auto; color: #409eff; font-weight: 500; }
  }
}

.items-section {
  h3 { font-size: 20px; color: #333; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid #f0f2f5; }
}

.item-list { min-height: 200px; }
.item-card-link { text-decoration: none; }

.item-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.3s ease;
  &:hover { transform: translateY(-4px); }

  .item-image { width: 100%; height: 160px; border-radius: 4px; }
  .image-placeholder {
    width: 100%; height: 160px;
    display: flex; align-items: center; justify-content: center;
    background: #f5f7fa; color: #c0c4cc; border-radius: 4px;
  }

  .item-info {
    padding: 12px 0 0;
    .item-title { font-size: 14px; color: #303133; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .item-meta { display: flex; justify-content: space-between; align-items: center; }
    .item-price { font-size: 16px; font-weight: 600; color: #f56c6c; margin: 0; }
    .item-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
  }
}

// 匹配对话框
.match-form {
  .degree-hint { font-size: 12px; color: #909399; margin-top: 4px; }
}

.match-results {
  .results-title { font-size: 14px; font-weight: 600; color: #409eff; margin-bottom: 12px; }
}

.match-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: #409eff; background: #ecf5ff; }

  .match-img { width: 72px; height: 72px; border-radius: 6px; flex-shrink: 0; }
  .match-info { flex: 1; min-width: 0; }
  .match-name { font-size: 14px; font-weight: 500; color: #303133; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .match-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  .match-price { font-size: 14px; font-weight: 600; color: #f56c6c; }
  .match-desc { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

@media (max-width: 768px) {
  .category-view { padding: 16px; }
  .page-header { h1 { font-size: 24px; } .subtitle { font-size: 14px; } }
  .category-filter {
    padding: 16px;
    .filter-header { flex-direction: column; align-items: flex-start; gap: 12px; .filter-actions { width: 100%; justify-content: space-between; } }
    .category-tags { .category-tag { padding: 6px 16px; font-size: 14px; } }
  }
  .items-section { h3 { font-size: 18px; } }
}
</style>
