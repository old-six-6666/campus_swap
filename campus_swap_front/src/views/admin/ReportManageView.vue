<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const filterStatus = ref(0)   // 默认只看待审核

// 驳回弹窗
const rejectVisible = ref(false)
const rejectRemark = ref('')
const rejectLoading = ref(false)
const currentId = ref(null)

const STATUS_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已处理', type: 'danger' },
  2: { label: '已驳回', type: 'info' },
}

const statusOptions = [
  { value: null, label: '全部' },
  { value: 0,    label: '待审核' },
  { value: 1,    label: '已处理' },
  { value: 2,    label: '已驳回' },
]

const REASON_MAP = {
  1: '违法违规',
  2: '色情低俗',
  3: '虚假信息',
  4: '侮辱谩骂',
  5: '广告骚扰',
  6: '其他',
}

async function fetchList() {
  loading.value = true
  try {
    const data = await adminApi.listReports({
      status: filterStatus.value ?? undefined,
      page: page.value,
      size: pageSize,
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

// 处理举报：内容下架
async function handleTakeDown(row) {
  await ElMessageBox.confirm(
    `确定下架该动态并标记举报为"已处理"？\n\n动态内容：「${row.postContent?.slice(0, 40)}...」`,
    '确认下架',
    { type: 'warning', confirmButtonText: '确定下架' }
  )
  await adminApi.reviewReport(row.id, 1, '')
  ElMessage.success('已下架该动态')
  fetchList()
}

// 驳回举报
function openReject(row) {
  currentId.value = row.id
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  rejectLoading.value = true
  try {
    await adminApi.reviewReport(currentId.value, 2, rejectRemark.value)
    ElMessage.success('已驳回举报')
    rejectVisible.value = false
    fetchList()
  } finally {
    rejectLoading.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="report-manage">
    <div class="toolbar">
      <h2>举报审核</h2>
      <el-select
        v-model="filterStatus"
        placeholder="状态筛选"
        clearable
        style="width: 120px"
        @change="() => { page = 1; fetchList() }"
      >
        <el-option
          v-for="o in statusOptions"
          :key="String(o.value)"
          :label="o.label"
          :value="o.value"
        />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column label="举报者" width="110">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.reporterAvatar" />
            <span>{{ row.reporterNickname }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="举报原因" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="warning">{{ REASON_MAP[row.reason] || '未知' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="补充说明" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>

      <el-table-column label="被举报动态" min-width="180">
        <template #default="{ row }">
          <div v-if="row.postContent !== '[动态已被删除]'" class="post-cell">
            <div class="post-author">{{ row.postUserNickname }}</div>
            <div class="post-content">{{ row.postContent }}</div>
            <div v-if="row.postImages?.length" class="post-images">
              <el-image
                v-for="(img, i) in row.postImages.slice(0, 3)"
                :key="i"
                :src="img"
                :preview-src-list="row.postImages"
                :initial-index="i"
                fit="cover"
                class="thumb"
              />
            </div>
          </div>
          <el-tag v-else type="info" size="small">动态已删除</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="审核备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>

      <el-table-column label="举报时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="danger" plain @click="handleTakeDown(row)">下架内容</el-button>
            <el-button size="small" type="info" plain @click="openReject(row)">驳回</el-button>
          </template>
          <span v-else class="reviewed-label">已审核</span>
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

    <!-- 驳回备注弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回举报" width="440px" :close-on-click-modal="false">
      <p style="color:#606266; margin-bottom:12px;">内容将保持正常显示，举报标记为"已驳回"。</p>
      <el-input
        v-model="rejectRemark"
        type="textarea"
        :rows="3"
        placeholder="驳回备注（选填）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectLoading" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.report-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.post-cell {
  .post-author {
    font-size: 12px;
    color: #909399;
    margin-bottom: 2px;
  }
  .post-content {
    font-size: 13px;
    color: #303133;
    max-height: 40px;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .post-images {
    display: flex;
    gap: 4px;
    margin-top: 4px;
    .thumb {
      width: 40px;
      height: 40px;
      border-radius: 4px;
      cursor: pointer;
    }
  }
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.reviewed-label {
  font-size: 12px;
  color: #909399;
}
</style>
