<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>库存管理</span>
          <el-button
              type="success"
              :loading="exporting"
              @click="handleExport('excel')"
          >
            导出Excel
          </el-button>
          <el-button
              plain
              :loading="exportingCsv"
              @click="handleExport('csv')"
          >
            导出CSV
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="商品编码">
          <el-input
              v-model="queryForm.productCode"
              placeholder="请输入商品编码"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item label="商品名称">
          <el-input
              v-model="queryForm.productName"
              placeholder="请输入商品名称"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          class="table-area"
      >
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="productCode" label="商品编码" width="160" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="specification" label="规格" width="140" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="quantity" label="当前库存" width="120" />
        <el-table-column prop="warningQuantity" label="预警值" width="120" />
        <el-table-column prop="lowStock" label="低库存" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.lowStock === 1 ? 'danger' : 'success'">
              {{ scope.row.lowStock === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="openAdjustDialog(scope.row)">
              调整库存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="adjustDialogVisible"
        title="库存调整"
        width="520px"
        destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="商品">
          <el-input :model-value="`${adjustingRow.productCode || ''} / ${adjustingRow.productName || ''}`" disabled />
        </el-form-item>

        <el-form-item label="当前库存">
          <el-input-number
              v-model="adjustForm.quantity"
              :min="0"
              controls-position="right"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="预警库存">
          <el-input-number
              v-model="adjustForm.warningQuantity"
              :min="0"
              controls-position="right"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="调整原因">
          <el-input
              v-model="adjustForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请输入调整原因，例如：盘点差异修正"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button
            type="primary"
            :loading="adjustSubmitting"
            @click="handleAdjustStock"
        >
          保存调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportStockList, getStockList, updateStock } from '../../api/stock'

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)
const exportingCsv = ref(false)
const adjustDialogVisible = ref(false)
const adjustSubmitting = ref(false)
const adjustingRow = reactive({
  productId: null,
  productCode: '',
  productName: ''
})
const adjustForm = reactive({
  quantity: 0,
  warningQuantity: 0,
  reason: '盘点差异修正'
})

const queryForm = reactive({
  productCode: '',
  productName: ''
})

const parsePageData = (payload) => {
  if (Array.isArray(payload)) {
    return {
      list: payload,
      total: payload.length
    }
  }

  return {
    list: Array.isArray(payload?.list) ? payload.list : [],
    total: typeof payload?.total === 'number' ? payload.total : 0
  }
}

const loadStockList = async () => {
  loading.value = true
  try {
    const res = await getStockList({
      productCode: queryForm.productCode,
      productName: queryForm.productName,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    if (res.data && res.data.code === 1) {
      const pageData = parsePageData(res.data.data)
      tableData.value = pageData.list
      total.value = pageData.total
    } else {
      tableData.value = []
      total.value = 0
      ElMessage.error(res.data?.message || '加载库存列表失败')
    }
  } catch (error) {
    console.error('加载库存列表失败：', error)
    tableData.value = []
    total.value = 0
    ElMessage.error(error?.response?.data?.message || error?.message || '加载库存列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadStockList()
}

const handleReset = () => {
  queryForm.productCode = ''
  queryForm.productName = ''
  pageNum.value = 1
  pageSize.value = 10
  loadStockList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadStockList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  pageNum.value = 1
  loadStockList()
}

const handleExport = async (format = 'excel') => {
  const normalizedFormat = format === 'csv' ? 'csv' : 'excel'
  try {
    if (normalizedFormat === 'csv') {
      exportingCsv.value = true
    } else {
      exporting.value = true
    }
    const res = await exportStockList({
      productCode: queryForm.productCode,
      productName: queryForm.productName,
      format: normalizedFormat
    })

    const blob = new Blob([res.data], {
      type: normalizedFormat === 'csv'
        ? 'text/csv;charset=utf-8;'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = normalizedFormat === 'csv' ? '库存列表.csv' : '库存列表.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    ElMessage.success(normalizedFormat === 'csv' ? '库存列表CSV导出成功' : '库存列表Excel导出成功')
  } catch (error) {
    console.error('导出库存列表失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '导出库存列表失败')
  } finally {
    if (normalizedFormat === 'csv') {
      exportingCsv.value = false
    } else {
      exporting.value = false
    }
  }
}

const openAdjustDialog = (row) => {
  adjustingRow.productId = row.productId
  adjustingRow.productCode = row.productCode || ''
  adjustingRow.productName = row.productName || ''
  adjustForm.quantity = Number(row.quantity ?? 0)
  adjustForm.warningQuantity = Number(row.warningQuantity ?? 0)
  adjustForm.reason = '盘点差异修正'
  adjustDialogVisible.value = true
}

const handleAdjustStock = async () => {
  if (!adjustingRow.productId) {
    ElMessage.error('未找到商品ID，无法调整库存')
    return
  }

  if (adjustForm.quantity < 0 || adjustForm.warningQuantity < 0) {
    ElMessage.error('库存和预警库存不能小于 0')
    return
  }

  if (!adjustForm.reason || !adjustForm.reason.trim()) {
    ElMessage.error('请填写调整原因')
    return
  }

  try {
    adjustSubmitting.value = true
    const res = await updateStock({
      productId: adjustingRow.productId,
      quantity: Number(adjustForm.quantity),
      warningQuantity: Number(adjustForm.warningQuantity),
      reason: adjustForm.reason.trim()
    })

    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || '库存调整成功')
      adjustDialogVisible.value = false
      await loadStockList()
    } else {
      ElMessage.error(res.data?.message || '库存调整失败')
    }
  } catch (error) {
    console.error('库存调整失败：', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '库存调整失败')
  } finally {
    adjustSubmitting.value = false
  }
}

onMounted(() => {
  loadStockList()
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

.query-form {
  margin-bottom: 16px;
}

.table-area {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

</style>
