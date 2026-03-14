<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/useUserStore'
import { userApi } from '@/api/modules/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 认证申请相关
const verifyInfo = ref(null)
const verifyLoading = ref(false)
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref()
const applyForm = ref({ school: '', studentId: '', realName: '', extraInfo: '' })

const STATUS_MAP = {
  0: { label: '审核中', type: 'warning', desc: '您的认证申请已提交，请耐心等待管理员审核。' },
  1: { label: '已认证', type: 'success', desc: '您已通过学生身份认证。' },
  2: { label: '已拒绝', type: 'danger', desc: '认证申请被拒绝，您可以修改信息后重新提交。' },
}

const canApply = computed(() => {
  if (!verifyInfo.value) return true
  return verifyInfo.value.status === 2  // 已拒绝可重新提交
})

async function fetchVerifyStatus() {
  verifyLoading.value = true
  try {
    verifyInfo.value = await userApi.getMyVerification()
  } catch {
    verifyInfo.value = null
  } finally {
    verifyLoading.value = false
  }
}

function openApply() {
  // 被拒绝时预填之前的信息
  if (verifyInfo.value?.status === 2) {
    applyForm.value = {
      school: verifyInfo.value.school,
      studentId: verifyInfo.value.studentId,
      realName: verifyInfo.value.realName,
      extraInfo: verifyInfo.value.extraInfo || '',
    }
  } else {
    applyForm.value = {
      school: userStore.userInfo?.school || '',
      studentId: '',
      realName: '',
      extraInfo: '',
    }
  }
  applyVisible.value = true
}

async function submitApply() {
  await applyFormRef.value.validate()
  applyLoading.value = true
  try {
    await userApi.applyVerify(applyForm.value)
    ElMessage.success('认证申请已提交，请等待审核')
    applyVisible.value = false
    fetchVerifyStatus()
  } finally {
    applyLoading.value = false
  }
}

onMounted(fetchVerifyStatus)
</script>

<template>
  <div class="profile-view">
    <!-- 个人信息 -->
    <el-card>
      <template #header>
        <h3 style="margin: 0;">个人信息</h3>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email }}</el-descriptions-item>
        <el-descriptions-item label="学校">{{ userStore.userInfo?.school || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="学生认证">
          <el-tag
            v-if="userStore.userInfo?.isVerified === 1"
            type="success"
            size="small"
          >已认证</el-tag>
          <el-tag v-else type="info" size="small">未认证</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 学生认证 -->
    <el-card style="margin-top: 20px" v-loading="verifyLoading">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <h3 style="margin: 0;">学生身份认证</h3>
          <el-button
            v-if="canApply"
            type="primary"
            size="small"
            @click="openApply"
          >
            {{ verifyInfo?.status === 2 ? '重新申请' : '申请认证' }}
          </el-button>
        </div>
      </template>

      <!-- 无申请记录 -->
      <div v-if="!verifyInfo" class="verify-empty">
        <el-icon size="40" color="#c0c4cc"><QuestionFilled /></el-icon>
        <p>您尚未提交学生认证申请</p>
        <p class="verify-hint">通过填写真实姓名和学号，即可获得学生认证标识，提升交易可信度。</p>
      </div>

      <!-- 有申请记录 -->
      <template v-else>
        <el-alert
          :type="STATUS_MAP[verifyInfo.status]?.type"
          :title="STATUS_MAP[verifyInfo.status]?.label"
          :description="verifyInfo.status === 2 ? (STATUS_MAP[2].desc + (verifyInfo.remark ? '  拒绝原因：' + verifyInfo.remark : '')) : STATUS_MAP[verifyInfo.status]?.desc"
          show-icon
          :closable="false"
          style="margin-bottom: 12px"
        />
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="学校">{{ verifyInfo.school }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ verifyInfo.studentId }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ verifyInfo.realName }}</el-descriptions-item>
          <el-descriptions-item label="补充说明">{{ verifyInfo.extraInfo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ verifyInfo.createdAt?.slice(0, 10) }}</el-descriptions-item>
          <el-descriptions-item v-if="verifyInfo.reviewedAt" label="审核时间">
            {{ verifyInfo.reviewedAt?.slice(0, 10) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-card>

    <!-- 申请弹窗 -->
    <el-dialog
      v-model="applyVisible"
      title="申请学生身份认证"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        请如实填写您的学校、学号和真实姓名，管理员将核验学生档案后进行审核。
      </el-alert>
      <el-form ref="applyFormRef" :model="applyForm" label-width="90px">
        <el-form-item
          label="学校名称"
          prop="school"
          :rules="[{ required: true, message: '请填写学校名称' }]"
        >
          <el-input v-model="applyForm.school" placeholder="请填写完整学校名称" />
        </el-form-item>
        <el-form-item
          label="学号"
          prop="studentId"
          :rules="[{ required: true, message: '请填写学号' }]"
        >
          <el-input v-model="applyForm.studentId" placeholder="请填写您的学号" />
        </el-form-item>
        <el-form-item
          label="真实姓名"
          prop="realName"
          :rules="[{ required: true, message: '请填写真实姓名' }]"
        >
          <el-input v-model="applyForm.realName" placeholder="请填写与档案一致的姓名" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="applyForm.extraInfo"
            placeholder="选填：专业、年级等辅助信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.profile-view {
  max-width: 700px;
  margin: 0 auto;
}

.verify-empty {
  text-align: center;
  padding: 20px 0;
  color: #909399;

  p { margin: 8px 0 0; }
}

.verify-hint {
  font-size: 13px;
  color: #c0c4cc;
}
</style>
