<script setup lang="ts">
import { ref } from 'vue';
import { request } from '../../utils/request';
import type { SessionUser } from '../../utils/session';
interface LoginResult {
  token: string;
  user: SessionUser;
}
const loading = ref(false);
async function login() {
  loading.value = true;
  try {
    const code = await new Promise<string>((resolve, reject) =>
      uni.login({
        provider: 'weixin',
        success: (r) => (r.code ? resolve(r.code) : reject(r)),
        fail: reject,
      }),
    );
    const data = await request<LoginResult>('/mini/auth/wechat-login', 'POST', {
      code,
    });
    uni.setStorageSync('token', data.token);
    uni.setStorageSync('user', data.user);
    uni.reLaunch({ url: '/pages/home/index' });
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <view class="login">
    <view class="brand">校</view>
    <view class="title">校园业务服务</view>
    <view class="desc">校园信息展示、地点查询与问题反馈</view>
    <button
      class="primary"
      :loading="loading"
      @click="login"
    >
      微信身份登录
    </button>
    <view class="tip">登录即表示同意《用户协议》和《隐私政策》</view>
  </view>
</template>
<style scoped>
.login {
  padding: 180rpx 60rpx;
  text-align: center;
}
.brand {
  width: 92rpx;
  height: 92rpx;
  border-radius: 24rpx;
  background: #2469dc;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42rpx;
  font-weight: 700;
  margin: 0 auto 42rpx;
}
.tip {
  font-size: 22rpx;
  color: #8a95a5;
  margin-top: 24rpx;
}
</style>
