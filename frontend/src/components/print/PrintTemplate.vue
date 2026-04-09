<template>
  <div class="print-page" v-loading="loading" :style="templateStyle">
    <div class="print-title">{{ title }}</div>
    <table class="print-table">
      <tr>
        <td colspan="6">{{ warehouseLabel }}：{{ warehouseName }}</td>
        <td colspan="3">单据编号</td>
        <td colspan="2">{{ detail.orderNo || '-' }}</td>
      </tr>
      <tr>
        <td colspan="6">{{ partyLabel }}：{{ partyName || '-' }}</td>
        <td colspan="3">录单日期</td>
        <td colspan="2">{{ detail.createdTime || '-' }}</td>
      </tr>
      <tr v-if="showSourceMeta">
        <td colspan="6">来源类型：{{ getSourceTypeText(detail.sourceType) }}</td>
        <td colspan="3">AI记录ID</td>
        <td colspan="2">{{ detail.sourceType === 'AI' ? (detail.aiRecordId || '-') : '-' }}</td>
      </tr>
      <tr v-if="showSourceMeta && detail.sourceType === 'AI'">
        <td colspan="6">AI任务号：{{ detail.aiTaskNo || '-' }}</td>
        <td colspan="3">源文件名</td>
        <td colspan="2">{{ detail.aiSourceFileName || '-' }}</td>
      </tr>
      <tr>
        <td colspan="11">备注摘要：{{ detail.remark || '-' }}</td>
      </tr>
      <tr>
        <th>序号</th>
        <th colspan="2">商品全名</th>
        <th>规格</th>
        <th>单位</th>
        <th>数量</th>
        <th>单价</th>
        <th colspan="2">金额</th>
        <th colspan="2">备注</th>
      </tr>
      <tr
        v-for="(item, index) in printItems"
        :key="`${rowKey}-${index}`"
        :class="['detail-row', { 'empty-row': item.isEmpty, nowrap: detailRowNoWrap }]"
      >
        <td>{{ displayCell(item.index) }}</td>
        <td colspan="2">{{ displayCell(item.productName) }}</td>
        <td>{{ displayCell(item.specification) }}</td>
        <td>{{ displayCell(item.unit) }}</td>
        <td>{{ displayCell(item.quantity) }}</td>
        <td>{{ displayCell(item.unitPrice) }}</td>
        <td colspan="2">{{ displayCell(item.amount) }}</td>
        <td colspan="2">{{ displayCell(item.remark) }}</td>
      </tr>
      <tr>
        <td colspan="6">合计</td>
        <td colspan="2">{{ detail.totalAmount ?? '-' }}</td>
        <td colspan="3"></td>
      </tr>
      <tr>
        <td colspan="11">金额合计（大写）：{{ toChineseAmount(detail.totalAmount) }}</td>
      </tr>
      <tr>
        <td colspan="6">公司名称：仓库管理系统</td>
        <td colspan="3">公司电话</td>
        <td colspan="2"></td>
      </tr>
      <tr>
        <td colspan="6">公司地址：-</td>
        <td colspan="3">联系人</td>
        <td colspan="2"></td>
      </tr>
    </table>

    <div class="print-signature">
      <span>经手人：</span>
      <span>录单人：</span>
      <span>审核人：</span>
      <span>库管（签字）：</span>
      <span>出纳（签字）：</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { buildPrintItems, displayCell, getSourceTypeText, toChineseAmount } from '../../utils/printAdapter'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  warehouseLabel: {
    type: String,
    required: true
  },
  warehouseName: {
    type: String,
    default: '总仓库'
  },
  partyLabel: {
    type: String,
    required: true
  },
  partyName: {
    type: String,
    default: ''
  },
  detail: {
    type: Object,
    required: true
  },
  rowKey: {
    type: String,
    default: 'print'
  },
  showSourceMeta: {
    type: Boolean,
    default: false
  },
  minRows: {
    type: Number,
    default: 12
  },
  detailRowHeight: {
    type: Number,
    default: 48
  },
  detailRowNoWrap: {
    type: Boolean,
    default: false
  }
})

const printItems = computed(() => buildPrintItems(props.detail.itemList, props.minRows))
const templateStyle = computed(() => ({
  '--detail-row-height': `${props.detailRowHeight}px`
}))
</script>

<style scoped>
.print-page {
  width: 1000px;
  margin: 0 auto;
  padding: 12px 8px;
  color: #000;
  background: #fff;
}

.print-title {
  text-align: center;
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 10px;
}

.print-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 20px;
}

.print-table td,
.print-table th {
  border: 1px solid #111;
  padding: 4px 6px;
  line-height: 1.35;
}

.detail-row td {
  height: var(--detail-row-height);
  min-height: var(--detail-row-height);
  padding-top: 0;
  padding-bottom: 0;
  vertical-align: middle;
  line-height: var(--detail-row-height);
}

.detail-row.nowrap td {
  white-space: nowrap;
}

.print-signature {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 18px;
}

@media print {
  .print-page {
    width: auto;
    margin: 0;
    padding: 0;
  }
}
</style>
