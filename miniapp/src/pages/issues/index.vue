<script setup lang="ts">
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { issueStatusLabels, issueTypeLabels } from '../../utils/labels'
import { request, type PageResult } from '../../utils/request'

interface IssueRow {
  id: number
  issueNo: string
  issueType: string
  description: string
  status: string
  processRemark?: string
  submittedAt: string
}

const rows = ref<IssueRow[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

const add = () => uni.navigateTo({ url: '/pages/issue-form/index' })
const open = (id: number) => uni.navigateTo({ url: `/pages/issue-detail/index?id=${id}` })

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    total.value = 0
    finished.value = false
  }
  if (finished.value) return
  loading.value = true
  try {
    const result = await request<PageResult<IssueRow>>('/mini/issues', 'GET', {
      page: page.value,
      pageSize,
    })
    rows.value = reset ? result.records : rows.value.concat(result.records)
    total.value = Number(result.total || 0)
    finished.value = rows.value.length >= total.value || result.records.length < pageSize
    if (!finished.value) page.value += 1
  } finally {
    loading.value = false
  }
}

onShow(() => load(true))
onReachBottom(() => load())
onPullDownRefresh(async () => {
  try {
    await load(true)
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<template>
  <view class="page list-page">
    <view v-for="row in rows" :key="row.id" class="card" @click="open(row.id)">
      <view class="row">
        <b>{{ issueTypeLabels[row.issueType] || row.issueType }}</b>
        <text class="tag">{{ issueStatusLabels[row.status] || row.status }}</text>
      </view>
      <view class="desc">{{ row.issueNo }} · {{ row.submittedAt }}</view>
      <view class="description">{{ row.description }}</view>
      <view v-if="row.processRemark" class="reply-preview">已有工作人员回复，点击查看详情</view>
    </view>
    <view v-if="!loading && rows.length === 0" class="empty">暂无问题记录</view>
    <view v-if="rows.length" class="load-state">
      {{ loading ? '正在加载...' : finished ? `已加载全部 ${total} 条问题` : '上拉加载更多' }}
    </view>
    <button class="primary add" @click.stop="add">提交问题</button>
  </view>
</template>

<style scoped>
.list-page { padding-bottom: 180rpx; }
.description { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-height: 1.6; }
.reply-preview { margin-top: 20rpx; padding: 16rpx 18rpx; border-radius: 12rpx; background: #f2f6ff; color: #2469dc; font-size: 24rpx; }
.empty, .load-state { padding: 70rpx 0; text-align: center; color: #8a95a5; font-size: 25rpx; }
.load-state { padding: 20rpx 0 50rpx; }
.add { position: fixed; right: 30rpx; bottom: 150rpx; width: 220rpx; z-index: 2; }
</style>
