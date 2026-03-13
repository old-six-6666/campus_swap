<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { useUserStore } from '@/stores/useUserStore'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const keyword = ref('')

/** 权限编辑弹窗状态 */
const permDialog = ref(false)
const permTarget = ref(null)       // 当前正在编辑权限的管理员行
const permChecked = ref([])        // 弹窗中勾选的权限列表
const permSaving = ref(false)

const PERM_OPTIONS = [
  { code: 'USER_MANAGE', label: '用户管理', desc: '可访问用户管理页面，禁/启用普通用户' },
  { code: 'ITEM_MANAGE', label: '商品管理', desc: '可访问商品管理页面，修改/删除商品' },
  { code: 'ITEM_AUDIT', label: '商品审核', desc: '可审核用户提交的商品，通过或拒绝上架' },
]

const STATUS_MAP = {
  0: { label: '正常', type: 'success' },
  1: { label: '禁用', type: 'info' },
}

async function fetchAdmins() {
  loading.value = true
  try {
    const data = await adminApi.listAdmins({ keyword: keyword.value, page: page.value, size: pageSize })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 打开权限编辑弹窗 */
function openPermDialog(row) {
  permTarget.value = row
  permChecked.value = [...(row.permissions || [])]
  permDialog.value = true
}

/** 保存权限 */
async function savePermissions() {
  permSaving.value = true
  try {
    await adminApi.setAdminPermissions(permTarget.value.id, permChecked.value)
    ElMessage.success('权限已更新')
    permDialog.value = false
    // 同步列表中该行的权限
    permTarget.value.permissions = [...permChecked.value]
  } finally {
    permSaving.value = false
  }
}

/** 将管理员降为普通用户 */
async function demoteToUser(row) {
  await ElMessageBox.confirm(
    `确定将「${row.nickname}」降级为普通用户吗？该操作会清空其所有权限。`,
    '降级管理员',
    { type: 'warning', confirmButtonText: '确认降级' }
  )
  await adminApi.updateUserRole(row.id, 0)
  // 同时清空权限
  await adminApi.setAdminPermissions(row.id, [])
  ElMessage.success('已降为普通用户')
  fetchAdmins()
}

function permLabel(code) {
  return PERM_OPTIONS.find(o => o.code === code)?.label ?? code
}

onMounted(fetchAdmins)
</script>

<template>
  <div class="admin-manage">
    <div class="toolbar">
      <h2>管理员管理</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索昵称 / 邮箱"
        clearable
        style="width: 240px"
        @keyup.enter="fetchAdmins"
        @clear="fetchAdmins"
      >
        <template #append>
          <el-button @click="fetchAdmins">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
      title="此页面仅超级管理员可见。在此可查看所有管理员账号并设置其细粒度权限。"
    />

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="school" label="学校" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type" size="small">
            {{ STATUS_MAP[row.status]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已有权限" min-width="200">
        <template #default="{ row }">
          <template v-if="row.permissions && row.permissions.length">
            <el-tag
              v-for="code in row.permissions"
              :key="code"
              type="success"
              size="small"
              style="margin-right: 4px"
            >
              {{ permLabel(code) }}
            </el-tag>
          </template>
          <span v-else class="no-perm">无权限</span>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openPermDialog(row)">
            编辑权限
          </el-button>
          <el-button size="small" type="warning" plain @click="demoteToUser(row)">
            降为用户
          </el-button>
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
      @current-change="fetchAdmins"
    />

    <!-- 权限编辑弹窗 -->
    <el-dialog
      v-model="permDialog"
      :title="`编辑权限 — ${permTarget?.nickname}`"
      width="420px"
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
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.no-perm {
  font-size: 12px;
  color: #c0c4cc;
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
