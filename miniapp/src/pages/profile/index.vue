<script setup lang="ts">
import { currentUser } from '../../utils/session'

const user = currentUser()
const roleLabel = user.role === 'AGENT' ? '代理账号' : '普通用户'

function changePassword() {
  uni.navigateTo({ url: '/pages/change-password/index' })
}

function logout() {
  uni.clearStorageSync()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<template>
  <view class="page">
    <view class="card profile">
      <view class="avatar">{{ user.realName?.slice(0, 1) }}</view>
      <view>
        <b>{{ user.realName }}</b>
        <view class="desc">{{ user.username }} · {{ roleLabel }}</view>
      </view>
    </view>
    <view class="card menu" @click="changePassword">
      <text>修改密码</text><text class="arrow">›</text>
    </view>
    <button class="logout" @click="logout">退出登录</button>
  </view>
</template>

<style scoped>
.profile { display: flex; gap: 24rpx; align-items: center; }
.avatar { width: 92rpx; height: 92rpx; border-radius: 24rpx; background: #e8f0ff; color: #2469dc; display: flex; align-items: center; justify-content: center; font-size: 38rpx; font-weight: 700; }
.menu { display: flex; align-items: center; justify-content: space-between; }
.arrow { color: #a6afbc; font-size: 42rpx; }
.logout { margin-top: 28rpx; color: #e34f4f; background: #fff; }
</style>
