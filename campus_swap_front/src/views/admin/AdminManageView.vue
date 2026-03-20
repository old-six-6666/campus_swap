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
const filterRole = ref(null)

/** 权限编辑弹窗状态 */
const permDialog = ref(false)
const permTarget = ref(null)
const permChecked = ref([])
const permSaving = ref(false)

const PERM_OPTIONS = [
  { code: 'USER_MANAGE',    label: '用户管理',   desc: '可访问用户管理页面，禁/启用普通用户' },
  { code: 'ITEM_MANAGE',    label: '商品管理',   desc: '可访问商品管理页面，修改/删除商品' },
  { code: 'ITEM_AUDIT',     label: '商品审核',   desc: '可审核用户提交的商品，通过或拒绝上架' },
  { code: 'STUDENT_MANAGE', label: '学生管理',   desc: '可管理学生档案与认证审核' },
  { code: 'CONTENT_AUDIT',  label: '举报审核',   desc: '可处理用户举报内容' },
  { code: 'CHAT_MANAGE',    label: '聊天管理',   desc: '可查看和删除用户会话' },
  { code: 'TRADE_MANAGE',   label: '交易管理',   desc: '可查看所有交易并强制终止' },
]

const ROLE_MAP = {
  0: { label: '普通用户', type: '' },
  1: { label: '管理员',   type: 'warning' },
  2: { label: '超级管理员', type: 'danger' },
}
const STATUS_MAP = {
  0: { label: '正常', type: 'success' },
  1: { label: '禁用', type: 'info' },
}
const roleOptions = [
  { value: 0, label: '普通用户' },
  { value: 1, label: '管理员' },
]
const filterRoleOptions = [
  { value: null, label: '全部角色' },
  { value: 0,    label: '普通用户' },
  { value: 1,    label: '管理员' },
]

async function fetchUsers() {
  loading.value = true
  try {
    const data = await adminApi.listUsers({
      keyword: keyword.value || undefined,
      role: filterRole.value ?? undefined,
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
  fetchUsers()
}

async function changeRole(row, newRole) {
  if (newRole === row.role) return
  const action = newRole === 1 ? '提升为管理员' : '降级为普通用户'
  try {
    await ElMessageBox.confirm(
      `确定将「${row.nickname}」${action}吗？`,
      '修改角色',
      { type: 'warning', cancelButtonText: '取消' }
    )
    await adminApi.updateUserRole(row.id, newRole)
    if (newRole === 0) {
      // 降级时同时清空权限
      await adminApi.setAdminPermissions(row.id, [])
    }
    showSuccess('角色已更新')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') throw e
  }
}

function openPermDialog(row) {
  permTarget.value = row
  permChecked.value = [...(row.permissions || [])]
  permDialog.value = true
}

async function savePermissions() {
  permSaving.value = true
  try {
    await adminApi.setAdminPermissions(permTarget.value.id, permChecked.value)
    showSuccess('权限已更新')
    permDialog.value = false
    permTarget.value.permissions = [...permChecked.value]
  } finally {
    permSaving.value = false
  }
}

function permLabel(code) {
  return PERM_OPTIONS.find(o => o.code === code)?.label ?? code
}

function isSelf(row) {
  return row.id === userStore.userInfo?.id
}

onMounted(fetchUsers)
</script>

<template>
  <div class="admin-manage">
    <div class="toolbar">
      <h2>管理员管理</h2>
      <div class="toolbar-right">
        <el-select
          v-model="filterRole"
          style="width: 120px"
          @change="onSearch"
        >
          <el-option
            v-for="o in filterRoleOptions"
            :key="String(o.value)"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索昵称 / 邮箱"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        >
          <template #append>
            <el-button @click="onSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
      title="在此可将普通用户提升为管理员并分配权限，或对管理员进行降级。"
    />

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="school" label="学校" min-width="120" show-overflow-tooltip />

      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-select
            v-if="!isSelf(row) && row.role < 2"
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

      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="已有权限" min-width="220">
        <template #default="{ row }">
          <template v-if="row.role === 1">
            <template v-if="row.permissions && row.permissions.length">
              <el-tag
                v-for="code in row.permissions"
                :key="code"
                type="success"
                size="small"
                style="margin: 2px"
              >{{ permLabel(code) }}</el-tag>
            </template>
            <span v-else class="no-perm">无权限</span>
          </template>
          <span v-else class="no-perm">—</span>
        </template>
      </el-table-column>

      <el-table-column label="注册时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.role === 1 && !isSelf(row)"
            size="small"
            type="primary"
            plain
            @click="openPermDialog(row)"
          >编辑权限</el-button>
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

    <!-- 权限编辑弹窗 -->
    <el-dialog
      v-model="permDialog"
      :title="`编辑权限 — ${permTarget?.nickname}`"
      width="460px"
    >
      <div class="perm-dialog-body">
        <p class="perm-hint">勾选该管理员可使用的功能模块：</p>
        <el-checkbox-group v-model="permChecked" class="perm-list">
          <el-checkbox
            v-for="opt in PERM_OPTIONS"
            :key="opt.code"
            :value="opt.code"
            class="perm-item"
          >
            <span class="perm-label">{{ opt.label }}</span>
            <span class="perm-desc">{{ opt.desc }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permDialog = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.admin-manage {
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

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.no-perm {
  font-size: 12px;
  color: #c0c4cc;
}

.self-label {
  font-size: 12px;
  color: #909399;
}

.perm-dialog-body {
  .perm-hint {
    margin: 0 0 12px;
    color: #606266;
    font-size: 14px;
  }
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.perm-item {
  display: flex;
  align-items: flex-start;
  height: auto;

  .perm-label {
    font-weight: 600;
    font-size: 14px;
    color: #303133;
  }

  .perm-desc {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}
</style>
