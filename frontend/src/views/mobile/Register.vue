<template>
  <div class="mobile-page">
    <van-nav-bar title="注册" left-arrow @click-left="$router.back()" />
    <div class="mobile-content">
      <div class="plain-panel">
        <van-form @submit="submit">
          <van-field v-model="form.username" label="账号" placeholder="请输入账号" required />
          <van-field v-model="form.password" label="密码" type="password" placeholder="请输入密码" required />
          <van-field v-model="form.nickname" label="昵称" placeholder="请输入昵称" />
          <van-field v-model="form.phone" label="手机号" placeholder="可选" />
          <van-field v-model="form.city" label="城市" placeholder="北京" />
          <van-field v-model="form.captchaCode" label="验证码" required>
            <template #button>
              <van-button size="small" type="primary" plain @click.prevent="loadCaptcha">{{ captcha.captchaText || '获取' }}</van-button>
            </template>
          </van-field>
          <van-button block type="primary" native-type="submit" :loading="loading">注册并登录</van-button>
        </van-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { getCaptcha } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captcha = reactive({ captchaKey: '', captchaText: '' })
const form = reactive({ username: '', password: '', nickname: '', phone: '', city: '北京', captchaKey: '', captchaCode: '' })

async function loadCaptcha() {
  const data = await getCaptcha()
  captcha.captchaKey = data.captchaKey
  captcha.captchaText = data.captchaText
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
}

async function submit() {
  loading.value = true
  try {
    await auth.register(form)
    showSuccessToast('注册成功')
    router.replace('/')
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>
