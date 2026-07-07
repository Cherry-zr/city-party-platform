export function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

export function formatCost(activity) {
  if (!activity) return ''
  if (activity.costType === 'FREE') return '免费'
  if (activity.costType === 'AA') return `AA制 约 ${activity.costAmount || 0} 元`
  if (activity.costType === 'FIXED') return `固定 ${activity.costAmount || 0} 元`
  return `预估 ${activity.costAmount || 0} 元`
}

export function assetUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return path
}
