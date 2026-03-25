<script setup>
import { ref, onMounted, computed } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)

// 表单弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const editingId = ref(null)
const form = ref({
  title: '', content: '', type: 1, sort: 0,
  validityMode: 'permanent',  // 'permanent' | 'duration' | 'range'
  durationDays: 7,
  timeRange: null,            // [startTime, endTime] for range mode
})

const TYPE_MAP = {
  1: { label: '普通', type: '' },
  2: { label: '重要', type: 'warning' },
  3: { label: '紧急', type: 'danger' },
}

const STATUS_MAP = {
  0: { label: '已下线', type: 'info' },
  1: { label: '上线中', type: 'success' },
}

async function fetchList() {
  loading.value = true
  try {
    list.value = await adminApi.listAnnouncements() || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', content: '', type: 1, sort: 0, validityMode: 'permanent', durationDays: 7, timeRange: null }
  dialogTitle.value = '新增公告'
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  let validityMode = 'permanent'
  let durationDays = 7
  let timeRange = null
  if (row.endTime) {
    if (row.startTime) {
      validityMode = 'range'
      timeRange = [row.startTime, row.endTime]
    } else {
      validityMode = 'duration'
      const remainingMs = new Date(row.endTime) - Date.now()
      durationDays = Math.max(1, Math.ceil(remainingMs / 86400000))
    }
  }
  form.value = {
    title: row.title, content: row.content, type: row.type, sort: row.sort,
    validityMode, durationDays, timeRange,
  }
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
}

function buildPayload() {
  const { title, content, type, sort, validityMode, durationDays, timeRange } = form.value
  const payload = { title, content, type, sort, startTime: null, endTime: null }
  if (validityMode === 'duration') {
    const end = new Date()
    end.setDate(end.getDate() + durationDays)
    payload.endTime = formatDateTime(end)
  } else if (validityMode === 'range' && timeRange) {
    payload.startTime = timeRange[0]
    payload.endTime = timeRange[1]
  }
  return payload
}

function formatDateTime(date) {
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function submitForm() {
  if (!form.value.title?.trim()) return ElMessage.warning('请输入标题')
  if (!form.value.content?.trim()) return ElMessage.warning('请输入内容')
  if (form.value.validityMode === 'range' && !form.value.timeRange) return ElMessage.warning('请选择时间段')
  submitting.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      await adminApi.updateAnnouncement(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await adminApi.createAnnouncement(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await adminApi.updateAnnouncementStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已上线' : '已下线')
  fetchList()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
  })
  await adminApi.deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  fetchList()
}

// 判断公告是否已过期
function isExpired(row) {
  if (!row.endTime) return false
  return new Date(row.endTime) <= new Date()
}

// 判断公告是否未到生效时间
function isNotStarted(row) {
  if (!row.startTime) return false
  return new Date(row.startTime) > new Date()
}

// 时效列文本
function validityText(row) {
  if (!row.endTime) return '永久'
  const end = new Date(row.endTime)
  if (row.startTime) {
    const start = new Date(row.startTime)
    return `${formatDateShort(start)} ~ ${formatDateShort(end)}`
  }
  if (isExpired(row)) return `已过期 (${formatDateShort(end)})`
  const days = Math.ceil((end - Date.now()) / 86400000)
  return `${days}天后到期`
}

function formatDateShort(date) {
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

onMounted(fetchList)
</script>

<template>
  <div class="announcement-manage">
    <div class="toolbar">
      <h2>公告管理</h2>
      <el-button type="primary" @click="openCreate">+ 新增公告</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe
      :row-class-name="({ row }) => isExpired(row) ? 'row-expired' : ''">
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />

      <el-table-column label="内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.content }}</template>
      </el-table-column>

      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="TYPE_MAP[row.type]?.type" size="small">
            {{ TYPE_MAP[row.type]?.label }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
          <el-tag v-if="isExpired(row)" type="danger" size="small" style="margin-left:4px">已过期</el-tag>
          <el-tag v-else-if="isNotStarted(row)" type="info" size="small" style="margin-left:4px">未开始</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="时效" min-width="150">
        <template #default="{ row }">
          <span :style="isExpired(row) ? 'color:#f56c6c' : ''">{{ validityText(row) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="sort" label="排序" width="70" />

      <el-table-column label="创建时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'info' : 'success'"
            plain
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '下线' : '上线' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" :close-on-click-modal="false">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">普通</el-radio>
            <el-radio :value="2">重要</el-radio>
            <el-radio :value="3">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">数值越大越靠前</span>
        </el-form-item>
        <el-form-item label="时效">
          <el-radio-group v-model="form.validityMode">
            <el-radio value="permanent">永久</el-radio>
            <el-radio value="duration">指定时长</el-radio>
            <el-radio value="range">指定时间段</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.validityMode === 'duration'" label="">
          <el-input-number v-model="form.durationDays" :min="1" :max="3650" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">天后自动下线</span>
        </el-form-item>
        <el-form-item v-if="form.validityMode === 'range'" label="">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.announcement-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

:deep(.row-expired) {
  color: #909399;
  background-color: #fafafa;
}
</style>
