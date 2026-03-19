<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { userApi } from '@/api/modules/user'
import { showSuccess, showError } from '@/utils/notify'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('byOld')

// ===== 旧密码方式 =====
const oldFormRef = ref(null)
const oldLoading = ref(false)
const oldForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const oldRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== oldForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleChangeByOld() {
  await oldFormRef.value.validate()
  oldLoading.value = true
  try {
    await userApi.changePasswordByOld({
      oldPassword: oldForm.oldPassword,
      newPassword: oldForm.newPassword,
    })
    await showSuccess('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    oldLoading.value = false
  }
}

// ===== 邮箱验证码方式 =====
const emailFormRef = ref(null)
const emailLoading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
const emailForm = reactive({ code: '', newPassword: '', confirmPassword: '' })
const emailRules = {
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== emailForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

let timer = null

async function handleSendCode() {
  const email = userStore.userInfo?.email
  if (!email) {
    showError('获取用户邮箱失败，请重新登录')
    return
  }
  codeSending.value = true
  try {
    await userApi.sendCode({ email, scene: 'CHANGE_PASSWORD' })
    showSuccess(`验证码已发送至 ${email}，请查收`)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        countdown.value = 0
      }
    }, 1000)
  } finally {
    codeSending.value = false
  }
}

async function handleChangeByEmail() {
  await emailFormRef.value.validate()
  emailLoading.value = true
  try {
    await userApi.changePasswordByEmail({
      code: emailForm.code,
      newPassword: emailForm.newPassword,
    })
    await showSuccess('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    emailLoading.value = false
  }
}
</script>

<template>
  <div class="change-password-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>修改密码</h3>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 旧密码方式 -->
        <el-tab-pane label="通过旧密码" name="byOld">
          <el-form
            ref="oldFormRef"
            :model="oldForm"
            :rules="oldRules"
            label-position="top"
            class="pwd-form"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                v-model="oldForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="oldForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="oldForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="oldLoading" @click="handleChangeByOld">
                确认修改
              </el-button>
              <el-button @click="router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 邮箱验证码方式 -->
        <el-tab-pane label="通过邮箱验证码" name="byEmail">
          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            label-position="top"
            class="pwd-form"
          >
            <el-form-item label="验证码" prop="code">
              <div class="code-row">
                <el-input v-model="emailForm.code" placeholder="请输入6位验证码" maxlength="6" />
                <el-button
                  :disabled="countdown > 0 || codeSending"
                  :loading="codeSending"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
                </el-button>
              </div>
              <div class="code-tip">验证码将发送至：{{ userStore.userInfo?.email }}</div>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="emailForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="emailForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="emailLoading" @click="handleChangeByEmail">
                确认修改
              </el-button>
              <el-button @click="router.back()">取消</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.change-password-view {
  max-width: 560px;
  margin: 0 auto;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: $letter-spacing-base;
}

.pwd-form {
  max-width: 440px;
  margin-top: 16px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;

  .el-input { flex: 1; }

  .el-button {
    white-space: nowrap;
    min-width: 116px;
    border-radius: 50px !important;
    border-color: $primary !important;
    color: $primary !important;
    font-weight: 500 !important;
  }
}

.code-tip {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 5px;
  letter-spacing: $letter-spacing-base;
}
</style>
