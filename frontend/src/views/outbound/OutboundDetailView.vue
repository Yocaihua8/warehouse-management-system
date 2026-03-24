<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>出库单详情</span>
        </div>
      </template>

      <div v-loading="loading">
        <el-descriptions
            title="单据信息"
            :column="2"
            border
            class="desc-area"
        >
          <el-descriptions-item label="出库单号">
            {{ detail.orderNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ detail.customerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            {{ detail.totalAmount ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.orderStatus === 1 ? 'success' : 'info'">
              {{ detail.orderStatus === 1 ? '已完成' : '待处理' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail.createdTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ detail.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="table-title">出库明细</div>

        <el-table
            :data="detail.itemList || []"
            border
            stripe
            class="table-area"
        >
          <el-table-column prop="productCode" label="商品编码" width="160" />
          <el-table-column prop="productName" label="商品名称" min-width="180" />
          <el-table-column prop="specification" label="规格" width="140" />
          <el-table-column prop="unit" label="单位" width="100" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="unitPrice" label="单价" width="120" />
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="remark" label="备注" min-width="180" />
        </el-table>

        <div class="form-actions">
          <el-button @click="handleBack">返回</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getOutboundDetail } from '../../api/outbound'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const detail = reactive({
  orderNo: '',
  customerName: '',
  totalAmount: null,
  orderStatus: null,
  createdTime: '',
  remark: '',
  itemList: []
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getOutboundDetail(id)

    if (res.data && res.data.code === 1) {
      const data = res.data.data || {}

      detail.orderNo = data.orderNo || ''
      detail.customerName = data.customerName || ''
      detail.totalAmount = data.totalAmount
      detail.orderStatus = data.orderStatus
      detail.createdTime = data.createdTime || ''
      detail.remark = data.remark || ''
      detail.itemList = data.itemList || []
    } else {
      ElMessage.error(res.data?.message || '加载出库单详情失败')
    }
  } catch (error) {
    console.error('加载出库单详情失败:', error)
    ElMessage.error('请求出库单详情接口失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/outbound/list')
}

onMounted(() => {
  loadDetail()
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

.desc-area {
  margin-bottom: 20px;
}

.table-title {
  margin: 12px 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.table-area {
  width: 100%;
}

.form-actions {
  margin-top: 20px;
}
</style>