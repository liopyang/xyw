<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { configApi } from '../../api/admin';
const route = useRoute(),
  items = ref<any[]>([]),
  saving = ref(''),
  group = computed(() => route.meta.configGroup);
const keys = computed(() =>
  group.value === 'driving'
    ? ['drivingC1NormalPrice', 'drivingC1FullPrice', 'drivingC2NormalPrice', 'drivingC2FullPrice']
    : ['duplicateWindowDays'],
);
async function load() {
  items.value = (await configApi.list()).data.filter((x) => keys.value.includes(x.configKey));
}
async function save(item: any) {
  saving.value = item.configKey;
  try {
    await configApi.update(item.configKey, String(item.configValue));
    ElMessage.success('配置已保存');
  } finally {
    saving.value = '';
  }
}
onMounted(load);
</script>
<template>
  <div class="page">
    <h1 class="page-title">{{ route.meta.title }}</h1>
    <p class="page-desc">修改后将应用于后续业务操作</p>
    <div class="panel configs">
      <div
        v-for="item in items"
        :key="item.configKey"
        class="config-row"
      >
        <div>
          <b>{{ item.description }}</b>
          <p>{{ item.configKey }}</p>
        </div>
        <el-input-number
          v-model="item.configValue"
          :min="0"
        />
        <el-button
          type="primary"
          :loading="saving === item.configKey"
          @click="save(item)"
        >
          保存
        </el-button>
      </div>
    </div>
  </div>
</template>
<style scoped>
.configs {
  width: 760px;
  padding: 10px 24px;
}
.config-row {
  display: grid;
  grid-template-columns: 1fr 180px 80px;
  align-items: center;
  gap: 15px;
  padding: 20px 0;
  border-bottom: 1px solid #edf0f4;
}
.config-row:last-child {
  border: 0;
}
.config-row p {
  margin: 5px 0 0;
  color: #97a1af;
  font-size: 12px;
}
</style>
