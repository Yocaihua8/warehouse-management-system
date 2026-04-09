<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>编辑商品</span>
        </div>
      </template>

      <el-form
          v-loading="loading"
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="product-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品编码" prop="productCode">
              <el-input v-model="form.productCode" placeholder="请输入商品编码" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="请输入规格" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="form.category" placeholder="请输入分类" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="销售价" prop="salePrice">
              <el-input-number
                  v-model="form.salePrice"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="自定义字段" prop="customFieldsJson">
              <el-input
                  v-model="form.customFieldsJson"
                  type="textarea"
                  :rows="4"
                  placeholder='请输入JSON对象，如 {"brand":"A牌","origin":"温州"}'
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" @click="handleSubmit">保存修改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, updateProduct } from '../../api/product'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  id: null,
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  category: '',
  salePrice: 0,
  customFieldsJson: '',
  remark: '',
  status: 1
})

const validateCustomFieldsJson = (_rule, value, callback) => {
  const text = (value || '').trim()
  if (!text) {
    callback()
    return
  }
  if (text.length > 4000) {
    callback(new Error('自定义字段长度不能超过4000个字符'))
    return
  }
  try {
    const parsed = JSON.parse(text)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      callback(new Error('自定义字段必须是JSON对象'))
      return
    }
    callback()
  } catch (e) {
    callback(new Error('自定义字段不是合法JSON'))
  }
}

const rules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  specification: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'change' }],
  customFieldsJson: [{ validator: validateCustomFieldsJson, trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadProductDetail = async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('商品ID无效')
    router.push('/product/list')
    return
  }

  form.id = id
  loading.value = true
  try {
    const res = await getProductDetail(id)
    if (!res.data || res.data.code !== 1 || !res.data.data) {
      ElMessage.error(res.data?.message || '加载商品详情失败')
      router.push('/product/list')
      return
    }

    const detail = res.data.data
    form.productCode = detail.productCode || ''
    form.productName = detail.productName || ''
    form.specification = detail.specification || ''
    form.unit = detail.unit || ''
    form.category = detail.category || ''
    form.salePrice = Number(detail.salePrice || 0)
    form.customFieldsJson = detail.customFieldsJson || ''
    form.remark = detail.remark || ''
    form.status = Number(detail.status ?? 1)
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('请求商品详情接口失败')
    router.push('/product/list')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/product/list')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const res = await updateProduct({
        id: form.id,
        productCode: form.productCode,
        productName: form.productName,
        specification: form.specification,
        unit: form.unit,
        category: form.category,
        salePrice: form.salePrice,
        customFieldsJson: form.customFieldsJson,
        remark: form.remark,
        status: form.status
      })

      if (res.data && res.data.code === 1) {
        ElMessage.success('修改商品成功')
        router.push('/product/list')
      } else {
        ElMessage.error(res.data?.message || '修改商品失败')
      }
    } catch (error) {
      console.error('修改商品失败:', error)
      ElMessage.error('请求修改商品接口失败')
    }
  })
}

onMounted(() => {
  loadProductDetail()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-form {
  max-width: 1000px;
}

.form-actions {
  margin-top: 20px;
}
</style>
