<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { useUserStore } from '@/stores/useUserStore'
import { ElMessageBox } from 'element-plus'
import { showSuccess } from '@/utils/notify'

const userStore = useUserStore()
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const keyword = ref('')

const ROLE_MAP = {
  0: { label: '普通用户', type: '' },
  1: { label: '管理员', type: 'warning' },
  2: { label: '超级管理员', type: 'danger' },
}
const STATUS_MAP = {
  0: { label: '正常', type: 'success' },
  1: { label: '禁用', type: 'info' },
}
// 超级管理员唯一，不允许通过界面将任何人提升为超级管理员
const roleOptions = [
  { value: 0, label: '普通用户' },
  { value: 1, label: '管理员' },
]

async function fetchUsers() {
  loading.value = true
  try {
    const data = await adminApi.listUsers({ keyword: keyword.value, page: page.value, size: pageSize })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${label}用户「${row.nickname}」吗？`, `${label}用户`, {
      type: 'warning',
      confirmButtonText: label,
      cancelButtonText: '取消',
    })
    await adminApi.updateUserStatus(row.id, newStatus)
    showSuccess(`已${label}`)
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') throw e
  }
}

async function changeRole(row, newRole) {
  if (newRole === row.role) return
  const label = roleOptions.find(r => r.value === newRole)?.label
  try {
    await ElMessageBox.confirm(`确定将「${row.nickname}」的角色改为「${label}」吗？`, '修改角色', {
      type: 'warning',
      cancelButtonText: '取消',
    })
    await adminApi.updateUserRole(row.id, newRole)
    showSuccess('角色已更新')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') throw e
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定永久删除用户「${row.nickname}」吗？此操作不可恢复！`, '删除用户', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await adminApi.deleteUser(row.id)
    showSuccess('已删除')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') throw e
  }
}

function isSelf(row) {
  return row.id === userStore.userInfo?.id
}
// 管理员不能操作同级或更高权限用户；超级管理员账号不可被任何人操作
function canOperate(row) {
  if (isSelf(row)) return false
  if (row.role >= 2) return false  // 超级管理员账号只读
  if (userStore.isSuperAdmin) return true
  return row.role < 1
}

onMounted(fetchUsers)
</script>

<template>
  <div class="user-manage">
    <div class="toolbar">
      <h2>用户管理</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索昵称 / 邮箱"
        clearable
        style="width: 240px"
        @keyup.enter="fetchUsers"
        @clear="fetchUsers"
      >
        <template #append>
          <el-button @click="fetchUsers">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="school" label="学校" min-width="120" show-overflow-tooltip />
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <!-- 超管只能修改普通用户/管理员的角色，超管账号本身只读 -->
          <el-select
            v-if="userStore.isSuperAdmin && !isSelf(row) && row.role < 2"
            :model-value="row.role"
            size="small"
            style="width: 120px"
            @change="(val) => changeRole(row, val)"
          >
            <el-option v-for="o in roleOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-tag v-else :type="ROLE_MAP[row.role]?.type" size="small">
            {{ ROLE_MAP[row.role]?.label }}
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
      <el-table-column label="注册时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="canOperate(row)">
            <el-button
              :type="row.status === 1 ? 'success' : 'warning'"
              size="small"
              plain
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-button>
            <el-button
              v-if="userStore.isSuperAdmin"
              size="small"
              type="danger"
              plain
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
          <span v-else-if="isSelf(row)" class="self-label">（本人）</span>
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
      @current-change="fetchUsers"
    />
  </div>
</template>

<style scoped lang="scss">
.user-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.self-label {
  font-size: 12px;
  color: #909399;
}
</style>
