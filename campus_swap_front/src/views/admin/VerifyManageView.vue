<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const filterStatus = ref(null)
const filterKeyword = ref('')

// 拒绝弹窗
const rejectVisible = ref(false)
const rejectRemark = ref('')
const rejectLoading = ref(false)
const currentId = ref(null)

const STATUS_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'info' },
}

const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已拒绝' },
]

async function fetchList() {
  loading.value = true
  try {
    const data = await adminApi.listVerifications({
      status: filterStatus.value ?? undefined,
      keyword: filterKeyword.value || undefined,
      page: page.value,
      size: pageSize,
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  await ElMessageBox.confirm(
    `确定通过「${row.realName}」（${row.school} ${row.studentId}）的认证申请？\n\n通过前将核验学生档案中是否存在匹配记录。`,
    '通过认证',
    { type: 'success', confirmButtonText: '通过' }
  )
  await adminApi.reviewVerification(row.id, 1, '')
  ElMessage.success('已通过认证')
  fetchList()
}

function openReject(row) {
  currentId.value = row.id
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectLoading.value = true
  try {
    await adminApi.reviewVerification(currentId.value, 2, rejectRemark.value)
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    fetchList()
  } finally {
    rejectLoading.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="verify-manage">
    <div class="toolbar">
      <h2>学生认证审核</h2>
      <div class="toolbar-right">
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          clearable
          style="width: 120px"
          @change="fetchList"
        >
          <el-option
            v-for="o in statusOptions"
            :key="String(o.value)"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-input
          v-model="filterKeyword"
          placeholder="搜索姓名 / 学号 / 学校"
          clearable
          style="width: 240px"
          @keyup.enter="fetchList"
          @clear="fetchList"
        >
          <template #append>
            <el-button @click="fetchList">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userNickname" label="申请用户" min-width="100" />
      <el-table-column prop="userEmail" label="邮箱" min-width="170" show-overflow-tooltip />
      <el-table-column prop="school" label="学校" min-width="140" show-overflow-tooltip />
      <el-table-column prop="studentId" label="学号" min-width="120" />
      <el-table-column prop="realName" label="真实姓名" width="100" />
      <el-table-column prop="extraInfo" label="补充说明" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="审核备注" min-width="130" show-overflow-tooltip />
      <el-table-column label="提交时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="success" plain @click="handleApprove(row)">通过</el-button>
            <el-button size="small" type="danger" plain @click="openReject(row)">拒绝</el-button>
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

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="填写拒绝原因" width="440px" :close-on-click-modal="false">
      <el-input
        v-model="rejectRemark"
        type="textarea"
        :rows="4"
        placeholder="请填写拒绝原因，用户将看到此内容"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.verify-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
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
