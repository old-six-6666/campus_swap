<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import { Download } from '@element-plus/icons-vue'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const keyword = ref('')
const filterCategory = ref('')
const filterStatus = ref(null)

const CATEGORIES = ['数码', '书籍', '服饰', '生活用品', '其他']
const STATUS_MAP = {
  0: { label: '在售', type: 'success' },
  1: { label: '已下架', type: 'info' },
  2: { label: '已售出', type: 'warning' },
}
const AUDIT_MAP = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已拒绝', type: 'danger' },
}
const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '在售' },
  { value: 1, label: '已下架' },
  { value: 2, label: '已售出' },
]

async function fetchItems() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value ?? undefined,
      page: page.value,
      size: pageSize,
    }
    const data = await adminApi.listItems(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(row, newStatus) {
  if (newStatus === row.status) return
  const label = STATUS_MAP[newStatus]?.label
  const currentLabel = STATUS_MAP[row.status]?.label || '未知状态'
  
  try {
    // 完全按照用户提供的示例样式
    await ElMessageBox.confirm(
      `确定将商品「${row.title}」的状态从「${currentLabel}」修改为「${label}」吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 执行状态修改
    await adminApi.updateItemStatus(row.id, newStatus)
    ElMessage.success('状态已更新')
    fetchItems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`状态修改失败: ${error.message || '未知错误'}`)
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定强制删除商品「${row.title}」吗？此操作不可恢复！`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    await adminApi.deleteItem(row.id)
    ElMessage.success('已删除')
    fetchItems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`删除失败: ${error.message || '未知错误'}`)
    }
  }
}

function resetFilter() {
  keyword.value = ''
  filterCategory.value = ''
  filterStatus.value = null
  page.value = 1
  fetchItems()
}

async function exportToExcel() {
  try {
    loading.value = true
    // 获取所有数据（不分页）
    const params = {
      keyword: keyword.value || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value ?? undefined,
      page: 1,
      size: 10000, // 获取大量数据
    }
    const data = await adminApi.listItems(params)
    const items = data.records || []
    
    if (items.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }
    
    // 准备Excel数据
    const excelData = items.map(item => ({
      'ID': item.id,
      '标题': item.title,
      '分类': item.category,
      '价格(元)': item.price,
      '描述': item.description,
      '发布者': item.sellerNickname,
      '发布者邮箱': item.sellerEmail,
      '审核状态': AUDIT_MAP[item.auditStatus]?.label || '未知',
      '商品状态': STATUS_MAP[item.status]?.label || '未知',
      '封面图片': item.coverImage || '无',
      '发布时间': item.createdAt,
      '更新时间': item.updatedAt,
    }))
    
    // 创建工作簿和工作表
    const worksheet = XLSX.utils.json_to_sheet(excelData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '商品列表')
    
    // 设置列宽
    const wscols = [
      { wch: 8 },   // ID
      { wch: 30 },  // 标题
      { wch: 10 },  // 分类
      { wch: 12 },  // 价格
      { wch: 40 },  // 描述
      { wch: 15 },  // 发布者
      { wch: 20 },  // 发布者邮箱
      { wch: 10 },  // 审核状态
      { wch: 10 },  // 商品状态
      { wch: 30 },  // 封面图片
      { wch: 18 },  // 发布时间
      { wch: 18 },  // 更新时间
    ]
    worksheet['!cols'] = wscols
    
    // 生成Excel文件
    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    
    // 生成文件名
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const filename = `商品列表_${date}.xlsx`
    
    // 下载文件
    saveAs(blob, filename)
    ElMessage.success(`已导出 ${items.length} 条数据`)
    
  } catch (error) {
    console.error('导出Excel失败:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

onMounted(fetchItems)
</script>

<template>
  <div class="item-manage">
    <div class="toolbar">
      <h2>商品管理</h2>
      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="搜索标题"
          clearable
          style="width: 180px"
          @keyup.enter="fetchItems"
          @clear="fetchItems"
        />
        <el-select v-model="filterCategory" placeholder="分类" clearable style="width: 110px" @change="fetchItems">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" style="width: 100px" @change="fetchItems">
          <el-option v-for="o in statusOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
        </el-select>
        <el-button @click="fetchItems">搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <el-button type="success" @click="exportToExcel" :loading="loading">
          <el-icon><Download /></el-icon>
          导出Excel
        </el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="70">
        <template #default="{ row }">
          <el-image
            v-if="row.coverImage"
            :src="row.coverImage"
            fit="cover"
            style="width:48px;height:48px;border-radius:4px"
          />
          <span v-else style="color:#c0c4cc;font-size:12px">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column label="价格" width="90">
        <template #default="{ row }">¥ {{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="sellerNickname" label="发布者" width="100" />
      <el-table-column label="审核" width="90">
        <template #default="{ row }">
          <el-tag :type="AUDIT_MAP[row.auditStatus]?.type" size="small">
            {{ AUDIT_MAP[row.auditStatus]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-select
            :model-value="row.status"
            size="small"
            style="width:110px"
            @change="(val) => handleStatusChange(row, val)"
          >
            <el-option
              v-for="o in statusOptions.filter(s => s.value !== null)"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="(val) => { page = val; fetchItems() }"
    />
  </div>
</template>

<style scoped lang="scss">
.item-manage {
  h2 { margin: 0; }
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>

<style lang="scss">
/* 消息提示美化 */
.el-message {
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  border: none;
  padding: 15px 20px;
  
  &--success {
    background: linear-gradient(135deg, #67C23A, #529b2d);
    border-left: 4px solid #85ce61;
  }
  
  &--error {
    background: linear-gradient(135deg, #F56C6C, #d64545);
    border-left: 4px solid #f78989;
  }
  
  &--info {
    background: linear-gradient(135deg, #909399, #73767a);
    border-left: 4px solid #a6a9ad;
  }
  
  &--warning {
    background: linear-gradient(135deg, #E6A23C, #c99133);
    border-left: 4px solid #ebb563;
  }
  
  .el-message__content {
    color: white;
    font-weight: 500;
    font-size: 14px;
  }
  
  .el-icon {
    color: white;
  }
}

/* 强制弹窗居中 */
.el-overlay {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  
  .el-overlay-dialog {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    
    .el-message-box {
      position: relative !important;
      top: auto !important;
      left: auto !important;
      transform: none !important;
      margin: 0 !important;
    }
  }
}
</style>
