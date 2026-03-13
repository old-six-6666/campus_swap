<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const keyword = ref('')
const filterCategory = ref('')
const filterStatus = ref(null)

const CATEGORIES = ['数码', '书籍', '服饰', '生活用品', '其他']
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
const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '在售' },
  { value: 1, label: '已下架' },
  { value: 2, label: '已售出' },
]

async function fetchItems() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value ?? undefined,
      page: page.value,
      size: pageSize,
    }
    const data = await adminApi.listItems(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(row, newStatus) {
  if (newStatus === row.status) return
  const label = STATUS_MAP[newStatus]?.label
  await ElMessageBox.confirm(`将商品「${row.title}」改为「${label}」吗？`, '修改状态', {
    type: 'warning',
  })
  await adminApi.updateItemStatus(row.id, newStatus)
  ElMessage.success('状态已更新')
  fetchItems()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定强制删除商品「${row.title}」吗？`, '强制删除', {
    type: 'error',
    confirmButtonText: '删除',
    confirmButtonClass: 'el-button--danger',
  })
  await adminApi.deleteItem(row.id)
  ElMessage.success('已删除')
  fetchItems()
}

function resetFilter() {
  keyword.value = ''
  filterCategory.value = ''
  filterStatus.value = null
  page.value = 1
  fetchItems()
}

onMounted(fetchItems)
</script>

<template>
  <div class="item-manage">
    <div class="toolbar">
      <h2>商品管理</h2>
      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索标题"
          clearable
          style="width: 180px"
          @keyup.enter="fetchItems"
          @clear="fetchItems"
        />
        <el-select v-model="filterCategory" placeholder="分类" clearable style="width: 110px" @change="fetchItems">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" style="width: 100px" @change="fetchItems">
          <el-option v-for="o in statusOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
        </el-select>
        <el-button @click="fetchItems">搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="70">
        <template #default="{ row }">
          <el-image
            v-if="row.coverImage"
            :src="row.coverImage"
            fit="cover"
            style="width:48px;height:48px;border-radius:4px"
          />
          <span v-else style="color:#c0c4cc;font-size:12px">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column label="价格" width="90">
        <template #default="{ row }">¥ {{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="sellerNickname" label="发布者" width="100" />
      <el-table-column label="审核" width="90">
        <template #default="{ row }">
          <el-tag :type="AUDIT_MAP[row.auditStatus]?.type" size="small">
            {{ AUDIT_MAP[row.auditStatus]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-select
            :model-value="row.status"
            size="small"
            style="width:110px"
            @change="(val) => handleStatusChange(row, val)"
          >
            <el-option
              v-for="o in statusOptions.filter(s => s.value !== null)"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="fetchItems"
    />
  </div>
</template>

<style scoped lang="scss">
.item-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
