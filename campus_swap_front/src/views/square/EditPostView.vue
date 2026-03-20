<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Goods,
  SuccessFilled,
  ChatRound,
  Search,
  Plus,
  ArrowLeft
} from '@element-plus/icons-vue'
import { squareApi } from '@/api/modules/square'
import request from '@/api/index'

const router = useRouter()
const route = useRoute()
const postId = Number(route.params.id)

const pageLoading = ref(true)
const submitting = ref(false)
const uploadFileList = ref([])

const form = reactive({
  type: 1,
  content: '',
  itemId: null,
  swapRecordId: null,
  tagIds: [],
  imageList: []
})

const myItems = ref([])
const mySwapRecords = ref([])
const availableTags = ref([
  { id: 1, name: '数码' }, { id: 2, name: '书籍' }, { id: 3, name: '服饰' },
  { id: 4, name: '生活用品' }, { id: 5, name: '体育用品' }, { id: 6, name: '文具' },
  { id: 7, name: '美妆' }, { id: 8, name: '学习资料' }, { id: 9, name: '电子产品' },
  { id: 10, name: '家居用品' }, { id: 11, name: '其他' }
])
const searchTag = ref('')

const postTypes = [
  { value: 1, label: '分享已发布的物品', icon: Goods, description: '选择你已发布的闲置物品进行分享' },
  { value: 2, label: '换物成功', icon: SuccessFilled, description: '庆祝成功完成换物' },
  { value: 3, label: '分享动态', icon: ChatRound, description: '分享换物心得或体验' },
  { value: 4, label: '求换动态', icon: Search, description: '发布你想要交换的物品' }
]

const selectedType = computed(() => postTypes.find(t => t.value === form.type))
const selectedItem = computed(() => myItems.value.find(item => item.id === form.itemId))
const filteredTags = computed(() => {
  if (!searchTag.value) return availableTags.value
  return availableTags.value.filter(tag =>
    tag.name.toLowerCase().includes(searchTag.value.toLowerCase())
  )
})

async function loadMyItems() {
  try {
    const response = await squareApi.getMyItemsForPost()
    myItems.value = response.records || []
  } catch {
    myItems.value = []
  }
}

async function loadMySwapRecords() {
  try {
    const res = await squareApi.getMySwapRecords()
    mySwapRecords.value = res || []
  } catch {
    mySwapRecords.value = []
  }
}

async function handleImageUpload({ file, onSuccess, onError }) {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const url = await request({ url: '/upload', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
    form.imageList.push(url)
    onSuccess(url)
  } catch (e) {
    onError(e)
    ElMessage.error('图片上传失败')
  }
}

function handleImageRemove(file) {
  const url = file.response || file.url
  const idx = form.imageList.indexOf(url)
  if (idx !== -1) form.imageList.splice(idx, 1)
}

function toggleTag(tagId) {
  const index = form.tagIds.indexOf(tagId)
  if (index === -1) form.tagIds.push(tagId)
  else form.tagIds.splice(index, 1)
}

function isTagSelected(tagId) {
  return form.tagIds.includes(tagId)
}

function getTagName(tagId) {
  return availableTags.value.find(t => t.id === tagId)?.name || ''
}

async function handleSubmit() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }
  if (form.type === 1 && !form.itemId) {
    ElMessage.warning('请选择要关联的物品')
    return
  }

  submitting.value = true
  try {
    await squareApi.updatePost(postId, {
      content: form.content.trim(),
      imageList: form.imageList,
      itemId: form.itemId || undefined
    })
    ElMessage.success('动态编辑成功')
    router.back()
  } catch (error) {
    console.error('编辑动态失败:', error)
    ElMessage.error('编辑失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    // 加载动态详情用于回填
    const detail = await squareApi.getPostDetail(postId)
    form.type = detail.type || 3
    form.content = detail.content || ''
    form.itemId = detail.item?.id || null
    form.imageList = detail.images ? [...detail.images] : []
    uploadFileList.value = form.imageList.map(url => ({ url, status: 'success' }))

    // 按类型加载关联数据
    if (form.type === 1) await loadMyItems()
    else if (form.type === 2) await loadMySwapRecords()
  } catch (error) {
    ElMessage.error('加载动态详情失败')
    router.back()
  } finally {
    pageLoading.value = false
  }
})
</script>

<template>
  <div class="edit-post-view" v-loading="pageLoading">
    <div v-if="!pageLoading" class="edit-container">
      <!-- 顶部导航 -->
      <div class="page-header">
        <el-button type="primary" link :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <h2 class="page-title">编辑动态</h2>
      </div>

      <div class="form-card">
        <!-- 动态类型标签（只读展示，不允许修改类型） -->
        <div class="form-section">
          <div class="section-title">动态类型</div>
          <div class="type-readonly">
            <el-icon class="type-icon"><component :is="selectedType.icon" /></el-icon>
            <span>{{ selectedType.label }}</span>
          </div>
        </div>

        <!-- 动态内容 -->
        <div class="form-section">
          <div class="section-title">动态内容</div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="分享你的想法、感受或经历..."
            maxlength="500"
            show-word-limit
            class="content-input"
          />
        </div>

        <!-- 图片上传 -->
        <div class="form-section">
          <div class="section-title">图片 <span class="section-hint">（最多9张）</span></div>
          <el-upload
            :file-list="uploadFileList"
            list-type="picture-card"
            :http-request="handleImageUpload"
            :on-remove="handleImageRemove"
            :limit="9"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="image-uploader"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </div>

        <!-- 关联物品（类型1） -->
        <div v-if="form.type === 1" class="form-section">
          <div class="section-title">关联物品</div>
          <div v-if="myItems.length === 0" class="empty-items">
            <el-empty description="暂无已发布的物品" :image-size="60" />
          </div>
          <div v-else class="items-grid">
            <div
              v-for="item in myItems"
              :key="item.id"
              class="item-card"
              :class="{ 'selected': form.itemId === item.id }"
              @click="form.itemId = item.id"
            >
              <div class="item-image">
                <img v-if="item.coverImage" :src="item.coverImage" alt="物品图片" />
                <div v-else class="image-placeholder">
                  <el-icon><Goods /></el-icon>
                </div>
              </div>
              <div class="item-info">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-price">¥ {{ item.price }}</div>
              </div>
              <el-icon v-if="form.itemId === item.id" class="check-icon">
                <SuccessFilled />
              </el-icon>
            </div>
          </div>
          <div v-if="selectedItem" class="selected-item-tip">
            已选择：<strong>{{ selectedItem.title }}</strong>
          </div>
        </div>

        <!-- 关联换物记录（类型2） -->
        <div v-else-if="form.type === 2" class="form-section">
          <div class="section-title">换物记录</div>
          <div class="swap-records">
            <div
              v-for="record in mySwapRecords"
              :key="record.id"
              class="swap-record-card"
              :class="{ 'selected': form.swapRecordId === record.id }"
              @click="form.swapRecordId = record.id"
            >
              <div class="swap-icon">🔄</div>
              <div class="swap-info">
                <div class="swap-items">
                  <span>{{ record.itemATitle }}</span>
                  <span class="swap-arrow">↔</span>
                  <span>{{ record.itemBTitle }}</span>
                </div>
                <div class="swap-meta">与 {{ record.partnerName }} 完成换物</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 标签选择 -->
        <div class="form-section">
          <div class="section-title">标签</div>
          <el-input
            v-model="searchTag"
            placeholder="搜索标签..."
            size="small"
            clearable
            class="tag-search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <div class="tags-container">
            <el-tag
              v-for="tag in filteredTags"
              :key="tag.id"
              :type="isTagSelected(tag.id) ? 'primary' : 'info'"
              class="tag-item"
              :class="{ 'selected': isTagSelected(tag.id) }"
              @click="toggleTag(tag.id)"
              size="large"
              effect="plain"
            >
              #{{ tag.name }}
            </el-tag>
          </div>
          <div v-if="form.tagIds.length > 0" class="selected-tags">
            <span class="selected-label">已选：</span>
            <el-tag
              v-for="tagId in form.tagIds"
              :key="tagId"
              type="primary"
              size="small"
              closable
              @close="toggleTag(tagId)"
            >
              #{{ getTagName(tagId) }}
            </el-tag>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button size="large" @click="router.back()">取消</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '保存中...' : '保存修改' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.edit-post-view {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    margin: 0;
  }
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.form-section {
  margin-bottom: 28px;

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;

    .section-hint {
      font-size: 12px;
      font-weight: 400;
      color: #909399;
    }
  }
}

.type-readonly {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f0f9ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  color: #409eff;
  font-weight: 500;

  .type-icon {
    font-size: 18px;
  }
}

.content-input {
  :deep(.el-textarea__inner) {
    resize: none;
    font-size: 15px;
    line-height: 1.6;
  }
}

.image-uploader {
  :deep(.el-upload--picture-card) {
    width: 90px;
    height: 90px;
    line-height: 90px;
  }
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 90px;
    height: 90px;
  }
}

.empty-items {
  padding: 20px 0;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 12px;

  .item-card {
    border: 2px solid #e4e7ed;
    border-radius: 10px;
    padding: 10px;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;

    &:hover {
      border-color: #409eff;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
    }

    &.selected {
      border-color: #409eff;
      background: #f0f9ff;
    }

    .item-image {
      width: 100%;
      height: 90px;
      border-radius: 6px;
      overflow: hidden;
      margin-bottom: 8px;
      background: #f5f7fa;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .image-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #c0c4cc;
        font-size: 24px;
      }
    }

    .item-info {
      .item-title {
        font-size: 13px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .item-price {
        font-size: 13px;
        color: #f56c6c;
        font-weight: 600;
      }
    }

    .check-icon {
      position: absolute;
      top: 6px;
      right: 6px;
      font-size: 18px;
      color: #409eff;
    }
  }
}

.selected-item-tip {
  font-size: 13px;
  color: #606266;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;

  strong { color: #409eff; }
}

.swap-records {
  .swap-record-card {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    padding: 14px 16px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    transition: all 0.2s;

    &:hover { border-color: #409eff; background: #f5f7fa; }
    &.selected { border-color: #409eff; background: #f0f9ff; }

    .swap-icon { font-size: 22px; }

    .swap-info {
      .swap-items {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        .swap-arrow { margin: 0 8px; color: #909399; }
      }
      .swap-meta { font-size: 12px; color: #909399; margin-top: 4px; }
    }
  }
}

.tag-search {
  margin-bottom: 12px;
  :deep(.el-input__wrapper) { border-radius: 20px; }
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;

  .tag-item {
    cursor: pointer;
    transition: all 0.2s;
    &:hover { transform: scale(1.05); }
    &.selected { background: #409eff; color: white; border-color: #409eff; }
  }
}

.selected-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  .selected-label { font-size: 13px; color: #606266; }
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #f0f2f5;
}

@media (max-width: 600px) {
  .form-card { padding: 20px 16px; }
  .items-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
