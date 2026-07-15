export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']
export const ACCEPTED_IMAGE_EXTENSIONS = '.jpg,.jpeg,.png,.webp'
export const MAX_SOURCE_IMAGE_SIZE = 10 * 1024 * 1024

export function validateSourceImage(file) {
  if (!file) return '请选择图片'
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
    return '仅支持 JPG、PNG、WebP 图片，暂不支持 HEIC 或 GIF'
  }
  if (file.size > MAX_SOURCE_IMAGE_SIZE) {
    return '原图不能超过 10MB'
  }
  return ''
}

export function revokeObjectUrl(url) {
  if (url?.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}
