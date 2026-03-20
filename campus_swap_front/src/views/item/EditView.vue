<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { showSuccess, showError, showWarning } from '@/utils/notify'
import { Plus } from '@element-plus/icons-vue'
import { CATEGORIES, CATEGORY_TAGS, COMMON_TAGS } from '@/constants/itemTags'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const pageLoading = ref(true)

const form = reactive({
  title: '',
  price: null,
  category: '',
  description: '',
  images: [],
  tags: [],
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const availableTags = computed(() => {
  if (!form.category) return []
  return [...(CATEGORY_TAGS[form.category] || []), ...COMMON_TAGS]
})

function onCategoryChange(val) {
  const valid = [...(CATEGORY_TAGS[val] || []), ...COMMON_TAGS]
  form.tags = form.tags.filter(t => valid.includes(t))
}

function toggleTag(tag) {
  const idx = form.tags.indexOf(tag)
  if (idx === -1) form.tags.push(tag)
  else form.tags.splice(idx, 1)
}

const fileList = ref([])

onMounted(async () => {
  try {
    const item = await itemApi.getDetail(route.params.id)
    form.title = item.title
    form.price = Number(item.price)
    form.category = item.category
    form.description = item.description
    form.images = item.images ? [...item.images] : []
    // 过滤掉分类本身，只保留额外标签
    form.tags = (item.tags || []).filter(t => t !== item.category)
    fileList.value = form.images.map((url, idx) => ({
      name: `image-${idx}`,
      url,
      status: 'success',
      response: url,
    }))
  } catch {
    showError('商品信息加载失败')
    router.back()
  } finally {
    pageLoading.value = false
  }
})

async function handleUpload({ file, onSuccess, onError }) {
  try {
    const url = await itemApi.uploadImage(file)
    form.images.push(url)
    onSuccess(url)
  } catch (e) {
    onError(e)
    showError('图片上传失败')
  }
}

function handleRemove(uploadFile) {
  const url = uploadFile.response ?? uploadFile.url
  form.images = form.images.filter((u) => u !== url)
}

function handleExceed() {
  showWarning('最多上传 9 张图片')
}

async function handleSave() {
  await formRef.value.validate()
  loading.value = true
  try {
    const tags = form.tags.includes(form.category)
      ? form.tags
      : [form.category, ...form.tags]
    await itemApi.update(route.params.id, { ...form, tags })
    await showSuccess('保存成功')
    router.push('/my-items')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-loading="pageLoading" class="edit-view">
    <el-card>
      <template #header>
        <h3>编辑闲置</h3>
      </template>

      <el-form
        v-if="!pageLoading"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品标题" />
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" @change="onCategoryChange">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <div v-if="!form.category" class="tag-tip">请先选择分类</div>
          <div v-else class="tag-selector">
            <el-check-tag
              v-for="tag in availableTags"
              :key="tag"
              :checked="form.tags.includes(tag)"
              @change="toggleTag(tag)"
            >{{ tag }}</el-check-tag>
          </div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请描述商品情况"
          />
        </el-form-item>

        <el-form-item label="图片">
          <div class="upload-tip">第一张图片将作为封面，最多上传 9 张</div>
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :http-request="handleUpload"
            :on-remove="handleRemove"
            :on-exceed="handleExceed"
            :limit="9"
            accept="image/jpeg,image/png,image/gif,image/webp"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.edit-view {
  max-width: 700px;
  margin: 0 auto;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-tip {
  font-size: 13px;
  color: #c0c4cc;
}
</style>
