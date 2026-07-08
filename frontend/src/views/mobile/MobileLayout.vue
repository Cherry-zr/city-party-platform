<template>
  <div class="mobile-page">
    <router-view />
    <van-tabbar route>
      <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/map" icon="location-o">地图</van-tabbar-item>
      <van-tabbar-item to="/publish" icon="plus">发布</van-tabbar-item>
      <van-tabbar-item to="/my-signups" icon="friends-o">报名</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o" :badge="notice.unreadCount || ''">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useNoticeStore } from '../../stores/notice'
import { connectRealtime, disconnectRealtime, onRealtimeMessage } from '../../utils/realtime'

const auth = useAuthStore()
const notice = useNoticeStore()

let unsubscribeMessage = null

function startRealtime() {
  if (!auth.token) return
  connectRealtime(auth.token)
  notice.loadUnreadCount()
}

onMounted(() => {
  unsubscribeMessage = onRealtimeMessage((payload) => {
    if (payload.type === 'NOTICE') {
      notice.receiveRealtimeNotice(payload)
    }
  })
  startRealtime()
})

watch(() => auth.token, (nextToken) => {
  disconnectRealtime()
  if (nextToken) {
    startRealtime()
  }
})

onBeforeUnmount(() => {
  if (unsubscribeMessage) {
    unsubscribeMessage()
  }
  disconnectRealtime()
})
</script>
