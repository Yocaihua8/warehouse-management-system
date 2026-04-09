<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleOpenAddDialog">新增用户</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="用户名">
          <el-input
            v-model="queryForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="queryForm.nickname"
            placeholder="请输入昵称"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border stripe class="table-area">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="180" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
              {{ row.role === 'ADMIN' ? '管理员' : '操作员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleOpenEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      :title="isEditMode ? '编辑用户' : '新增用户'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" :disabled="isEditMode" />
        </el-form-item>
        <el-form-item :label="isEditMode ? '重置密码' : '密码'" prop="password">
          <el-input
            v-model="editForm.password"
            type="password"
            show-password
            :placeholder="isEditMode ? '留空表示不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="操作员" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEditMode ? '保存' : '新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addUser, deleteUser, getUserList, updateUser } from '../../api/user'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const queryForm = reactive({
  username: '',
  nickname: ''
})

const editDialogVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  role: 'OPERATOR',
  status: 1
})

const isEditMode = computed(() => editForm.id != null)

const editRules = computed(() => ({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: isEditMode.value
    ? []
    : [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '角色不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}))

const parsePageData = (payload) => {
  if (Array.isArray(payload)) {
    return { list: payload, total: payload.length }
  }
  return {
    list: Array.isArray(payload?.list) ? payload.list : [],
    total: typeof payload?.total === 'number' ? payload.total : 0
  }
}

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      username: queryForm.username,
      nickname: queryForm.nickname,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.data?.code === 1) {
      const pageData = parsePageData(res.data?.data)
      tableData.value = pageData.list
      total.value = pageData.total
    } else {
      tableData.value = []
      total.value = 0
      ElMessage.error(res.data?.message || '查询用户列表失败')
    }
  } catch (error) {
    tableData.value = []
    total.value = 0
    console.error('查询用户列表失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '查询用户列表失败')
  } finally {
    loading.value = false
  }
}

const resetEditForm = () => {
  editForm.id = null
  editForm.username = ''
  editForm.password = ''
  editForm.nickname = ''
  editForm.role = 'OPERATOR'
  editForm.status = 1
}

const handleOpenAddDialog = () => {
  resetEditForm()
  editDialogVisible.value = true
}

const handleOpenEditDialog = (row) => {
  resetEditForm()
  editForm.id = row.id
  editForm.username = row.username || ''
  editForm.nickname = row.nickname || ''
  editForm.role = row.role || 'OPERATOR'
  editForm.status = row.status === 0 ? 0 : 1
  editDialogVisible.value = true
}

const handleSubmit = async () => {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch (error) {
    return
  }

  submitLoading.value = true
  try {
    let res
    if (isEditMode.value) {
      res = await updateUser({
        id: editForm.id,
        password: editForm.password || undefined,
        nickname: editForm.nickname,
        role: editForm.role,
        status: editForm.status
      })
    } else {
      res = await addUser({
        username: editForm.username,
        password: editForm.password,
        nickname: editForm.nickname,
        role: editForm.role,
        status: editForm.status
      })
    }

    if (res.data?.code === 1) {
      ElMessage.success(res.data?.data || (isEditMode.value ? '修改用户成功' : '新增用户成功'))
      editDialogVisible.value = false
      await loadUserList()
      return
    }
    ElMessage.error(res.data?.message || (isEditMode.value ? '修改用户失败' : '新增用户失败'))
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '保存用户失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户【${row.username}】吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteUser(row.id)
    if (res.data?.code === 1) {
      ElMessage.success('删除用户成功')
      if (tableData.value.length === 1 && pageNum.value > 1) {
        pageNum.value -= 1
      }
      await loadUserList()
      return
    }
    ElMessage.error(res.data?.message || '删除用户失败')
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除用户失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '删除用户失败')
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadUserList()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.nickname = ''
  pageNum.value = 1
  pageSize.value = 10
  loadUserList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadUserList()
}

const handleCurrentChange = (currentPage) => {
  pageNum.value = currentPage
  loadUserList()
}

onMounted(() => {
  loadUserList()
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
