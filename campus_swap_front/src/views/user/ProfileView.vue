<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/useUserStore'
import { userApi } from '@/api/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

// 表单数据
const form = ref({
  nickname: '',
  phone: '',
  school: ''
})

// 编辑模式
const isEditing = ref(false)
const loading = ref(false)

// 学校搜索相关
const schoolSearchQuery = ref('')
const schoolOptions = ref([])
const schoolLoading = ref(false)

// 初始化表单数据
function initForm() {
  form.value = {
    nickname: userStore.userInfo?.nickname || '',
    phone: userStore.userInfo?.phone || '',
    school: userStore.userInfo?.school || ''
  }
}

// 搜索学校
async function searchSchools(query) {
  if (!query || query.length < 2) {
    schoolOptions.value = []
    return
  }
  
  schoolLoading.value = true
  try {
    // 使用免费的大学API，添加CORS代理
    const apiUrl = `https://universities.hipolabs.com/search?name=${encodeURIComponent(query)}&country=China`
    
    // 尝试直接访问，如果失败则使用CORS代理
    let response
    try {
      response = await fetch(apiUrl)
    } catch (directError) {
      // 如果直接访问失败，尝试使用CORS代理
      console.log('直接访问失败，尝试使用CORS代理')
      const proxyUrl = `https://api.allorigins.win/raw?url=${encodeURIComponent(apiUrl)}`
      response = await fetch(proxyUrl)
    }
    
    if (!response.ok) {
      throw new Error(`API请求失败: ${response.status}`)
    }
    
    const universities = await response.json()
    
    // 提取学校名称，限制数量
    schoolOptions.value = universities
      .slice(0, 20) // 限制最多20个结果
      .map(university => ({
        value: university.name,
        label: `${university.name} (${university.country})`
      }))
    
    // 如果没有结果，显示提示
    if (schoolOptions.value.length === 0) {
      schoolOptions.value = [{
        value: query,
        label: `未找到匹配的学校，按回车使用"${query}"`
      }]
    }
  } catch (error) {
    console.error('搜索学校失败:', error)
    // 提供静态备选列表
    schoolOptions.value = getFallbackSchools(query)
    ElMessage.warning('学校搜索服务暂时不可用，已显示常用学校列表')
  } finally {
    schoolLoading.value = false
  }
}

// 获取备选学校列表
function getFallbackSchools(query) {
  const commonSchools = [
    '北京大学', '清华大学', '复旦大学', '上海交通大学', '浙江大学',
    '南京大学', '中国科学技术大学', '哈尔滨工业大学', '西安交通大学',
    '武汉大学', '华中科技大学', '中山大学', '四川大学', '南开大学',
    '天津大学', '山东大学', '吉林大学', '厦门大学', '东南大学', '同济大学'
  ]
  
  const lowerQuery = query.toLowerCase()
  const matched = commonSchools.filter(school =>
    school.toLowerCase().includes(lowerQuery)
  )
  
  if (matched.length > 0) {
    return matched.map(school => ({
      value: school,
      label: `${school} (中国)`
    }))
  }
  
  // 如果没有匹配，返回常用学校列表
  return commonSchools.slice(0, 5).map(school => ({
    value: school,
    label: `${school} (中国)`
  }))
}

// 学校搜索输入处理
function handleSchoolSearch(query) {
  schoolSearchQuery.value = query
  searchSchools(query)
}

// 开始编辑
function startEdit() {
  initForm()
  isEditing.value = true
}

// 取消编辑
function cancelEdit() {
  isEditing.value = false
}

// 保存个人信息
async function saveProfile() {
  if (!form.value.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  
  loading.value = true
  try {
    await userApi.updateProfile({
      nickname: form.value.nickname.trim(),
      phone: form.value.phone.trim(),
      school: form.value.school.trim()
    })
    
    // 更新本地存储的用户信息
    await userStore.fetchProfile()
    
    ElMessage.success('个人信息更新成功')
    isEditing.value = false
  } catch (error) {
    ElMessage.error(`更新失败: ${error.message || '未知错误'}`)
  } finally {
    loading.value = false
  }
}

// 页面加载时初始化
onMounted(() => {
  initForm()
})
</script>

<template>
  <div class="profile-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3 style="margin: 0;">个人信息</h3>
          <el-button 
            v-if="!isEditing" 
            type="primary" 
            size="small" 
            @click="startEdit"
          >
            修改信息
          </el-button>
          <div v-else>
            <el-button size="small" @click="cancelEdit">取消</el-button>
            <el-button 
              type="primary" 
              size="small" 
              :loading="loading" 
              @click="saveProfile"
            >
              保存
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查看模式 -->
      <div v-if="!isEditing">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email }}</el-descriptions-item>
          <el-descriptions-item label="学校">{{ userStore.userInfo?.school || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未填写' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 编辑模式 -->
      <div v-else>
        <el-form :model="form" label-width="80px" size="medium">
          <el-form-item label="昵称" required>
            <el-input 
              v-model="form.nickname" 
              placeholder="请输入昵称" 
              maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="手机号">
            <el-input 
              v-model="form.phone" 
              placeholder="请输入手机号"
              maxlength="11"
            />
          </el-form-item>

          <el-form-item label="学校">
            <el-autocomplete
              v-model="form.school"
              :fetch-suggestions="handleSchoolSearch"
              :loading="schoolLoading"
              placeholder="输入学校名称搜索"
              clearable
              style="width: 100%"
              @select="(item) => form.school = item.value"
            >
              <template #default="{ item }">
                <div class="school-option">
                  <span class="school-name">{{ item.value }}</span>
                  <span class="school-country">{{ item.label.split('(')[1]?.replace(')', '') }}</span>
                </div>
              </template>
            </el-autocomplete>
            <div class="form-tip">
              输入至少2个字符开始搜索，支持中文和英文学校名称
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.profile-view {
  max-width: 700px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.school-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 8px 0;
  
  .school-name {
    font-weight: 500;
    color: #303133;
  }
  
  .school-country {
    font-size: 12px;
    color: #909399;
    background: #f4f4f5;
    padding: 2px 6px;
    border-radius: 3px;
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

:deep(.el-descriptions) {
  .el-descriptions-item__label {
    width: 100px;
    font-weight: 500;
  }
}
</style>
