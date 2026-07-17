<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app';
import { ref } from 'vue';
import {
  auditStatusLabels,
  businessTypeLabels,
  exportStatusLabels,
  sourceChannelLabels,
} from '../../utils/labels';
import { request } from '../../utils/request';
import { requireAgent } from '../../utils/session';

interface OrderDetail {
  id?: number;
  orderNo?: string;
  businessType?: string;
  name?: string;
  phone?: string;
  businessNumber?: string;
  sourceChannel?: string;
  auditStatus?: string;
  remark?: string;
  createdAt?: string;
  studentNo?: string;
  idCardLastSix?: string;
  exportStatus?: string;
  licenseType?: string;
  classType?: string;
  paymentAmount?: number;
  renewalAmount?: number;
}

const row = ref<OrderDetail>({});
const id = ref(0);
const loaded = ref(false);

async function load() {
  row.value = await request<OrderDetail>(`/mini/orders/${id.value}`);
}

onLoad(async (query: Record<string, string> = {}) => {
  if (!requireAgent()) return;
  id.value = Number(query.id);
  if (!Number.isFinite(id.value) || id.value <= 0) {
    uni.showToast({ title: '订单参数错误', icon: 'none' });
    return;
  }
  await load();
  loaded.value = true;
});

onShow(() => {
  if (loaded.value && id.value) load();
});

const edit = () => uni.navigateTo({ url: `/pages/order-edit/index?id=${id.value}` });

async function confirmCancel() {
  uni.showModal({
    title: '确认作废',
    content: '订单作废后将无法继续办理，确定要作废吗？',
    success: async (result) => {
      if (!result.confirm) return;
      await request(`/mini/orders/${id.value}`, 'DELETE');
      uni.showToast({ title: '已作废' });
      setTimeout(() => uni.navigateBack(), 500);
    },
  });
}
</script>

<template>
  <view class="page">
    <view
      v-if="row.id"
      class="card"
    >
      <view class="row">
        <b>{{ row.name }}</b>
        <text class="tag">{{ auditStatusLabels[row.auditStatus || ''] || row.auditStatus }}</text>
      </view>
      <view class="desc">{{ row.orderNo }}</view>
      <view class="item">
        业务类型：{{ businessTypeLabels[row.businessType || ''] || row.businessType }}
      </view>
      <view class="item">
        订单来源：{{ sourceChannelLabels[row.sourceChannel || ''] || row.sourceChannel }}
      </view>
      <view class="item">联系电话：{{ row.phone || '-' }}</view>

      <template v-if="row.businessType === 'CAMPUS_CARD'">
        <view class="item">业务号码：{{ row.businessNumber || '-' }}</view>
      </template>

      <template v-if="row.businessType === 'CAMPUS_NETWORK'">
        <view class="item">新办号码：{{ row.businessNumber || '-' }}</view>
        <view class="item">学号：{{ row.studentNo || '-' }}</view>
        <view class="item">身份证后六位：{{ row.idCardLastSix || '-' }}</view>
        <view class="item">
          导出状态：{{ exportStatusLabels[row.exportStatus || ''] || row.exportStatus || '未导出' }}
        </view>
      </template>

      <template v-if="row.businessType === 'DRIVING_SCHOOL'">
        <view class="item">车型：{{ row.licenseType || '-' }}</view>
        <view class="item">
          班型：{{
            row.classType === 'NORMAL' ? '普通班' : row.classType === 'FULL' ? '全包班' : '-'
          }}
        </view>
        <view class="item">
          缴费数额：{{ row.paymentAmount == null ? '-' : `¥${row.paymentAmount}` }}
        </view>
      </template>

      <template v-if="row.businessType === 'RENEWAL'">
        <view class="item">业务号码：{{ row.businessNumber || '-' }}</view>
        <view class="item">
          续费金额：{{ row.renewalAmount == null ? '-' : `¥${row.renewalAmount}` }}
        </view>
      </template>

      <view class="item">创建时间：{{ row.createdAt || '-' }}</view>
      <view class="item no-border">备注：{{ row.remark || '-' }}</view>
    </view>
    <view
      v-else
      class="empty"
    >
      正在加载订单...
    </view>
    <button
      v-if="row.id && row.exportStatus !== 'EXPORTED'"
      class="primary"
      @click="edit"
    >
      编辑订单
    </button>
    <button
      v-if="row.id && row.auditStatus === 'PENDING'"
      class="danger"
      @click="confirmCancel"
    >
      作废订单
    </button>
  </view>
</template>

<style scoped>
.item {
  padding: 18rpx 0;
  border-bottom: 1px solid #edf0f4;
  font-size: 28rpx;
}
.no-border {
  border-bottom: 0;
}
.empty {
  padding: 100rpx 0;
  text-align: center;
  color: #8a95a5;
}
button {
  margin-top: 18rpx;
}
.danger {
  background: #fff0f0;
  color: #e34f4f;
}
</style>
