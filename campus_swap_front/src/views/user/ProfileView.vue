<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/useUserStore'
import { userApi } from '@/api/modules/user'
import { itemApi } from '@/api/modules/item'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const userStore = useUserStore()

const verifyInfo = ref(null)
const verifyLoading = ref(false)
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref()
const applyForm = ref({ school: '', studentId: '', realName: '', extraInfo: '' })

// 头像上传
const avatarUploading = ref(false)
const avatarInputRef = ref(null)

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  avatarUploading.value = true
  try {
    const url = await itemApi.uploadImage(file)
    await userApi.updateProfile({ avatar: url })
    await userStore.fetchProfile()
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

const STATUS_MAP = {
  0: { label: '审核中', type: 'warning', desc: '您的认证申请已提交，请耐心等待管理员审核。' },
  1: { label: '已认证', type: 'success', desc: '您已通过学生身份认证。' },
  2: { label: '已拒绝', type: 'danger', desc: '认证申请被拒绝，您可以修改信息后重新提交。' },
}

const canApply = computed(() => {
  if (!verifyInfo.value) return true
  return verifyInfo.value.status === 2
})

async function fetchVerifyStatus() {
  verifyLoading.value = true
  try {
    verifyInfo.value = await userApi.getMyVerification()
  } catch (error) {
    console.error('获取认证状态失败:', error)
    verifyInfo.value = null
  } finally {
    verifyLoading.value = false
  }
}

function openApply() {
  if (verifyInfo.value?.status === 2) {
    applyForm.value = {
      school: verifyInfo.value.school,
      studentId: verifyInfo.value.studentId,
      realName: verifyInfo.value.realName,
      extraInfo: verifyInfo.value.extraInfo || '',
    }
  } else {
    applyForm.value = { school: userStore.userInfo?.school || '', studentId: '', realName: '', extraInfo: '' }
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
    <div class="section-card">
      <div class="section-header">
        <h3 class="section-title">个人信息</h3>
      </div>

      <!-- 头像 -->
      <div class="avatar-row">
        <div class="avatar-wrap" @click="avatarInputRef?.click()" :title="avatarUploading ? '上传中...' : '点击修改头像'">
          <el-avatar
            :size="80"
            :src="userStore.getAvatar(userStore.userInfo?.avatar)"
            class="profile-avatar"
            style="object-fit:cover"
          />
          <div class="avatar-overlay">
            <span v-if="!avatarUploading">修改</span>
            <el-icon v-else class="is-loading"><Loading /></el-icon>
          </div>
        </div>
        <input
          ref="avatarInputRef"
          type="file"
          accept="image/*"
          style="display:none"
          @change="handleAvatarChange"
        />
        <div class="avatar-hint">点击头像可修改，支持 JPG / PNG / GIF</div>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">昵称</span>
          <span class="info-value">{{ userStore.userInfo?.nickname }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ userStore.userInfo?.email }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">学校</span>
          <span class="info-value">{{ userStore.userInfo?.school || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">手机号</span>
          <span class="info-value">{{ userStore.userInfo?.phone || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">学生认证</span>
          <el-tag v-if="verifyInfo?.status === 1" size="small" effect="plain">已认证</el-tag>
          <el-tag v-else type="info" size="small" effect="plain">未认证</el-tag>
        </div>
      </div>
    </div>

    <!-- 学生认证 -->
    <div class="section-card" v-loading="verifyLoading">
      <div class="section-header">
        <h3 class="section-title">学生身份认证</h3>
        <el-button v-if="canApply" type="primary" size="small" round @click="openApply">
          {{ verifyInfo?.status === 2 ? '重新申请' : '申请认证' }}
        </el-button>
      </div>

      <div v-if="!verifyInfo" class="verify-empty">
        <div class="verify-icon">🎓</div>
        <p class="verify-empty-title">尚未申请学生认证</p>
        <p class="verify-empty-hint">通过填写真实姓名和学号，即可获得学生认证标识，提升交易可信度。</p>
      </div>

      <template v-else>
        <el-alert
          :type="STATUS_MAP[verifyInfo.status]?.type"
          :title="STATUS_MAP[verifyInfo.status]?.label"
          :description="verifyInfo.status === 2
            ? (STATUS_MAP[2].desc + (verifyInfo.remark ? '  拒绝原因：' + verifyInfo.remark : ''))
            : STATUS_MAP[verifyInfo.status]?.desc"
          show-icon
          :closable="false"
          style="margin-bottom: 16px; border-radius: 12px;"
        />
        <div class="verify-detail-grid">
          <div class="vd-item"><span class="vd-label">学校</span><span>{{ verifyInfo.school }}</span></div>
          <div class="vd-item"><span class="vd-label">学号</span><span>{{ verifyInfo.studentId }}</span></div>
          <div class="vd-item"><span class="vd-label">真实姓名</span><span>{{ verifyInfo.realName }}</span></div>
          <div class="vd-item"><span class="vd-label">补充说明</span><span>{{ verifyInfo.extraInfo || '—' }}</span></div>
          <div class="vd-item"><span class="vd-label">提交时间</span><span>{{ verifyInfo.createdAt?.slice(0, 10) }}</span></div>
          <div v-if="verifyInfo.reviewedAt" class="vd-item">
            <span class="vd-label">审核时间</span><span>{{ verifyInfo.reviewedAt?.slice(0, 10) }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- 申请弹窗 -->
    <el-dialog v-model="applyVisible" title="申请学生身份认证" width="500px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" style="margin-bottom: 18px; border-radius: 12px;">
        请如实填写您的学校、学号和真实姓名，管理员将核验学生档案后进行审核。
      </el-alert>
      <el-form ref="applyFormRef" :model="applyForm" label-width="90px">
        <el-form-item label="学校名称" prop="school" :rules="[{ required: true, message: '请填写学校名称' }]">
          <el-input v-model="applyForm.school" placeholder="请填写完整学校名称" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId" :rules="[{ required: true, message: '请填写学号' }]">
          <el-input v-model="applyForm.studentId" placeholder="请填写您的学号" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName" :rules="[{ required: true, message: '请填写真实姓名' }]">
          <el-input v-model="applyForm.realName" placeholder="请填写与档案一致的姓名" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="applyForm.extraInfo" placeholder="选填：专业、年级等辅助信息" maxlength="200" show-word-limit />
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
@import '@/assets/styles/variables.scss';

.profile-view {
  max-width: 700px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 24px 28px;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    letter-spacing: $letter-spacing-base;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 500px) { grid-template-columns: 1fr; }
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  background: $bg-subtle;
  border-radius: $border-radius-sm;
}

.info-label {
  font-size: 12px;
  color: $text-secondary;
  font-weight: 500;
  letter-spacing: $letter-spacing-base;
}

.info-value {
  font-size: 14px;
  color: $text-primary;
  font-weight: 500;
}

.verify-empty {
  text-align: center;
  padding: 28px 0;

  .verify-icon { font-size: 48px; margin-bottom: 12px; }
  .verify-empty-title { font-size: 15px; font-weight: 600; color: $text-regular; margin-bottom: 6px; }
  .verify-empty-hint { font-size: 13px; color: $text-secondary; line-height: 1.6; max-width: 340px; margin: 0 auto; }
}

.verify-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  @media (max-width: 500px) { grid-template-columns: 1fr; }
}

.vd-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  background: $bg-subtle;
  border-radius: $border-radius-sm;
  font-size: 13px;

  .vd-label {
    font-size: 11px;
    color: $text-secondary;
    font-weight: 500;
    letter-spacing: $letter-spacing-base;
  }
}

/* ── 头像区域 ─────────────────────────────── */
.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;

  &:hover .avatar-overlay { opacity: 1; }
}

.profile-avatar {
  display: block;
  border: 3px solid $primary-light;
  box-shadow: $shadow-sm;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-hint {
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.6;
}
</style>