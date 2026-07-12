<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { issueApi } from '../../api/admin'
import { maskPhone } from '../../utils/privacy'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)

function queryText(value: unknown) {
  return typeof value === 'string' ? value : ''
}

const q = reactive({
  page: 1,
  pageSize: 20,
  status: queryText(route.query.status),
  issueType: queryText(route.query.issueType),
  keyword: queryText(route.query.keyword),
})

const statusNames: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
const typeNames: Record<string, string> = { CAMPUS_CARD: '校园卡问题', CAMPUS_NETWORK: '校园网问题', DRIVING_SCHOOL: '驾校问题', RENEWAL: '续费问题', ACCOUNT: '账号问题', OTHER: '其他问题' }

let loadSequence = 0
async function load() {
  const sequence = ++loadSequence
  loading.value = true
  try {
    const result = await issueApi.list(q)
    if (sequence !== loadSequence) return
    rows.value = result.data.records
    total.value = result.data.total
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function search() {
  q.page = 1
  load()
}

watch(
  () => [q.status, q.issueType],
  () => search(),
)

watch(
  () => [route.query.status, route.query.issueType, route.query.keyword],
  ([status, issueType, keyword]) => {
    q.status = queryText(status)
    q.issueType = queryText(issueType)
    q.keyword = queryText(keyword)
    search()
  },
)

onMounted(load)
</script>

<template>
  <div class="page">
    <h1 class="page-title">问题管理</h1>
    <p class="page-desc">处理代理和普通用户提交的问题</p>
    <div class="filter-bar panel">
      <el-select v-model="q.status" clearable placeholder="处理状态" style="width: 130px">
        <el-option v-for="(value, key) in statusNames" :key="key" :label="value" :value="key" />
      </el-select>
      <el-select v-model="q.issueType" clearable placeholder="问题类型" style="width: 150px">
        <el-option v-for="(value, key) in typeNames" :key="key" :label="value" :value="key" />
      </el-select>
      <el-input v-model="q.keyword" placeholder="编号、提交人、手机号或描述" clearable style="width: 280px" @keyup.enter="search" @clear="search" />
      <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
    </div>
    <div class="panel table-panel">
      <div class="table-tools"><b>问题列表</b></div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="issueNo" label="问题编号" width="175" />
        <el-table-column prop="submitterName" label="提交人" />
        <el-table-column label="提交人类型"><template #default="{ row }">{{ row.submitterType === 'AGENT' ? '代理' : '普通用户' }}</template></el-table-column>
        <el-table-column label="联系电话" width="130"><template #default="{ row }">{{ maskPhone(row.contactPhone) }}</template></el-table-column>
        <el-table-column label="问题类型" width="130"><template #default="{ row }">{{ typeNames[row.issueType] }}</template></el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'PROCESSING' ? 'primary' : row.status === 'RESOLVED' ? 'success' : 'info'">{{ statusNames[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/issues/${row.id}`)">处理</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="q.page" v-model:page-size="q.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </div>
  </div>
</template>
