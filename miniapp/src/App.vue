<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { currentUser } from './utils/session'

onLaunch(() => {
  const token = uni.getStorageSync('token')
  const user = currentUser()
  if (!token || !['AGENT', 'USER'].includes(user.role)) {
    uni.removeStorageSync('token')
    uni.removeStorageSync('user')
    uni.reLaunch({ url: '/pages/login/index' })
  }
})
</script>

<style>
page { background: #f4f7fb; color: #182230; font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; }
.page { padding: 28rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 22rpx; }
.title { font-size: 42rpx; font-weight: 700; margin-bottom: 10rpx; }
.desc { font-size: 26rpx; color: #8a95a5; margin-bottom: 30rpx; }
.input { box-sizing: border-box; width: 100%; height: 88rpx; background: #f6f8fb; border-radius: 14rpx; padding: 0 24rpx; margin-bottom: 20rpx; }
.textarea { box-sizing: border-box; width: 100%; height: 220rpx; background: #f6f8fb; border-radius: 14rpx; padding: 22rpx; margin-bottom: 20rpx; }
.primary { background: #2469dc; color: #fff; border-radius: 14rpx; }
.label { display: block; font-size: 26rpx; margin: 22rpx 0 12rpx; }
.tag { display: inline-block; padding: 8rpx 16rpx; border-radius: 10rpx; background: #fff5df; color: #d58a18; font-size: 22rpx; }
.row { display: flex; justify-content: space-between; align-items: center; }
</style>
