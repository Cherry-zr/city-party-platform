<template>
  <van-nav-bar :title="pageTitle" left-arrow @click-left="$router.back()" />
  <div class="chat-page">
    <van-empty v-if="!loading && !access.canAccess" image="error" :description="access.reason || '暂无群聊权限'" />
    <template v-else>
      <div v-if="socketStatus !== 'connected'" class="chat-status">
        {{ statusText }}
      </div>
      <div ref="messageListRef" class="chat-messages">
        <van-empty v-if="!loading && messages.length === 0" description="暂无聊天消息" />
        <div
          v-for="message in messages"
          :key="message.messageId"
          class="chat-row"
          :class="{ mine: message.senderId === auth.user?.id }"
        >
          <van-image round width="36" height="36" :src="message.senderAvatar || avatarFallback" />
          <div class="chat-bubble-wrap">
            <div class="chat-sender">{{ message.senderNickname || '用户' }} · {{ formatDateTime(message.createdAt) }}</div>
            <div class="chat-bubble">{{ message.content }}</div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <van-field
          v-model="content"
          rows="1"
          autosize
          type="textarea"
          maxlength="1000"
          placeholder="输入聊天内容"
          @keyup.enter.prevent="send"
        >
          <template #button>
            <van-button size="small" type="primary" :disabled="sendDisabled" @click="send">发送</van-button>
          </template>
        </van-field>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import { checkChatAccess, getChatMessages } from '../../api/chat'
import { useAuthStore } from '../../stores/auth'
import { connectRealtime, getRealtimeStatus, onRealtimeMessage, onRealtimeStatus, sendRealtime } from '../../utils/realtime'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const auth = useAuthStore()
const activityId = Number(route.params.id)
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

const loading = ref(false)
const access = ref({
  canAccess: false,
  reason: '',
  activityId,
  activityTitle: ''
})
const messages = ref([])
const content = ref('')
const socketStatus = ref(getRealtimeStatus())
const messageListRef = ref(null)

let unsubscribeMessage = null
let unsubscribeStatus = null

const pageTitle = computed(() => access.value.activityTitle || '活动群聊')
const sendDisabled = computed(() => !content.value.trim() || socketStatus.value !== 'connected' || !access.value.canAccess)
const statusText = computed(() => {
  const map = {
    connecting: '正在连接群聊...',
    reconnecting: '连接已断开，正在重连...',
    error: 'WebSocket 连接异常，请稍后重试',
    disconnected: 'WebSocket 未连接'
  }
  return map[socketStatus.value] || 'WebSocket 未连接'
})

async function loadAccessAndMessages() {
  if (!auth.isLogin) {
    access.value = {
      canAccess: false,
      reason: '请先登录后进入活动群聊',
      activityId,
      activityTitle: ''
    }
    return
  }
  loading.value = true
  try {
    access.value = await checkChatAccess(activityId)
    if (access.value.canAccess) {
      await loadMessages()
      connectRealtime(auth.token)
    }
  } catch (error) {
    access.value = {
      canAccess: false,
      reason: error.message || '无法进入活动群聊',
      activityId,
      activityTitle: ''
    }
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  const data = await getChatMessages(activityId, { current: 1, size: 50 })
  messages.value = data.records || []
  await scrollToBottom()
}

async function send() {
  const text = content.value.trim()
  if (!text) return
  const sent = sendRealtime({
    type: 'CHAT',
    activityId,
    content: text
  })
  if (!sent) {
    showFailToast('WebSocket 未连接，暂时不能发送')
    return
  }
  content.value = ''
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  unsubscribeStatus = onRealtimeStatus((status) => {
    socketStatus.value = status
  })
  unsubscribeMessage = onRealtimeMessage(async (payload) => {
    if (payload.type === 'ERROR') {
      showFailToast(payload.message || 'WebSocket 消息处理失败')
      return
    }
    if (payload.type !== 'CHAT' || Number(payload.activityId) !== activityId) {
      return
    }
    if (!messages.value.some((item) => item.messageId === payload.messageId)) {
      messages.value.push(payload)
      await scrollToBottom()
    }
  })
  loadAccessAndMessages()
})

onBeforeUnmount(() => {
  if (unsubscribeMessage) {
    unsubscribeMessage()
  }
  if (unsubscribeStatus) {
    unsubscribeStatus()
  }
})
</script>
