<script setup lang="ts">
import { reactive, ref } from 'vue'
import { request } from '../../utils/request'

const saving = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function save() {
  if (!form.oldPassword) {
    uni.showToast({ title: '请输入当前密码', icon: 'none' })
    return
  }
  if (form.newPassword.length < 8) {
    uni.showToast({ title: '新密码至少 8 位', icon: 'none' })
    return
  }
  if (form.newPassword === form.oldPassword) {
    uni.showToast({ title: '新密码不能与当前密码相同', icon: 'none' })
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await request('/auth/change-password', 'POST', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    uni.clearStorageSync()
    uni.showToast({ title: '密码已修改，请重新登录', icon: 'none', duration: 1800 })
    setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 900)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="label">当前密码</text>
      <input v-model="form.oldPassword" class="input" password placeholder="请输入当前密码" />
      <text class="label">新密码</text>
      <input v-model="form.newPassword" class="input" password placeholder="至少 8 位" />
      <text class="label">确认新密码</text>
      <input v-model="form.confirmPassword" class="input" password placeholder="请再次输入新密码" @confirm="save" />
      <view class="tip">修改成功后需要使用新密码重新登录。</view>
      <button class="primary" :loading="saving" :disabled="saving" @click="save">确认修改</button>
    </view>
  </view>
</template>

<style scoped>
.tip { margin: 8rpx 0 28rpx; color: #8a95a5; font-size: 24rpx; }
</style>
