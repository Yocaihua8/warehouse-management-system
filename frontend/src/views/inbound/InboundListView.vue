<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="page-title">入库单列表</span>
          <el-button type="primary" @click="goCreate">新增入库单</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="单号">
          <el-input
              v-model="queryForm.orderNo"
              placeholder="请输入入库单号"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
          :data="tableData"
          border
          v-loading="loading"
          style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="入库单号" min-width="180" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="160" />
        <el-table-column prop="totalAmount" label="总金额" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag type="success" v-if="row.orderStatus === 2">已完成</el-tag>
            <el-tag v-else>未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getInboundOrderList } from '../../api/inbound'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)


const queryForm = reactive({
  orderNo: ''
})

const pagination = reactive({
  total: 0,
  pageNum: 1,
  pageSize: 10
})

const loadTableData = async () => {
  try {
    loading.value = true

    const res = await getInboundOrderList({
      orderNo: queryForm.orderNo,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    console.log('入库单列表响应：', res.data)

    if (res.data && res.data.code === 1) {
      const data = res.data.data

      if (Array.isArray(data)) {
        tableData.value = data
        total.value = data.length
      } else {
        tableData.value = data?.list || []
        total.value = data?.total ?? tableData.value.length
      }
    }
  } catch (error) {
    console.error('加载入库单列表失败：', error)
    ElMessage.error(error?.response?.data?.message || '加载入库单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadTableData()
}

const handleReset = () => {
  queryForm.orderNo = ''
  pageNum.value = 1
  pageSize.value = 10
  loadInboundList()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadTableData()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadTableData()
}

const goCreate = () => {
  router.push('/inbound/create')
}

const goDetail = (id) => {
  router.push(`/inbound/detail/${id}`)
}

const handleDetail = (id) => {
  router.push(`/inbound/detail/${id}`)
}

const handleCurrentChange = (currentPage) => {
  pageNum.value = currentPage
  loadInboundList()
}

onMounted(() => {
  loadTableData()
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

.query-form {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>