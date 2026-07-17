<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { request } from '../../utils/request';
import { apiUrl } from '../../config/env';
interface Article {
  id: number;
  title: string;
  subtitle?: string;
  summary?: string;
  categoryCode: string;
  coverMediaId?: number;
}
const site = ref({
  siteName: '校园业务服务',
  topDescription: '本页面提供信息整理和问题反馈服务。',
});
const notice = ref({ notice: '', noticeEnabled: false });
const articles = ref<Article[]>([]);
const banners = ref<any[]>([]);
const cards = ref<any[]>([]);

function articleForCard(card: any): Article | null {
  const article = articles.value.find((item) => item.id === Number(card.articleId));

  if (!article) {
    return null;
  }

  return {
    ...article,
    title: card.title || article.title,
    summary: card.summary || article.summary,
  };
}

function configuredArticles(): Article[] {
  return cards.value
    .filter((card) => card.enabled !== false)
    .sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0))
    .map(articleForCard)
    .filter((article): article is Article => article !== null);
}

const displayedArticles = computed(() => {
  return cards.value.length ? configuredArticles() : articles.value;
});

function parse(value: unknown) {
  if (typeof value === 'string') {
    try {
      return JSON.parse(value);
    } catch {
      return {};
    }
  }

  return value || {};
}

function mediaUrl(item: any) {
  return item.url || apiUrl(`/mini/public/media/${item.mediaId}`);
}

function applyConfig(configKey: string, value: any) {
  if (configKey === 'site') {
    Object.assign(site.value, value);
    return;
  }

  if (configKey === 'notice') {
    Object.assign(notice.value, value);
    return;
  }

  if (configKey === 'banners') {
    banners.value = (value || [])
      .filter((banner: any) => banner.enabled !== false)
      .sort((left: any, right: any) => (left.sortOrder || 0) - (right.sortOrder || 0));
    return;
  }

  if (configKey === 'cards') {
    cards.value = value || [];
  }
}

async function load() {
  const data = await request<any>('/mini/public/home');
  articles.value = data.articles || [];

  for (const item of data.configs || []) {
    const value = parse(item.configValue);
    applyConfig(item.configKey, value);
  }
}

function preview(url: string) {
  if (url) {
    uni.previewImage({ urls: banners.value.map(mediaUrl), current: url });
  }
}

function openArticle(id: number) {
  uni.navigateTo({ url: `/pages/content/detail?id=${id}` });
}
onShow(load);
</script>
<template>
  <view class="page">
    <view class="hero">
      <view class="title">{{ site.siteName }}</view>
      <view class="desc">{{ site.topDescription }}</view>
    </view>
    <view
      v-if="notice.noticeEnabled && notice.notice"
      class="notice"
    >
      {{ notice.notice }}
    </view>
    <swiper
      v-if="banners.length"
      class="banner"
      indicator-dots
      autoplay
    >
      <swiper-item
        v-for="item in banners"
        :key="item.mediaId"
      >
        <image
          :src="mediaUrl(item)"
          mode="aspectFill"
          @click="preview(mediaUrl(item))"
        />
      </swiper-item>
    </swiper>
    <view class="section-title">校园信息</view>
    <view class="grid">
      <view
        v-for="item in displayedArticles"
        :key="item.id"
        class="card info"
        @click="openArticle(item.id)"
      >
        <view class="icon">{{ item.title.slice(0, 1) }}</view>
        <b>{{ item.title }}</b>
        <text>{{ item.summary || item.subtitle || '查看服务说明' }}</text>
      </view>
    </view>
    <view class="disclaimer">具体套餐、办理结果和服务规则，以对应运营单位正式说明为准。</view>
  </view>
</template>
<style scoped>
.hero {
  padding: 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2469dc, #5b91ed);
  color: #fff;
}
.notice {
  margin-top: 20rpx;
  padding: 20rpx;
  border-radius: 14rpx;
  background: #fff7df;
  color: #8b6518;
}
.banner,
.banner image {
  width: 100%;
  height: 300rpx;
  border-radius: 20rpx;
  margin-top: 20rpx;
}
.section-title {
  font-weight: 700;
  margin: 32rpx 0 18rpx;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}
.info {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.info text {
  font-size: 24rpx;
  color: #8a95a5;
}
.icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 16rpx;
  background: #e8f0ff;
  color: #2469dc;
  display: flex;
  align-items: center;
  justify-content: center;
}
.disclaimer {
  margin: 32rpx 8rpx;
  color: #8a95a5;
  font-size: 22rpx;
  line-height: 1.7;
}
</style>
