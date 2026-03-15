<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const filterSchool = ref('')
const filterKeyword = ref('')

// 添加单条
const addVisible = ref(false)
const addForm = ref({ school: '', studentId: '', realName: '', extraInfo: '' })
const addLoading = ref(false)
const addFormRef = ref()

// 批量导入
const batchVisible = ref(false)
const batchText = ref('')
const batchLoading = ref(false)

async function fetchRecords() {
  loading.value = true
  try {
    const data = await adminApi.listStudentRecords({
      school: filterSchool.value || undefined,
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

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除「${row.school}」学号 ${row.studentId}（${row.realName}）的档案吗？`,
    '删除档案',
    { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
  )
  await adminApi.deleteStudentRecord(row.id)
  ElMessage.success('已删除')
  fetchRecords()
}

async function submitAdd() {
  await addFormRef.value.validate()
  addLoading.value = true
  try {
    await adminApi.addStudentRecord(addForm.value)
    ElMessage.success('添加成功')
    addVisible.value = false
    addForm.value = { school: '', studentId: '', realName: '', extraInfo: '' }
    fetchRecords()
  } finally {
    addLoading.value = false
  }
}

function openAdd() {
  addForm.value = { school: '', studentId: '', realName: '', extraInfo: '' }
  addVisible.value = true
}

// 批量导入：每行格式 "学校,学号,姓名[,附加信息]"
async function submitBatch() {
  const lines = batchText.value.trim().split('\n').filter(l => l.trim())
  if (!lines.length) {
    ElMessage.warning('内容不能为空')
    return
  }
  const records = []
  const parseErrors = []
  lines.forEach((line, idx) => {
    const parts = line.split(',').map(p => p.trim())
    if (parts.length < 3 || !parts[0] || !parts[1] || !parts[2]) {
      parseErrors.push(`第${idx + 1}行格式错误（需：学校,学号,姓名[,附加信息]）`)
      return
    }
    records.push({ school: parts[0], studentId: parts[1], realName: parts[2], extraInfo: parts[3] || '' })
  })
  if (parseErrors.length) {
    ElMessage.error(parseErrors.slice(0, 3).join('\n') + (parseErrors.length > 3 ? `…等${parseErrors.length}处错误` : ''))
    return
  }
  batchLoading.value = true
  try {
    const count = await adminApi.batchImportStudentRecords({ records })
    ElMessage.success(`成功导入 ${count} 条（共 ${records.length} 条）`)
    batchVisible.value = false
    batchText.value = ''
    fetchRecords()
  } finally {
    batchLoading.value = false
  }
}

onMounted(fetchRecords)
</script>

<template>
  <div class="student-manage">
    <div class="toolbar">
      <h2>学生档案管理</h2>
      <div class="toolbar-right">
        <el-input
          v-model="filterSchool"
          placeholder="按学校筛选"
          clearable
          style="width: 180px"
          @keyup.enter="fetchRecords"
          @clear="fetchRecords"
        />
        <el-input
          v-model="filterKeyword"
          placeholder="搜索学号 / 姓名"
          clearable
          style="width: 200px"
          @keyup.enter="fetchRecords"
          @clear="fetchRecords"
        >
          <template #append>
            <el-button @click="fetchRecords">搜索</el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="openAdd">+ 添加档案</el-button>
        <el-button @click="batchVisible = true">批量导入</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="school" label="学校" min-width="140" show-overflow-tooltip />
      <el-table-column prop="studentId" label="学号" min-width="130" />
      <el-table-column prop="realName" label="真实姓名" min-width="100" />
      <el-table-column prop="extraInfo" label="附加信息" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createdByNickname" label="录入人" width="110" />
      <el-table-column label="录入时间" width="110">
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
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="fetchRecords"
    />

    <!-- 添加档案弹窗 -->
    <el-dialog v-model="addVisible" title="添加学生档案" width="480px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" label-width="80px">
        <el-form-item label="学校" prop="school" :rules="[{ required: true, message: '请填写学校名称' }]">
          <el-input v-model="addForm.school" placeholder="例：某某大学" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId" :rules="[{ required: true, message: '请填写学号' }]">
          <el-input v-model="addForm.studentId" placeholder="例：20210001" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName" :rules="[{ required: true, message: '请填写真实姓名' }]">
          <el-input v-model="addForm.realName" />
        </el-form-item>
        <el-form-item label="附加信息">
          <el-input v-model="addForm.extraInfo" placeholder="选填：专业、年级等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="batchVisible" title="批量导入学生档案" width="560px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        每行一条记录，格式：<strong>学校,学号,姓名[,附加信息]</strong>（逗号分隔）<br>
        同一学校内已存在的学号将自动跳过，不会报错。
      </el-alert>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="10"
        placeholder="某某大学,20210001,张三,计算机科学与技术2021级&#10;某某大学,20210002,李四"
      />
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="submitBatch">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.student-manage {
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
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
