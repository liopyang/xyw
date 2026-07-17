<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app';
import { computed, ref } from 'vue';
import { request } from '../../utils/request';
const categories = ref<any[]>([]),
  places = ref<any[]>([]),
  selected = ref<number>();
const shown = computed(() =>
  selected.value ? places.value.filter((p) => p.categoryId === selected.value) : places.value,
);
const markers = computed(() =>
  shown.value.map((p) => ({
    id: p.id,
    longitude: Number(p.longitude),
    latitude: Number(p.latitude),
    title: p.placeName,
    width: 28,
    height: 36,
  })),
);
onShow(async () => {
  categories.value = await request('/mini/public/places/categories');
  places.value = await request('/mini/public/places');
});
function marker(e: any) {
  uni.navigateTo({ url: `/pages/map/detail?id=${e.detail.markerId}` });
}
function openPlace(id: number) {
  uni.navigateTo({ url: `/pages/map/detail?id=${id}` });
}
</script>
<template>
  <view class="map-page">
    <scroll-view
      scroll-x
      class="filters"
    >
      <button
        size="mini"
        @click="selected = undefined"
      >
        全部
      </button>
      <button
        v-for="c in categories"
        :key="c.id"
        size="mini"
        @click="selected = c.id"
      >
        {{ c.categoryName }}
      </button>
    </scroll-view>
    <map
      class="map"
      :longitude="Number(shown[0]?.longitude || 112.93)"
      :latitude="Number(shown[0]?.latitude || 28.23)"
      :markers="markers"
      show-location
      @markertap="marker"
    />
    <scroll-view
      scroll-y
      class="list"
    >
      <view
        v-for="p in shown"
        :key="p.id"
        class="place"
        @click="openPlace(p.id)"
      >
        <b>{{ p.placeName }}</b>
        <text>{{ p.address }}</text>
      </view>
    </scroll-view>
  </view>
</template>
<style scoped>
.map-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.filters {
  white-space: nowrap;
  padding: 16rpx;
  background: #fff;
}
.filters button {
  display: inline-block;
  width: auto;
  margin-right: 12rpx;
}
.map {
  width: 100%;
  height: 55vh;
}
.list {
  flex: 1;
}
.place {
  padding: 22rpx 28rpx;
  background: #fff;
  border-bottom: 1px solid #edf0f5;
}
.place b,
.place text {
  display: block;
}
.place text {
  color: #8a95a5;
  font-size: 24rpx;
  margin-top: 8rpx;
}
</style>
