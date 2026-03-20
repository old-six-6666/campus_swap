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

async function fetchList() {
  loading.value = true
  try {
    const data = await adminApi.listChats({
      keyword: keyword.value || undefined,
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

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除该会话及其所有消息吗？\n\n参与者：${row.user1Nickname} 与 ${row.user2Nickname}`,
    '删除会话',
    { type: 'warning', confirmButtonText: '确定删除' }
  )
  await adminApi.deleteChat(row.conversationId)
  ElMessage.success('会话已删除')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="chat-manage">
    <div class="toolbar">
      <h2>聊天管理</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索用户昵称"
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

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="conversationId" label="ID" width="70" />

      <el-table-column label="参与者甲" min-width="130">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.user1Avatar" />
            <span>{{ row.user1Nickname || row.user1Id }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="参与者乙" min-width="130">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="24" :src="row.user2Avatar" />
            <span>{{ row.user2Nickname || row.user2Id }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="关联商品" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.itemTitle">{{ row.itemTitle }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column label="最后消息" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.lastMsg">{{ row.lastMsg }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>

      <el-table-column label="未读消息" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.totalUnread > 0" type="warning" size="small">{{ row.totalUnread }}</el-tag>
          <span v-else class="muted">0</span>
        </template>
      </el-table-column>

      <el-table-column label="最后活跃" width="110">
        <template #default="{ row }">{{ row.lastMsgTime?.slice(0, 10) || '—' }}</template>
      </el-table-column>

      <el-table-column label="创建时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="100" fixed="right">
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
      @current-change="fetchList"
    />
  </div>
</template>

<style scoped lang="scss">
.chat-manage {
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

.muted {
  color: #909399;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
