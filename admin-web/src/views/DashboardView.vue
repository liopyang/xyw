<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { ArrowRight, Coin, Connection, Tickets, Van } from '@element-plus/icons-vue'
import { dashboardApi, type CardStat, type RankingItem, type Todos, type TrendQuery } from '../api/dashboard'
import type { BusinessType } from '../types/api'

const router = useRouter()
const loading = ref(true)
const cards = ref<CardStat[]>([])
const ranking = ref<RankingItem[]>([])
const todos = reactive<Todos>({ pendingOrders: 0, unexportedNetworks: 0, pendingIssues: 0, processingIssues: 0, monthlyAgentOrders: 0 })
const trendEl = ref<HTMLDivElement>()
const chart = ref<echarts.ECharts>()
const trendParams = reactive({ businessType: 'CAMPUS_CARD', range: '7d' })
const trendDates = ref<[string, string]>()
const rankParams = reactive({ type: 'CAMPUS_CARD', range: 'today' })

const meta: Record<BusinessType, { name: string; color: string; icon: object }> = {
  CAMPUS_CARD: { name: '校园卡', color: '#2d73eb', icon: Tickets },
  CAMPUS_NETWORK: { name: '校园网', color: '#27a780', icon: Connection },
  DRIVING_SCHOOL: { name: '驾校', color: '#f09342', icon: Van },
  RENEWAL: { name: '续费', color: '#8366e9', icon: Coin },
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function selectTrendRange(value: string) {
  if (value !== 'custom' || trendDates.value) return
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  trendDates.value = [formatDate(start), formatDate(end)]
}

async function loadTrend() {
  const params: TrendQuery = { businessType: trendParams.businessType, range: trendParams.range }
  if (trendParams.range === 'custom') {
    if (!trendDates.value?.[0] || !trendDates.value?.[1]) return
    params.start = trendDates.value[0]
    params.end = trendDates.value[1]
  }
  const res = await dashboardApi.trend(params)
  await nextTick()
  if (!trendEl.value) return
  chart.value ??= echarts.init(trendEl.value)
  chart.value.setOption({
    grid: { left: 42, right: 24, top: 30, bottom: 32 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', boundaryGap: false, data: res.data.map(item => item.date), axisLine: { lineStyle: { color: '#dce3ed' } }, axisLabel: { color: '#8490a1' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf1f6' } }, axisLabel: { color: '#8490a1' } },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: res.data.map(item => item.count),
      lineStyle: { width: 3, color: '#2d73eb' },
      itemStyle: { color: '#2d73eb' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(45,115,235,.22)' }, { offset: 1, color: 'rgba(45,115,235,0)' }]) },
    }],
  })
}

async function loadRanking() {
  ranking.value = (await dashboardApi.ranking(rankParams)).data
}

async function load() {
  loading.value = true
  try {
    const [cardResult, todoResult] = await Promise.all([dashboardApi.cards(), dashboardApi.todos()])
    cards.value = cardResult.data
    Object.assign(todos, todoResult.data)
    await Promise.all([loadTrend(), loadRanking()])
  } finally {
    loading.value = false
  }
}

function todo(path: string, query: Record<string, string>) {
  router.push({ path, query })
}

function resize() {
  chart.value?.resize()
}

watch(
  () => [trendParams.businessType, trendParams.range, trendDates.value?.[0], trendDates.value?.[1]],
  () => loadTrend(),
)
watch(() => [rankParams.type, rankParams.range], () => loadRanking())

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="heading"><div><h1 class="page-title">数据看板</h1><p class="page-desc">掌握今日业务进展与关键待办</p></div><span class="date">数据更新于刚刚</span></div>
    <div class="cards">
      <div
        v-for="card in cards"
        :key="card.businessType"
        class="stat panel"
        role="button"
        tabindex="0"
        @click="todo('/orders', { businessType: card.businessType })"
        @keyup.enter="todo('/orders', { businessType: card.businessType })"
        @keyup.space.prevent="todo('/orders', { businessType: card.businessType })"
      >
        <div class="stat-top"><span class="stat-icon" :style="{ background: meta[card.businessType].color + '16', color: meta[card.businessType].color }"><component :is="meta[card.businessType].icon" /></span><b>{{ meta[card.businessType].name }}</b></div>
        <div class="figures"><div><strong>{{ card.today }}</strong><span>今日订单</span></div><i></i><div><strong>{{ card.month }}</strong><span>本月订单</span></div></div>
      </div>
    </div>
    <div class="grid">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div><h3>业务趋势</h3><p>已确认订单数量变化</p></div>
          <div class="controls">
            <el-select v-model="trendParams.businessType" style="width: 130px"><el-option v-for="(value, key) in meta" :key="key" :label="value.name" :value="key" /></el-select>
            <el-select v-model="trendParams.range" style="width: 110px" @change="selectTrendRange"><el-option label="近7天" value="7d" /><el-option label="近30天" value="30d" /><el-option label="本月" value="month" /><el-option label="自定义" value="custom" /></el-select>
            <el-date-picker v-if="trendParams.range === 'custom'" v-model="trendDates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" style="width: 240px" />
          </div>
        </div>
        <div ref="trendEl" class="chart"></div>
      </section>
      <section class="panel rank-panel">
        <div class="panel-head"><div><h3>代理排名</h3><p>已确认归属订单</p></div><el-select v-model="rankParams.range" style="width: 90px"><el-option label="今日" value="today" /><el-option label="本月" value="month" /></el-select></div>
        <el-select v-model="rankParams.type" class="rank-type"><el-option label="校园卡数量" value="CAMPUS_CARD" /><el-option label="校园网数量" value="CAMPUS_NETWORK" /><el-option label="驾校报名" value="DRIVING_SCHOOL" /><el-option label="续费数量" value="RENEWAL" /><el-option label="总订单" value="TOTAL" /></el-select>
        <div class="rank-list"><div v-for="(item, index) in ranking.slice(0, 5)" :key="item.agentId" class="rank-row"><span :class="['rank-no', index < 3 ? 'top' : '']">{{ index + 1 }}</span><span class="rank-avatar">{{ item.agentName.slice(0, 1) }}</span><b>{{ item.agentName }}</b><strong>{{ item.count }} <small>单</small></strong></div><el-empty v-if="!ranking.length" :image-size="70" description="暂无排名数据" /></div>
      </section>
    </div>
    <section class="panel todo-panel">
      <div class="panel-head"><div><h3>待办事项</h3><p>需要关注与处理的业务</p></div></div>
      <div class="todos">
        <button @click="todo('/orders', { auditStatus: 'PENDING' })"><span class="dot blue"></span><div><strong>{{ todos.pendingOrders }}</strong><p>待确认订单</p></div><ArrowRight /></button>
        <button @click="todo('/orders', { businessType: 'CAMPUS_NETWORK', exportStatus: 'NOT_EXPORTED' })"><span class="dot orange"></span><div><strong>{{ todos.unexportedNetworks }}</strong><p>校园网未导出</p></div><ArrowRight /></button>
        <button @click="todo('/issues', { status: 'PENDING' })"><span class="dot red"></span><div><strong>{{ todos.pendingIssues }}</strong><p>待处理问题</p></div><ArrowRight /></button>
        <button @click="todo('/issues', { status: 'PROCESSING' })"><span class="dot purple"></span><div><strong>{{ todos.processingIssues }}</strong><p>处理中问题</p></div><ArrowRight /></button>
        <button @click="todo('/agents', { range: 'month' })"><span class="dot green"></span><div><strong>{{ todos.monthlyAgentOrders }}</strong><p>本月代理订单</p></div><ArrowRight /></button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.heading { display: flex; justify-content: space-between; }
.date { margin-top: 9px; color: #8a95a5; font-size: 13px; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat { padding: 20px; cursor: pointer; }
.stat:focus-visible { outline: 2px solid #2d73eb; outline-offset: 2px; }
.stat-top { display: flex; align-items: center; gap: 10px; }
.stat-icon { width: 38px; height: 38px; padding: 9px; border-radius: 10px; }
.figures { display: flex; align-items: center; justify-content: space-around; margin-top: 22px; }
.figures div { display: flex; flex-direction: column; }
.figures strong { font-size: 27px; }
.figures span { margin-top: 5px; color: #8b96a6; font-size: 12px; }
.figures i { width: 1px; height: 35px; background: #e9edf3; }
.grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr); gap: 16px; margin-top: 16px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 22px; }
.panel-head h3 { margin: 0; font-size: 17px; }
.panel-head p { margin: 5px 0 0; color: #8b96a6; font-size: 12px; }
.controls { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }
.chart { height: 312px; }
.rank-type { width: calc(100% - 44px); margin: 0 22px; }
.rank-list { padding: 12px 22px 17px; }
.rank-row { display: flex; align-items: center; gap: 10px; height: 44px; border-bottom: 1px solid #f0f2f6; }
.rank-row:last-child { border: 0; }
.rank-no { width: 22px; color: #97a1af; text-align: center; }
.rank-no.top { color: #e99a2e; font-weight: 800; }
.rank-avatar { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 50%; background: #edf3fd; color: #276bd7; font-size: 12px; }
.rank-row b { flex: 1; font-size: 13px; }
.rank-row strong { color: #253247; }
.rank-row small { color: #9aa4b2; font-weight: 400; }
.todo-panel { margin-top: 16px; }
.todos { display: grid; grid-template-columns: repeat(5, 1fr); padding: 0 22px 22px; }
.todos button { display: flex; align-items: center; padding: 8px 18px; border: 0; border-right: 1px solid #edf0f4; background: #fff; text-align: left; cursor: pointer; }
.todos button:first-child { padding-left: 0; }
.todos button:last-child { border: 0; }
.todos button div { flex: 1; margin-left: 12px; }
.todos strong { font-size: 22px; }
.todos p { margin: 4px 0 0; color: #8a95a5; font-size: 12px; }
.todos svg { width: 15px; color: #a9b1bd; }
.dot { width: 9px; height: 38px; border-radius: 5px; }
.blue { background: #4a83e9; }.orange { background: #f2a14a; }.red { background: #e76f6a; }.purple { background: #8c6fe8; }.green { background: #32ae85; }
</style>
