<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { issueApi } from '../../api/admin';
const route = useRoute(),
  router = useRouter(),
  loading = ref(false),
  issue = ref<any>({}),
  process = reactive({ status: 'PENDING', processRemark: '' });
async function load() {
  issue.value = (await issueApi.detail(Number(route.params.id))).data;
  process.status = issue.value.status;
  process.processRemark = issue.value.process_remark || issue.value.processRemark || '';
}
async function save() {
  loading.value = true;
  try {
    await issueApi.status(Number(route.params.id), process);
    ElMessage.success('处理结果已保存');
    await load();
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="detail-grid">
      <section class="panel detail">
        <h1 class="page-title">问题详情</h1>
        <el-descriptions
          :column="2"
          border
        >
          <el-descriptions-item label="问题编号">
            {{ issue.issue_no || issue.issueNo }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ issue.submitted_at || issue.submittedAt }}
          </el-descriptions-item>
          <el-descriptions-item label="提交人">
            {{ issue.submitter_name || issue.submitterName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ issue.contact_phone || issue.contactPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="关联业务号码">
            {{ issue.business_number || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="问题类型">{{ issue.issue_type }}</el-descriptions-item>
          <el-descriptions-item
            label="问题描述"
            :span="2"
          >
            {{ issue.description }}
          </el-descriptions-item>
        </el-descriptions>
        <div
          v-if="issue.images?.length"
          class="images"
        >
          <el-image
            v-for="img in issue.images"
            :key="img.id"
            :src="img.imageUrl"
            :preview-src-list="issue.images.map((x: any) => x.imageUrl)"
          />
        </div>
      </section>
      <section class="panel process">
        <h3>处理问题</h3>
        <el-form label-position="top">
          <el-form-item label="处理状态">
            <el-select v-model="process.status">
              <el-option
                label="待处理"
                value="PENDING"
              />
              <el-option
                label="处理中"
                value="PROCESSING"
              />
              <el-option
                label="已解决"
                value="RESOLVED"
              />
              <el-option
                label="已关闭"
                value="CLOSED"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input
              v-model="process.processRemark"
              type="textarea"
              :rows="7"
            />
          </el-form-item>
          <el-button @click="router.back()">返回</el-button>
          <el-button
            type="primary"
            :loading="loading"
            @click="save"
          >
            保存处理结果
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>
<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
.detail,
.process {
  padding: 26px;
}
.detail h1 {
  margin-bottom: 24px;
}
.process h3 {
  margin-top: 0;
}
.process :deep(.el-select) {
  width: 100%;
}
.images {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.images .el-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
}
</style>
