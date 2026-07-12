<script setup lang="ts">
import { reactive, ref } from 'vue'
import { request } from '../../utils/request'
import type { SessionUser } from '../../utils/session'

interface LoginResult {
  token: string
  user: SessionUser
}

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function login() {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请输入账号和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await request<LoginResult>('/auth/login', 'POST', form)
    if (!['AGENT', 'USER'].includes(data.user.role)) {
      uni.showToast({ title: '管理账号请使用 Web 管理端登录', icon: 'none' })
      return
    }
    uni.setStorageSync('token', data.token)
    uni.setStorageSync('user', data.user)
    uni.reLaunch({ url: '/pages/home/index' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <view class="brand">校</view>
    <view class="title">校园业务服务</view>
    <view class="desc">代理订单与问题服务平台</view>
    <input v-model="form.username" class="input" placeholder="手机号或账号" />
    <input v-model="form.password" class="input" password placeholder="密码" @confirm="login" />
    <button class="primary" :loading="loading" @click="login">登录</button>
  </view>
</template>

<style scoped>
.login { padding: 180rpx 60rpx; }
.brand { width: 92rpx; height: 92rpx; border-radius: 24rpx; background: #2469dc; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 42rpx; font-weight: 700; margin-bottom: 42rpx; }
</style>
