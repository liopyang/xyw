<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { userApi } from '../../api/admin'
import { maskPhone } from '../../utils/privacy'

const rows = ref<any[]>([])
const total = ref(0)
const dialog = ref(false)
const editing = ref<any>()
const q = reactive({ page: 1, pageSize: 20, keyword: '' })
const form = reactive({ username: '', realName: '', phone: '', password: '' })

async function load() {
  const result = await userApi.list(q)
  rows.value = result.data.records
  total.value = result.data.total
}

function open(row?: any) {
  editing.value = row
  Object.assign(form, row
    ? { username: row.username, realName: row.realName, phone: row.phone, password: '' }
    : { username: '', realName: '', phone: '', password: '' })
  dialog.value = true
}

async function save() {
  if (editing.value) await userApi.update(editing.value.id, form)
  else await userApi.create(form)
  ElMessage.success('管理员已保存')
  dialog.value = false
  load()
}

async function status(row: any) {
  try {
    await ElMessageBox.confirm(`确定${row.status === 1 ? '停用' : '启用'}该管理员吗？`, '账号状态')
  } catch {
    return
  }
  await userApi.status(row.id, row.status === 1 ? 0 : 1)
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="heading">
      <div><h1 class="page-title">管理员账号</h1><p class="page-desc">管理门店工作人员登录权限</p></div>
      <el-button type="primary" :icon="Plus" @click="open()">新增管理员</el-button>
    </div>
    <div class="filter-bar panel">
      <el-input v-model="q.keyword" placeholder="账号、姓名或手机号" style="width: 260px" @keyup.enter="load" />
      <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
    </div>
    <div class="panel table-panel">
      <div class="table-tools"><b>账号列表</b></div>
      <el-table :data="rows" stripe>
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column label="手机号"><template #default="{ row }">{{ maskPhone(row.phone) }}</template></el-table-column>
        <el-table-column label="角色"><template #default="{ row }">{{ row.role === 'OWNER' ? '老板' : '管理员' }}</template></el-table-column>
        <el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作"><template #default="{ row }"><template v-if="row.role !== 'OWNER'"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="status(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button></template></template></el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialog" :title="editing ? '编辑管理员' : '新增管理员'" width="480px">
      <el-form label-position="top">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item :label="editing ? '新密码（不修改请留空）' : '初始密码'"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.heading { display: flex; justify-content: space-between; }</style>
