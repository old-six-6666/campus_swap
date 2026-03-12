<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ title: '', price: '', category: '', description: '', coverImage: '' })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const categories = ['数码', '书籍', '服饰', '生活用品', '其他']

async function handlePublish() {
  await formRef.value.validate()
  loading.value = true
  try {
    await itemApi.publish(form)
    ElMessage.success('发布成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="publish-view">
    <el-card>
      <template #header>
        <h3>发布闲置</h3>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品标题" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" placeholder="请输入价格" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述商品情况" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handlePublish">发布</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.publish-view {
  max-width: 700px;
  margin: 0 auto;
}
</style>
