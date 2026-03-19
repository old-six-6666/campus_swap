<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { showSuccess, showWarning } from '@/utils/notify'
import { Key } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

const form = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })

const rules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

let timer = null

async function handleSendCode() {
  if (!form.email) { showWarning('请先输入邮箱'); return }
  codeSending.value = true
  try {
    await userApi.sendCode({ email: form.email, scene: 'FORGOT_PASSWORD' })
    showSuccess('验证码已发送，请查收邮件')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) { clearInterval(timer); countdown.value = 0 }
    }, 1000)
  } finally {
    codeSending.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userApi.forgotPassword({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    await showSuccess('密码重置成功，请重新登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-view">
    <div class="bg-blob bg-blob-1"></div>
    <div class="bg-blob bg-blob-2"></div>

    <div class="auth-card">
      <div class="auth-brand">
        <img src="/logo.png" alt="换了吗" class="brand-logo" />
        <h1 class="brand-name">换了吗</h1>
      </div>

      <h2 class="auth-title">重置密码</h2>
      <p class="auth-subtitle">通过邮箱验证码安全重置您的密码</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册时的邮箱" size="large" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入6位验证码" maxlength="6" size="large" />
            <el-button
              :disabled="countdown > 0 || codeSending"
              :loading="codeSending"
              class="code-btn"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请设置新密码" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" size="large" @click="handleSubmit">
            确认重置
          </el-button>
        </el-form-item>
      </el-form>

      <p class="auth-footer">
        <RouterLink to="/login" class="link-primary">← 返回登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.auth-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(145deg, #e8f6f8 0%, #f0faf9 40%, #fafcfd 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
  filter: blur(70px);
  pointer-events: none;
}
.bg-blob-1 { width: 350px; height: 350px; background: $primary; top: -80px; left: -60px; }
.bg-blob-2 { width: 280px; height: 280px; background: $warning; bottom: -60px; right: -40px; }

.auth-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 36px 36px 28px;
  box-shadow: $shadow-lg;
  border: 1px solid rgba(27, 153, 170, 0.1);
}

.auth-brand {
  text-align: center;
  margin-bottom: 20px;

  .brand-logo {
    width: 52px;
    height: 52px;
    object-fit: contain;
    margin: 0 auto 8px;
  }

  .brand-name {
    font-size: 18px;
    font-weight: 700;
    color: $primary;
    letter-spacing: $letter-spacing-wide;
  }
}

.auth-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  text-align: center;
  margin-bottom: 6px;
  letter-spacing: $letter-spacing-base;
}

.auth-subtitle {
  text-align: center;
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 22px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
  .el-input { flex: 1; }
}

.code-btn {
  white-space: nowrap;
  min-width: 112px;
  border-radius: 50px !important;
  border-color: $primary !important;
  color: $primary !important;
  font-weight: 500 !important;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: $letter-spacing-wide;
}

.auth-footer {
  text-align: center;
  font-size: 13px;
  margin-top: 16px;
}

.link-primary {
  color: $primary;
  font-weight: 500;
  &:hover { text-decoration: underline; }
}

@media (max-width: 480px) {
  .auth-card { padding: 28px 20px 24px; }
}
</style>
