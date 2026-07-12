export type LicenseType = 'C1' | 'C2'
export type ClassType = 'NORMAL' | 'FULL'
export type DrivingPriceMap = Record<`${LicenseType}_${ClassType}`, number>

const storageKey = 'drivingPrices'

const emptyPrices: DrivingPriceMap = {
  C1_NORMAL: 0,
  C1_FULL: 0,
  C2_NORMAL: 0,
  C2_FULL: 0,
}

function numberValue(value: unknown) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0
}

export function normalizeDrivingPrices(homeData: Record<string, unknown> | null | undefined): DrivingPriceMap {
  const raw = (homeData?.drivingPrices || {}) as Record<string, unknown>
  const c1 = (raw.C1 || {}) as Record<string, unknown>
  const c2 = (raw.C2 || {}) as Record<string, unknown>
  return {
    C1_NORMAL: numberValue(raw.C1_NORMAL ?? raw.drivingC1NormalPrice ?? c1.NORMAL ?? homeData?.drivingC1NormalPrice),
    C1_FULL: numberValue(raw.C1_FULL ?? raw.drivingC1FullPrice ?? c1.FULL ?? homeData?.drivingC1FullPrice),
    C2_NORMAL: numberValue(raw.C2_NORMAL ?? raw.drivingC2NormalPrice ?? c2.NORMAL ?? homeData?.drivingC2NormalPrice),
    C2_FULL: numberValue(raw.C2_FULL ?? raw.drivingC2FullPrice ?? c2.FULL ?? homeData?.drivingC2FullPrice),
  }
}

export function saveDrivingPrices(homeData: Record<string, unknown> | null | undefined) {
  const prices = normalizeDrivingPrices(homeData)
  if (Object.values(prices).some((price) => price > 0)) uni.setStorageSync(storageKey, prices)
  return prices
}

export function cachedDrivingPrices(): DrivingPriceMap {
  return { ...emptyPrices, ...(uni.getStorageSync(storageKey) || {}) }
}

export function drivingPrice(prices: DrivingPriceMap, licenseType: LicenseType, classType: ClassType) {
  return prices[`${licenseType}_${classType}`]
}
