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
  <div class="login-view">
    <el-card class="login-card">
      <h2 class="title">登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="bottom-links">
        <RouterLink to="/forgot-password" class="forgot-link">忘记密码？</RouterLink>
        <span>还没有账号？<RouterLink to="/register">立即注册</RouterLink></span>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.login-card {
  width: 400px;

  .title {
    text-align: center;
    margin-bottom: 24px;
    font-size: 22px;
    color: #303133;
  }

  .bottom-links {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: #606266;
    margin-top: 12px;

    .forgot-link {
      color: #909399;

      &:hover {
        color: #409eff;
      }
    }
  }
}
</style>
