<script setup>
import { ref, onMounted, watch } from 'vue'
import { squareApi } from '@/api/modules/square'

const props = defineProps({
  selectedTags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:selectedTags'])

const tags = ref([])
const loading = ref(false)
const localSelectedTags = ref([...props.selectedTags])

// 获取热门标签
async function fetchHotTags() {
  loading.value = true
  try {
    tags.value = await squareApi.getHotTags()
  } catch (error) {
    console.error('获取标签失败:', error)
    // 接口异常时回退到空列表
    tags.value = []
  } finally {
    loading.value = false
  }
}

// 切换标签选择
function toggleTag(tagId) {
  const index = localSelectedTags.value.indexOf(tagId)
  if (index === -1) {
    localSelectedTags.value.push(tagId)
  } else {
    localSelectedTags.value.splice(index, 1)
  }
  emit('update:selectedTags', [...localSelectedTags.value])
}

// 清空所有选择
function clearSelection() {
  localSelectedTags.value = []
  emit('update:selectedTags', [])
}

// 根据数量计算标签大小
function getTagSize(count) {
  if (count > 100) return 'large'
  if (count > 50) return 'default'
  return 'small'
}

// 根据数量计算标签类型
function getTagType(count) {
  if (count > 100) return 'primary'
  if (count > 50) return 'success'
  if (count > 30) return 'info'
  return ''
}

onMounted(() => {
  fetchHotTags()
})

defineExpose({ refresh: fetchHotTags })

// 监听props变化
watch(() => props.selectedTags, (newVal) => {
  localSelectedTags.value = [...newVal]
}, { deep: true })
</script>

<template>
  <div class="tag-filter">
    <div class="filter-header">
      <h3 class="filter-title">热门标签</h3>
      <div class="filter-actions">
        <el-button 
          v-if="localSelectedTags.length > 0" 
          size="small"
          type="text"
          @click="clearSelection"
        >
          清空选择
        </el-button>
        <el-tooltip content="点击标签进行筛选，支持多选" placement="top">
          <el-tag size="small" type="info">提示：点击标签筛选</el-tag>
        </el-tooltip>
      </div>
    </div>

    <div v-loading="loading" class="tags-container">
      <div class="tags-list">
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          :type="localSelectedTags.includes(tag.id) ? 'primary' : getTagType(tag.count)"
          :size="getTagSize(tag.count)"
          :class="{ 'selected': localSelectedTags.includes(tag.id) }"
          class="tag-item"
          @click="toggleTag(tag.id)"
          effect="plain"
        >
          #{{ tag.name }}
          <span class="tag-count">{{ tag.count }}</span>
        </el-tag>
      </div>

      <!-- 已选标签提示 -->
      <div v-if="localSelectedTags.length > 0" class="selected-tags">
        <div class="selected-label">已选择：</div>
        <div class="selected-list">
          <el-tag
            v-for="tagId in localSelectedTags"
            :key="tagId"
            type="primary"
            size="small"
            closable
            @close="toggleTag(tagId)"
            class="selected-tag"
          >
            #{{ tags.find(t => t.id === tagId)?.name || tagId }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tag-filter {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  
  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .filter-title {
      font-size: 18px;
      color: #303133;
      margin: 0;
      font-weight: 600;
    }
    
    .filter-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
  
  .tags-container {
    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-bottom: 16px;
      
      .tag-item {
        cursor: pointer;
        transition: all 0.3s ease;
        padding: 8px 16px;
        border-radius: 20px;
        font-weight: 500;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
        
        &.selected {
          background: linear-gradient(135deg, #409eff, #66b1ff);
          border-color: #409eff;
          color: white;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
        }
        
        .tag-count {
          margin-left: 6px;
          font-size: 12px;
          opacity: 0.8;
        }
      }
    }
    
    .selected-tags {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
      padding-top: 16px;
      border-top: 1px solid #e4e7ed;
      
      .selected-label {
        color: #606266;
        font-size: 14px;
        font-weight: 500;
      }
      
      .selected-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        
        .selected-tag {
          background: #f0f9ff;
          border-color: #d9ecff;
          color: #409eff;
          
          &:hover {
            background: #ecf5ff;
          }
        }
      }
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .tag-filter {
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
    
    .tags-container {
      .tags-list {
        .tag-item {
          padding: 6px 12px;
          font-size: 13px;
        }
      }
    }
  }
}
</style>