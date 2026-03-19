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

const tradeDialogVisible = ref(false)
const myItems = ref([])
const myItemsLoading = ref(false)
const selectedMyItemId = ref(null)
const tradeSubmitting = ref(false)
const activeTrade = ref(null)

const images = computed(() => {
  if (!item.value) return []
  if (item.value.images && item.value.images.length > 0) return item.value.images
  if (item.value.coverImage) return [item.value.coverImage]
  return []
})

const isMine = computed(() => item.value?.sellerId === userStore.userInfo?.id)

const itemStatusMap = { 0: '在售', 1: '已下架', 2: '已售出', 3: '交换中' }
const itemStatusType = { 0: 'success', 1: 'info', 2: 'danger', 3: 'warning' }

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
  if (isMine.value) { showInfo('这是您自己发布的商品'); return }
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
    const res = await itemApi.getMyItems({ page: 1, size: 100 })
    myItems.value = (res.records || []).filter(i => i.status === 0 && i.auditStatus === 1)
  } finally {
    myItemsLoading.value = false
  }
}

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
    const myItem = myItems.value.find(i => i.id === selectedMyItemId.value)
    const msgContent = `【以物换物申请】我想用「${myItem?.title || '我的物品'}」换取您的「${item.value.title}」，点击查看交易详情：/trade/${tradeResult.id}`
    await chatApi.sendMessage({ receiverId: item.value.sellerId, itemId: item.value.id, content: msgContent })
    ElMessage.success('交换申请已发送！对方将在消息中收到通知。')
    tradeDialogVisible.value = false
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
  try {
    activeTrade.value = await tradeApi.getItemActiveTrade(route.params.id)
  } catch {
    activeTrade.value = null
  }
})
</script>

<template>
  <div>
    <div v-loading="loading" class="item-detail">
      <el-empty v-if="!loading && !item" description="商品不存在" />
      <template v-if="item">
        <div class="detail-layout">
          <!-- 左栏：图片 -->
          <div class="image-col">
            <el-carousel
              v-if="images.length > 1"
              height="380px"
              indicator-position="outside"
              class="image-carousel"
            >
              <el-carousel-item v-for="(src, idx) in images" :key="idx">
                <el-image :src="src" fit="contain" class="carousel-image" />
              </el-carousel-item>
            </el-carousel>
            <el-image
              v-else-if="images.length === 1"
              :src="images[0]"
              fit="contain"
              class="main-image"
            />
            <div v-else class="no-image">暂无图片</div>

            <div v-if="images.length > 2" class="thumb-row">
              <el-image
                v-for="(src, idx) in images"
                :key="idx"
                :src="src"
                fit="cover"
                class="thumb"
              />
            </div>
          </div>

          <!-- 右栏：信息 -->
          <div class="info-col">
            <div class="item-tags">
              <el-tag size="small">{{ item.category }}</el-tag>
              <el-tag v-if="item.status !== 0" :type="itemStatusType[item.status]" size="small">
                {{ itemStatusMap[item.status] }}
              </el-tag>
            </div>

            <h2 class="item-title">{{ item.title }}</h2>
            <p class="item-price">¥ {{ item.price }}</p>

            <div class="divider"></div>

            <p class="item-desc">{{ item.description }}</p>

            <div class="divider"></div>

            <div class="seller-info" @click="router.push({ name: 'UserHome', params: { id: item.sellerId } })">
              <el-avatar :size="40" :src="item.sellerAvatar" />
              <div class="seller-detail">
                <span class="seller-name">{{ item.sellerNickname }}</span>
                <span class="seller-label">卖家</span>
              </div>
            </div>

            <div class="action-buttons">
              <el-button type="primary" size="large" class="action-btn" @click="contactSeller">
                联系卖家
              </el-button>
              <el-button
                v-if="!isMine && item.status === 0 && item.auditStatus === 1"
                size="large"
                class="action-btn swap-btn"
                @click="openTradeDialog"
              >
                以物换物
              </el-button>
            </div>

            <div v-if="activeTrade" class="active-trade-tip">
              <el-tag type="warning" size="small">该物品正参与交易中</el-tag>
              <el-button
                v-if="activeTrade.myRole === 'initiator' || activeTrade.myRole === 'receiver'"
                link type="primary" size="small"
                @click="$router.push({ name: 'TradeDetail', params: { id: activeTrade.id } })"
              >
                查看交易 →
              </el-button>
            </div>

            <el-alert
              v-if="item.status === 2"
              title="该物品已完成交换"
              type="info"
              :closable="false"
              style="margin-top: 14px; border-radius: 12px;"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- 以物换物弹窗 -->
    <el-dialog v-model="tradeDialogVisible" title="发起以物换物" width="520px" :close-on-click-modal="false">
      <div class="trade-dialog-content">
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

        <div class="my-items-section">
          <div class="dialog-label">选择您要交换的物品</div>
          <div v-loading="myItemsLoading">
            <el-empty v-if="!myItemsLoading && myItems.length === 0"
              description="您暂无可用于交换的物品" :image-size="60" />
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
        <el-button type="primary" :loading="tradeSubmitting" :disabled="!selectedMyItemId" @click="submitTrade">
          发送交换申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.item-detail {
  max-width: 1000px;
  margin: 0 auto;
  background: $bg-card;
  padding: 28px;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
}

.detail-layout {
  display: flex;
  gap: 40px;

  @media (max-width: 768px) {
    flex-direction: column;
    gap: 24px;
  }
}

.image-col {
  flex: 0 0 45%;
  min-width: 0;
}

.info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.image-carousel {
  border-radius: $border-radius;
  overflow: hidden;
}

.carousel-image, .main-image {
  width: 100%;
  height: 380px;
  border-radius: $border-radius;
}

.main-image { max-height: 400px; }

.no-image {
  width: 100%;
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-subtle;
  color: $text-secondary;
  border-radius: $border-radius;
  font-size: 14px;
}

.thumb-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;

  .thumb {
    width: 72px;
    height: 72px;
    border-radius: $border-radius-sm;
    cursor: pointer;
    border: 2px solid transparent;
    transition: $transition-fast;
    &:hover { border-color: $primary; }
  }
}

.item-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.item-title {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 10px;
  line-height: 1.4;
  letter-spacing: $letter-spacing-base;
}

.item-price {
  font-size: 28px;
  font-weight: 800;
  color: $warning;
  margin-bottom: 16px;
  letter-spacing: 0.02em;
}

.divider {
  height: 1px;
  background: $border-color;
  margin: 14px 0;
}

.item-desc {
  font-size: 14px;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
  letter-spacing: $letter-spacing-base;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  cursor: pointer;
  padding: 12px 14px;
  background: $bg-subtle;
  border-radius: $border-radius-sm;
  transition: $transition-fast;

  &:hover { background: rgba(27, 153, 170, 0.08); }

  .seller-detail {
    display: flex;
    flex-direction: column;
    .seller-name { font-size: 14px; font-weight: 600; color: $text-primary; }
    .seller-label { font-size: 12px; color: $text-secondary; }
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.action-btn {
  flex: 1;
  min-width: 120px;
}

.swap-btn {
  border-color: $warning !important;
  color: #8a6200 !important;
  background: rgba(241, 198, 94, 0.1) !important;
  &:hover {
    background: rgba(241, 198, 94, 0.2) !important;
    border-color: darken(#F1C65E, 10%) !important;
  }
}

.active-trade-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ── 弹窗样式 ─────────────────────────── */
.trade-dialog-content { padding: 4px 0; }

.dialog-label {
  font-size: 13px;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 10px;
  letter-spacing: $letter-spacing-base;
}

.target-item { margin-bottom: 8px; }

.item-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: $bg-subtle;
  border-radius: $border-radius-sm;

  .preview-img {
    width: 64px;
    height: 64px;
    border-radius: $border-radius-sm;
    flex-shrink: 0;
  }

  .preview-title { font-size: 14px; font-weight: 600; color: $text-primary; margin-bottom: 3px; }
  .preview-owner { font-size: 12px; color: $text-secondary; }
}

.swap-icon-center {
  text-align: center;
  font-size: 26px;
  color: $primary;
  margin: 10px 0;
}

.my-items-section { margin-top: 4px; }

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
  border: 2px solid $border-color;
  border-radius: $border-radius-sm;
  padding: 8px;
  cursor: pointer;
  transition: $transition-fast;
  text-align: center;

  &:hover { border-color: $primary; background: rgba(27, 153, 170, 0.05); }
  &.selected { border-color: $primary; background: rgba(27, 153, 170, 0.08); }

  .option-img {
    width: 100%;
    height: 70px;
    border-radius: 10px;
  }

  .option-title {
    font-size: 12px;
    color: $text-primary;
    margin-top: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .check-icon {
    position: absolute;
    top: 5px;
    right: 5px;
    color: $primary;
    font-size: 16px;
    background: #fff;
    border-radius: 50%;
  }
}
</style>