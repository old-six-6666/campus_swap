<script setup>
import { useUserStore } from '@/stores/useUserStore'

const userStore = useUserStore()
</script>

<template>
  <div class="profile-view">
    <el-card>
      <template #header>
        <h3 style="margin: 0;">个人信息</h3>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email }}</el-descriptions-item>
        <el-descriptions-item label="学校">{{ userStore.userInfo?.school || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未填写' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.profile-view {
  max-width: 700px;
  margin: 0 auto;
}
</style>
