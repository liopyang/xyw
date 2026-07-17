<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { fileUrl } from '../../config/env';
import { issueStatusLabels, issueTypeLabels } from '../../utils/labels';
import { request } from '../../utils/request';

interface IssueImage {
  id?: number;
  imageUrl?: string;
}

interface IssueDetail {
  id?: number;
  issueNo?: string;
  issueType?: string;
  description?: string;
  status?: string;
  contactPhone?: string;
  businessNumber?: string;
  processRemark?: string;
  processorName?: string;
  submittedAt?: string;
  processedAt?: string;
  images?: Array<IssueImage | string>;
}

const issue = ref<IssueDetail>({});
const imageUrls = computed(() =>
  (issue.value.images || [])
    .map((image) => fileUrl(typeof image === 'string' ? image : image.imageUrl))
    .filter(Boolean),
);

onLoad(async (query: Record<string, string> = {}) => {
  const id = Number(query.id);
  if (!Number.isFinite(id) || id <= 0) {
    uni.showToast({ title: '问题参数错误', icon: 'none' });
    return;
  }
  issue.value = await request<IssueDetail>(`/mini/issues/${id}`);
});

function preview(current: string) {
  uni.previewImage({ current, urls: imageUrls.value });
}
</script>

<template>
  <view class="page">
    <view
      v-if="issue.id"
      class="card"
    >
      <view class="row">
        <b>{{ issueTypeLabels[issue.issueType || ''] || issue.issueType }}</b>
        <text class="tag">{{ issueStatusLabels[issue.status || ''] || issue.status }}</text>
      </view>
      <view class="desc">{{ issue.issueNo }}</view>
      <view class="item">联系电话：{{ issue.contactPhone || '-' }}</view>
      <view class="item">关联业务号码：{{ issue.businessNumber || '-' }}</view>
      <view class="item">提交时间：{{ issue.submittedAt || '-' }}</view>
      <view class="section-title">问题描述</view>
      <view class="content">{{ issue.description || '-' }}</view>
      <template v-if="imageUrls.length">
        <view class="section-title">问题图片</view>
        <view class="image-list">
          <image
            v-for="url in imageUrls"
            :key="url"
            :src="url"
            mode="aspectFill"
            @click="preview(url)"
          />
        </view>
      </template>
    </view>

    <view
      v-if="issue.id"
      class="card reply-card"
    >
      <view class="section-title reply-title">工作人员回复</view>
      <view
        v-if="issue.processRemark"
        class="content"
      >
        {{ issue.processRemark }}
      </view>
      <view
        v-else
        class="no-reply"
      >
        问题正在处理中，暂时还没有回复
      </view>
      <view
        v-if="issue.processorName || issue.processedAt"
        class="reply-meta"
      >
        {{ issue.processorName || '工作人员' }}
        <text v-if="issue.processedAt">· {{ issue.processedAt }}</text>
      </view>
    </view>
    <view
      v-if="!issue.id"
      class="empty"
    >
      正在加载问题详情...
    </view>
  </view>
</template>

<style scoped>
.item {
  padding: 14rpx 0;
  color: #586577;
  font-size: 26rpx;
}
.section-title {
  margin: 30rpx 0 14rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.content {
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-all;
}
.image-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
}
.image-list image {
  width: 100%;
  height: 190rpx;
  border-radius: 12rpx;
  background: #f1f4f8;
}
.reply-title {
  margin-top: 0;
}
.reply-card {
  border-left: 6rpx solid #2469dc;
}
.no-reply {
  color: #8a95a5;
}
.reply-meta {
  margin-top: 22rpx;
  color: #8a95a5;
  font-size: 23rpx;
}
.empty {
  padding: 100rpx 0;
  text-align: center;
  color: #8a95a5;
}
</style>
