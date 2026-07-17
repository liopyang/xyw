<script setup lang="ts">
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import { ref } from 'vue';
import { auditStatusLabels, businessTypeLabels } from '../../utils/labels';
import { request, type PageResult } from '../../utils/request';
import { currentUser } from '../../utils/session';

interface OrderRow {
  id: number;
  orderNo: string;
  businessType: string;
  name: string;
  phone: string;
  businessNumber?: string;
  auditStatus: string;
  createdAt: string;
}

const rows = ref<OrderRow[]>([]);
const user = currentUser();
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const loading = ref(false);
const finished = ref(false);

const add = () => uni.navigateTo({ url: '/pages/order-form/index' });
const open = (id: number) => uni.navigateTo({ url: `/pages/order-detail/index?id=${id}` });

async function load(reset = false) {
  if (user.role !== 'AGENT' || loading.value) return;
  if (reset) {
    page.value = 1;
    total.value = 0;
    finished.value = false;
  }
  if (finished.value) return;
  loading.value = true;
  try {
    const result = await request<PageResult<OrderRow>>('/mini/orders', 'GET', {
      page: page.value,
      pageSize,
    });
    rows.value = reset ? result.records : rows.value.concat(result.records);
    total.value = Number(result.total || 0);
    finished.value = rows.value.length >= total.value || result.records.length < pageSize;
    if (!finished.value) page.value += 1;
  } finally {
    loading.value = false;
  }
}

onShow(() => load(true));
onReachBottom(() => load());
onPullDownRefresh(async () => {
  try {
    await load(true);
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<template>
  <view class="page list-page">
    <view
      v-if="user.role !== 'AGENT'"
      class="card empty-card"
    >
      普通用户无订单功能，可在“问题”页面提交业务问题。
    </view>
    <view
      v-for="row in rows"
      :key="row.id"
      class="card"
      @click="open(row.id)"
    >
      <view class="row">
        <b>{{ row.name }} · {{ businessTypeLabels[row.businessType] || row.businessType }}</b>
        <text class="tag">{{ auditStatusLabels[row.auditStatus] || row.auditStatus }}</text>
      </view>
      <view class="desc">{{ row.orderNo }}</view>
      <view>{{ row.phone }}　{{ row.businessNumber || '-' }}</view>
      <view class="desc created-at">{{ row.createdAt }}</view>
    </view>
    <view
      v-if="user.role === 'AGENT' && !loading && rows.length === 0"
      class="empty"
    >
      暂无订单
    </view>
    <view
      v-if="user.role === 'AGENT' && rows.length"
      class="load-state"
    >
      {{ loading ? '正在加载...' : finished ? `已加载全部 ${total} 条订单` : '上拉加载更多' }}
    </view>
    <button
      v-if="user.role === 'AGENT'"
      class="primary add"
      @click.stop="add"
    >
      提交订单
    </button>
  </view>
</template>

<style scoped>
.list-page {
  padding-bottom: 180rpx;
}
.created-at {
  margin: 14rpx 0 0;
}
.empty-card {
  color: #697586;
  line-height: 1.7;
}
.empty,
.load-state {
  padding: 70rpx 0;
  text-align: center;
  color: #8a95a5;
  font-size: 25rpx;
}
.load-state {
  padding: 20rpx 0 50rpx;
}
.add {
  position: fixed;
  right: 30rpx;
  bottom: 150rpx;
  width: 220rpx;
  z-index: 2;
}
</style>
