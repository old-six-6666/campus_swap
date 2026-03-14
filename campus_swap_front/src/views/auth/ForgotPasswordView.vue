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
  if (!form.email) {
    showWarning('请先输入邮箱')
    return
  }
  codeSending.value = true
  try {
    await userApi.sendCode({ email: form.email, scene: 'FORGOT_PASSWORD' })
    showSuccess('验证码已发送，请查收邮件')
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
  <div class="forgot-view">
    <el-card class="forgot-card">
      <h2 class="title">重置密码</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册时的邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入6位验证码" maxlength="6" />
            <el-button
              :disabled="countdown > 0 || codeSending"
              :loading="codeSending"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请设置新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            确认重置
          </el-button>
        </el-form-item>
      </el-form>
      <p class="back-tip">
        <RouterLink to="/login">← 返回登录</RouterLink>
      </p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.forgot-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.forgot-card {
  width: 420px;

  .title {
    text-align: center;
    margin-bottom: 24px;
    font-size: 22px;
    color: #303133;
  }

  .code-row {
    display: flex;
    gap: 8px;
    width: 100%;

    .el-input {
      flex: 1;
    }

    .el-button {
      white-space: nowrap;
      min-width: 110px;
    }
  }

  .back-tip {
    text-align: center;
    font-size: 14px;
    color: #606266;
    margin-top: 12px;
  }
}
</style>
