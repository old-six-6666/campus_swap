<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tradeApi } from '@/api/modules/trade'
import { useUserStore } from '@/stores/useUserStore'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const trade = ref(null)
const logs = ref([])
const appeals = ref([])
const loading = ref(false)
const logLoading = ref(false)
const deliverDialogVisible = ref(false)
const logisticsInput = ref('')
const appealDialogVisible = ref(false)
const appealContent = ref('')

// ===== 状态配置 =====
const STATUS_CONFIG = {
  PENDING_MATCH:           { label: '等待匹配',   type: 'warning', step: 0 },
  MATCHED:                 { label: '已匹配',     type: 'primary', step: 1 },
  AUDIT_PENDING:           { label: '审核中',     type: 'warning', step: 2 },
  AUDIT_PASSED:            { label: '审核通过',   type: 'success', step: 2 },
  AUDIT_REJECTED:          { label: '审核驳回',   type: 'danger',  step: 2 },
  WAITING_DELIVERY:        { label: '等待发货',   type: 'primary', step: 3 },
  BOTH_DELIVERED:          { label: '双方已发货', type: 'primary', step: 4 },
  WAITING_CONFIRM_RECEIPT: { label: '等待收货',   type: 'primary', step: 4 },
  COMPLETED:               { label: '已完成',     type: 'success', step: 5 },
  TERMINATED:              { label: '已终止',     type: 'info',    step: -1 },
}

// 状态步骤（用于步骤条，无审核时跳过审核步骤）
const STEPS_WITH_AUDIT    = ['等待匹配', '已匹配', '审核', '等待发货', '双方发货', '完成']
const STEPS_WITHOUT_AUDIT = ['等待匹配', '已匹配', '等待发货', '双方发货', '完成']

const steps = computed(() => trade.value?.auditMode ? STEPS_WITH_AUDIT : STEPS_WITHOUT_AUDIT)
const currentStep = computed(() => {
  if (!trade.value) return 0
  const cfg = STATUS_CONFIG[trade.value.status]
  if (!cfg) return 0
  let step
  if (!trade.value.auditMode) {
    // 无审核时步骤偏移
    const noAuditMap = { 0: 0, 1: 1, 3: 2, 4: 3, 5: 4 }
    step = noAuditMap[cfg.step] ?? cfg.step
  } else {
    step = cfg.step
  }
  // step+1 让当前步骤变为 finish（绿色打勾），下一步变为 active
  // 覆盖：BOTH_DELIVERED/WAITING_CONFIRM_RECEIPT（"双方发货"变绿）和 COMPLETED（全部变绿）
  const advanceStatuses = ['BOTH_DELIVERED', 'WAITING_CONFIRM_RECEIPT', 'COMPLETED']
  if (advanceStatuses.includes(trade.value.status)) step += 1
  return step
})

const isTerminal = computed(() => {
  return ['COMPLETED', 'TERMINATED', 'AUDIT_REJECTED'].includes(trade.value?.status)
})

const myRole = computed(() => trade.value?.myRole)
const isInitiator = computed(() => myRole.value === 'initiator')
const isReceiver  = computed(() => myRole.value === 'receiver')

// ===== 可用操作判断 =====
const canAccept = computed(() =>
  trade.value?.status === 'PENDING_MATCH' && isReceiver.value
)
const canReject = computed(() =>
  trade.value?.status === 'PENDING_MATCH' && isReceiver.value
)
const canCancel = computed(() =>
  trade.value?.status === 'PENDING_MATCH' && isInitiator.value
)
const canDeliver = computed(() => {
  if (trade.value?.status !== 'WAITING_DELIVERY') return false
  if (isInitiator.value && !trade.value.initiatorDelivered) return true
  if (isReceiver.value  && !trade.value.receiverDelivered)  return true
  return false
})
const canConfirmReceipt = computed(() => {
  const s = trade.value?.status
  if (s !== 'BOTH_DELIVERED' && s !== 'WAITING_CONFIRM_RECEIPT') return false
  if (isInitiator.value && !trade.value.initiatorConfirmedReceipt) return true
  if (isReceiver.value  && !trade.value.receiverConfirmedReceipt)  return true
  return false
})
const canTerminate = computed(() =>
  !isTerminal.value && (isInitiator.value || isReceiver.value)
)
const canAppeal = computed(() =>
  !isTerminal.value && (isInitiator.value || isReceiver.value)
)

// ===== 操作方法 =====
async function handleAccept() {
  try {
    await ElMessageBox.confirm('确定同意这次以物换物交易吗？', '同意交换', {
      confirmButtonText: '确定同意',
      cancelButtonText: '再想想',
      type: 'info',
    })
    loading.value = true
    trade.value = await tradeApi.match(trade.value.id, { receiverItemId: trade.value.receiverItemId })
    ElMessage.success('已同意交换！')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleReject() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝交换', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '填写原因...',
    })
    loading.value = true
    await tradeApi.terminate(trade.value.id, { reason: reason || '乙方拒绝交换' })
    ElMessage.info('已拒绝此次交换')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消这次交换申请吗？', '取消申请', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '不取消',
    })
    loading.value = true
    await tradeApi.terminate(trade.value.id, { reason: '申请方取消' })
    ElMessage.info('已取消申请')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleDeliver() {
  loading.value = true
  try {
    trade.value = await tradeApi.deliver(trade.value.id, { logistics: logisticsInput.value })
    ElMessage.success('已确认发货！')
    deliverDialogVisible.value = false
    logisticsInput.value = ''
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleConfirmReceipt() {
  try {
    await ElMessageBox.confirm('确认已收到对方的物品了吗？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '还没',
      type: 'success',
    })
    loading.value = true
    trade.value = await tradeApi.confirmReceipt(trade.value.id)
    ElMessage.success('收货确认成功！')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleTerminate() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入终止原因', '终止交易', {
      confirmButtonText: '确认终止',
      cancelButtonText: '取消',
      inputPlaceholder: '填写原因...',
    })
    loading.value = true
    await tradeApi.terminate(trade.value.id, { reason })
    ElMessage.info('交易已终止')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmitAppeal() {
  if (!appealContent.value.trim()) {
    ElMessage.warning('请填写申诉内容')
    return
  }
  loading.value = true
  try {
    await tradeApi.submitAppeal(trade.value.id, { content: appealContent.value })
    ElMessage.success('申诉已提交，等待管理员处理')
    appealDialogVisible.value = false
    appealContent.value = ''
    appeals.value = await tradeApi.getAppeals(trade.value.id)
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    loading.value = false
  }
}

// ===== 加载数据 =====
async function reload() {
  const id = route.params.id
  ;[trade.value, logs.value, appeals.value] = await Promise.all([
    tradeApi.getDetail(id),
    tradeApi.getLogs(id),
    tradeApi.getAppeals(id),
  ])
}

onMounted(async () => {
  loading.value = true
  try {
    await reload()
  } finally {
    loading.value = false
  }
})

function fmtTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function goItem(itemId) {
  if (itemId) router.push({ name: 'ItemDetail', params: { id: itemId } })
}
</script>

<template>
  <div v-loading="loading" class="trade-detail">
    <el-empty v-if="!loading && !trade" description="交易不存在或无权查看" />

    <template v-if="trade">
      <!-- ===== 顶部状态栏 ===== -->
      <div class="top-bar">
        <div class="trade-id">
          <el-icon><DocumentCopy /></el-icon>
          {{ trade.tradeNo }}
        </div>
        <el-tag :type="STATUS_CONFIG[trade.status]?.type || 'info'" size="large">
          {{ STATUS_CONFIG[trade.status]?.label || trade.status }}
        </el-tag>
      </div>

      <!-- ===== 步骤条（终止时不显示）===== -->
      <el-steps
        v-if="trade.status !== 'TERMINATED' && trade.status !== 'AUDIT_REJECTED'"
        :active="currentStep"
        finish-status="success"
        class="trade-steps"
        align-center
      >
        <el-step v-for="s in steps" :key="s" :title="s" />
      </el-steps>
      <el-alert
        v-else
        :title="trade.status === 'TERMINATED' ? ('交易已终止：' + (trade.terminateReason || '无原因')) : ('审核驳回：' + (trade.auditRemark || '无原因'))"
        type="error"
        show-icon
        :closable="false"
        class="terminated-alert"
      />

      <!-- ===== 核心操作按钮 ===== -->
      <div class="action-bar">
        <el-button v-if="canAccept" type="success" size="large" :loading="loading" @click="handleAccept">
          ✅ 同意交换
        </el-button>
        <el-button v-if="canReject" type="danger" plain size="large" :loading="loading" @click="handleReject">
          ❌ 拒绝
        </el-button>
        <el-button v-if="canCancel" plain size="large" :loading="loading" @click="handleCancel">
          取消申请
        </el-button>
        <el-button
          v-if="canDeliver"
          type="primary"
          size="large"
          :loading="loading"
          @click="deliverDialogVisible = true"
        >
          📦 确认已发货
        </el-button>
        <el-button
          v-if="canConfirmReceipt"
          type="success"
          size="large"
          :loading="loading"
          @click="handleConfirmReceipt"
        >
          🎉 确认已收货
        </el-button>
        <el-button
          v-if="canTerminate && !canAccept && !canReject && !canCancel"
          type="danger"
          plain
          size="small"
          :loading="loading"
          @click="handleTerminate"
        >
          终止交易
        </el-button>
        <el-button
          v-if="canAppeal"
          type="warning"
          plain
          size="small"
          :loading="loading"
          @click="appealDialogVisible = true"
        >
          提交申诉
        </el-button>
      </div>

      <!-- ===== 交换物品展示 ===== -->
      <el-row :gutter="24" class="items-section">
        <!-- 甲方物品 -->
        <el-col :span="11">
          <div class="item-card initiator-card" @click="goItem(trade.initiatorItemId)">
            <div class="item-card-label">
              <el-tag type="primary" size="small">甲方</el-tag>
              <span class="user-name">{{ trade.initiatorNickname }}</span>
              <el-tag v-if="myRole === 'initiator'" size="small" type="warning">（我）</el-tag>
            </div>
            <el-image
              :src="trade.initiatorItemCoverImage"
              fit="cover"
              class="item-image"
            >
              <template #error>
                <div class="img-placeholder">暂无图片</div>
              </template>
            </el-image>
            <div class="item-name">{{ trade.initiatorItemTitle || '(未设置)' }}</div>

            <!-- 发货状态 -->
            <div v-if="trade.status !== 'PENDING_MATCH' && trade.status !== 'MATCHED' && trade.status !== 'AUDIT_PENDING' && trade.status !== 'AUDIT_PASSED'" class="delivery-status">
              <el-tag :type="trade.initiatorDelivered ? 'success' : 'info'" size="small">
                {{ trade.initiatorDelivered ? '已发货' : '未发货' }}
              </el-tag>
              <el-tag v-if="trade.initiatorConfirmedReceipt" type="success" size="small" style="margin-left:4px">已收货</el-tag>
              <div v-if="trade.initiatorLogistics" class="logistics-info">
                物流：{{ trade.initiatorLogistics }}
              </div>
            </div>
          </div>
        </el-col>

        <!-- 交换图标 -->
        <el-col :span="2" class="arrow-col">
          <div class="exchange-icon">⇌</div>
        </el-col>

        <!-- 乙方物品 -->
        <el-col :span="11">
          <div class="item-card receiver-card" @click="goItem(trade.receiverItemId)">
            <div class="item-card-label">
              <el-tag type="success" size="small">乙方</el-tag>
              <span class="user-name">{{ trade.receiverNickname || '等待中...' }}</span>
              <el-tag v-if="myRole === 'receiver'" size="small" type="warning">（我）</el-tag>
            </div>
            <el-image
              :src="trade.receiverItemCoverImage"
              fit="cover"
              class="item-image"
            >
              <template #error>
                <div class="img-placeholder">{{ trade.receiverItemId ? '暂无图片' : '等待乙方确认' }}</div>
              </template>
            </el-image>
            <div class="item-name">{{ trade.receiverItemTitle || '（等待乙方确认）' }}</div>

            <!-- 发货状态 -->
            <div v-if="trade.status !== 'PENDING_MATCH' && trade.status !== 'MATCHED' && trade.status !== 'AUDIT_PENDING' && trade.status !== 'AUDIT_PASSED'" class="delivery-status">
              <el-tag :type="trade.receiverDelivered ? 'success' : 'info'" size="small">
                {{ trade.receiverDelivered ? '已发货' : '未发货' }}
              </el-tag>
              <el-tag v-if="trade.receiverConfirmedReceipt" type="success" size="small" style="margin-left:4px">已收货</el-tag>
              <div v-if="trade.receiverLogistics" class="logistics-info">
                物流：{{ trade.receiverLogistics }}
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- ===== 截止时间提醒 ===== -->
      <el-alert
        v-if="trade.status === 'WAITING_DELIVERY' && trade.deliveryDeadline"
        :title="`请在 ${fmtTime(trade.deliveryDeadline)} 前完成发货，否则交易将自动终止`"
        type="warning"
        show-icon
        :closable="false"
        class="deadline-alert"
      />
      <el-alert
        v-if="(trade.status === 'BOTH_DELIVERED' || trade.status === 'WAITING_CONFIRM_RECEIPT') && trade.receiptDeadline"
        :title="`请在 ${fmtTime(trade.receiptDeadline)} 前确认收货，否则交易将自动终止`"
        type="warning"
        show-icon
        :closable="false"
        class="deadline-alert"
      />

      <!-- ===== 时间线 ===== -->
      <div class="timeline-section">
        <h3>交易进度</h3>
        <el-timeline>
          <el-timeline-item v-if="trade.createdAt" timestamp="发起申请" placement="top">
            {{ fmtTime(trade.createdAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.matchedAt" timestamp="乙方确认匹配" placement="top" color="#409eff">
            {{ fmtTime(trade.matchedAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.auditPassedAt" timestamp="审核通过" placement="top" color="#67c23a">
            {{ fmtTime(trade.auditPassedAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.waitingDeliveryAt" timestamp="进入发货阶段" placement="top" color="#409eff">
            {{ fmtTime(trade.waitingDeliveryAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.bothDeliveredAt" timestamp="双方均已发货" placement="top" color="#409eff">
            {{ fmtTime(trade.bothDeliveredAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.completedAt" timestamp="交易完成 🎉" placement="top" color="#67c23a">
            {{ fmtTime(trade.completedAt) }}
          </el-timeline-item>
          <el-timeline-item v-if="trade.terminatedAt" timestamp="交易终止" placement="top" color="#f56c6c">
            {{ fmtTime(trade.terminatedAt) }} — {{ trade.terminateReason }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- ===== 操作日志 ===== -->
      <div class="log-section">
        <h3>操作日志</h3>
        <el-table :data="logs" size="small" stripe>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="变更" min-width="220">
            <template #default="{ row }">
              <span v-if="row.fromStatus" class="log-from">{{ row.fromStatusDesc }}</span>
              <span v-if="row.fromStatus" class="log-arrow"> → </span>
              <span class="log-to">{{ row.toStatusDesc }}</span>
            </template>
          </el-table-column>
          <el-table-column label="触发方式" width="90">
            <template #default="{ row }">
              <el-tag :type="row.triggerType === 1 ? 'info' : 'primary'" size="small">
                {{ row.triggerTypeDesc }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作人" width="100">
            <template #default="{ row }">{{ row.operatorNickname || '系统' }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- ===== 我的申诉 ===== -->
      <div v-if="appeals.length > 0" class="log-section">
        <h3>我的申诉</h3>
        <el-table :data="appeals" size="small" stripe>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="申诉内容" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'"
                size="small"
              >{{ row.statusDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="管理员回复" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- ===== 发货对话框 ===== -->
    <el-dialog v-model="deliverDialogVisible" title="确认发货" width="400px">
      <el-form>
        <el-form-item label="物流凭证（选填）">
          <el-input
            v-model="logisticsInput"
            type="textarea"
            :rows="3"
            placeholder="快递公司 + 单号，例如：顺丰 SF1234567890"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleDeliver">确认已发货</el-button>
      </template>
    </el-dialog>

    <!-- ===== 申诉对话框 ===== -->
    <el-dialog v-model="appealDialogVisible" title="提交申诉" width="440px" :close-on-click-modal="false">
      <p style="color:#606266; margin-bottom:12px;">请详细描述您遇到的问题，管理员将尽快处理。</p>
      <el-input
        v-model="appealContent"
        type="textarea"
        :rows="4"
        placeholder="请描述申诉原因，例如：对方未按约定发货、物品与描述不符等..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="loading" @click="handleSubmitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.trade-detail {
  max-width: 900px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  .trade-id {
    font-family: monospace;
    font-size: 13px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.trade-steps {
  background: #fff;
  padding: 20px 24px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.terminated-alert, .deadline-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 16px;
  background: #fff;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.items-section {
  margin-bottom: 16px;
}

.item-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: box-shadow 0.2s;
  height: 100%;
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
}

.item-card-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  .user-name {
    font-size: 13px;
    font-weight: 500;
    color: #303133;
  }
}

.item-image {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  .img-placeholder {
    width: 100%;
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #c0c4cc;
    font-size: 14px;
    border-radius: 8px;
  }
}

.item-name {
  margin-top: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  text-align: center;
}

.delivery-status {
  margin-top: 8px;
  text-align: center;
  .logistics-info {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    word-break: break-all;
  }
}

.arrow-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.exchange-icon {
  font-size: 32px;
  color: #409eff;
  text-align: center;
}

.initiator-card { border-top: 3px solid #409eff; }
.receiver-card  { border-top: 3px solid #67c23a; }

.timeline-section, .log-section {
  background: #fff;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
  }
}

.log-from { color: #909399; }
.log-arrow { color: #c0c4cc; }
.log-to    { color: #303133; font-weight: 500; }
</style>
