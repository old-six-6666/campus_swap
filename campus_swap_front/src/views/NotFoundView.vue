<template>
  <div class="not-found-view">
    <div class="not-found-inner">
      <div class="not-found-emoji">🔍</div>
      <h2 class="not-found-title">页面不存在</h2>
      <p class="not-found-sub">您访问的页面已消失或从未存在过</p>
      <RouterLink to="/">
        <el-button type="primary" size="large" round>返回首页</el-button>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.not-found-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.not-found-inner {
  text-align: center;
  padding: 40px 32px;
  background: $bg-card;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
  max-width: 380px;
  width: 100%;
}

.not-found-emoji {
  font-size: 64px;
  margin-bottom: 16px;
}

.not-found-title {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
  letter-spacing: $letter-spacing-base;
}

.not-found-sub {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 24px;
  letter-spacing: $letter-spacing-base;
}
</style>