<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tradeApi } from '@/api/modules/trade'

const router = useRouter()

const loading = ref(false)
const trades = ref([])
const activeTab = ref('ALL')

const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '等待匹配', value: 'PENDING_MATCH' },
  { label: '进行中', value: 'ACTIVE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已终止', value: 'TERMINATED' },
]

// 进行中 = 除 PENDING_MATCH、COMPLETED、TERMINATED、AUDIT_REJECTED 之外的状态
const ACTIVE_STATUSES = ['MATCHED', 'AUDIT_PENDING', 'AUDIT_PASSED', 'WAITING_DELIVERY', 'BOTH_DELIVERED', 'WAITING_CONFIRM_RECEIPT']

const filteredTrades = computed(() => {
  if (activeTab.value === 'ALL') return trades.value
  if (activeTab.value === 'ACTIVE') return trades.value.filter(t => ACTIVE_STATUSES.includes(t.status))
  return trades.value.filter(t => t.status === activeTab.value)
})

async function loadTrades() {
  loading.value = true
  try {
    trades.value = await tradeApi.getMyTrades()
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push({ name: 'TradeDetail', params: { id } })
}

const STATUS_CONFIG = {
  PENDING_MATCH:           { label: '等待匹配', type: 'warning' },
  MATCHED:                 { label: '已匹配',   type: 'primary' },
  AUDIT_PENDING:           { label: '审核中',   type: 'warning' },
  AUDIT_PASSED:            { label: '审核通过', type: 'success' },
  AUDIT_REJECTED:          { label: '审核驳回', type: 'danger'  },
  WAITING_DELIVERY:        { label: '等待发货', type: 'primary' },
  BOTH_DELIVERED:          { label: '双方已发货', type: 'primary' },
  WAITING_CONFIRM_RECEIPT: { label: '等待收货', type: 'primary' },
  COMPLETED:               { label: '已完成',   type: 'success' },
  TERMINATED:              { label: '已终止',   type: 'info'    },
}

function statusTag(status) {
  return STATUS_CONFIG[status] || { label: status, type: 'info' }
}

onMounted(loadTrades)
</script>

<template>
  <div class="trade-list">
    <div class="page-header">
      <h2>我的以物换物</h2>
    </div>

    <!-- 标签页筛选 -->
    <el-tabs v-model="activeTab" class="trade-tabs">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>

    <div v-loading="loading" class="trade-cards">
      <el-empty v-if="!loading && filteredTrades.length === 0" description="暂无交易记录" />

      <div
        v-for="trade in filteredTrades"
        :key="trade.id"
        class="trade-card"
        @click="goDetail(trade.id)"
      >
        <!-- 状态 & 编号 -->
        <div class="card-header">
          <span class="trade-no">{{ trade.tradeNo }}</span>
          <el-tag :type="statusTag(trade.status).type" size="small">
            {{ statusTag(trade.status).label }}
          </el-tag>
        </div>

        <!-- 物品交换展示 -->
        <div class="item-exchange">
          <!-- 甲方物品 -->
          <div class="item-side">
            <el-image
              :src="trade.initiatorItemCoverImage"
              class="item-thumb"
              fit="cover"
            >
              <template #error><div class="thumb-placeholder">无图</div></template>
            </el-image>
            <div class="item-info">
              <div class="item-title">{{ trade.initiatorItemTitle || '（待填写）' }}</div>
              <div class="item-owner">{{ trade.initiatorNickname }}</div>
            </div>
          </div>

          <!-- 箭头 -->
          <div class="exchange-arrow">⇌</div>

          <!-- 乙方物品 -->
          <div class="item-side">
            <el-image
              :src="trade.receiverItemCoverImage"
              class="item-thumb"
              fit="cover"
            >
              <template #error><div class="thumb-placeholder">待匹配</div></template>
            </el-image>
            <div class="item-info">
              <div class="item-title">{{ trade.receiverItemTitle || '（等待确认）' }}</div>
              <div class="item-owner">{{ trade.receiverNickname || '等待中...' }}</div>
            </div>
          </div>
        </div>

        <!-- 底部：时间 & 我的角色 -->
        <div class="card-footer">
          <span class="role-badge" :class="trade.myRole">
            {{ trade.myRole === 'initiator' ? '发起方' : trade.myRole === 'receiver' ? '接收方' : '' }}
          </span>
          <span class="created-time">{{ new Date(trade.createdAt).toLocaleString('zh-CN') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.trade-list {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 8px;
  h2 {
    font-size: 22px;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: $letter-spacing-base;
  }
}

.trade-tabs { margin-bottom: 16px; }

.trade-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trade-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 16px 20px;
  cursor: pointer;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
  transition: $transition-base;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
    border-color: $primary-light;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .trade-no {
    font-size: 12px;
    color: $text-secondary;
    font-family: monospace;
  }
}

.item-exchange {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-side {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-thumb {
  width: 56px;
  height: 56px;
  border-radius: $border-radius-sm;
  flex-shrink: 0;
  border: 1px solid $border-color;

  .thumb-placeholder {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $bg-subtle;
    color: $text-secondary;
    font-size: 12px;
    border-radius: $border-radius-sm;
  }
}

.item-info {
  .item-title {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 160px;
    letter-spacing: $letter-spacing-base;
  }

  .item-owner {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.exchange-arrow {
  font-size: 22px;
  color: $primary;
  flex-shrink: 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid $border-color;
}

.role-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 50px;

  &.initiator { background: rgba(27, 153, 170, 0.1); color: $primary; }
  &.receiver  { background: rgba(46, 204, 113, 0.1); color: $success; }
}

.created-time {
  font-size: 12px;
  color: $text-secondary;
}
</style>
