<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)

// 表单弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const editingId = ref(null)
const form = ref({ title: '', content: '', type: 1, sort: 0 })

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
  form.value = { title: '', content: '', type: 1, sort: 0 }
  dialogTitle.value = '新增公告'
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = { title: row.title, content: row.content, type: row.type, sort: row.sort }
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.title?.trim()) return ElMessage.warning('请输入标题')
  if (!form.value.content?.trim()) return ElMessage.warning('请输入内容')
  submitting.value = true
  try {
    if (editingId.value) {
      await adminApi.updateAnnouncement(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await adminApi.createAnnouncement(form.value)
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

onMounted(fetchList)
</script>

<template>
  <div class="announcement-manage">
    <div class="toolbar">
      <h2>公告管理</h2>
      <el-button type="primary" @click="openCreate">+ 新增公告</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
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

      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
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
</style>
