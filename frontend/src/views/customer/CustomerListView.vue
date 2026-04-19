<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>客户管理</span>
          <el-button type="primary" @click="handleAdd">
            新增客户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="客户编码">
          <el-input
              v-model="queryForm.customerCode"
              placeholder="请输入客户编码"
              clearable
              style="width: 220px"
          />
        </el-form-item>

        <el-form-item label="客户名称">
          <el-input
              v-model="queryForm.customerName"
              placeholder="请输入客户名称"
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
        <el-table-column prop="customerCode" label="客户编码" width="160" />
        <el-table-column prop="customerName" label="客户名称" min-width="180" />
        <el-table-column prop="contactPerson" label="联系人" width="140" />
        <el-table-column prop="phone" label="联系电话" width="160" />
        <el-table-column prop="address" label="地址" min-width="220" />
        <el-table-column label="自定义字段摘要" min-width="220">
          <template #default="scope">
            <el-tooltip
              v-if="scope.row.customFieldsJson"
              placement="top"
              effect="dark"
            >
              <template #content>
                <ProductCustomFieldsDisplay :value="scope.row.customFieldsJson" />
              </template>
              <ProductCustomFieldsDisplay compact :value="scope.row.customFieldsJson" />
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">
              删除
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import ProductCustomFieldsDisplay from '../../components/product/ProductCustomFieldsDisplay.vue'
import { deleteCustomer, getCustomerList } from '../../api/customer'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryForm = reactive({
  customerCode: '',
  customerName: ''
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

const loadCustomerList = async () => {
  loading.value = true
  try {
    const res = await getCustomerList({
      customerCode: queryForm.customerCode,
      customerName: queryForm.customerName,
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
      ElMessage.error(res.data?.message || '查询客户列表失败')
    }
  } catch (error) {
    tableData.value = []
    total.value = 0
    console.error('加载客户列表失败:', error)
    ElMessage.error('请求客户列表接口失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadCustomerList()
}

const handleReset = () => {
  queryForm.customerCode = ''
  queryForm.customerName = ''
  pageNum.value = 1
  pageSize.value = 10
  loadCustomerList()
}

const handleAdd = () => {
  router.push('/customer/create')
}

const handleEdit = (row) => {
  router.push({
    path: `/customer/update/${row.id}`,
    query: {
      customerCode: row.customerCode,
      customerName: row.customerName,
      contactPerson: row.contactPerson,
      phone: row.phone,
      address: row.address,
      remark: row.remark || '',
      status: row.status
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除客户【${row.customerName}】吗？`,
        '删除确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const res = await deleteCustomer(row.id)

    if (res.data && res.data.code === 1) {
      ElMessage.success('删除客户成功')

      if (tableData.value.length === 1 && pageNum.value > 1) {
        pageNum.value -= 1
      }

      loadCustomerList()
    } else {
      ElMessage.error(res.data?.message || '删除客户失败')
    }
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除客户失败:', error)
    ElMessage.error('请求删除客户接口失败')
  }
}

const handleCurrentChange = (currentPage) => {
  pageNum.value = currentPage
  loadCustomerList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadCustomerList()
}

onMounted(() => {
  loadCustomerList()
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
