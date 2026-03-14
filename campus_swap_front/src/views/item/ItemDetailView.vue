<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { tradeApi } from '@/api/modules/trade'
import { chatApi } from '@/api/modules/chat'
import { useUserStore } from '@/stores/useUserStore'
import { showWarning, showInfo } from '@/utils/notify'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const item = ref(null)
const loading = ref(false)

// ===== 以物换物相关 =====
const tradeDialogVisible = ref(false)
const myItems = ref([])
const myItemsLoading = ref(false)
const selectedMyItemId = ref(null)
const tradeSubmitting = ref(false)
const activeTrade = ref(null)  // 该物品当前活跃的交易

// 有图列表：优先用 images，无则用 coverImage
const images = computed(() => {
  if (!item.value) return []
  if (item.value.images && item.value.images.length > 0) return item.value.images
  if (item.value.coverImage) return [item.value.coverImage]
  return []
})

// 是否是自己发布的商品
const isMine = computed(() => item.value?.sellerId === userStore.userInfo?.id)

// 物品状态
const itemStatusMap = { 0: '在售', 1: '已下架', 2: '已售出', 3: '交换中' }
const itemStatusType = { 0: 'success', 1: 'info', 2: 'danger', 3: 'warning' }

// 是否可以发起交换（在售 + 已审核通过 + 不是自己的）
const canInitiateTrade = computed(() =>
  item.value &&
  item.value.status === 0 &&
  item.value.auditStatus === 1 &&
  !isMine.value &&
  userStore.isLoggedIn
)

function contactSeller() {
  if (!userStore.isLoggedIn) {
    showWarning('请先登录后再联系卖家')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (isMine.value) {
    showInfo('这是您自己发布的商品')
    return
  }
  router.push({
    name: 'Chat',
    query: {
      to: item.value.sellerId,
      item: item.value.id,
      nickname: item.value.sellerNickname,
      avatar: item.value.sellerAvatar || '',
      itemTitle: item.value.title,
    },
  })
}

// 点击"发起以物换物"
async function openTradeDialog() {
  if (!userStore.isLoggedIn) {
    showWarning('请先登录后再发起交换')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  selectedMyItemId.value = null
  tradeDialogVisible.value = true
  myItemsLoading.value = true
  try {
    // 加载用户自己的可交换物品（在售 + 已审核通过）
    const res = await itemApi.getMyItems({ page: 1, size: 100 })
    myItems.value = (res.records || []).filter(i => i.status === 0 && i.auditStatus === 1)
  } finally {
    myItemsLoading.value = false
  }
}

// 确认发起交换
async function submitTrade() {
  if (!selectedMyItemId.value) {
    ElMessage.warning('请先选择您要拿来交换的物品')
    return
  }
  tradeSubmitting.value = true
  try {
    const tradeResult = await tradeApi.initiate({
      initiatorItemId: selectedMyItemId.value,
      receiverItemId: item.value.id,
      receiverId: item.value.sellerId,
      remark: `我想用我的物品换取您的「${item.value.title}」`,
    })

    // 在消息中发送交易链接
    const myItem = myItems.value.find(i => i.id === selectedMyItemId.value)
    const msgContent = `【以物换物申请】我想用「${myItem?.title || '我的物品'}」换取您的「${item.value.title}」，点击查看交易详情：/trade/${tradeResult.id}`
    await chatApi.sendMessage({
      receiverId: item.value.sellerId,
      itemId: item.value.id,
      content: msgContent,
    })

    ElMessage.success('交换申请已发送！对方将在消息中收到通知。')
    tradeDialogVisible.value = false

    // 跳转到交易详情页
    router.push({ name: 'TradeDetail', params: { id: tradeResult.id } })
  } catch (e) {
    ElMessage.error(e?.message || '发起交换失败')
  } finally {
    tradeSubmitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    item.value = await itemApi.getDetail(route.params.id)
  } finally {
    loading.value = false
  }
  // 查询该物品的活跃交易（独立请求，失败不影响商品详情显示）
  try {
    activeTrade.value = await tradeApi.getItemActiveTrade(route.params.id)
  } catch {
    activeTrade.value = null
  }
})
</script>

<template>
  <div v-loading="loading" class="item-detail">
    <el-empty v-if="!loading && !item" description="商品不存在" />
    <template v-if="item">
      <el-row :gutter="24">
        <!-- 左栏：图片展示 -->
        <el-col :span="10">
          <!-- 多图时使用轮播 -->
          <el-carousel
            v-if="images.length > 1"
            height="360px"
            indicator-position="outside"
            class="image-carousel"
          >
            <el-carousel-item v-for="(src, idx) in images" :key="idx">
              <el-image :src="src" fit="contain" class="carousel-image" />
            </el-carousel-item>
          </el-carousel>
          <!-- 单图直接展示 -->
          <el-image
            v-else-if="images.length === 1"
            :src="images[0]"
            fit="contain"
            class="main-image"
          />
          <div v-else class="no-image">暂无图片</div>
        </el-col>

        <!-- 右栏：商品信息 -->
        <el-col :span="14">
          <h2 class="item-title">{{ item.title }}</h2>
          <p class="item-price">¥ {{ item.price }}</p>
          <div class="item-tags">
            <el-tag class="item-category" size="small">{{ item.category }}</el-tag>
            <!-- 商品状态标签 -->
            <el-tag
              v-if="item.status !== 0"
              :type="itemStatusType[item.status] || 'info'"
              size="small"
            >
              {{ itemStatusMap[item.status] || item.status }}
            </el-tag>
            <!-- 交换中提示 -->
            <el-tag v-if="item.status === 3" type="warning" size="small">
              🔄 已被换走
            </el-tag>
          </div>
          <el-divider />
          <p class="item-desc">{{ item.description }}</p>
          <el-divider />
          <!-- 卖家信息 -->
          <div class="seller-info">
            <el-avatar :size="36" :src="item.sellerAvatar" />
            <span class="seller-name">{{ item.sellerNickname }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button type="primary" size="large" class="contact-btn" @click="contactSeller">
              联系卖家
            </el-button>

            <!-- 以物换物按钮 -->
            <el-button
              v-if="!isMine && item.status === 0 && item.auditStatus === 1"
              type="warning"
              size="large"
              class="swap-btn"
              @click="openTradeDialog"
            >
              🔄 以物换物
            </el-button>

            <!-- 活跃交易提示 -->
            <div v-if="activeTrade" class="active-trade-tip">
              <el-tag type="warning">该物品正参与交易中</el-tag>
              <el-button
                v-if="activeTrade.myRole === 'initiator' || activeTrade.myRole === 'receiver'"
                link
                type="primary"
                @click="$router.push({ name: 'TradeDetail', params: { id: activeTrade.id } })"
              >
                查看交易 →
              </el-button>
            </div>

            <!-- 已完成交换 -->
            <el-alert
              v-if="item.status === 2"
              title="该物品已完成交换"
              type="info"
              :closable="false"
              class="sold-alert"
            />
          </div>
        </el-col>
      </el-row>

      <!-- 底部缩略图导航（3 张以上时展示） -->
      <div v-if="images.length > 2" class="thumb-row">
        <el-image
          v-for="(src, idx) in images"
          :key="idx"
          :src="src"
          fit="cover"
          class="thumb"
        />
      </div>
    </template>
  </div>

  <!-- ===== 以物换物发起对话框 ===== -->
  <el-dialog
    v-model="tradeDialogVisible"
    title="🔄 发起以物换物"
    width="520px"
    :close-on-click-modal="false"
  >
    <div class="trade-dialog-content">
      <!-- 目标物品 -->
      <div class="target-item">
        <div class="dialog-label">对方物品</div>
        <div class="item-preview">
          <el-image :src="item?.coverImage" fit="cover" class="preview-img" />
          <div>
            <div class="preview-title">{{ item?.title }}</div>
            <div class="preview-owner">{{ item?.sellerNickname }}</div>
          </div>
        </div>
      </div>

      <div class="swap-icon-center">⇌</div>

      <!-- 选择自己的物品 -->
      <div class="my-items-section">
        <div class="dialog-label">选择您要交换的物品</div>
        <div v-loading="myItemsLoading">
          <el-empty v-if="!myItemsLoading && myItems.length === 0" description="您暂无可用于交换的物品（需在售且已审核通过）" :image-size="60" />
          <div class="my-items-grid">
            <div
              v-for="myItem in myItems"
              :key="myItem.id"
              class="my-item-option"
              :class="{ selected: selectedMyItemId === myItem.id }"
              @click="selectedMyItemId = myItem.id"
            >
              <el-image :src="myItem.coverImage" fit="cover" class="option-img" />
              <div class="option-title">{{ myItem.title }}</div>
              <el-icon v-if="selectedMyItemId === myItem.id" class="check-icon"><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="tradeDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="tradeSubmitting"
        :disabled="!selectedMyItemId"
        @click="submitTrade"
      >
        发送交换申请
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.item-detail {
  max-width: 1000px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.image-carousel {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 360px;
}

.main-image {
  width: 100%;
  max-height: 400px;
  border-radius: 8px;
}

.no-image {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  border-radius: 8px;
}

.item-title {
  font-size: 20px;
  margin-bottom: 8px;
}

.item-price {
  font-size: 24px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 8px;
}

.item-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.item-desc {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  .seller-name {
    font-size: 14px;
    color: #303133;
  }
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.contact-btn, .swap-btn {
  width: 150px;
}

.active-trade-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.sold-alert {
  width: 100%;
  border-radius: 6px;
}

.thumb-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
  .thumb {
    width: 80px;
    height: 80px;
    border-radius: 4px;
    cursor: pointer;
    border: 2px solid transparent;
    &:hover { border-color: #409eff; }
  }
}

// ===== 对话框样式 =====
.trade-dialog-content {
  padding: 4px 0;
}

.dialog-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.target-item {
  margin-bottom: 8px;
}

.item-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;
  .preview-img {
    width: 60px;
    height: 60px;
    border-radius: 6px;
    flex-shrink: 0;
    border: 1px solid #ebeef5;
  }
  .preview-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
  .preview-owner {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}

.swap-icon-center {
  text-align: center;
  font-size: 24px;
  color: #409eff;
  margin: 8px 0;
}

.my-items-section {
  margin-top: 4px;
}

.my-items-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  max-height: 240px;
  overflow-y: auto;
  padding: 4px;
}

.my-item-option {
  position: relative;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  &:hover { border-color: #409eff; background: #ecf5ff; }
  &.selected { border-color: #409eff; background: #ecf5ff; }

  .option-img {
    width: 100%;
    height: 70px;
    border-radius: 4px;
    border: 1px solid #ebeef5;
  }
  .option-title {
    font-size: 12px;
    color: #303133;
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .check-icon {
    position: absolute;
    top: 4px;
    right: 4px;
    color: #409eff;
    font-size: 16px;
  }
}
</style>
