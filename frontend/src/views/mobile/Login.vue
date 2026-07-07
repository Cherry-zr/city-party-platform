<template>
  <div class="mobile-page">
    <van-nav-bar title="登录" />
    <div class="mobile-content">
      <div class="plain-panel">
        <van-form @submit="submit">
          <van-field v-model="form.username" name="username" label="账号" placeholder="admin / user01" required />
          <van-field v-model="form.password" name="password" label="密码" type="password" placeholder="123456" required />
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
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { getCaptcha } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captcha = reactive({ captchaKey: '', captchaText: '' })
const form = reactive({ username: 'user01', password: '123456', captchaKey: '', captchaCode: '' })

async function loadCaptcha() {
  const data = await getCaptcha()
  captcha.captchaKey = data.captchaKey
  captcha.captchaText = data.captchaText
  form.captchaKey = data.captchaKey
  form.captchaCode = data.captchaText
}

async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    showSuccessToast('登录成功')
    router.replace(auth.isAdmin ? '/admin/dashboard' : '/')
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>
