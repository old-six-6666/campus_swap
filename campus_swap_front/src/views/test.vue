<template>
  <transition name="fade">
    <div v-if="isVisible" class="login-overlay" @click.self="closeModal">
      <div class="login-container">
        <button class="close-btn" @click="closeModal">×</button>

        <div class="form-header">
          <div class="market-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M3 3H21V19C21 20.1046 20.1046 21 19 21H5C3.89543 21 3 20.1046 3 19V3Z" stroke="#4FD1C5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M8 7V3M16 7V3M3 7H21" stroke="#4FD1C5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <h2>{{ isRegister ? '加入欢乐卖' : '欢迎回来' }}</h2>
          <p class="subtitle">{{ isRegister ? '开启你的校园宝贝淘金之旅' : '继续探索学长学姐的珍藏' }}</p>
        </div>

        <form @submit.prevent="handleSubmit">

          <div class="input-group">
            <label for="username">用户名 / 邮箱</label>
            <div class="input-wrapper">
              <span class="icon">👤</span>
              <input
                  type="text"
                  id="username"
                  v-model="formData.username"
                  placeholder="请输入你的学号或邮箱"
                  required
              />
            </div>
          </div>

          <div class="input-group">
            <div class="label-wrapper">
              <label for="password">密码</label>
              <a v-if="!isRegister" href="#" class="forgot-password">忘记密码？</a>
            </div>
            <div class="input-wrapper">
              <span class="icon">🔒</span>
              <input
                  :type="showPassword ? 'text' : 'password'"
                  id="password"
                  v-model="formData.password"
                  placeholder="请输入密码"
                  required
              />
              <span class="toggle-password" @click="showPassword = !showPassword">
                {{ showPassword ? '🙈' : '👁️' }}
              </span>
            </div>
          </div>

          <transition name="slide-fade">
            <div v-if="isRegister" class="input-group">
              <label for="confirmPassword">确认密码</label>
              <div class="input-wrapper">
                <span class="icon">✅</span>
                <input
                    type="password"
                    id="confirmPassword"
                    v-model="formData.confirmPassword"
                    placeholder="请再次输入密码"
                    required
                />
              </div>
            </div>
          </transition>

          <button type="submit" class="submit-btn">
            {{ isRegister ? '立即注册' : '登录' }}
          </button>
        </form>

        <div class="form-footer">
          <p>
            {{ isRegister ? '已有账号？' : '还没有账号？' }}
            <a href="#" @click.prevent="isRegister = !isRegister">
              {{ isRegister ? '现在登录' : '立即注册' }}
              </span>
          </p>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, reactive } from 'vue';

// 接收父组件控制显示的 props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'login-success', 'register-success']);

// 控制模态框显示/隐藏
const isVisible = ref(props.modelValue);
// 切换登录/注册状态
const isRegister = ref(false);
// 控制密码显示/隐藏
const showPassword = ref(false);

// 表单数据
const formData = reactive({
  username: '',
  password: '',
  confirmPassword: ''
});

// 关闭模态框
const closeModal = () => {
  isVisible.value = false;
  emit('update:modelValue', false);
  // 重置表单
  isRegister.value = false;
  formData.username = '';
  formData.password = '';
  formData.confirmPassword = '';
};

// 监听 props 变化以同步 isVisible
import { watch } from 'vue';
watch(() => props.modelValue, (newVal) => {
  isVisible.value = newVal;
});

// 处理表单提交（需对接你的后端 API）
const handleSubmit = () => {
  if (isRegister.value) {
    // 注册逻辑
    if (formData.password !== formData.confirmPassword) {
      alert('两次输入的密码不一致，请重新输入。');
      return;
    }
    console.log('注册数据:', formData);
    // 这里调用你的注册 API (e.g., from user.js)
    emit('register-success', formData);
  } else {
    // 登录逻辑
    console.log('登录数据:', formData);
    // 这里调用你的登录 API (e.g., from user.js)
    emit('login-success', formData);
  }
};
</script>

<style scoped>
/* 定义核心颜色变量 */
:root {
  --primary-mint: #4FD1C5; /* 薄荷绿/浅青色，提取自图片 */
  --primary-mint-dark: #38B2AC;
  --bg-off-white: #F7FAFC; /* 米白色背景 */
  --text-main: #2D3748; /* 深灰文字 */
  --text-subtitle: #718096; /* 辅文字 */
  --border-color: #E2E8F0; /* 边框 */
  --shadow-main: 0 10px 25px rgba(0, 0, 0, 0.05); /* 柔和阴影 */
}

/* 遮罩层 */
.login-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4); /* 半透明黑色遮罩 */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000; /* 确保在最上层 */
  backdrop-filter: blur(3px); /* 增加毛玻璃效果，提升现代感 */
}

/* 登录容器 */
.login-container {
  background-color: #FFFFFF;
  width: 90%;
  max-width: 420px;
  padding: 40px;
  border-radius: 24px; /* 大圆角，增加亲和力，符合扁平化风格 */
  box-shadow: var(--shadow-main);
  position: relative;
  overflow: hidden;
}

/* 关闭按钮 */
.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: none;
  border: none;
  font-size: 28px;
  color: var(--text-subtitle);
  cursor: pointer;
  padding: 0;
  line-height: 1;
}
.close-btn:hover {
  color: var(--primary-mint);
}

/* 表单头部 */
.form-header {
  text-align: center;
  margin-bottom: 35px;
}
.market-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 15px;
}
.market-icon svg {
  width: 100%;
  height: 100%;
}
.form-header h2 {
  font-size: 26px;
  color: var(--text-main);
  margin: 0;
  font-weight: 600;
}
.form-header .subtitle {
  font-size: 14px;
  color: var(--text-subtitle);
  margin: 8px 0 0;
}

/* 输入组 */
.input-group {
  margin-bottom: 22px;
}
.input-group label {
  display: block;
  font-size: 14px;
  color: var(--text-main);
  margin-bottom: 8px;
  font-weight: 500;
}
.label-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.forgot-password {
  font-size: 13px;
  color: var(--primary-mint);
  text-decoration: none;
}
.forgot-password:hover {
  text-decoration: underline;
}

/* 输入框外层容器 */
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.input-wrapper .icon {
  position: absolute;
  left: 15px;
  font-size: 18px;
  color: var(--text-subtitle);
}
.input-wrapper input {
  width: 100%;
  padding: 12px 15px 12px 48px; /* 留出左侧图标空间 */
  border: 2px solid var(--border-color); /* 清晰的扁平化边框 */
  border-radius: 12px; /* 圆角输入框 */
  font-size: 15px;
  color: var(--text-main);
  background-color: var(--bg-off-white);
  transition: border-color 0.2s;
}
.input-wrapper input:focus {
  outline: none;
  border-color: var(--primary-mint); /* 聚焦时变为薄荷绿 */
}
.input-wrapper input::placeholder {
  color: #A0AEC0;
}

/* 密码切换按钮 */
.toggle-password {
  position: absolute;
  right: 15px;
  cursor: pointer;
  color: var(--text-subtitle);
  font-size: 16px;
  user-select: none;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  padding: 14px;
  background-color: var(--primary-mint); /* 主色调薄荷绿 */
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
  margin-top: 10px;
}
.submit-btn:hover {
  background-color: var(--primary-mint-dark);
}
.submit-btn:active {
  transform: translateY(1px); /* 点击时的扁平化交互 */
}

/* 底部切换逻辑 */
.form-footer {
  text-align: center;
  margin-top: 25px;
  font-size: 14px;
  color: var(--text-subtitle);
}
.form-footer a {
  color: var(--primary-mint);
  text-decoration: none;
  font-weight: 500;
  margin-left: 5px;
}
.form-footer a:hover {
  text-decoration: underline;
}

/* --- 动画 --- */

/* 遮罩层淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 注册框滑入动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>