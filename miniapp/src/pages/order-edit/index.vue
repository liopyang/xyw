<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref, watch } from 'vue'
import {
  cachedDrivingPrices,
  drivingPrice,
  saveDrivingPrices,
  type ClassType,
  type DrivingPriceMap,
  type LicenseType,
} from '../../utils/driving-prices'
import { request } from '../../utils/request'
import { requireAgent } from '../../utils/session'

interface EditableOrder {
  id?: number
  businessType?: string
  name?: string
  phone?: string
  businessNumber?: string
  sourceChannel?: string
  studentNo?: string
  idCardLastSix?: string
  licenseType?: LicenseType
  classType?: ClassType
  paymentAmount?: number
  renewalAmount?: number
  remark?: string
}

const licenseTypes: LicenseType[] = ['C1', 'C2']
const classTypes: ClassType[] = ['NORMAL', 'FULL']
const classTypeNames = ['普通班', '全包班']
const form = ref<EditableOrder>({})
const id = ref(0)
const saving = ref(false)
const prices = ref<DrivingPriceMap>(cachedDrivingPrices())

function applyDrivingPrice() {
  if (form.value.businessType !== 'DRIVING_SCHOOL' || !form.value.licenseType || !form.value.classType) return
  const amount = drivingPrice(prices.value, form.value.licenseType, form.value.classType)
  if (amount > 0) form.value.paymentAmount = amount
}

watch([() => form.value.licenseType, () => form.value.classType], applyDrivingPrice)

onLoad(async (query: Record<string, string> = {}) => {
  if (!requireAgent()) return
  id.value = Number(query.id)
  if (!Number.isFinite(id.value) || id.value <= 0) {
    uni.showToast({ title: '订单参数错误', icon: 'none' })
    return
  }
  form.value = await request<EditableOrder>(`/mini/orders/${id.value}`)
  if (form.value.businessType === 'DRIVING_SCHOOL') {
    const home = await request<Record<string, unknown>>('/mini/home')
    const latest = saveDrivingPrices(home)
    if (Object.values(latest).some((price) => price > 0)) prices.value = latest
    applyDrivingPrice()
  }
})

function validate() {
  if (!form.value.name?.trim() || !/^1\d{10}$/.test(form.value.phone || '')) return '请填写姓名和正确手机号'
  if (form.value.businessType !== 'DRIVING_SCHOOL' && !form.value.businessNumber?.trim()) return '请填写业务号码'
  if (form.value.businessType === 'CAMPUS_NETWORK' && !form.value.studentNo?.trim()) return '请填写学号'
  if (form.value.businessType === 'CAMPUS_NETWORK' && !/^\d{6}$/.test(form.value.idCardLastSix || '')) return '身份证后六位应为 6 位数字'
  if (form.value.businessType === 'DRIVING_SCHOOL' && Number(form.value.paymentAmount) <= 0) return '驾校价格尚未配置'
  if (form.value.businessType === 'RENEWAL' && Number(form.value.renewalAmount) <= 0) return '续费金额必须大于 0'
  return ''
}

async function save() {
  const error = validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  saving.value = true
  try {
    await request(`/mini/orders/${id.value}`, 'PUT', form.value)
    uni.showToast({ title: '保存成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="label">姓名</text>
      <input v-model="form.name" class="input" maxlength="30" />
      <text class="label">联系电话</text>
      <input v-model="form.phone" class="input" type="number" maxlength="11" />

      <template v-if="form.businessType !== 'DRIVING_SCHOOL'">
        <text class="label">业务号码</text>
        <input v-model="form.businessNumber" class="input" />
      </template>

      <template v-if="form.businessType === 'CAMPUS_NETWORK'">
        <text class="label">学号</text>
        <input v-model="form.studentNo" class="input" />
        <text class="label">身份证后六位</text>
        <input v-model="form.idCardLastSix" class="input" type="number" maxlength="6" />
      </template>

      <template v-if="form.businessType === 'DRIVING_SCHOOL'">
        <text class="label">车型</text>
        <picker :range="licenseTypes" @change="form.licenseType = licenseTypes[Number($event.detail.value)]">
          <view class="input picker-value">{{ form.licenseType }}</view>
        </picker>
        <text class="label">班型</text>
        <picker :range="classTypeNames" @change="form.classType = classTypes[Number($event.detail.value)]">
          <view class="input picker-value">{{ form.classType === 'NORMAL' ? '普通班' : '全包班' }}</view>
        </picker>
        <text class="label">缴费数额</text>
        <view class="input picker-value price">¥{{ form.paymentAmount || 0 }}</view>
        <view class="hint">车型或班型变化后，价格按后台配置自动更新</view>
      </template>

      <template v-if="form.businessType === 'RENEWAL'">
        <text class="label">续费金额</text>
        <input v-model.number="form.renewalAmount" class="input" type="digit" />
      </template>

      <text class="label">备注</text>
      <textarea v-model="form.remark" class="textarea" maxlength="500" />
      <button class="primary" :loading="saving" :disabled="saving" @click="save">保存修改</button>
    </view>
  </view>
</template>

<style scoped>
.picker-value { display: flex; align-items: center; }
.price { color: #2469dc; font-weight: 600; }
.hint { margin: -10rpx 0 16rpx; color: #8a95a5; font-size: 23rpx; }
</style>
