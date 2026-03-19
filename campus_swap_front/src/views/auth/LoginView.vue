<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/useUserStore'
import { showSuccess } from '@/utils/notify'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({ email: '', password: '' })

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    await showSuccess('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-view">
    <!-- 背景装饰 -->
    <div class="bg-blob bg-blob-1"></div>
    <div class="bg-blob bg-blob-2"></div>

    <div class="auth-card">
      <!-- 品牌头部 -->
      <div class="auth-brand">
        <img src="/logo2.png" alt="换了吗" class="brand-logo" />
        <h1 class="brand-name">换了吗</h1>
        <p class="brand-tagline">校园闲置 · 一换即合</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" size="large" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        <RouterLink to="/forgot-password" class="link-muted">忘记密码？</RouterLink>
        <span>还没有账号？<RouterLink to="/register" class="link-primary">立即注册</RouterLink></span>
      </div>
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

/* 背景装饰气泡 */
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
  top: -100px; left: -80px;
}
.bg-blob-2 {
  width: 320px; height: 320px;
  background: $warning;
  bottom: -80px; right: -60px;
}

.auth-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 40px 36px 32px;
  box-shadow: $shadow-lg;
  border: 1px solid rgba(27, 153, 170, 0.1);
}

.auth-brand {
  text-align: center;
  margin-bottom: 28px;

  .brand-logo {
    width: 150px;
    height: 150px;
    object-fit: contain;
    margin: 0 auto 10px;
  }

  .brand-name {
    font-size: 22px;
    font-weight: 700;
    color: $primary;
    letter-spacing: $letter-spacing-wide;
    margin-bottom: 4px;
  }

  .brand-tagline {
    font-size: 13px;
    color: $text-secondary;
    letter-spacing: $letter-spacing-base;
  }
}

.auth-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  text-align: center;
  margin-bottom: 24px;
  letter-spacing: $letter-spacing-base;
}

.auth-form {
  :deep(.el-form-item__label) { font-weight: 500; }
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: $letter-spacing-wide;
}

.auth-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: $text-secondary;
  margin-top: 16px;
}

.link-muted {
  color: $text-secondary;
  transition: color 0.2s;
  &:hover { color: $primary; }
}

.link-primary {
  color: $primary;
  font-weight: 500;
  &:hover { text-decoration: underline; }
}

@media (max-width: 480px) {
  .auth-card { padding: 32px 24px 28px; }
}
</style>
