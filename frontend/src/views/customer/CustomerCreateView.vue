<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>新增客户</span>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="customer-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户编码" prop="customerCode">
              <el-input v-model="form.customerCode" placeholder="请输入客户编码" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户名称" />
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
import { addCustomer } from '../../api/customer'
import {
  createEmptyCustomField,
  serializeCustomFieldRows,
  validateCustomFieldRows
} from '../../utils/productCustomFields'

const router = useRouter()
const formRef = ref()

const form = reactive({
  customerCode: '',
  customerName: '',
  contactPerson: '',
  phone: '',
  address: '',
  customFields: [createEmptyCustomField()],
  remark: '',
  status: 1
})

const validateCustomFields = (_rule, _value, callback) => {
  const errorMessage = validateCustomFieldRows(form.customFields)
  if (errorMessage) {
    callback(new Error(errorMessage))
    return
  }
  callback()
}

const rules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  customFields: [{ validator: validateCustomFields, trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const handleCustomFieldsChange = (value) => {
  form.customFields = value
  formRef.value?.validateField('customFields').catch(() => {})
}

const handleBack = () => {
  router.push('/customer/list')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const customFieldsJson = serializeCustomFieldRows(form.customFields)
      const res = await addCustomer({
        customerCode: form.customerCode,
        customerName: form.customerName,
        contactPerson: form.contactPerson,
        phone: form.phone,
        address: form.address,
        customFieldsJson,
        remark: form.remark,
        status: form.status
      })

      if (res.data && res.data.code === 1) {
        ElMessage.success('新增客户成功')
        router.push('/customer/list')
      } else {
        ElMessage.error(res.data?.message || '新增客户失败')
      }
    } catch (error) {
      console.error('新增客户失败:', error)
      ElMessage.error('请求新增客户接口失败')
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

.customer-form {
  max-width: 1000px;
}

.form-actions {
  margin-top: 20px;
}
</style>
