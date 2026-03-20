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
const filterStatus = ref('')

// 强制终止弹窗
const terminateVisible = ref(false)
const terminateReason = ref('')
const terminateLoading = ref(false)
const currentId = ref(null)

const STATUS_MAP = {
  PENDING_MATCH:           { label: '等待匹配',   type: 'info' },
  MATCHED:                 { label: '已匹配',     type: 'primary' },
  AUDIT_PENDING:           { label: '审核中',     type: 'warning' },
  AUDIT_PASSED:            { label: '审核通过',   type: 'success' },
  AUDIT_REJECTED:          { label: '审核驳回',   type: 'danger' },
  WAITING_DELIVERY:        { label: '等待发货',   type: 'primary' },
  BOTH_DELIVERED:          { label: '双方已发货', type: 'primary' },
  WAITING_CONFIRM_RECEIPT: { label: '等待收货',   type: 'primary' },
  COMPLETED:               { label: '交易完成',   type: 'success' },
  TERMINATED:              { label: '已终止',     type: 'danger' },
}

const TERMINAL_STATUS = new Set(['COMPLETED', 'TERMINATED', 'AUDIT_REJECTED'])

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING_MATCH',           label: '等待匹配' },
  { value: 'MATCHED',                 label: '已匹配' },
  { value: 'AUDIT_PENDING',           label: '审核中' },
  { value: 'AUDIT_PASSED',            label: '审核通过' },
  { value: 'AUDIT_REJECTED',          label: '审核驳回' },
  { value: 'WAITING_DELIVERY',        label: '等待发货' },
  { value: 'BOTH_DELIVERED',          label: '双方已发货' },
  { value: 'WAITING_CONFIRM_RECEIPT', label: '等待收货' },
  { value: 'COMPLETED',               label: '交易完成' },
  { value: 'TERMINATED',              label: '已终止' },
]

async function fetchList() {
  loading.value = true
  try {
    const data = await adminApi.listTrades({
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined,
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

function openTerminate(row) {
  currentId.value = row.id
  terminateReason.value = ''
  terminateVisible.value = true
}

async function submitTerminate() {
  terminateLoading.value = true
  try {
    await adminApi.terminateTrade(currentId.value, terminateReason.value)
    ElMessage.success('交易已强制终止')
    terminateVisible.value = false
    fetchList()
  } finally {
    terminateLoading.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="trade-manage">
    <div class="toolbar">
      <h2>交易管理</h2>
      <div class="toolbar-right">
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          clearable
          style="width: 140px"
          @change="onSearch"
        >
          <el-option
            v-for="o in statusOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索交易单号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #append>
            <el-button @click="onSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column prop="tradeNo" label="交易单号" min-width="200" show-overflow-tooltip />

      <el-table-column label="甲方" min-width="120">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.initiatorAvatar" />
            <span>{{ row.initiatorNickname || row.initiatorId }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="甲方商品" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="item-cell" v-if="row.initiatorItemTitle">
            <el-image
              v-if="row.initiatorItemCoverImage"
              :src="row.initiatorItemCoverImage"
              fit="cover"
              class="item-thumb"
            />
            <span>{{ row.initiatorItemTitle }}</span>
          </div>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column label="乙方" min-width="120">
        <template #default="{ row }">
          <div v-if="row.receiverId" class="user-cell">
            <el-avatar :size="24" :src="row.receiverAvatar" />
            <span>{{ row.receiverNickname || row.receiverId }}</span>
          </div>
          <span v-else class="muted">待匹配</span>
        </template>
      </el-table-column>

      <el-table-column label="乙方商品" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="item-cell" v-if="row.receiverItemTitle">
            <el-image
              v-if="row.receiverItemCoverImage"
              :src="row.receiverItemCoverImage"
              fit="cover"
              class="item-thumb"
            />
            <span>{{ row.receiverItemTitle }}</span>
          </div>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="创建时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="!TERMINAL_STATUS.has(row.status)"
            size="small"
            type="danger"
            plain
            @click="openTerminate(row)"
          >强制终止</el-button>
          <span v-else class="muted">已终态</span>
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

    <!-- 强制终止弹窗 -->
    <el-dialog v-model="terminateVisible" title="强制终止交易" width="440px" :close-on-click-modal="false">
      <p style="color:#606266; margin-bottom:12px;">交易将被标记为"已终止"，此操作不可撤销。</p>
      <el-input
        v-model="terminateReason"
        type="textarea"
        :rows="3"
        placeholder="终止原因（选填，留空将显示'管理员强制终止'）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="terminateVisible = false">取消</el-button>
        <el-button type="danger" :loading="terminateLoading" @click="submitTerminate">确认终止</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.trade-manage {
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

.item-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;

  .item-thumb {
    width: 32px;
    height: 32px;
    border-radius: 4px;
    flex-shrink: 0;
  }
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
