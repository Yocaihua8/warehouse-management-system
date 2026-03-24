<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>新增出库单</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" label-width="100px" class="outbound-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户" required>
              <el-select
                  v-model="form.customerId"
                  placeholder="请选择客户"
                  filterable
                  style="width: 100%"
              >
                <el-option
                    v-for="item in customerOptions"
                    :key="item.id"
                    :label="item.customerName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="items-header">
          <span>出库明细</span>
          <el-button type="primary" @click="handleAddItem">新增明细</el-button>
        </div>

        <div
            v-for="(item, index) in form.itemList"
            :key="index"
            class="item-card"
        >
          <el-row :gutter="16">
            <el-col :span="7">
              <el-form-item :label="`商品${index + 1}`" required>
                <el-select
                    v-model="item.productId"
                    placeholder="请选择商品"
                    filterable
                    style="width: 100%"
                >
                  <el-option
                      v-for="product in productOptions"
                      :key="product.id"
                      :label="`${product.productCode} / ${product.productName}`"
                      :value="product.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="4">
              <el-form-item label="数量" required>
                <el-input-number
                    v-model="item.quantity"
                    :min="1"
                    controls-position="right"
                    style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="5">
              <el-form-item label="单价" required>
                <el-input-number
                    v-model="item.unitPrice"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="6">
              <el-form-item label="明细备注">
                <el-input v-model="item.remark" placeholder="请输入明细备注" />
              </el-form-item>
            </el-col>

            <el-col :span="2" class="item-action">
              <el-button
                  type="danger"
                  link
                  @click="handleRemoveItem(index)"
                  :disabled="form.itemList.length === 1"
              >
                删除
              </el-button>
            </el-col>
          </el-row>
        </div>

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
          <el-button type="primary" @click="handleSubmit">提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getCustomerList } from '../../api/customer'
import { getProductList } from '../../api/product'
import { addOutboundOrder } from '../../api/outbound'

const router = useRouter()
const formRef = ref()

const customerOptions = ref([])
const productOptions = ref([])

const createEmptyItem = () => ({
  productId: null,
  quantity: 1,
  unitPrice: 0,
  remark: ''
})

const form = reactive({
  customerId: null,
  remark: '',
  itemList: [createEmptyItem()]
})

const loadCustomers = async () => {
  try {
    const res = await getCustomerList({})
    if (res.data && res.data.code === 1) {
      const data = res.data.data
      customerOptions.value = Array.isArray(data) ? data : (data?.list || [])
    } else {
      ElMessage.error(res.data?.message || '加载客户列表失败')
    }
  } catch (error) {
    console.error('加载客户列表失败:', error)
    ElMessage.error('请求客户列表接口失败')
  }
}

const loadProducts = async () => {
  try {
    const res = await getProductList({})
    if (res.data && res.data.code === 1) {
      const data = res.data.data
      productOptions.value = Array.isArray(data) ? data : (data?.list || [])
    } else {
      ElMessage.error(res.data?.message || '加载商品列表失败')
    }
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('请求商品列表接口失败')
  }
}

const handleAddItem = () => {
  form.itemList.push(createEmptyItem())
}

const handleRemoveItem = (index) => {
  if (form.itemList.length === 1) return
  form.itemList.splice(index, 1)
}

const handleBack = () => {
  router.push('/outbound/list')
}

const validateForm = () => {
  if (!form.customerId) {
    ElMessage.warning('请选择客户')
    return false
  }

  if (!form.itemList.length) {
    ElMessage.warning('请至少添加一条出库明细')
    return false
  }

  for (const item of form.itemList) {
    if (!item.productId) {
      ElMessage.warning('请选择商品')
      return false
    }
    if (!item.quantity || item.quantity <= 0) {
      ElMessage.warning('出库数量必须大于0')
      return false
    }
    if (item.unitPrice === null || item.unitPrice === undefined || item.unitPrice < 0) {
      ElMessage.warning('出库单价不能小于0')
      return false
    }
  }

  return true
}

const handleSubmit = async () => {
  if (!validateForm()) return

  try {
    const res = await addOutboundOrder({
      customerId: form.customerId,
      remark: form.remark,
      itemList: form.itemList.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        remark: item.remark
      }))
    })

    if (res.data && res.data.code === 1) {
      ElMessage.success('新增出库单成功')
      router.push('/outbound/list')
    } else {
      ElMessage.error(res.data?.message || '新增出库单失败')
    }
  } catch (error) {
    console.error('新增出库单失败:', error)
    ElMessage.error('请求新增出库单接口失败')
  }
}

onMounted(() => {
  loadCustomers()
  loadProducts()
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

.outbound-form {
  max-width: 1100px;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 16px;
  font-size: 16px;
  font-weight: 600;
}

.item-card {
  padding: 16px 16px 0;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.item-action {
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-actions {
  margin-top: 24px;
}
</style>