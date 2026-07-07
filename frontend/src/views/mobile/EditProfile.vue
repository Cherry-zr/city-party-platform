<template>
  <van-nav-bar title="编辑资料" left-arrow @click-left="$router.back()" />
  <div class="mobile-content">
    <van-form @submit="submit">
      <div class="plain-panel">
        <van-field v-model="form.nickname" label="昵称" />
        <van-field v-model="form.city" label="城市" />
        <van-field v-model="tagText" label="兴趣标签" placeholder="AA制,周末" />
        <van-field v-model="form.bio" label="简介" type="textarea" />
      </div>
      <van-button block type="primary" native-type="submit" :loading="loading">保存</van-button>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { showSuccessToast } from 'vant'
import { updateProfile } from '../../api/user'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const tagText = ref((auth.user?.interestTags || []).join(','))
const form = reactive({
  nickname: auth.user?.nickname || '',
  city: auth.user?.city || '',
  bio: auth.user?.bio || ''
})

async function submit() {
  loading.value = true
  try {
    await updateProfile({ ...form, interestTags: tagText.value.split(',').map((item) => item.trim()).filter(Boolean) })
    await auth.refreshMe()
    showSuccessToast('已保存')
  } finally {
    loading.value = false
  }
}
</script>
