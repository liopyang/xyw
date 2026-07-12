<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { orderApi, type OrderPayload } from '../../api/orders'
import { agentApi, configApi } from '../../api/admin'

interface AgentOption { id: number; name: string }
interface ConfigItem { configKey: string; configValue: string | number }

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const hydrating = ref(false)
const agents = ref<AgentOption[]>([])
const drivingPrices = reactive<Record<string, number>>({})
const id = computed(() => Number(route.params.id) || 0)
const editing = computed(() => route.query.edit === '1' || !id.value)

const form = reactive<OrderPayload>({
  businessType: 'CAMPUS_CARD',
  name: '',
  phone: '',
  businessNumber: '',
  sourceChannel: 'STORE',
  auditStatus: 'CONFIRMED',
  remark: '',
  licenseType: 'C1',
  classType: 'NORMAL',
})

const needsBusinessNumber = computed(() => ['CAMPUS_CARD', 'CAMPUS_NETWORK', 'RENEWAL'].includes(form.businessType))
const businessNumberLabel = computed(() => form.businessType === 'CAMPUS_NETWORK' ? '新办号码' : '业务号码')
const rules = computed<FormRules>(() => ({
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式错误', trigger: 'blur' },
  ],
  sourceChannel: [{ required: true, message: '请选择来源', trigger: 'change' }],
  businessNumber: needsBusinessNumber.value ? [{ required: true, message: `请输入${businessNumberLabel.value}`, trigger: 'blur' }] : [],
  studentNo: form.businessType === 'CAMPUS_NETWORK' ? [{ required: true, message: '请输入学号', trigger: 'blur' }] : [],
  idCardLastSix: form.businessType === 'CAMPUS_NETWORK' ? [
    { required: true, message: '请输入身份证后六位', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '身份证后六位必须是 6 位数字', trigger: 'blur' },
  ] : [],
  licenseType: form.businessType === 'DRIVING_SCHOOL' ? [{ required: true, message: '请选择车型', trigger: 'change' }] : [],
  classType: form.businessType === 'DRIVING_SCHOOL' ? [{ required: true, message: '请选择班型', trigger: 'change' }] : [],
  paymentAmount: form.businessType === 'DRIVING_SCHOOL' ? [{ required: true, message: '请输入缴费数额', trigger: 'change' }] : [],
  renewalAmount: form.businessType === 'RENEWAL' ? [{ required: true, message: '请输入续费金额', trigger: 'change' }] : [],
}))

const priceConfigKeys: Record<string, string> = {
  C1_NORMAL: 'drivingC1NormalPrice',
  C1_FULL: 'drivingC1FullPrice',
  C2_NORMAL: 'drivingC2NormalPrice',
  C2_FULL: 'drivingC2FullPrice',
}

function applyDrivingPrice() {
  const combination = `${form.licenseType}_${form.classType}`
  const price = drivingPrices[priceConfigKeys[combination] || '']
  if (Number.isFinite(price)) form.paymentAmount = price
}

watch(
  () => [form.businessType, form.licenseType, form.classType],
  () => {
    if (!hydrating.value && form.businessType === 'DRIVING_SCHOOL') applyDrivingPrice()
    nextTick(() => formRef.value?.clearValidate())
  },
)

async function load() {
  loading.value = true
  try {
    const [agentResult, configResult] = await Promise.all([
      agentApi.list({ page: 1, pageSize: 100, status: 1 }),
      configApi.list(),
    ])
    agents.value = agentResult.data.records.map((item: AgentOption) => ({ id: item.id, name: item.name }))
    for (const item of configResult.data as ConfigItem[]) {
      if (!Object.values(priceConfigKeys).includes(item.configKey)) continue
      const value = Number(item.configValue)
      if (Number.isFinite(value)) drivingPrices[item.configKey] = value
    }

    if (id.value) {
      hydrating.value = true
      const detail = await orderApi.detail(id.value)
      Object.assign(form, detail.data)
      await nextTick()
      hydrating.value = false
    } else if (form.businessType === 'DRIVING_SCHOOL') {
      applyDrivingPrice()
    }
  } finally {
    hydrating.value = false
    loading.value = false
  }
}

async function save() {
  if (!editing.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: OrderPayload = { ...form }
    if (id.value) delete payload.auditStatus
    if (id.value) await orderApi.update(id.value, payload)
    else await orderApi.create(payload)
    ElMessage.success(id.value ? '订单已保存' : '订单已创建')
    router.push('/orders')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="back" @click="router.back()"><el-icon><ArrowLeft /></el-icon>返回订单列表</div>
    <div class="form-panel panel">
      <div class="form-head">
        <div>
          <h1>{{ id ? (editing ? '编辑订单' : '订单详情') : '新增订单' }}</h1>
          <p>{{ id ? '查看和维护订单业务信息' : '录入新的校园业务订单' }}</p>
        </div>
        <el-button v-if="id && !editing" type="primary" @click="router.replace({ query: { edit: '1' } })">编辑订单</el-button>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="!editing">
        <div class="form-grid">
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="form.businessType" :disabled="!!id">
              <el-option label="校园卡" value="CAMPUS_CARD" />
              <el-option label="校园网" value="CAMPUS_NETWORK" />
              <el-option label="驾校" value="DRIVING_SCHOOL" />
              <el-option label="续费" value="RENEWAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
          <el-form-item v-if="needsBusinessNumber" :label="businessNumberLabel" prop="businessNumber"><el-input v-model="form.businessNumber" /></el-form-item>
          <el-form-item label="来源渠道" prop="sourceChannel">
            <el-select v-model="form.sourceChannel"><el-option label="线上" value="ONLINE" /><el-option label="代理" value="AGENT" /><el-option label="门店" value="STORE" /></el-select>
          </el-form-item>
          <el-form-item label="归属代理"><el-select v-model="form.agentId" clearable filterable><el-option v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" /></el-select></el-form-item>
          <el-form-item v-if="id" label="审核状态">
            <el-tag :type="form.auditStatus === 'CONFIRMED' ? 'success' : 'warning'">{{ form.auditStatus === 'CONFIRMED' ? '已确认' : '待确认' }}</el-tag>
            <span class="audit-hint">审核状态请在订单列表中操作</span>
          </el-form-item>

          <template v-if="form.businessType === 'CAMPUS_NETWORK'">
            <el-form-item label="学号" prop="studentNo"><el-input v-model="form.studentNo" /></el-form-item>
            <el-form-item label="身份证后六位" prop="idCardLastSix"><el-input v-model="form.idCardLastSix" maxlength="6" /></el-form-item>
          </template>

          <template v-if="form.businessType === 'DRIVING_SCHOOL'">
            <el-form-item label="车型" prop="licenseType"><el-select v-model="form.licenseType"><el-option label="C1" value="C1" /><el-option label="C2" value="C2" /></el-select></el-form-item>
            <el-form-item label="班型" prop="classType"><el-select v-model="form.classType"><el-option label="普通班" value="NORMAL" /><el-option label="全包班" value="FULL" /></el-select></el-form-item>
            <el-form-item label="缴费数额" prop="paymentAmount"><el-input-number v-model="form.paymentAmount" :min="0" :precision="2" controls-position="right" /><span class="field-hint">默认价格由业务配置带出，可手动修改</span></el-form-item>
          </template>

          <el-form-item v-if="form.businessType === 'RENEWAL'" label="续费金额" prop="renewalAmount"><el-input-number v-model="form.renewalAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
        <div v-if="editing" class="actions"><el-button @click="router.back()">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存订单</el-button></div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.back { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 16px; color: #64748b; font-size: 14px; cursor: pointer; }
.form-panel { max-width: 980px; padding: 28px; }
.form-head { display: flex; justify-content: space-between; }
.form-head h1 { margin: 0; font-size: 22px; }
.form-head p { margin: 6px 0 28px; color: #8a95a5; font-size: 13px; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 22px; }
.form-grid :deep(.el-select), .form-grid :deep(.el-input-number) { width: 100%; }
.audit-hint, .field-hint { display: block; margin-top: 5px; color: #8a95a5; font-size: 12px; line-height: 1.4; }
.audit-hint { display: inline; margin: 0 0 0 8px; }
.actions { display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid #edf0f4; padding-top: 20px; }
</style>
