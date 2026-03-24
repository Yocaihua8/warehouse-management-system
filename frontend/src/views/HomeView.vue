<template>
  <div class="home-container">
    <el-card shadow="never" class="home-card">
      <h2>仓库管理系统</h2>
      <p class="desc">当前已完成后端主业务链路，前端正在逐步补齐。</p>

      <el-row :gutter="16" class="card-row">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">商品管理</div>
            <div class="stat-desc">维护商品基础信息</div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">入库管理</div>
            <div class="stat-desc">登记入库单并联动库存</div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-title">出库管理</div>
            <div class="stat-desc">登记出库单并扣减库存</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-container {
  padding: 24px;
}

.home-card {
  border-radius: 12px;
}

.desc {
  margin-top: 8px;
  color: #606266;
}

.card-row {
  margin-top: 24px;
}

.stat-card {
  border-radius: 12px;
}

.stat-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stat-desc {
  font-size: 14px;
  color: #909399;
}
</style>