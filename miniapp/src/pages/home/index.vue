<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { saveDrivingPrices } from '../../utils/driving-prices'
import { request } from '../../utils/request'
import { currentUser } from '../../utils/session'

interface HomeData {
  role?: string
  todayOrders?: number
  monthOrders?: number
  pendingOrders?: number
  confirmedOrders?: number
  drivingPrices?: Record<string, unknown>
  [key: string]: unknown
}

const data = ref<HomeData>({})
const user = currentUser()
const go = (url: string) => uni.navigateTo({ url })

onShow(async () => {
  data.value = await request<HomeData>('/mini/home')
  saveDrivingPrices(data.value)
})
</script>

<template>
  <view class="page">
    <view class="title">你好，{{ user.realName }}</view>
    <view class="desc">今天也要认真服务每一位同学</view>
    <view v-if="user.role === 'AGENT'" class="stats">
      <view class="card"><b>{{ data.todayOrders || 0 }}</b><text>今日订单</text></view>
      <view class="card"><b>{{ data.monthOrders || 0 }}</b><text>本月订单</text></view>
      <view class="card"><b>{{ data.pendingOrders || 0 }}</b><text>待确认</text></view>
      <view class="card"><b>{{ data.confirmedOrders || 0 }}</b><text>已确认</text></view>
    </view>
    <view v-if="user.role === 'AGENT'" class="card">
      <b>快速提交订单</b>
      <view class="actions">
        <button size="mini" @click="go('/pages/order-form/index?type=CAMPUS_CARD')">校园卡</button>
        <button size="mini" @click="go('/pages/order-form/index?type=CAMPUS_NETWORK')">校园网</button>
        <button size="mini" @click="go('/pages/order-form/index?type=DRIVING_SCHOOL')">驾校</button>
        <button size="mini" @click="go('/pages/order-form/index?type=RENEWAL')">续费</button>
      </view>
    </view>
    <view class="card" @click="go('/pages/issue-form/index')">
      <b>遇到问题？</b>
      <view class="desc">提交问题，工作人员会及时处理</view>
    </view>
  </view>
</template>

<style scoped>
.stats { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
.stats .card { display: flex; flex-direction: column; }
.stats b { font-size: 48rpx; }
.stats text { font-size: 24rpx; color: #8a95a5; margin-top: 8rpx; }
.actions { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; margin-top: 26rpx; }
.actions button { width: 100%; margin: 0; background: #edf3ff; color: #2469dc; }
</style>
