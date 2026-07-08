<template>
  <van-nav-bar title="系统通知" left-arrow right-text="全部已读" @click-left="$router.back()" @click-right="readAll" />
  <div class="mobile-content">
    <van-empty v-if="!notice.loading && notice.items.length === 0" description="暂无系统通知" />
    <div v-for="item in notice.items" :key="item.id" class="plain-panel notice-item" @click="read(item)">
      <van-space align="start" fill>
        <div style="flex: 1">
          <div class="activity-title">{{ item.title }}</div>
          <div class="activity-meta">{{ item.content }}</div>
          <div class="activity-meta">{{ formatDateTime(item.createdAt) }}</div>
        </div>
        <van-tag :type="item.read ? 'default' : 'primary'">{{ item.read ? '已读' : '未读' }}</van-tag>
      </van-space>
      <van-button v-if="item.relatedId" size="small" plain type="primary" @click.stop="$router.push(`/activities/${item.relatedId}`)">
        查看活动
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { showSuccessToast } from 'vant'
import { useNoticeStore } from '../../stores/notice'
import { formatDateTime } from '../../utils/format'

const notice = useNoticeStore()

async function read(item) {
  await notice.markRead(item)
}

async function readAll() {
  if (notice.unreadCount === 0) return
  await notice.markAllRead()
  showSuccessToast('已全部标记为已读')
}

onMounted(() => {
  notice.loadNotices({ current: 1, size: 50 })
})
</script>
