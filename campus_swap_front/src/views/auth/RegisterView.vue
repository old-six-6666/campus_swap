<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { showSuccess, showWarning } from '@/utils/notify'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

const form = reactive({ email: '', code: '', nickname: '', password: '', confirmPassword: '' })

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.password) callback(new Error('两次密码不一致'))
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
    await userApi.sendCode({ email: form.email, scene: 'REGISTER' })
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

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userApi.register(form)
    await showSuccess('注册成功，请登录')
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
        <p class="brand-tagline">校园闲置 · 一换即合</p>
      </div>

      <h2 class="auth-title">创建账号</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
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
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码（至少6位）" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" size="large" @click="handleRegister">
            立即注册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="auth-footer">
        已有账号？<RouterLink to="/login" class="link-primary">立即登录</RouterLink>
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
  opacity: 0.18;
  filter: blur(60px);
  pointer-events: none;
}
.bg-blob-1 {
  width: 400px; height: 400px;
  background: $primary;
  top: -100px; right: -80px;
}
.bg-blob-2 {
  width: 300px; height: 300px;
  background: $primary-light;
  bottom: -60px; left: -60px;
}

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
  margin-bottom: 24px;

  .brand-logo {
    width: 56px;
    height: 56px;
    object-fit: contain;
    margin: 0 auto 8px;
  }

  .brand-name {
    font-size: 20px;
    font-weight: 700;
    color: $primary;
    letter-spacing: $letter-spacing-wide;
    margin-bottom: 4px;
  }

  .brand-tagline {
    font-size: 12px;
    color: $text-secondary;
    letter-spacing: $letter-spacing-base;
  }
}

.auth-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  text-align: center;
  margin-bottom: 20px;
  letter-spacing: $letter-spacing-base;
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
  &:disabled { opacity: 0.6; }
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: $letter-spacing-wide;
}

.auth-footer {
  text-align: center;
  color: $text-secondary;
  font-size: 13px;
  margin-top: 14px;
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
