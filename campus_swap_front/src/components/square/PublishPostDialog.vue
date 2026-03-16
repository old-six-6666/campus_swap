<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Goods, 
  SuccessFilled, 
  ChatRound, 
  Search,
  Plus,
  Close
} from '@element-plus/icons-vue'
import { squareApi } from '@/api/modules/square'
import request from '@/api/index'

// 图片上传
const uploadFileList = ref([])

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

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'success'])

// 动态类型选项
const postTypes = [
  { value: 1, label: '分享已发布的物品', icon: Goods, description: '选择你已发布的闲置物品进行分享' },
  { value: 2, label: '换物成功', icon: SuccessFilled, description: '庆祝成功完成换物' },
  { value: 3, label: '分享动态', icon: ChatRound, description: '分享换物心得或体验' },
  { value: 4, label: '求换动态', icon: Search, description: '发布你想要交换的物品' }
]

// 表单数据
const form = reactive({
  type: 1,
  content: '',
  itemId: null,
  swapRecordId: null,
  tagIds: [],
  imageList: []
})

// 状态
const loading = ref(false)
const submitting = ref(false)
const step = ref(1) // 1:选择类型, 2:填写内容
const myItems = ref([])
const mySwapRecords = ref([])
const availableTags = ref([])
const searchTag = ref('')

// 计算属性
const selectedType = computed(() => {
  return postTypes.find(t => t.value === form.type)
})

const selectedItem = computed(() => {
  return myItems.value.find(item => item.id === form.itemId)
})

const selectedSwapRecord = computed(() => {
  return mySwapRecords.value.find(record => record.id === form.swapRecordId)
})

const filteredTags = computed(() => {
  if (!searchTag.value) return availableTags.value
  return availableTags.value.filter(tag => 
    tag.name.toLowerCase().includes(searchTag.value.toLowerCase())
  )
})

// 方法
function handleTypeSelect(type) {
  form.type = type
  step.value = 2
  
  // 根据类型加载相关数据
  if (type === 1) {
    loadMyItems()
  } else if (type === 2) {
    loadMySwapRecords()
  }
  
  loadTags()
}

function handleBack() {
  step.value = 1
  resetForm()
}

function resetForm() {
  form.content = ''
  form.itemId = null
  form.swapRecordId = null
  form.tagIds = []
  form.imageList = []
  uploadFileList.value = []
}

async function loadMyItems() {
  try {
    // 获取用户已发布的物品列表
    const response = await squareApi.getMyItemsForPost()
    // 响应是分页数据，提取records字段
    myItems.value = response.records || []
    
    // 如果没有已发布的物品，显示提示
    if (myItems.value.length === 0) {
      ElMessage.info('你还没有发布过任何物品，请先到"发布闲置"页面发布物品')
    }
  } catch (error) {
    console.error('加载物品失败:', error)
    // 如果API调用失败，使用空数组
    myItems.value = []
    ElMessage.error('加载物品列表失败，请稍后重试')
  }
}

async function loadMySwapRecords() {
  try {
    const res = await squareApi.getMySwapRecords()
    mySwapRecords.value = res || []
    if (mySwapRecords.value.length === 0) {
      ElMessage.info('暂无已完成的换物记录')
    }
  } catch (error) {
    console.error('加载换物记录失败:', error)
    mySwapRecords.value = []
  }
}

async function loadTags() {
  try {
    // 暂时使用模拟数据，避免API调用错误
    availableTags.value = [
      { id: 1, name: '数码' },
      { id: 2, name: '书籍' },
      { id: 3, name: '服饰' },
      { id: 4, name: '生活用品' },
      { id: 5, name: '体育用品' },
      { id: 6, name: '文具' },
      { id: 7, name: '美妆' },
      { id: 8, name: '学习资料' },
      { id: 9, name: '电子产品' },
      { id: 10, name: '家居用品' },
      { id: 11, name: '其他' }
    ]
    
    // 实际API调用（暂时注释，等后端实现）
    // const response = await squareApi.getHotTags()
    // availableTags.value = response.data || []
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

function toggleTag(tagId) {
  const index = form.tagIds.indexOf(tagId)
  if (index === -1) {
    form.tagIds.push(tagId)
  } else {
    form.tagIds.splice(index, 1)
  }
}

function isTagSelected(tagId) {
  return form.tagIds.includes(tagId)
}

function getTagName(tagId) {
  const tag = availableTags.value.find(t => t.id === tagId)
  return tag ? tag.name : ''
}

async function handleSubmit() {
  // 验证表单
  if (!form.content.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }
  
  if (form.type === 1 && !form.itemId) {
    ElMessage.warning('请选择要发布的物品')
    return
  }
  
  if (form.type === 2 && !form.swapRecordId) {
    ElMessage.warning('请选择换物记录')
    return
  }
  
  submitting.value = true
  try {
    // 准备提交数据
    const postData = {
      type: form.type,
      content: form.content.trim(),
      tagIds: form.tagIds,
      imageList: form.imageList
    }
    
    if (form.type === 1) {
      postData.itemId = form.itemId
    } else if (form.type === 2) {
      postData.swapRecordId = form.swapRecordId
    }
    
    // 调用真实API - API拦截器已经处理了错误，成功时返回数据
    await squareApi.createPost(postData)
    
    ElMessage.success('动态发布成功！')
    emit('success')
    handleClose()
  } catch (error) {
    console.error('发布动态失败:', error)
    ElMessage.error('发布失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
  setTimeout(() => {
    step.value = 1
    resetForm()
  }, 300)
}

// 监听对话框显示
watch(() => props.visible, (newVal) => {
  if (newVal) {
    step.value = 1
    resetForm()
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="handleClose"
    title="发布动态"
    width="600px"
    :close-on-click-modal="false"
    class="publish-post-dialog"
  >
    <!-- 步骤1：选择动态类型 -->
    <div v-if="step === 1" class="type-selection-step">
      <div class="step-title">选择动态类型</div>
      <div class="type-grid">
        <div
          v-for="type in postTypes"
          :key="type.value"
          class="type-card"
          :class="{ 'selected': form.type === type.value }"
          @click="handleTypeSelect(type.value)"
        >
          <div class="type-icon">
            <el-icon size="32">
              <component :is="type.icon" />
            </el-icon>
          </div>
          <div class="type-info">
            <div class="type-label">{{ type.label }}</div>
            <div class="type-description">{{ type.description }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 步骤2：填写动态内容 -->
    <div v-else class="content-step">
      <div class="step-header">
        <el-button type="text" @click="handleBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="step-title">
          <el-icon class="type-icon-small">
            <component :is="selectedType.icon" />
          </el-icon>
          {{ selectedType.label }}
        </div>
      </div>

      <div class="form-container">
        <!-- 动态内容 -->
        <div class="form-section">
          <div class="section-title">动态内容</div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的想法、感受或经历..."
            maxlength="500"
            show-word-limit
            class="content-input"
          />
        </div>

        <!-- 图片上传 -->
        <div class="form-section">
          <div class="section-title">添加图片 <span class="section-hint">（最多9张）</span></div>
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
          <div class="section-title">选择物品</div>
          <div class="items-grid">
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
            </div>
          </div>
          <div v-if="form.itemId" class="selected-item-info">
            已选择：<strong>{{ selectedItem?.title }}</strong>
          </div>
        </div>

        <!-- 关联换物记录（类型2） -->
        <div v-else-if="form.type === 2" class="form-section">
          <div class="section-title">选择换物记录</div>
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
                  <span class="item-a">{{ record.itemATitle }}</span>
                  <span class="swap-arrow">↔</span>
                  <span class="item-b">{{ record.itemBTitle }}</span>
                </div>
                <div class="swap-meta">
                  与 {{ record.partnerName }} 于 {{ record.completedAt ? new Date(record.completedAt).toLocaleDateString() : '' }} 完成换物
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 标签选择 -->
        <div class="form-section">
          <div class="section-title">添加标签</div>
          <div class="tag-search">
            <el-input
              v-model="searchTag"
              placeholder="搜索标签..."
              size="small"
              clearable
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
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
            <div class="selected-label">已选择：</div>
            <div class="selected-tags-list">
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
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          class="submit-btn"
        >
          {{ submitting ? '发布中...' : '发布动态' }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.publish-post-dialog {
  :deep(.el-dialog__header) {
    margin: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #f0f2f5;
    
    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.type-selection-step {
  padding: 24px;
  
  .step-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
    text-align: center;
  }
  
  .type-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    
    .type-card {
      border: 2px solid #e4e7ed;
      border-radius: 12px;
      padding: 20px;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 16px;
      
      &:hover {
        border-color: #409eff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
      }
      
      &.selected {
        border-color: #409eff;
        background: #f0f9ff;
        
        .type-icon {
          color: #409eff;
        }
        
        .type-label {
          color: #409eff;
        }
      }
      
      .type-icon {
        color: #909399;
        flex-shrink: 0;
      }
      
      .type-info {
        flex: 1;
        
        .type-label {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 4px;
        }
        
        .type-description {
          font-size: 13px;
          color: #909399;
          line-height: 1.4;
        }
      }
    }
  }
}

.content-step {
  .step-header {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f2f5;
    display: flex;
    align-items: center;
    gap: 16px;
    
    .back-btn {
      padding: 0;
      color: #606266;
      
      &:hover {
        color: #409eff;
      }
    }
    
    .step-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 8px;
      
      .type-icon-small {
        color: #409eff;
      }
    }
  }
  
  .form-container {
    max-height: 500px;
    overflow-y: auto;
    padding: 24px;
    
    .form-section {
      margin-bottom: 24px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .section-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 12px;
      }
      
      .content-input {
        :deep(.el-textarea__inner) {
          resize: none;
        }
      }

      .section-hint {
        font-size: 12px;
        font-weight: 400;
        color: #909399;
      }

      .image-uploader {
        :deep(.el-upload--picture-card) {
          width: 80px;
          height: 80px;
          line-height: 80px;
        }
        :deep(.el-upload-list--picture-card .el-upload-list__item) {
          width: 80px;
          height: 80px;
        }
      }
      
      .items-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        margin-bottom: 12px;
        
        .item-card {
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          padding: 12px;
          cursor: pointer;
          transition: all 0.3s ease;
          
          &:hover {
            border-color: #409eff;
            transform: translateY(-2px);
          }
          
          &.selected {
            border-color: #409eff;
            background: #f0f9ff;
          }
          
          .item-image {
            width: 100%;
            height: 80px;
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
            }
          }
          
          .item-info {
            .item-title {
              font-size: 14px;
              font-weight: 500;
              color: #303133;
              margin-bottom: 6px;
            }
            
            .item-price {
              font-size: 13px;
              color: #f56c6c;
              font-weight: 600;
            }
          }
        }
      }
      
      .selected-item-info {
        font-size: 13px;
        color: #606266;
        padding: 8px 12px;
        background: #f5f7fa;
        border-radius: 6px;
        margin-top: 12px;
        
        strong {
          color: #409eff;
        }
      }
    }
    
    .swap-records {
      .swap-record-card {
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        padding: 16px;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;
        
        &:hover {
          border-color: #409eff;
          background: #f5f7fa;
        }
        
        &.selected {
          border-color: #409eff;
          background: #f0f9ff;
        }
        
        .swap-icon {
          font-size: 24px;
          flex-shrink: 0;
        }
        
        .swap-info {
          flex: 1;
          
          .swap-items {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;
            
            .item-a, .item-b {
              font-size: 14px;
              font-weight: 500;
              color: #303133;
            }
            
            .swap-arrow {
              color: #909399;
            }
          }
          
          .swap-meta {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
    
    .tag-search {
      margin-bottom: 12px;
      
      .search-input {
        :deep(.el-input__wrapper) {
          border-radius: 20px;
        }
      }
    }
    
    .tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 16px;
      
      .tag-item {
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
          transform: scale(1.05);
        }
        
        &.selected {
          background: #409eff;
          color: white;
        }
      }
    }
    
    .selected-tags {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .selected-label {
        font-size: 13px;
        color: #606266;
        flex-shrink: 0;
      }
      
      .selected-tags-list {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }
    }
  }
  
  .action-buttons {
    padding: 20px 24px;
    border-top: 1px solid #f0f2f5;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    
    .submit-btn {
      min-width: 100px;
    }
  }
}
</style>