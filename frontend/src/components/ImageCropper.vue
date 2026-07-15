<template>
  <van-popup
    :show="show"
    position="bottom"
    :style="{ height: '100%' }"
    :close-on-click-overlay="false"
    safe-area-inset-bottom
    @opened="initializeCropper"
    @closed="handleClosed"
  >
    <div class="image-cropper-page">
      <van-nav-bar
        :title="title"
        left-text="取消"
        right-text="确定"
        :right-disabled="confirming"
        @click-left="close"
        @click-right="confirmCrop"
      />

      <div class="image-cropper-body">
        <p class="image-cropper-tip">拖动图片调整位置，可使用双指、滚轮或下方滑块缩放</p>
        <div class="image-cropper-stage">
          <div
            ref="containerRef"
            class="image-cropper-container"
            :class="{ 'is-round': round }"
            :style="containerStyle"
          >
            <img ref="imageRef" :src="sourceUrl" alt="待裁剪图片" />
          </div>
        </div>

        <div class="image-cropper-controls">
          <span>缩小</span>
          <van-slider
            v-model="zoomValue"
            :min="-50"
            :max="100"
            :step="1"
            bar-height="4px"
            @change="applySliderZoom"
          />
          <span>放大</span>
        </div>
        <van-button block plain native-type="button" :disabled="confirming" @click="resetCropper">重置</van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import Cropper from 'cropperjs'
import { showFailToast } from 'vant'

const props = defineProps({
  show: Boolean,
  sourceUrl: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '裁剪图片'
  },
  aspectRatio: {
    type: Number,
    required: true
  },
  outputWidth: {
    type: Number,
    required: true
  },
  outputHeight: {
    type: Number,
    required: true
  },
  fileName: {
    type: String,
    default: 'cropped-image.jpg'
  },
  round: Boolean
})

const emit = defineEmits(['update:show', 'confirm', 'closed'])
const containerRef = ref(null)
const imageRef = ref(null)
const zoomValue = ref(0)
const lastSliderValue = ref(0)
const confirming = ref(false)
const containerStyle = computed(() => ({ aspectRatio: `${props.outputWidth} / ${props.outputHeight}` }))

let cropper = null
let lastValidTransform = null
let transformFrame = 0
let restoringTransform = false

const template = computed(() => `
  <cropper-canvas background scale-step="0.05">
    <cropper-image initial-center-size="cover" scalable translatable></cropper-image>
    <cropper-shade></cropper-shade>
    <cropper-handle action="move" plain></cropper-handle>
    <cropper-selection initial-coverage="0.88" aspect-ratio="${props.aspectRatio}" outlined>
      <cropper-grid role="grid" bordered covered></cropper-grid>
      <cropper-crosshair centered></cropper-crosshair>
      <cropper-handle action="move" theme-color="rgba(255, 255, 255, 0.2)"></cropper-handle>
    </cropper-selection>
  </cropper-canvas>
`)

async function initializeCropper() {
  destroyCropper()
  if (!props.sourceUrl || !imageRef.value || !containerRef.value) return
  await nextTick()
  try {
    cropper = new Cropper(imageRef.value, {
      container: containerRef.value,
      template: template.value
    })
    const cropperImage = cropper.getCropperImage()
    const selection = cropper.getCropperSelection()
    if (!cropperImage || !selection) {
      throw new Error('Cropper elements are unavailable')
    }
    await cropperImage.$ready()
    selection.aspectRatio = props.aspectRatio
    selection.initialAspectRatio = props.aspectRatio
    selection.$reset().$center()
    if (props.round) {
      selection.style.borderRadius = '50%'
      selection.style.overflow = 'hidden'
    }
    cropperImage.addEventListener('transform', scheduleBoundaryCheck)
    window.requestAnimationFrame(() => {
      if (coversSelection(cropperImage, selection)) {
        lastValidTransform = cropperImage.$getTransform()
      }
    })
  } catch (error) {
    destroyCropper()
    showFailToast('图片加载失败，请重新选择')
  }
}

function scheduleBoundaryCheck() {
  if (restoringTransform || !cropper) return
  window.cancelAnimationFrame(transformFrame)
  transformFrame = window.requestAnimationFrame(() => {
    const cropperImage = cropper?.getCropperImage()
    const selection = cropper?.getCropperSelection()
    if (!cropperImage || !selection) return
    if (coversSelection(cropperImage, selection)) {
      lastValidTransform = cropperImage.$getTransform()
      return
    }
    if (lastValidTransform) {
      restoringTransform = true
      cropperImage.$setTransform(lastValidTransform)
      restoringTransform = false
    }
  })
}

function coversSelection(cropperImage, selection) {
  const imageRect = cropperImage.getBoundingClientRect()
  const selectionRect = selection.getBoundingClientRect()
  const tolerance = 1
  return imageRect.left <= selectionRect.left + tolerance
    && imageRect.top <= selectionRect.top + tolerance
    && imageRect.right >= selectionRect.right - tolerance
    && imageRect.bottom >= selectionRect.bottom - tolerance
}

function applySliderZoom(value) {
  const cropperImage = cropper?.getCropperImage()
  if (!cropperImage) return
  const previous = lastSliderValue.value
  const before = cropperImage.$getTransform().join(',')
  cropperImage.$zoom((value - previous) / 100)
  window.requestAnimationFrame(() => {
    const after = cropperImage.$getTransform().join(',')
    if (before === after) {
      zoomValue.value = previous
      return
    }
    lastSliderValue.value = value
  })
}

function resetCropper() {
  const cropperImage = cropper?.getCropperImage()
  const selection = cropper?.getCropperSelection()
  if (!cropperImage || !selection) return
  lastValidTransform = null
  cropperImage.$resetTransform().$center('cover')
  selection.$reset().$center()
  zoomValue.value = 0
  lastSliderValue.value = 0
  window.requestAnimationFrame(() => {
    if (coversSelection(cropperImage, selection)) {
      lastValidTransform = cropperImage.$getTransform()
    }
  })
}

async function confirmCrop() {
  const selection = cropper?.getCropperSelection()
  const cropperImage = cropper?.getCropperImage()
  if (!selection || !cropperImage || !coversSelection(cropperImage, selection)) {
    showFailToast('请确保图片完整覆盖裁剪区域')
    return
  }
  confirming.value = true
  try {
    const canvas = await selection.$toCanvas({
      width: props.outputWidth,
      height: props.outputHeight,
      beforeDraw(context, targetCanvas) {
        context.fillStyle = '#ffffff'
        context.fillRect(0, 0, targetCanvas.width, targetCanvas.height)
      }
    })
    const blob = await canvasToBlob(canvas)
    if (!blob || blob.size > 5 * 1024 * 1024) {
      throw new Error('Cropped image is too large')
    }
    const file = new File([blob], props.fileName, {
      type: 'image/jpeg',
      lastModified: Date.now()
    })
    emit('confirm', file)
    close()
  } catch (error) {
    showFailToast('图片裁剪失败，请重试')
  } finally {
    confirming.value = false
  }
}

function canvasToBlob(canvas) {
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9))
}

function close() {
  emit('update:show', false)
}

function handleClosed() {
  destroyCropper()
  emit('closed')
}

function destroyCropper() {
  window.cancelAnimationFrame(transformFrame)
  const cropperImage = cropper?.getCropperImage()
  cropperImage?.removeEventListener('transform', scheduleBoundaryCheck)
  cropper?.destroy()
  cropper = null
  lastValidTransform = null
  zoomValue.value = 0
  lastSliderValue.value = 0
}

onBeforeUnmount(destroyCropper)
</script>

<style scoped>
.image-cropper-page {
  min-height: 100%;
  background: #111;
  color: #fff;
}

.image-cropper-body {
  padding: 18px 16px 24px;
}

.image-cropper-tip {
  margin: 0 0 18px;
  color: #d1d5db;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

.image-cropper-stage {
  display: flex;
  min-height: 46vh;
  align-items: center;
  justify-content: center;
}

.image-cropper-container {
  position: relative;
  width: 100%;
  max-height: 62vh;
  overflow: hidden;
  background: #000;
}

.image-cropper-container.is-round {
  width: min(82vw, 390px);
}

.image-cropper-container :deep(cropper-canvas) {
  width: 100%;
  height: 100%;
}

.image-cropper-controls {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  margin: 20px 4px;
  color: #d1d5db;
  font-size: 12px;
}

.image-cropper-body > .van-button {
  border-color: #6b7280;
  background: transparent;
  color: #fff;
}
</style>
