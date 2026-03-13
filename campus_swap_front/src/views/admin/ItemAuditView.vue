<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 15
const keyword = ref('')
const filterCategory = ref('')

const CATEGORIES = ['数码', '书籍', '服饰', '生活用品', '其他']

/** 详情弹窗 */
const detailDialog = ref(false)
const detailItem = ref(null)
const detailImages = computed(() => {
  if (!detailItem.value) return []
  if (detailItem.value.images?.length) return detailItem.value.images
  if (detailItem.value.coverImage) return [detailItem.value.coverImage]
  return []
})

/** 拒绝弹窗 */
const rejectDialog = ref(false)
const rejectTarget = ref(null)
const rejectRemark = ref('')
const rejectSaving = ref(false)

async function fetchItems() {
  loading.value = true
  try {
    const data = await adminApi.listPendingItems({
      keyword: keyword.value || undefined,
      category: filterCategory.value || undefined,
      page: page.value,
      size: pageSize,
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detailItem.value = row
  detailDialog.value = true
}

async function handleApprove(row) {
  await ElMessageBox.confirm(`确定通过商品「${row.title}」的审核？通过后将在主页展示。`, '通过审核', {
    type: 'success',
    confirmButtonText: '通过',
  })
  await adminApi.auditItem(row.id, 1, '')
  ElMessage.success('已通过')
  detailDialog.value = false
  fetchItems()
}

function openRejectDialog(row) {
  rejectTarget.value = row
  rejectRemark.value = ''
  rejectDialog.value = true
}

async function confirmReject() {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectSaving.value = true
  try {
    await adminApi.auditItem(rejectTarget.value.id, 2, rejectRemark.value.trim())
    ElMessage.success('已拒绝')
    rejectDialog.value = false
    detailDialog.value = false
    fetchItems()
  } finally {
    rejectSaving.value = false
  }
}

function resetFilter() {
  keyword.value = ''
  filterCategory.value = ''
  page.value = 1
  fetchItems()
}

onMounted(fetchItems)
</script>

<template>
  <div class="item-audit">
    <div class="toolbar">
      <h2>商品审核 <el-badge v-if="total" :value="total" type="danger" /></h2>
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
        <el-button @click="fetchItems">搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="暂无待审核商品" />

    <el-table v-else v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="70">
        <template #default="{ row }">
          <el-image
            v-if="row.coverImage"
            :src="row.coverImage"
            fit="cover"
            style="width:48px;height:48px;border-radius:4px;cursor:pointer"
            @click="openDetail(row)"
          />
          <span v-else style="color:#c0c4cc;font-size:12px">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="商品标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="80" />
      <el-table-column label="价格" width="90">
        <template #default="{ row }">¥ {{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="sellerNickname" label="发布者" width="100" />
      <el-table-column label="提交时间" width="110">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">查看详情</el-button>
          <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
          <el-button size="small" type="danger" plain @click="openRejectDialog(row)">拒绝</el-button>
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
      @current-change="fetchItems"
    />

    <!-- 商品详情弹窗（与主页展示一致） -->
    <el-dialog
      v-model="detailDialog"
      :title="detailItem?.title"
      width="800px"
      top="5vh"
    >
      <div v-if="detailItem" class="detail-body">
        <el-row :gutter="24">
          <!-- 左：图片 -->
          <el-col :span="11">
            <el-carousel
              v-if="detailImages.length > 1"
              height="300px"
              indicator-position="outside"
            >
              <el-carousel-item v-for="(src, idx) in detailImages" :key="idx">
                <el-image :src="src" fit="contain" style="width:100%;height:300px" />
              </el-carousel-item>
            </el-carousel>
            <el-image
              v-else-if="detailImages.length === 1"
              :src="detailImages[0]"
              fit="contain"
              style="width:100%;max-height:340px;border-radius:6px"
            />
            <div v-else class="no-image">暂无图片</div>
          </el-col>

          <!-- 右：信息 -->
          <el-col :span="13">
            <h2 class="d-title">{{ detailItem.title }}</h2>
            <p class="d-price">¥ {{ detailItem.price }}</p>
            <el-tag size="small">{{ detailItem.category }}</el-tag>
            <el-divider />
            <p class="d-desc">{{ detailItem.description }}</p>
            <el-divider />
            <div class="d-seller">
              <el-avatar :size="32" :src="detailItem.sellerAvatar" />
              <span class="d-seller-name">{{ detailItem.sellerNickname }}</span>
            </div>
            <p class="d-time">提交时间：{{ detailItem.createdAt?.slice(0, 10) }}</p>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
        <el-button type="danger" plain @click="openRejectDialog(detailItem)">拒绝</el-button>
        <el-button type="success" @click="handleApprove(detailItem)">通过审核</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog
      v-model="rejectDialog"
      :title="`拒绝商品 — ${rejectTarget?.title}`"
      width="420px"
    >
      <el-form>
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="3"
            placeholder="请填写拒绝原因，将会显示给用户"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.item-audit {
  h2 {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
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

/* 详情弹窗样式 */
.detail-body {
  padding: 4px 0;
}

.no-image {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  border-radius: 6px;
}

.d-title {
  font-size: 20px;
  margin: 0 0 10px;
  color: #303133;
}

.d-price {
  font-size: 24px;
  font-weight: 600;
  color: #f56c6c;
  margin: 0 0 10px;
}

.d-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  margin: 0;
}

.d-seller {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;

  .d-seller-name {
    font-size: 14px;
    color: #303133;
  }
}

.d-time {
  font-size: 12px;
  color: #909399;
  margin: 0;
}
</style>
