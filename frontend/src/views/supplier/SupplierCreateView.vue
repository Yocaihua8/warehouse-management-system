<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>新增供应商</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="supplier-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="请输入供应商编码" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入地址" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="自定义字段" prop="customFields">
              <ProductCustomFieldsEditor
                v-model="form.customFields"
                @update:model-value="handleCustomFieldsChange"
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
        </el-row>

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" @click="handleSubmit">提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ProductCustomFieldsEditor from '../../components/product/ProductCustomFieldsEditor.vue'
import { addSupplier } from '../../api/supplier'
import {
  createEmptyCustomField,
  serializeCustomFieldRows,
  validateCustomFieldRows
} from '../../utils/productCustomFields'

const router = useRouter()
const formRef = ref()

const form = reactive({
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  phone: '',
  address: '',
  customFields: [createEmptyCustomField()],
  remark: ''
})

const rules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  customFields: [{
    validator: (_rule, _value, callback) => {
      const errorMessage = validateCustomFieldRows(form.customFields)
      if (errorMessage) {
        callback(new Error(errorMessage))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

const handleCustomFieldsChange = (value) => {
  form.customFields = value
  formRef.value?.validateField('customFields').catch(() => {})
}

const handleBack = () => {
  router.push('/supplier/list')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const customFieldsJson = serializeCustomFieldRows(form.customFields)
      const res = await addSupplier({
        supplierCode: form.supplierCode,
        supplierName: form.supplierName,
        contactPerson: form.contactPerson,
        phone: form.phone,
        address: form.address,
        customFieldsJson,
        remark: form.remark
      })

      if (res.data && res.data.code === 1) {
        ElMessage.success('新增供应商成功')
        router.push('/supplier/list')
      } else {
        ElMessage.error(res.data?.message || '新增供应商失败')
      }
    } catch (error) {
      console.error('新增供应商失败:', error)
      ElMessage.error('请求新增供应商接口失败')
    }
  })
}
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

.supplier-form {
  max-width: 1000px;
}

.form-actions {
  margin-top: 20px;
}
</style>
