<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref, watch } from 'vue'
import {
  cachedDrivingPrices,
  drivingPrice,
  saveDrivingPrices,
  type ClassType,
  type DrivingPriceMap,
  type LicenseType,
} from '../../utils/driving-prices'
import { businessTypeLabels, sourceChannelLabels } from '../../utils/labels'
import { request } from '../../utils/request'
import { requireAgent } from '../../utils/session'

type BusinessType = 'CAMPUS_CARD' | 'CAMPUS_NETWORK' | 'DRIVING_SCHOOL' | 'RENEWAL'
type SourceChannel = 'AGENT' | 'ONLINE'

interface OrderForm {
  businessType: BusinessType
  name: string
  phone: string
  businessNumber: string
  sourceChannel: SourceChannel
  remark: string
  studentNo: string
  idCardLastSix: string
  licenseType: LicenseType
  classType: ClassType
  paymentAmount: number
  renewalAmount: number
}

const businessTypes: BusinessType[] = ['CAMPUS_CARD', 'CAMPUS_NETWORK', 'DRIVING_SCHOOL', 'RENEWAL']
const businessTypeNames = businessTypes.map((type) => businessTypeLabels[type])
const sourceChannels: SourceChannel[] = ['AGENT', 'ONLINE']
const sourceChannelNames = sourceChannels.map((source) => sourceChannelLabels[source])
const licenseTypes: LicenseType[] = ['C1', 'C2']
const classTypes: ClassType[] = ['NORMAL', 'FULL']
const classTypeNames = ['普通班', '全包班']

const saving = ref(false)
const prices = ref<DrivingPriceMap>(cachedDrivingPrices())
const form = reactive<OrderForm>({
  businessType: 'CAMPUS_CARD',
  name: '',
  phone: '',
  businessNumber: '',
  sourceChannel: 'AGENT',
  remark: '',
  studentNo: '',
  idCardLastSix: '',
  licenseType: 'C1',
  classType: 'NORMAL',
  paymentAmount: 0,
  renewalAmount: 0,
})

function applyDrivingPrice() {
  form.paymentAmount = drivingPrice(prices.value, form.licenseType, form.classType)
}

watch([() => form.licenseType, () => form.classType], applyDrivingPrice)

onLoad(async (query: Record<string, string> = {}) => {
  if (!requireAgent()) return
  if (businessTypes.includes(query.type as BusinessType)) form.businessType = query.type as BusinessType
  const home = await request<Record<string, unknown>>('/mini/home')
  const latest = saveDrivingPrices(home)
  if (Object.values(latest).some((price) => price > 0)) prices.value = latest
  applyDrivingPrice()
})

function validate() {
  if (!form.name.trim() || !/^1\d{10}$/.test(form.phone)) return '请填写姓名和正确手机号'
  if (form.businessType !== 'DRIVING_SCHOOL' && !form.businessNumber.trim()) return '请填写业务号码'
  if (form.businessType === 'CAMPUS_NETWORK' && !form.studentNo.trim()) return '请填写学号'
  if (form.businessType === 'CAMPUS_NETWORK' && !/^\d{6}$/.test(form.idCardLastSix)) return '身份证后六位应为 6 位数字'
  if (form.businessType === 'DRIVING_SCHOOL' && form.paymentAmount <= 0) return '驾校价格尚未配置，请联系管理员'
  if (form.businessType === 'RENEWAL' && form.renewalAmount <= 0) return '续费金额必须大于 0'
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
    await request('/mini/orders', 'POST', form)
    uni.showToast({ title: '提交成功' })
    setTimeout(() => uni.navigateBack(), 500)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="label">业务类型</text>
      <picker :range="businessTypeNames" @change="form.businessType = businessTypes[Number($event.detail.value)]">
        <view class="input picker-value">{{ businessTypeLabels[form.businessType] }}</view>
      </picker>

      <text class="label">订单来源</text>
      <picker :range="sourceChannelNames" @change="form.sourceChannel = sourceChannels[Number($event.detail.value)]">
        <view class="input picker-value">{{ sourceChannelLabels[form.sourceChannel] }}</view>
      </picker>

      <text class="label">姓名</text>
      <input v-model="form.name" class="input" maxlength="30" />
      <text class="label">联系电话</text>
      <input v-model="form.phone" class="input" type="number" maxlength="11" />

      <template v-if="form.businessType !== 'DRIVING_SCHOOL'">
        <text class="label">{{ form.businessType === 'CAMPUS_NETWORK' ? '新办号码' : '业务号码' }}</text>
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
        <view class="input picker-value price">{{ form.paymentAmount > 0 ? `¥${form.paymentAmount}` : '价格未配置' }}</view>
        <view class="hint">价格由后台业务配置自动确定</view>
      </template>

      <template v-if="form.businessType === 'RENEWAL'">
        <text class="label">续费金额</text>
        <input v-model.number="form.renewalAmount" class="input" type="digit" />
      </template>

      <text class="label">备注</text>
      <textarea v-model="form.remark" class="textarea" maxlength="500" />
      <button class="primary" :loading="saving" :disabled="saving" @click="save">提交订单</button>
    </view>
  </view>
</template>

<style scoped>
.picker-value { display: flex; align-items: center; }
.price { color: #2469dc; font-weight: 600; }
.hint { margin: -10rpx 0 16rpx; color: #8a95a5; font-size: 23rpx; }
</style>
