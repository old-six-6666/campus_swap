<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { squareApi } from '@/api/modules/square'
import { showSuccess, showError, showWarning } from '@/utils/notify'
import { Plus } from '@element-plus/icons-vue'
import { CATEGORIES, CATEGORY_TAGS, COMMON_TAGS } from '@/constants/itemTags'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const syncToSquare = ref(false)

const form = reactive({
  title: '',
  price: '',
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

async function handlePublish() {
  await formRef.value.validate()
  loading.value = true
  try {
    const tags = form.tags.includes(form.category)
      ? form.tags
      : [form.category, ...form.tags]
    const itemId = await itemApi.publish({ ...form, tags })
    if (syncToSquare.value && itemId) {
      try {
        await squareApi.createPost({
          type: 1,
          content: `我发布了新闲置：${form.title}，快来看看吧～`,
          itemId,
          imageList: [],
          tagIds: [],
        })
        await showSuccess('发布成功，已同步到广场')
      } catch {
        await showSuccess('发布成功，但同步广场失败')
      }
    } else {
      await showSuccess('发布成功')
    }
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
            placeholder="请描述商品情况（新旧程度、配件、购入时间等）"
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
          <el-checkbox v-model="syncToSquare">同步发布到广场动态</el-checkbox>
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
