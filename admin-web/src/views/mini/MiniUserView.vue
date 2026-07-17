<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { miniContentApi } from '../../api/miniContent';
const rows = ref<any[]>([]);
const keyword = ref('');
const dialog = ref(false);
const selected = ref<any>();
const form = reactive({
  roleCode: 'AGENT',
  name: '',
  phone: '',
  level: 'NORMAL',
});
async function load() {
  rows.value = (await miniContentApi.users(keyword.value)).data;
}
function open(row: any) {
  selected.value = row;
  Object.assign(form, {
    roleCode: 'AGENT',
    name: row.nickname || '',
    phone: row.phone || '',
    level: row.agentLevel || 'NORMAL',
  });
  dialog.value = true;
}
async function save() {
  await miniContentApi.setUserRole(selected.value.id, form);
  dialog.value = false;
  ElMessage.success('用户角色已更新，用户刷新登录状态后生效');
  load();
}
async function revoke(row: any) {
  await miniContentApi.setUserRole(row.id, { roleCode: 'USER' });
  ElMessage.success('已取消代理身份');
  load();
}
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="heading">
      <div>
        <h1 class="page-title">小程序用户</h1>
        <p class="page-desc">普通用户不能自行成为代理，由老板在此授权</p>
      </div>
      <div>
        <el-input
          v-model="keyword"
          placeholder="昵称、手机号或代理编号"
          @keyup.enter="load"
        />
        <el-button @click="load">搜索</el-button>
      </div>
    </div>
    <div class="panel table-panel">
      <el-table :data="rows">
        <el-table-column
          prop="nickname"
          label="昵称"
        />
        <el-table-column
          prop="phone"
          label="手机号"
        />
        <el-table-column
          prop="roleCode"
          label="角色"
        />
        <el-table-column
          prop="agentNo"
          label="代理编号"
        />
        <el-table-column
          prop="agentLevel"
          label="代理等级"
        />
        <el-table-column
          prop="lastLoginAt"
          label="最近登录"
        />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              v-if="row.roleCode !== 'AGENT'"
              link
              type="primary"
              @click="open(row)"
            >
              设为代理
            </el-button>
            <el-button
              v-else
              link
              type="warning"
              @click="revoke(row)"
            >
              取消代理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      v-model="dialog"
      title="授予代理身份"
      width="520px"
    >
      <el-form label-width="90px">
        <el-form-item label="代理姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="代理等级">
          <el-select v-model="form.level">
            <el-option
              label="普通代理"
              value="NORMAL"
            />
            <el-option
              label="高级代理"
              value="ADVANCED"
            />
            <el-option
              label="校园负责人"
              value="CAMPUS_LEADER"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="save"
        >
          确认授权
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.heading > div:last-child {
  display: flex;
  gap: 8px;
}
</style>
