<template>
  <van-nav-bar title="编辑资料" left-arrow @click-left="$router.back()" />
  <div class="mobile-content">
    <van-form @submit="submit">
      <div class="plain-panel">
        <div class="avatar-editor">
          <img class="avatar-editor-preview" :src="avatarPreviewUrl" alt="头像预览" />
          <div class="avatar-editor-actions">
            <van-button size="small" plain type="primary" native-type="button" @click="chooseAvatar">
              {{ hasCustomAvatar ? '更换头像' : '选择头像' }}
            </van-button>
            <van-button
              v-if="hasCustomAvatar"
              size="small"
              plain
              type="danger"
              native-type="button"
              @click="removeAvatar"
            >
              删除头像
            </van-button>
          </div>
          <div class="avatar-editor-help">支持 JPG、PNG、WebP，原图不超过 10MB</div>
          <input
            ref="avatarInputRef"
            class="visually-hidden-input"
            type="file"
            :accept="ACCEPTED_IMAGE_EXTENSIONS"
            @change="handleAvatarSelected"
          />
        </div>
        <van-field v-model="form.nickname" label="昵称" />
        <van-field v-model="form.city" label="城市" />
        <van-field v-model="tagText" label="兴趣标签" placeholder="AA制,周末" />
        <van-field v-model="form.bio" label="简介" type="textarea" />
      </div>
      <van-button block type="primary" native-type="submit" :loading="loading">保存</van-button>
    </van-form>
    <ImageCropper
      v-model:show="showAvatarCropper"
      :source-url="cropSourceUrl"
      title="裁剪头像"
      :aspect-ratio="1"
      :output-width="512"
      :output-height="512"
      file-name="avatar.jpg"
      round
      @confirm="handleAvatarCropped"
      @closed="releaseCropSource"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import { updateProfile } from '../../api/user'
import ImageCropper from '../../components/ImageCropper.vue'
import { useAuthStore } from '../../stores/auth'
import { ACCEPTED_IMAGE_EXTENSIONS, revokeObjectUrl, validateSourceImage } from '../../utils/image'

const auth = useAuthStore()
const loading = ref(false)
const showAvatarCropper = ref(false)
const avatarInputRef = ref(null)
const cropSourceUrl = ref('')
const pendingAvatarFile = ref(null)
const pendingAvatarPreviewUrl = ref('')
const avatarRemoved = ref(false)
const tagText = ref((auth.user?.interestTags || []).join(','))
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'
const form = reactive({
  nickname: auth.user?.nickname || '',
  city: auth.user?.city || '',
  bio: auth.user?.bio || ''
})
const avatarPreviewUrl = computed(() => {
  if (pendingAvatarPreviewUrl.value) return pendingAvatarPreviewUrl.value
  if (!avatarRemoved.value && auth.user?.avatarUrl) return auth.user.avatarUrl
  return avatarFallback
})
const hasCustomAvatar = computed(() => Boolean(
  pendingAvatarFile.value || (!avatarRemoved.value && auth.user?.avatarUrl)
))

function chooseAvatar() {
  avatarInputRef.value?.click()
}

function handleAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  const errorMessage = validateSourceImage(file)
  if (errorMessage) {
    showFailToast(errorMessage)
    return
  }
  releaseCropSource()
  cropSourceUrl.value = URL.createObjectURL(file)
  showAvatarCropper.value = true
}

function handleAvatarCropped(file) {
  revokeObjectUrl(pendingAvatarPreviewUrl.value)
  pendingAvatarFile.value = file
  pendingAvatarPreviewUrl.value = URL.createObjectURL(file)
  avatarRemoved.value = false
}

function removeAvatar() {
  revokeObjectUrl(pendingAvatarPreviewUrl.value)
  pendingAvatarFile.value = null
  pendingAvatarPreviewUrl.value = ''
  avatarRemoved.value = Boolean(auth.user?.avatarUrl)
}

function releaseCropSource() {
  revokeObjectUrl(cropSourceUrl.value)
  cropSourceUrl.value = ''
}

async function submit() {
  loading.value = true
  try {
    await updateProfile(
      { ...form, interestTags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean) },
      { avatarFile: pendingAvatarFile.value, removeAvatar: avatarRemoved.value }
    )
    await auth.refreshMe()
    revokeObjectUrl(pendingAvatarPreviewUrl.value)
    pendingAvatarFile.value = null
    pendingAvatarPreviewUrl.value = ''
    avatarRemoved.value = false
    showSuccessToast('已保存')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  releaseCropSource()
  revokeObjectUrl(pendingAvatarPreviewUrl.value)
})
</script>

<style scoped>
.avatar-editor {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0 18px;
  border-bottom: 1px solid #ebedf0;
  margin-bottom: 4px;
}

.avatar-editor-preview {
  width: 96px;
  height: 96px;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  background: #d8dde3;
  object-fit: cover;
}

.avatar-editor-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.avatar-editor-help {
  margin-top: 8px;
  color: #969799;
  font-size: 12px;
}

.visually-hidden-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  clip-path: inset(50%);
}
</style>
