<template>
  <div class="slider-captcha">
    <div v-if="loading" class="captcha-state">
      <van-loading size="22">正在生成安全验证...</van-loading>
    </div>
    <template v-else-if="challenge">
      <div
        class="captcha-image"
        :style="{ aspectRatio: challenge.imageWidth + ' / ' + challenge.imageHeight }"
      >
        <img :src="challenge.backgroundImage" alt="滑块验证码背景" />
        <img
          class="captcha-piece"
          :src="challenge.sliderImage"
          alt=""
          :style="pieceStyle"
        />
      </div>
      <div class="captcha-control">
        <input
          v-model.number="offsetX"
          data-testid="registration-captcha-slider"
          type="range"
          min="0"
          :max="maxOffset"
          :disabled="verifying || verified"
          aria-label="拖动滑块完成拼图"
          @pointerdown="startInteraction"
          @keydown="startInteraction"
          @input="recordTrace"
          @change="submitVerification"
        />
        <span>{{ captchaHint }}</span>
      </div>
    </template>
    <div v-else class="captcha-state">
      <span>安全验证加载失败</span>
      <van-button size="small" type="primary" plain @click="reload">重新加载</van-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showFailToast } from 'vant'
import { getRegistrationCaptcha, verifyRegistrationCaptcha } from '../api/auth'

const emit = defineEmits(['verified'])

const challenge = ref(null)
const loading = ref(false)
const verifying = ref(false)
const verified = ref(false)
const offsetX = ref(0)
const trace = ref([])
const startedAt = ref(0)

const maxOffset = computed(() => {
  if (!challenge.value) return 0
  return challenge.value.imageWidth - challenge.value.sliderWidth
})

const pieceStyle = computed(() => {
  if (!challenge.value) return {}
  return {
    top: (challenge.value.sliderY / challenge.value.imageHeight) * 100 + '%',
    left: (offsetX.value / challenge.value.imageWidth) * 100 + '%',
    width: (challenge.value.sliderWidth / challenge.value.imageWidth) * 100 + '%'
  }
})

const captchaHint = computed(() => {
  if (verified.value) return '验证通过'
  if (verifying.value) return '正在验证...'
  return '拖动滑块，将拼图放入缺口'
})

function startInteraction() {
  if (!startedAt.value) {
    startedAt.value = Date.now()
    trace.value = [Number(offsetX.value)]
  }
}

function recordTrace() {
  startInteraction()
  const current = Number(offsetX.value)
  if (trace.value.at(-1) !== current && trace.value.length < 200) {
    trace.value.push(current)
  }
}

async function submitVerification() {
  if (!challenge.value || verifying.value || verified.value) return
  recordTrace()
  verifying.value = true
  try {
    const data = await verifyRegistrationCaptcha({
      challengeId: challenge.value.challengeId,
      offsetX: Number(offsetX.value),
      durationMs: Date.now() - startedAt.value,
      trace: trace.value
    })
    verified.value = true
    emit('verified', data.captchaToken)
  } catch (error) {
    showFailToast(error.message || '滑块验证失败，请重试')
    await reload()
  } finally {
    verifying.value = false
  }
}

async function reload() {
  loading.value = true
  challenge.value = null
  offsetX.value = 0
  trace.value = []
  startedAt.value = 0
  verified.value = false
  emit('verified', '')
  try {
    challenge.value = await getRegistrationCaptcha()
  } catch {
    challenge.value = null
  } finally {
    loading.value = false
  }
}

defineExpose({ reload })
onMounted(reload)
</script>

<style scoped>
.slider-captcha {
  padding: 12px 16px 16px;
  border-top: 1px solid #ebedf0;
}

.captcha-image {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 8px;
  background: #f4f5f7;
}

.captcha-image > img:first-child {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-piece {
  position: absolute;
  height: auto;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.45));
  pointer-events: none;
}

.captcha-control {
  position: relative;
  margin-top: 10px;
}

.captcha-control input {
  display: block;
  width: 100%;
  margin: 0;
  accent-color: #1989fa;
}

.captcha-control span {
  display: block;
  margin-top: 6px;
  color: #646566;
  font-size: 12px;
  text-align: center;
}

.captcha-state {
  display: flex;
  min-height: 86px;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #646566;
  font-size: 13px;
}
</style>
