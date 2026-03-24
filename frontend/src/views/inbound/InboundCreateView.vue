<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="page-title">新增入库单</span>
          <div class="header-actions">
            <el-button type="primary" plain>智能识别导入</el-button>
            <el-button
                type="success"
                :loading="submitting"
                native-type="button"
                @click="handleSubmit"
            >
              提交保存
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="base-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商名称">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="table-toolbar">
        <span class="section-title">入库明细</span>
        <el-button type="primary" @click="addItem">新增明细</el-button>
      </div>

      <el-table :data="form.itemList" border style="width: 100%">
        <el-table-column label="商品信息" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-select
                  v-model="row.productId"
                  placeholder="请选择商品"
                  filterable
                  clearable
                  :loading="productLoading"
                  style="width: 100%"
                  @change="(value) => handleProductChange(row, value)"
              >
                <el-option
                    v-for="item in productOptions"
                    :key="item.id"
                    :label="`${item.productName}（ID: ${item.id}）`"
                    :value="item.id"
                />
              </el-select>

              <el-input
                  v-model="row.productName"
                  placeholder="商品名称自动带出"
                  disabled
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number
                v-model="row.quantity"
                :min="1"
                controls-position="right"
                style="width: 100%"
            />
          </template>
        </el-table-column>

        <el-table-column label="单价" width="160">
          <template #default="{ row }">
            <el-input-number
                v-model="row.unitPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
            />
          </template>
        </el-table-column>

        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <span>{{ calcAmount(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="请输入备注" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button
                type="danger"
                link
                @click="removeItem($index)"
                :disabled="form.itemList.length === 1"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { saveInboundOrder } from '../../api/inbound'
import { getProductList } from '../../api/product'

const submitting = ref(false)
const productOptions = ref([])
const productLoading = ref(false)

const createEmptyItem = () => ({
  productId: null,
  productName: '',
  quantity: 1,
  unitPrice: 0,
  remark: ''
})

const form = reactive({
  supplierName: '',
  remark: '',
  itemList: [createEmptyItem()]
})

const addItem = () => {
  form.itemList.push(createEmptyItem())
}

const removeItem = (index) => {
  if (form.itemList.length === 1) {
    ElMessage.warning('至少保留一条入库明细')
    return
  }
  form.itemList.splice(index, 1)
}

const calcAmount = (row) => {
  const quantity = Number(row.quantity || 0)
  const unitPrice = Number(row.unitPrice || 0)
  return (quantity * unitPrice).toFixed(2)
}

const loadProductOptions = async () => {
  try {
    productLoading.value = true
    const res = await getProductList({
      pageNum: 1,
      pageSize: 200
    })

    console.log('商品列表响应：', res.data)

    if (res.data?.code === 1) {
      productOptions.value = res.data?.data?.list || []
    } else {
      ElMessage.error(res.data?.message || '加载商品列表失败')
    }
  } catch (error) {
    console.error('加载商品列表失败：', error)
    ElMessage.error(error?.response?.data?.message || '加载商品列表失败')
  } finally {
    productLoading.value = false
  }
}

const handleProductChange = (row, productId) => {
  const selectedProduct = productOptions.value.find(
      item => Number(item.id) === Number(productId)
  )

  if (selectedProduct) {
    row.productName = selectedProduct.productName
  } else {
    row.productName = ''
  }
}

const validateForm = () => {
  if (!form.supplierName || !form.supplierName.trim()) {
    ElMessage.error('供应商名称不能为空')
    return false
  }

  if (!form.itemList.length) {
    ElMessage.error('入库明细不能为空')
    return false
  }

  for (let i = 0; i < form.itemList.length; i++) {
    const item = form.itemList[i]

    if (!item.productId) {
      ElMessage.error(`第 ${i + 1} 行商品不能为空`)
      return false
    }

    if (!item.quantity || item.quantity <= 0) {
      ElMessage.error(`第 ${i + 1} 行数量必须大于 0`)
      return false
    }

    if (item.unitPrice === null || item.unitPrice === undefined || item.unitPrice < 0) {
      ElMessage.error(`第 ${i + 1} 行单价不能小于 0`)
      return false
    }
  }

  return true
}

const buildPayload = () => {
  return {
    supplierName: form.supplierName.trim(),
    remark: form.remark,
    itemList: form.itemList.map(item => ({
      productId: Number(item.productId),
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      remark: item.remark
    }))
  }
}

const resetForm = () => {
  form.supplierName = ''
  form.remark = ''
  form.itemList = [createEmptyItem()]
}

const handleSubmit = async () => {
  if (!validateForm()) return

  const payload = buildPayload()
  console.log('提交参数：', payload)

  try {
    submitting.value = true
    const res = await saveInboundOrder(payload)
    console.log('响应数据：', res.data)

    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || '保存成功')
      resetForm()
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (error) {
    console.error('保存入库单失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProductOptions()
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.base-form {
  margin-bottom: 24px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.product-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>