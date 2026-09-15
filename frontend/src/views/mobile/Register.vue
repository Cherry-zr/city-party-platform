<template>
  <div class="mobile-page">
    <van-nav-bar title="注册" left-arrow @click-left="$router.back()" />
    <div class="mobile-content">
      <div class="plain-panel">
        <van-form autocomplete="off" @submit="submit">
          <van-field v-model="form.username" name="registerAccount" label="账号" autocomplete="off" placeholder="请输入账号" required />
          <van-field v-model="form.password" name="registerCredential" label="密码" type="password" autocomplete="new-password" placeholder="请输入密码" required />
          <van-field v-model="form.nickname" label="昵称" placeholder="请输入昵称" />
          <van-field v-model="form.phone" label="手机号" placeholder="可选" />
          <van-field v-model="form.city" label="城市" placeholder="北京" />
          <SliderCaptcha ref="captchaRef" @verified="form.captchaToken = $event" />
          <van-button block type="primary" native-type="submit" :loading="loading">注册并登录</van-button>
        </van-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import SliderCaptcha from '../../components/SliderCaptcha.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captchaRef = ref(null)
const form = reactive({ username: '', password: '', nickname: '', phone: '', city: '北京', captchaToken: '' })

async function submit() {
  if (!form.captchaToken) {
    showFailToast('请先完成滑块验证')
    return
  }
  loading.value = true
  try {
    await auth.register(form)
    showSuccessToast('注册成功')
    router.replace('/')
  } catch (error) {
    form.captchaToken = ''
    await captchaRef.value?.reload()
  } finally {
    loading.value = false
  }
}
</script>
