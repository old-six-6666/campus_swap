<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { ElMessageBox } from 'element-plus'
import { showSuccess } from '@/utils/notify'

const router = useRouter()
const items = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 10

const STATUS_MAP = {
  0: { label: '在售', type: 'success' },
  1: { label: '已下架', type: 'info' },
  2: { label: '已售出', type: 'warning' },
}
const AUDIT_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
}

async function fetchMyItems() {
  loading.value = true
  try {
    const data = await itemApi.getMyItems({ page: page.value, size: pageSize })
    items.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await itemApi.remove(item.id)
    showSuccess('删除成功')
    if (items.value.length === 1 && page.value > 1) page.value--
    fetchMyItems()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') throw e
  }
}

function handleEdit(item) {
  router.push(`/item/edit/${item.id}`)
}

onMounted(fetchMyItems)
</script>

<template>
  <div class="my-items-view">
    <div class="page-header">
      <h2>我的闲置</h2>
      <el-button type="primary" @click="router.push('/publish')">发布新闲置</el-button>
    </div>

    <el-card v-loading="loading">
      <el-empty v-if="!loading && items.length === 0" description="还没有发布任何闲置">
        <el-button type="primary" @click="router.push('/publish')">立即发布</el-button>
      </el-empty>

      <template v-else>
        <div v-for="item in items" :key="item.id" class="item-row">
          <!-- 封面图 -->
          <el-image
            :src="item.coverImage"
            fit="cover"
            class="item-thumb"
            lazy
          >
            <template #error>
              <div class="thumb-placeholder"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>

          <!-- 商品信息 -->
          <div class="item-body">
            <div class="item-title" @click="router.push(`/item/${item.id}`)">
              {{ item.title }}
            </div>
            <div class="item-meta">
              <span class="price">¥ {{ item.price }}</span>
              <!-- 审核状态（优先展示，未通过时覆盖显示意义） -->
              <el-tag :type="AUDIT_MAP[item.auditStatus]?.type" size="small">
                {{ AUDIT_MAP[item.auditStatus]?.label }}
              </el-tag>
              <!-- 已通过审核时才显示上架状态 -->
              <el-tag v-if="item.auditStatus === 1" :type="STATUS_MAP[item.status]?.type" size="small">
                {{ STATUS_MAP[item.status]?.label }}
              </el-tag>
              <span class="category">{{ item.category }}</span>
            </div>
            <!-- 拒绝原因提示 -->
            <div v-if="item.auditStatus === 2 && item.auditRemark" class="audit-remark">
              拒绝原因：{{ item.auditRemark }}
            </div>
            <div class="item-time">{{ item.createdAt?.slice(0, 10) }}</div>
          </div>

          <!-- 操作按钮 -->
          <div class="item-actions">
            <el-button size="small" @click="handleEdit(item)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(item)">删除</el-button>
          </div>
        </div>

        <!-- 分页 -->
        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          class="pagination"
          @current-change="fetchMyItems"
        />
      </template>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.my-items-view {
  max-width: 860px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: $letter-spacing-base;
  }
}

:deep(.el-card) {
  border-radius: $border-radius-lg !important;
  padding: 8px 0;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid $border-color;
  transition: $transition-fast;

  &:last-child { border-bottom: none; }
  &:hover { background: $bg-subtle; border-radius: $border-radius-sm; }
}

.item-thumb {
  width: 84px;
  height: 84px;
  border-radius: $border-radius-sm;
  flex-shrink: 0;
}

.thumb-placeholder {
  width: 84px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-subtle;
  color: $text-secondary;
  border-radius: $border-radius-sm;
}

.item-body {
  flex: 1;
  min-width: 0;

  .item-title {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    cursor: pointer;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 8px;
    letter-spacing: $letter-spacing-base;
    transition: color 0.2s;

    &:hover { color: $primary; }
  }

  .item-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 5px;
    flex-wrap: wrap;

    .price {
      font-size: 16px;
      font-weight: 700;
      color: $warning;
    }

    .category {
      font-size: 12px;
      color: $text-secondary;
      background: $bg-subtle;
      padding: 2px 8px;
      border-radius: 50px;
    }
  }

  .item-time {
    font-size: 12px;
    color: $text-secondary;
  }

  .audit-remark {
    font-size: 12px;
    color: $danger;
    margin-top: 4px;
  }
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
