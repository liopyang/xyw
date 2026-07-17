<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app';
import { ref } from 'vue';
import { request } from '../../utils/request';
import { currentUser } from '../../utils/session';
const user = ref<any>(currentUser());
const go = (url: string) => uni.navigateTo({ url });
onShow(async () => {
  if (uni.getStorageSync('token')) {
    user.value = await request('/mini/auth/me');
    uni.setStorageSync('user', user.value);
  }
});
function logout() {
  uni.clearStorageSync();
  uni.reLaunch({ url: '/pages/login/index' });
}
function cancel() {
  uni.showModal({
    title: '申请注销账号',
    content: '注销后将无法继续访问个人问题和代理工作台，确定继续吗？',
    success: async (r) => {
      if (r.confirm) {
        await request('/mini/auth/cancel-account', 'POST');
        logout();
      }
    },
  });
}
function showAgreement(title: string) {
  uni.showModal({
    title,
    content:
      '本小程序仅提供校园信息整理、问题反馈和代理业务信息提交服务，不接入教务系统，不提供支付或公开内容发布。',
    showCancel: false,
  });
}
</script>
<template>
  <view class="page">
    <view class="card profile">
      <view class="avatar">{{ (user.nickname || user.realName || '用').slice(0, 1) }}</view>
      <view>
        <b>{{ user.nickname || user.realName || '微信用户' }}</b>
        <view class="desc">{{ user.role === 'AGENT' ? '代理' : '普通用户' }} · 账号正常</view>
        <view
          v-if="user.role === 'AGENT'"
          class="desc"
        >
          {{ user.agentNo }} · {{ user.agentLevel }}
        </view>
      </view>
    </view>
    <view
      v-if="user.role === 'AGENT'"
      class="card agent"
    >
      <b>代理工作台</b>
      <view class="actions">
        <button
          size="mini"
          @click="go('/pages/order-form/index?type=CAMPUS_CARD')"
        >
          校园卡
        </button>
        <button
          size="mini"
          @click="go('/pages/order-form/index?type=CAMPUS_NETWORK')"
        >
          校园网
        </button>
        <button
          size="mini"
          @click="go('/pages/order-form/index?type=DRIVING_SCHOOL')"
        >
          驾校
        </button>
        <button
          size="mini"
          @click="go('/pages/order-form/index?type=RENEWAL')"
        >
          续费
        </button>
      </view>
      <view
        class="menu"
        @click="go('/pages/orders/index')"
      >
        <text>我的订单</text>
        <text>›</text>
      </view>
    </view>
    <view
      class="card menu"
      @click="go('/pages/issues/index')"
    >
      <text>我的问题</text>
      <text>›</text>
    </view>
    <view
      class="card menu"
      @click="showAgreement('隐私政策')"
    >
      <text>隐私政策</text>
      <text>›</text>
    </view>
    <view
      class="card menu"
      @click="showAgreement('用户协议')"
    >
      <text>用户协议</text>
      <text>›</text>
    </view>
    <view
      class="card menu danger"
      @click="cancel"
    >
      <text>账号注销</text>
      <text>›</text>
    </view>
    <button
      class="logout"
      @click="logout"
    >
      退出登录
    </button>
  </view>
</template>
<style scoped>
.profile {
  display: flex;
  gap: 24rpx;
  align-items: center;
}
.avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 24rpx;
  background: #e8f0ff;
  color: #2469dc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 700;
}
.menu {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}
.agent {
  margin-top: 20rpx;
}
.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin: 20rpx 0;
}
.actions button {
  width: 100%;
  margin: 0;
}
.danger {
  color: #d64b4b;
}
.logout {
  margin-top: 28rpx;
  color: #e34f4f;
  background: #fff;
}
</style>
