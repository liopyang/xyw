<script setup lang="ts">
import { reactive, ref } from 'vue'
import { issueTypeLabels } from '../../utils/labels'
import { request, uploadFile } from '../../utils/request'

type IssueType = 'CAMPUS_CARD' | 'CAMPUS_NETWORK' | 'DRIVING_SCHOOL' | 'RENEWAL' | 'ACCOUNT' | 'OTHER'

const issueTypes: IssueType[] = ['CAMPUS_CARD', 'CAMPUS_NETWORK', 'DRIVING_SCHOOL', 'RENEWAL', 'ACCOUNT', 'OTHER']
const issueTypeNames = issueTypes.map((type) => issueTypeLabels[type])
const saving = ref(false)
const images = ref<string[]>([])
const form = reactive({
  issueType: 'CAMPUS_CARD' as IssueType,
  description: '',
  contactPhone: '',
  businessNumber: '',
})

function choose() {
  uni.chooseImage({
    count: 3 - images.value.length,
    success: (result) => images.value.push(...result.tempFilePaths),
  })
}

function removeImage(index: number) {
  images.value.splice(index, 1)
}

async function save() {
  if (!form.description.trim() || !/^1\d{10}$/.test(form.contactPhone)) {
    uni.showToast({ title: '请完整填写问题和手机号', icon: 'none' })
    return
  }
  saving.value = true
  let created = false
  try {
    const result = await request<{ id: number }>('/mini/issues', 'POST', form)
    created = true
    try {
      await Promise.all(images.value.map((path) => uploadFile(`/mini/issues/${result.id}/images`, path)))
      uni.showToast({ title: '提交成功' })
    } catch {
      uni.showToast({ title: '问题已提交，部分图片上传失败', icon: 'none', duration: 2500 })
    }
    setTimeout(() => uni.navigateBack(), 800)
  } finally {
    if (!created) saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="label">问题类型</text>
      <picker :range="issueTypeNames" @change="form.issueType = issueTypes[Number($event.detail.value)]">
        <view class="input picker-value">{{ issueTypeLabels[form.issueType] }}</view>
      </picker>
      <text class="label">联系电话</text>
      <input v-model="form.contactPhone" class="input" type="number" maxlength="11" />
      <text class="label">关联业务号码</text>
      <input v-model="form.businessNumber" class="input" />
      <text class="label">问题描述</text>
      <textarea v-model="form.description" class="textarea" maxlength="1000" />
      <text class="label">问题图片（最多 3 张）</text>
      <view class="image-list">
        <view v-for="(path, index) in images" :key="path" class="image-wrap">
          <image :src="path" mode="aspectFill" />
          <view class="remove" @click="removeImage(index)">×</view>
        </view>
        <view v-if="images.length < 3" class="add-image" @click="choose">＋</view>
      </view>
      <button class="primary" :loading="saving" :disabled="saving" @click="save">提交问题</button>
    </view>
  </view>
</template>

<style scoped>
.picker-value { display: flex; align-items: center; }
.image-list { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 28rpx; }
.image-wrap { position: relative; }
.image-wrap, .image-wrap image, .add-image { width: 150rpx; height: 150rpx; border-radius: 12rpx; }
.add-image { background: #f1f4f8; color: #97a1af; display: flex; align-items: center; justify-content: center; font-size: 54rpx; }
.remove { position: absolute; top: -12rpx; right: -12rpx; width: 38rpx; height: 38rpx; line-height: 34rpx; text-align: center; border-radius: 50%; background: #e34f4f; color: white; font-size: 32rpx; }
</style>
