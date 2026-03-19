<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { itemApi } from '@/api/modules/item'
import { squareApi } from '@/api/modules/square'
import { showSuccess, showError, showWarning } from '@/utils/notify'
import { Plus } from '@element-plus/icons-vue'

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
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const categories = ['数码', '书籍', '服饰', '生活用品', '其他']
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
    const itemId = await itemApi.publish(form)
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
    <div class="page-header">
      <h2 class="page-title">发布闲置</h2>
      <p class="page-sub">将您的闲置物品发布出来，等待有缘人</p>
    </div>

    <div class="publish-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="left">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" placeholder="简洁描述您的物品" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="价格（元）" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            placeholder="0.00"
            style="width: 200px"
          />
          <span class="price-hint">设为 0 表示免费赠送</span>
        </el-form-item>

        <el-form-item label="商品分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 200px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="描述商品的新旧程度、配件情况、购入时间等..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品图片">
          <div>
            <div class="upload-tip">第一张图片将作为封面，最多上传 9 张</div>
            <el-upload
              v-model:file-list="fileList"
              list-type="picture-card"
              :http-request="handleUpload"
              :on-remove="handleRemove"
              :on-exceed="handleExceed"
              :limit="9"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="item-upload"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item>
          <div class="sync-row">
            <el-checkbox v-model="syncToSquare">同步发布到广场动态</el-checkbox>
            <span class="sync-hint">勾选后将自动在广场分享此物品</span>
          </div>
        </el-form-item>

        <el-form-item>
          <div class="action-row">
            <el-button type="primary" :loading="loading" size="large" @click="handlePublish">
              发布闲置
            </el-button>
            <el-button size="large" @click="router.back()">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';

.publish-view {
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  .page-title {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: $letter-spacing-base;
    margin-bottom: 4px;
  }

  .page-sub {
    font-size: 14px;
    color: $text-secondary;
    letter-spacing: $letter-spacing-base;
  }
}

.publish-card {
  background: $bg-card;
  border-radius: $border-radius-lg;
  padding: 32px 36px;
  box-shadow: $shadow-card;
  border: 1px solid $border-color;
}

.price-hint {
  font-size: 12px;
  color: $text-secondary;
  margin-left: 12px;
}

.upload-tip {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 10px;
  letter-spacing: $letter-spacing-base;
}

.item-upload {
  :deep(.el-upload--picture-card) {
    width: 100px;
    height: 100px;
    border-radius: $border-radius-sm !important;

    .upload-icon { font-size: 24px; color: $primary; }
  }

  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 100px;
    height: 100px;
    border-radius: $border-radius-sm;
  }
}

.sync-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .sync-hint {
    font-size: 12px;
    color: $text-secondary;
  }
}

.action-row {
  display: flex;
  gap: 12px;
}

@media (max-width: 600px) {
  .publish-card { padding: 24px 20px; }
}
</style>