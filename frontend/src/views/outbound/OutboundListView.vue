<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>出库单列表</span>
          <el-button type="primary" @click="handleAdd">
            新增出库单
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="出库单号">
          <el-input
              v-model="queryForm.orderNo"
              placeholder="请输入出库单号"
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
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="出库单号" min-width="180" />
        <el-table-column prop="customerName" label="客户名称" min-width="180" />
        <el-table-column prop="totalAmount" label="总金额" width="120" />
        <el-table-column prop="orderStatus" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.orderStatus === 1 ? 'success' : 'info'">
              {{ scope.row.orderStatus === 1 ? '已完成' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column prop="remark" label="备注" min-width="220" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleDetail(scope.row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getOutboundList } from '../../api/outbound'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)


const queryForm = reactive({
  orderNo: ''
})

const loadOutboundList = async () => {
  loading.value = true
  try {
    const res = await getOutboundList({
      orderNo: queryForm.orderNo,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    if (res.data && res.data.code === 1) {
      const data = res.data.data
      if (Array.isArray(data)) {
        tableData.value = data
        total.value = data.length
      } else {
        tableData.value = data?.list || []
        total.value = data?.total || 0
      }
    } else {
      ElMessage.error(res.data?.message || '查询出库单列表失败')
    }
  } catch (error) {
    console.error('加载出库单列表失败:', error)
    ElMessage.error('请求出库单列表接口失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadOutboundList()
}

const handleReset = () => {
  queryForm.orderNo = ''
  pageNum.value = 1
  pageSize.value = 10
  loadOutboundList()
}

const handleAdd = () => {
  router.push('/outbound/create')
}

const handleDetail = (id) => {
  router.push(`/outbound/detail/${id}`)
}

const handleCurrentChange = (currentPage) => {
  pageNum.value = currentPage
  loadOutboundList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadOutboundList()
}

onMounted(() => {
  loadOutboundList()
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