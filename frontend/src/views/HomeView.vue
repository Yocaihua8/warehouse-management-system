<template>
  <div class="home-container">
    <h2 class="page-title">仓库管理系统首页</h2>

    <el-row :gutter="20" class="card-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">商品总数</div>
          <div class="stat-value">{{ dashboard.productCount }}</div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">客户总数</div>
          <div class="stat-value">{{ dashboard.customerCount }}</div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">库存商品数</div>
          <div class="stat-value">{{ dashboard.stockProductCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="card-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card warning-card">
          <div class="stat-title">低库存商品数</div>
          <div class="stat-value">{{ dashboard.lowStockCount }}</div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">入库单总数</div>
          <div class="stat-value">{{ dashboard.inboundOrderCount }}</div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">出库单总数</div>
          <div class="stat-value">{{ dashboard.outboundOrderCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="trend-card">
      <template #header>
        <div class="trend-header">
          <span>近 7 天入库 / 出库趋势（已完成单）</span>
          <el-button type="primary" link @click="loadDashboardTrend">刷新</el-button>
        </div>
      </template>

      <div class="trend-legend">
        <span class="legend-item"><i class="legend-dot inbound"></i>入库</span>
        <span class="legend-item"><i class="legend-dot outbound"></i>出库</span>
      </div>

      <div v-if="trendPoints.length === 0" class="trend-empty">暂无趋势数据</div>

      <svg v-else class="trend-svg" viewBox="0 0 760 260" preserveAspectRatio="none">
        <line x1="40" y1="20" x2="40" y2="220" class="axis-line" />
        <line x1="40" y1="220" x2="730" y2="220" class="axis-line" />

        <polyline :points="inboundPolylinePoints" class="line inbound-line" />
        <polyline :points="outboundPolylinePoints" class="line outbound-line" />

        <g v-for="point in chartPoints" :key="point.statDate">
          <circle :cx="point.x" :cy="point.inboundY" r="3.5" class="point inbound-point" />
          <circle :cx="point.x" :cy="point.outboundY" r="3.5" class="point outbound-point" />
          <text :x="point.x" y="242" class="x-label">{{ point.shortDate }}</text>
        </g>
      </svg>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardData, getDashboardTrend } from '../api/dashboard'

const dashboard = reactive({
  productCount: 0,
  customerCount: 0,
  stockProductCount: 0,
  lowStockCount: 0,
  inboundOrderCount: 0,
  outboundOrderCount: 0
})

const trendPoints = ref([])

const loadDashboardData = async () => {
  try {
    const res = await getDashboardData()
    const result = res.data

    if (result.code === 1) {
      Object.assign(dashboard, result.data)
    } else {
      ElMessage.error(result.message || '获取首页统计数据失败')
    }
  } catch (error) {
    ElMessage.error('请求首页统计数据失败')
    console.error(error)
  }
}

const loadDashboardTrend = async () => {
  try {
    const res = await getDashboardTrend(7)
    const result = res.data
    if (result.code === 1) {
      trendPoints.value = Array.isArray(result.data) ? result.data : []
    } else {
      ElMessage.error(result.message || '获取趋势数据失败')
    }
  } catch (error) {
    ElMessage.error('请求趋势数据失败')
    console.error(error)
  }
}

const chartPoints = computed(() => {
  if (!trendPoints.value.length) {
    return []
  }
  const left = 40
  const right = 730
  const top = 20
  const bottom = 220
  const width = right - left
  const height = bottom - top

  const maxValue = Math.max(
      1,
      ...trendPoints.value.map(item => Number(item.inboundCount || 0)),
      ...trendPoints.value.map(item => Number(item.outboundCount || 0))
  )

  return trendPoints.value.map((item, index) => {
    const ratio = trendPoints.value.length === 1 ? 0 : index / (trendPoints.value.length - 1)
    const x = left + ratio * width
    const inbound = Number(item.inboundCount || 0)
    const outbound = Number(item.outboundCount || 0)
    const inboundY = bottom - (inbound / maxValue) * height
    const outboundY = bottom - (outbound / maxValue) * height
    const statDate = item.statDate || ''
    return {
      statDate,
      shortDate: statDate.length >= 10 ? statDate.slice(5) : statDate,
      x: Number(x.toFixed(2)),
      inboundY: Number(inboundY.toFixed(2)),
      outboundY: Number(outboundY.toFixed(2))
    }
  })
})

const inboundPolylinePoints = computed(() => chartPoints.value.map(point => `${point.x},${point.inboundY}`).join(' '))
const outboundPolylinePoints = computed(() => chartPoints.value.map(point => `${point.x},${point.outboundY}`).join(' '))

onMounted(() => {
  loadDashboardData()
  loadDashboardTrend()
})
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.card-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 10px;
}

.stat-title {
  font-size: 16px;
  color: #909399;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.warning-card .stat-value {
  color: #e6a23c;
}

.trend-card {
  border-radius: 10px;
}

.trend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.trend-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 8px;
  color: #606266;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.legend-dot.inbound {
  background: #409eff;
}

.legend-dot.outbound {
  background: #67c23a;
}

.trend-empty {
  color: #909399;
  padding: 16px 0;
}

.trend-svg {
  width: 100%;
  height: 260px;
}

.axis-line {
  stroke: #dcdfe6;
  stroke-width: 1;
}

.line {
  fill: none;
  stroke-width: 2.5;
}

.inbound-line {
  stroke: #409eff;
}

.outbound-line {
  stroke: #67c23a;
}

.point {
  stroke: #fff;
  stroke-width: 1.5;
}

.inbound-point {
  fill: #409eff;
}

.outbound-point {
  fill: #67c23a;
}

.x-label {
  fill: #909399;
  font-size: 11px;
  text-anchor: middle;
}
</style>
