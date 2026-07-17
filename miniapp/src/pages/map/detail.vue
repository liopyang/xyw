<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app';
import { ref } from 'vue';
import { request } from '../../utils/request';
const place = ref<any>({});
const copy = (value: string) => uni.setClipboardData({ data: value });
function open() {
  uni.openLocation({
    longitude: Number(place.value.longitude),
    latitude: Number(place.value.latitude),
    name: place.value.placeName,
    address: place.value.address,
  });
}
function call() {
  uni.makePhoneCall({ phoneNumber: place.value.contactPhone });
}
onLoad(async (q) => {
  place.value = await request(`/mini/public/places/${q?.id}`);
  uni.setNavigationBarTitle({ title: place.value.placeName });
});
</script>
<template>
  <view class="page">
    <view class="title">{{ place.placeName }}</view>
    <view class="desc">{{ place.categoryName }}</view>
    <view class="card">
      <view class="row">
        <b>地址</b>
        <text>{{ place.address }}</text>
        <button
          size="mini"
          @click="copy(place.address)"
        >
          复制
        </button>
      </view>
      <view
        v-if="place.businessHours"
        class="row"
      >
        <b>营业时间</b>
        <text>{{ place.businessHours }}</text>
      </view>
      <view
        v-if="place.contactPhone"
        class="row"
      >
        <b>联系电话</b>
        <text>{{ place.contactPhone }}</text>
        <button
          size="mini"
          @click="copy(place.contactPhone)"
        >
          复制
        </button>
        <button
          size="mini"
          @click="call"
        >
          拨打
        </button>
      </view>
      <view class="desc">{{ place.summary }}</view>
      <button
        class="primary"
        @click="open"
      >
        使用微信地图导航
      </button>
    </view>
  </view>
</template>
<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx 0;
  border-bottom: 1px solid #edf0f5;
}
.row text {
  flex: 1;
}
.primary {
  margin-top: 28rpx;
}
</style>
