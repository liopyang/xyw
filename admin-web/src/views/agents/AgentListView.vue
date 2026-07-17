<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import { agentApi } from '../../api/admin';
import { useAuthStore } from '../../stores/auth';
import { maskPhone } from '../../utils/privacy';
const router = useRouter(),
  auth = useAuthStore(),
  loading = ref(false),
  rows = ref<any[]>([]),
  total = ref(0),
  q = reactive({
    page: 1,
    pageSize: 20,
    keyword: '',
    status: undefined as number | undefined,
  });
const levelName = (level: string) =>
  (
    ({
      NORMAL: '普通代理',
      ADVANCED: '高级代理',
      CAMPUS_LEADER: '校园负责人',
    }) as Record<string, string>
  )[level] || level;
async function load() {
  loading.value = true;
  try {
    const r = await agentApi.list(q);
    rows.value = r.data.records;
    total.value = r.data.total;
  } finally {
    loading.value = false;
  }
}
async function status(row: any) {
  await ElMessageBox.confirm(
    `确定${row.status === 1 ? '停用' : '启用'}代理 ${row.name} 吗？`,
    '账号状态',
  );
  await agentApi.status(row.id, row.status === 1 ? 0 : 1);
  ElMessage.success('状态已更新');
  load();
}
watch(
  () => q.status,
  () => {
    q.page = 1;
    load();
  },
);
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="heading">
      <div>
        <h1 class="page-title">代理管理</h1>
        <p class="page-desc">管理代理账号及订单表现</p>
      </div>
      <el-button
        v-if="auth.isOwner"
        type="primary"
        :icon="Plus"
        @click="router.push('/agents/create')"
      >
        新增代理
      </el-button>
    </div>
    <div class="filter-bar panel">
      <el-input
        v-model="q.keyword"
        placeholder="代理编号、姓名或手机号"
        clearable
        style="width: 260px"
        @keyup.enter="load"
      />
      <el-select
        v-model="q.status"
        clearable
        placeholder="账号状态"
        style="width: 130px"
      >
        <el-option
          label="启用"
          :value="1"
        />
        <el-option
          label="停用"
          :value="0"
        />
      </el-select>
      <el-button
        type="primary"
        :icon="Search"
        @click="load"
      >
        搜索
      </el-button>
    </div>
    <div class="panel table-panel">
      <div class="table-tools"><b>代理列表</b></div>
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
      >
        <el-table-column
          prop="agentNo"
          label="代理编号"
          width="150"
        />
        <el-table-column
          prop="name"
          label="姓名"
        />
        <el-table-column
          label="手机号"
          width="130"
        >
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column
          label="代理等级"
          width="120"
        >
          <template #default="{ row }">{{ levelName(row.level) }}</template>
        </el-table-column>
        <el-table-column
          prop="todayOrders"
          label="今日订单"
        />
        <el-table-column
          prop="monthOrders"
          label="本月订单"
        />
        <el-table-column label="账号状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
          width="170"
        />
        <el-table-column
          label="操作"
          width="150"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="router.push(`/agents/${row.id}`)"
            >
              查看
            </el-button>
            <el-button
              v-if="auth.isOwner"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="status(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="q.page"
          v-model:page-size="q.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="load"
        />
      </div>
    </div>
  </div>
</template>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
}
</style>
