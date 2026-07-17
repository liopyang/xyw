export function maskPhone(value?: string | null) {
  if (!value) return '-';
  return value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}
