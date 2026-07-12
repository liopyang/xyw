<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { orderApi, type NetworkExportFilters, type OrderQuery } from '../../api/orders'
import { agentApi } from '../../api/admin'
import type { Order } from '../../types/api'
import { useAuthStore } from '../../stores/auth'
import { maskPhone } from '../../utils/privacy'

interface AgentOption { id: number; name: string }

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const exportingBatch = ref(false)
const exportingId = ref<number>()
const searchVisible = ref(false)
const searchInput = ref<HTMLInputElement>()
const orders = ref<Order[]>([])
const agents = ref<AgentOption[]>([])
const total = ref(0)
const dates = ref<[string, string]>()
const filterRouteQueryKeys = [
  'businessType',
  'sourceChannel',
  'agentId',
  'auditStatus',
  'exportStatus',
  'keyword',
  'includeDeleted',
  'startTime',
  'endTime',
  'page',
  'pageSize',
] as const

function routeText(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function routeNumber(value: unknown) {
  const parsed = Number(routeText(value))
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
}

const query = reactive<OrderQuery>({
  page: 1,
  pageSize: 20,
  businessType: routeText(route.query.businessType),
  sourceChannel: routeText(route.query.sourceChannel),
  agentId: routeNumber(route.query.agentId),
  auditStatus: routeText(route.query.auditStatus),
  exportStatus: routeText(route.query.exportStatus),
})

const typeNames = { CAMPUS_CARD: '校园卡', CAMPUS_NETWORK: '校园网', DRIVING_SCHOOL: '驾校', RENEWAL: '续费' }
const sourceNames = { ONLINE: '线上', AGENT: '代理', STORE: '门店' }
const networkMode = computed(() => query.businessType === 'CAMPUS_NETWORK')

let loadSequence = 0
async function load() {
  const sequence = ++loadSequence
  loading.value = true
  try {
    query.startTime = dates.value?.[0]
    query.endTime = dates.value?.[1]
    const res = await orderApi.list(query)
    if (sequence !== loadSequence) return
    orders.value = [...res.data.records].sort((a, b) => {
      const timeResult = b.createdAt.localeCompare(a.createdAt)
      return timeResult || b.id - a.id
    })
    total.value = res.data.total
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

async function loadAgents() {
  const res = await agentApi.list({ page: 1, pageSize: 100 })
  agents.value = res.data.records.map((item: AgentOption) => ({ id: item.id, name: item.name }))
}

function search() {
  query.page = 1
  load()
}

async function toggleSearch() {
  searchVisible.value = !searchVisible.value
  if (searchVisible.value) {
    await nextTick()
    searchInput.value?.focus()
  } else if (query.keyword) {
    query.keyword = ''
    search()
  }
}

async function reset() {
  Object.assign(query, {
    page: 1,
    businessType: '',
    sourceChannel: '',
    agentId: undefined,
    auditStatus: '',
    exportStatus: '',
    keyword: '',
    includeDeleted: false,
  })
  dates.value = undefined

  const nextRouteQuery = { ...route.query }
  filterRouteQueryKeys.forEach((key) => delete nextRouteQuery[key])
  await router.replace({ path: route.path, query: nextRouteQuery, hash: route.hash })

  clearTimeout(filterTimer)
  filterTimer = setTimeout(search, 120)
}

async function action(kind: 'confirm' | 'void' | 'restore', row: Order) {
  const labels = { confirm: '确认', void: '作废', restore: '恢复' }
  try {
    await ElMessageBox.confirm(`确定要${labels[kind]}订单 ${row.orderNo} 吗？`, `${labels[kind]}订单`, { type: kind === 'void' ? 'warning' : 'info' })
  } catch {
    return
  }
  if (kind === 'confirm') await orderApi.confirm(row.id)
  else if (kind === 'void') await orderApi.void(row.id)
  else await orderApi.restore(row.id)
  ElMessage.success(`订单已${labels[kind]}`)
  load()
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function networkExportFilters(): Partial<NetworkExportFilters> {
  return {
    sourceChannel: query.sourceChannel || undefined,
    agentId: query.agentId,
    exportStatus: query.exportStatus || undefined,
    startTime: dates.value?.[0],
    endTime: dates.value?.[1],
  }
}

async function exportExcel() {
  exportingBatch.value = true
  try {
    const filters = networkExportFilters()
    const countResult = await orderApi.exportableNetworkCount(filters)
    const count = countResult.data.total
    if (!count) {
      ElMessage.warning('当前条件下没有已确认、未作废的校园网订单可导出')
      return
    }
    try {
      await ElMessageBox.confirm(
        `当前条件下共有 ${count} 条可导出订单。导出成功后，这些订单将标记为已导出。`,
        '确认批量导出',
        { confirmButtonText: `导出 ${count} 条`, type: 'warning' },
      )
    } catch {
      return
    }
    const blob = await orderApi.exportNetwork(filters)
    downloadBlob(blob, `校园网订单_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success(`已成功导出 ${count} 条订单`)
    await load()
  } finally {
    exportingBatch.value = false
  }
}

async function exportOne(row: Order) {
  exportingId.value = row.id
  try {
    const blob = await orderApi.exportNetwork({ orderId: row.id })
    downloadBlob(blob, `校园网订单_${row.orderNo}.xlsx`)
    ElMessage.success('该订单已导出')
    await load()
  } finally {
    exportingId.value = undefined
  }
}

let filterTimer: ReturnType<typeof setTimeout> | undefined
watch(
  () => [
    query.businessType,
    query.sourceChannel,
    query.agentId,
    query.auditStatus,
    query.exportStatus,
    query.includeDeleted,
    dates.value?.[0],
    dates.value?.[1],
  ],
  () => {
    clearTimeout(filterTimer)
    filterTimer = setTimeout(search, 120)
  },
)

onMounted(() => {
  load()
  loadAgents()
})
</script>

<template>
  <div class="page">
    <div class="heading">
      <div>
        <h1 class="page-title">订单管理</h1>
        <p class="page-desc">统一管理校园卡、校园网、驾校与续费业务</p>
      </div>
      <div>
        <el-button v-if="networkMode" :icon="Download" :loading="exportingBatch" @click="exportExcel">批量导出校园网订单</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/orders/create')">新增订单</el-button>
      </div>
    </div>

    <div class="filter-bar panel">
      <el-select v-model="query.businessType" clearable placeholder="业务类型" style="width: 140px">
        <el-option v-for="(value, key) in typeNames" :key="key" :label="value" :value="key" />
      </el-select>
      <el-select v-model="query.sourceChannel" clearable placeholder="来源渠道" style="width: 130px">
        <el-option v-for="(value, key) in sourceNames" :key="key" :label="value" :value="key" />
      </el-select>
      <el-select v-model="query.agentId" clearable filterable placeholder="归属代理" style="width: 150px">
        <el-option v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" />
      </el-select>
      <el-select v-model="query.auditStatus" clearable placeholder="审核状态" style="width: 130px">
        <el-option label="待确认" value="PENDING" />
        <el-option label="已确认" value="CONFIRMED" />
      </el-select>
      <el-select v-if="networkMode" v-model="query.exportStatus" clearable placeholder="导出状态" style="width: 130px">
        <el-option label="未导出" value="NOT_EXPORTED" />
        <el-option label="已导出" value="EXPORTED" />
      </el-select>
      <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始时间" end-placeholder="结束时间" style="width: 260px" />
      <el-button type="primary" :icon="Search" @click="toggleSearch">搜索</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <transition name="search-slide">
        <div v-if="searchVisible" class="keyword-search">
          <el-input ref="searchInput" v-model="query.keyword" clearable placeholder="搜索订单编号、姓名、手机号、业务号码、代理或备注" @keyup.enter="search" @clear="search">
            <template #append><el-button :icon="Search" aria-label="执行搜索" @click="search" /></template>
          </el-input>
        </div>
      </transition>
    </div>

    <div class="panel table-panel">
      <div class="table-tools">
        <b>订单列表 <span class="count">共 {{ total }} 条</span></b>
        <el-checkbox v-if="auth.isOwner" v-model="query.includeDeleted">显示已作废</el-checkbox>
      </div>
      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单编号" min-width="174" fixed />
        <el-table-column label="业务类型" width="96"><template #default="{ row }"><el-tag>{{ typeNames[row.businessType as keyof typeof typeNames] }}</el-tag></template></el-table-column>
        <el-table-column prop="name" label="姓名" width="88" />
        <el-table-column label="联系电话" width="126"><template #default="{ row }">{{ maskPhone(row.phone) }}</template></el-table-column>
        <el-table-column prop="businessNumber" label="业务号码" min-width="125" show-overflow-tooltip />
        <el-table-column label="来源" width="78"><template #default="{ row }">{{ sourceNames[row.sourceChannel as keyof typeof sourceNames] }}</template></el-table-column>
        <el-table-column prop="agentName" label="归属代理" width="100"><template #default="{ row }">{{ row.agentName || '-' }}</template></el-table-column>
        <el-table-column label="审核状态" width="96"><template #default="{ row }"><el-tag :type="row.auditStatus === 'CONFIRMED' ? 'success' : 'warning'">{{ row.auditStatus === 'CONFIRMED' ? '已确认' : '待确认' }}</el-tag></template></el-table-column>
        <el-table-column v-if="networkMode" label="导出状态" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.exportStatus === 'NOT_EXPORTED'"
              size="small"
              type="danger"
              plain
              :disabled="row.auditStatus !== 'CONFIRMED' || row.deleted || exportingId !== undefined"
              :loading="exportingId === row.id"
              :class="['export-tag', { 'is-disabled': row.auditStatus !== 'CONFIRMED' || row.deleted, 'is-loading': exportingId === row.id }]"
              @click="exportOne(row)"
            >未导出</el-button>
            <el-tag v-else type="info">已导出</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间 ↓" width="168" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">查看</el-button>
            <el-button v-if="!row.deleted" link type="primary" @click="router.push(`/orders/${row.id}?edit=1`)">编辑</el-button>
            <el-button v-if="!row.deleted && row.auditStatus === 'PENDING'" link type="success" @click="action('confirm', row)">确认</el-button>
            <el-button v-if="!row.deleted" link type="danger" @click="action('void', row)">作废</el-button>
            <el-button v-if="row.deleted && auth.isOwner" link type="warning" @click="action('restore', row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @change="load" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.heading { display: flex; justify-content: space-between; align-items: flex-start; }
.count { margin-left: 6px; color: #8a95a5; font-size: 12px; font-weight: 400; }
.keyword-search { width: 360px; }
.search-slide-enter-active, .search-slide-leave-active { transition: all .18s; }
.search-slide-enter-from, .search-slide-leave-to { opacity: 0; transform: translateX(-8px); }
.export-tag {
  min-height: 28px;
  padding: 0 9px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}
.export-tag.is-disabled { cursor: not-allowed; opacity: .65; }
.export-tag.is-loading { cursor: wait; opacity: .65; }
</style>
