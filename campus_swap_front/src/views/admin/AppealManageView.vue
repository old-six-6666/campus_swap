<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const filterStatus = ref('')

// 处理弹窗
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const currentId = ref(null)
const reviewAction = ref(1)
const reviewRemark = ref('')

const STATUS_MAP = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已处理', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 0,  label: '待处理' },
  { value: 1,  label: '已处理' },
  { value: 2,  label: '已驳回' },
]

async function fetchList() {
  loading.value = true
  try {
    const data = await adminApi.listAppeals({
      status: filterStatus.value !== '' ? filterStatus.value : undefined,
      page: page.value,
      size: pageSize,
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  fetchList()
}

function openReview(row) {
  currentId.value = row.id
  reviewAction.value = 1
  reviewRemark.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  reviewLoading.value = true
  try {
    await adminApi.reviewAppeal(currentId.value, reviewAction.value, reviewRemark.value)
    ElMessage.success('申诉已处理')
    reviewVisible.value = false
    fetchList()
  } finally {
    reviewLoading.value = false
  }
}

function fmtTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(fetchList)
</script>

<template>
  <div class="appeal-manage">
    <div class="toolbar">
      <h2>交易申诉</h2>
      <div class="toolbar-right">
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          clearable
          style="width: 130px"
          @change="onSearch"
        >
          <el-option
            v-for="o in statusOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column label="交易单号" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.tradeNo || row.tradeId }}</template>
      </el-table-column>

      <el-table-column label="申诉人" min-width="120">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.appellantAvatar" />
            <span>{{ row.appellantNickname || row.appellantId }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="申诉内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.content }}</template>
      </el-table-column>

      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="管理员回复" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>

      <el-table-column label="提交时间" width="110">
        <template #default="{ row }">{{ fmtTime(row.createdAt)?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            size="small"
            type="primary"
            plain
            @click="openReview(row)"
          >处理</el-button>
          <span v-else class="muted">已处理</span>
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
      @current-change="fetchList"
    />

    <!-- 处理申诉弹窗 -->
    <el-dialog v-model="reviewVisible" title="处理申诉" width="440px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="reviewAction">
            <el-radio :value="1">已处理（问题属实，介入处理）</el-radio>
            <el-radio :value="2">已驳回（申诉不成立）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="reviewRemark"
            type="textarea"
            :rows="3"
            placeholder="填写处理说明或回复用户..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.appeal-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .toolbar-right {
    display: flex;
    gap: 10px;
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
