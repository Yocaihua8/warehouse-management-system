<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>编辑供应商</span>
        </div>
      </template>

      <el-form
        v-loading="loading"
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
import { getSupplierDetail, updateSupplier } from '../../api/supplier'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  id: null,
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  phone: '',
  address: '',
  remark: '',
  status: 1
})

const rules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadSupplierDetail = async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('供应商ID无效')
    router.push('/supplier/list')
    return
  }

  form.id = id
  loading.value = true
  try {
    const res = await getSupplierDetail(id)
    if (!res.data || res.data.code !== 1 || !res.data.data) {
      ElMessage.error(res.data?.message || '加载供应商详情失败')
      router.push('/supplier/list')
      return
    }

    const detail = res.data.data
    form.supplierCode = detail.supplierCode || ''
    form.supplierName = detail.supplierName || ''
    form.contactPerson = detail.contactPerson || ''
    form.phone = detail.phone || ''
    form.address = detail.address || ''
    form.remark = detail.remark || ''
    form.status = Number(detail.status ?? 1)
  } catch (error) {
    console.error('加载供应商详情失败:', error)
    ElMessage.error('请求供应商详情接口失败')
    router.push('/supplier/list')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/supplier/list')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      const res = await updateSupplier({
        id: form.id,
        supplierCode: form.supplierCode,
        supplierName: form.supplierName,
        contactPerson: form.contactPerson,
        phone: form.phone,
        address: form.address,
        remark: form.remark,
        status: form.status
      })

      if (res.data && res.data.code === 1) {
        ElMessage.success('修改供应商成功')
        router.push('/supplier/list')
      } else {
        ElMessage.error(res.data?.message || '修改供应商失败')
      }
    } catch (error) {
      console.error('修改供应商失败:', error)
      ElMessage.error('请求修改供应商接口失败')
    }
  })
}

onMounted(() => {
  loadSupplierDetail()
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

.supplier-form {
  max-width: 1000px;
}

.form-actions {
  margin-top: 20px;
}
</style>
