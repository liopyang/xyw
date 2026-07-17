<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app';
import { ref } from 'vue';
import { request } from '../../utils/request';
import { apiUrl } from '../../config/env';
const article = ref<any>({}),
  blocks = ref<any[]>([]);
function copy(value: string, message = '内容已复制') {
  uni.setClipboardData({
    data: value,
    success: () => uni.showToast({ title: message, icon: 'none' }),
  });
}
function call(value: string) {
  uni.makePhoneCall({ phoneNumber: value });
}
function preview(url: string) {
  uni.previewImage({ urls: [url] });
}
function mediaUrl(block: any) {
  return block.url || apiUrl(`/mini/public/media/${block.mediaId}`);
}
onLoad(async (q) => {
  const data = await request<any>(`/mini/public/content/articles/${q?.id}`);
  article.value = data;
  blocks.value =
    typeof data.contentBlocks === 'string'
      ? JSON.parse(data.contentBlocks)
      : data.contentBlocks || [];
  uni.setNavigationBarTitle({ title: data.title });
});
</script>
<template>
  <view class="page">
    <view class="title">{{ article.title }}</view>
    <view class="desc">{{ article.subtitle }}</view>
    <view class="card content">
      <template
        v-for="(b, i) in blocks"
        :key="i"
      >
        <view
          v-if="b.type === 'heading'"
          class="h1"
        >
          {{ b.text }}
        </view>
        <view
          v-else-if="b.type === 'subheading'"
          class="h2"
        >
          {{ b.text }}
        </view>
        <view
          v-else-if="b.type === 'notice'"
          class="notice"
        >
          {{ b.text }}
        </view>
        <view
          v-else-if="b.type === 'paragraph'"
          class="p"
        >
          {{ b.text }}
        </view>
        <image
          v-else-if="b.type === 'image' && b.mediaId"
          :src="mediaUrl(b)"
          mode="widthFix"
          @click="preview(mediaUrl(b))"
        />
        <view
          v-else-if="b.type === 'copy_link'"
          class="action"
        >
          <text>{{ b.label || b.text }}：{{ b.value }}</text>
          <button
            size="mini"
            @click="copy(b.value, '链接已复制，请前往浏览器打开')"
          >
            复制链接
          </button>
        </view>
        <view
          v-else-if="b.type === 'copy_text'"
          class="action"
        >
          <text>{{ b.text }}</text>
          <button
            size="mini"
            @click="copy(b.value || b.text)"
          >
            复制
          </button>
        </view>
        <view
          v-else-if="b.type === 'phone'"
          class="action"
        >
          <text>{{ b.text || b.value }}</text>
          <button
            size="mini"
            @click="copy(b.value)"
          >
            复制
          </button>
          <button
            v-if="b.callEnabled"
            size="mini"
            @click="call(b.value)"
          >
            拨打
          </button>
        </view>
        <view
          v-else-if="b.type === 'divider'"
          class="divider"
        />
      </template>
    </view>
  </view>
</template>
<style scoped>
.content {
  margin-top: 24rpx;
}
.h1 {
  font-size: 38rpx;
  font-weight: 700;
  margin: 28rpx 0 14rpx;
}
.h2 {
  font-size: 31rpx;
  font-weight: 650;
  margin: 24rpx 0 12rpx;
}
.p {
  line-height: 1.8;
  margin: 16rpx 0;
}
.notice {
  padding: 20rpx;
  background: #fff7df;
  border-radius: 12rpx;
  color: #805d18;
}
.content image {
  width: 100%;
  margin: 16rpx 0;
  border-radius: 14rpx;
}
.action {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 0;
}
.action text {
  flex: 1;
  word-break: break-all;
}
.divider {
  height: 1px;
  background: #e8edf4;
  margin: 24rpx 0;
}
</style>
