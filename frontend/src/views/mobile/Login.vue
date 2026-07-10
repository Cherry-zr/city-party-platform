<template>
  <div class="mobile-page">
    <van-nav-bar title="登录" />
    <div class="mobile-content">
      <div class="plain-panel">
        <van-form @submit="submit">
          <van-field v-model="form.username" name="username" label="账号" placeholder="admin / user01" required />
          <van-field v-model="form.password" name="password" label="密码" type="password" placeholder="请输入密码" required />
          <van-field v-model="form.captchaCode" name="captchaCode" label="验证码" placeholder="请输入验证码" required>
            <template #button>
              <van-button size="small" type="primary" plain @click.prevent="loadCaptcha">{{ captcha.captchaText || '获取' }}</van-button>
            </template>
          </van-field>
          <van-button block type="primary" native-type="submit" :loading="loading">登录</van-button>
        </van-form>
        <van-button block plain style="margin-top: 12px" @click="$router.push('/register')">去注册</van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { getCaptcha } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const captcha = reactive({ captchaKey: '', captchaText: '' })
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })

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
    await auth.login(form)
    showSuccessToast('登录成功')
    router.replace(resolveRedirect())
  } finally {
    loading.value = false
  }
}

function resolveRedirect() {
  const redirect = route.query.redirect
  if (auth.isAdmin) {
    return isSafeRedirect(redirect)
      ? redirect
      : '/admin/dashboard'
  }
  if (!isSafeRedirect(redirect) || redirect.startsWith('/admin')) {
    return '/'
  }
  return redirect
}

function isSafeRedirect(value) {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
}

onMounted(loadCaptcha)
</script>
