<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus';
import { miniContentApi } from '../../api/miniContent';
const loading = ref(false),
  rows = ref<any[]>([]);
async function load() {
  loading.value = true;
  try {
    rows.value = (await miniContentApi.media()).data;
  } finally {
    loading.value = false;
  }
}
async function upload(options: UploadRequestOptions) {
  try {
    await miniContentApi.uploadMedia(options.file);
    options.onSuccess({});
    ElMessage.success('图片已上传');
    load();
  } catch {
    ElMessage.error('图片上传失败');
  }
}
async function remove(id: number) {
  await ElMessageBox.confirm('仅未被使用的素材可以删除，确定继续吗？', '删除素材', {
    type: 'warning',
  });
  await miniContentApi.deleteMedia(id);
  ElMessage.success('素材已删除');
  load();
}
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="heading">
      <div>
        <h1 class="page-title">图片素材</h1>
        <p class="page-desc">图片保存到私有对象存储，支持 JPEG、PNG、WebP，单张不超过 5MB</p>
      </div>
      <el-upload
        :http-request="upload"
        :show-file-list="false"
        accept="image/jpeg,image/png,image/webp"
      >
        <el-button type="primary">上传图片</el-button>
      </el-upload>
    </div>
    <div
      class="panel table-panel"
      v-loading="loading"
    >
      <el-table :data="rows">
        <el-table-column
          prop="id"
          label="素材 ID"
          width="90"
        />
        <el-table-column
          prop="originalFilename"
          label="原始文件名"
        />
        <el-table-column
          prop="contentType"
          label="类型"
        />
        <el-table-column
          prop="fileSize"
          label="大小"
        >
          <template #default="{ row }">{{ (row.fileSize / 1024).toFixed(1) }} KB</template>
        </el-table-column>
        <el-table-column
          prop="usageStatus"
          label="使用状态"
        />
        <el-table-column
          prop="createdAt"
          label="上传时间"
        />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              @click="remove(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<style scoped>
.heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
</style>
