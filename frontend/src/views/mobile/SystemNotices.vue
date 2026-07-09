<template>
  <van-nav-bar
    title="通知中心"
    left-arrow
    right-text="全部已读"
    @click-left="$router.back()"
    @click-right="readAll"
  />
  <div class="mobile-content">
    <div class="notice-summary">未读通知 {{ notice.unreadCount }} 条</div>
    <div v-if="notice.loading && notice.items.length === 0" class="page-state">
      <van-loading vertical>正在加载通知...</van-loading>
    </div>
    <van-empty v-else-if="notice.error" image="error" :description="notice.error">
      <van-button size="small" type="primary" plain @click="load">重新加载</van-button>
    </van-empty>
    <van-empty v-else-if="notice.items.length === 0" description="暂无系统通知" />
    <template v-else>
      <div v-for="item in notice.items" :key="item.id" class="plain-panel notice-item" :class="{ unread: !item.read }" @click="read(item)">
        <div class="notice-head">
          <div class="notice-main">
            <div class="activity-title">{{ item.title }}</div>
            <div class="activity-meta">{{ item.content }}</div>
            <div class="activity-meta">{{ formatDateTime(item.createdAt) }}</div>
          </div>
          <van-tag :type="item.read ? 'default' : 'primary'">{{ item.read ? '已读' : '未读' }}</van-tag>
        </div>
        <van-button v-if="item.relatedId" size="small" plain type="primary" @click.stop="$router.push(`/activities/${item.relatedId}`)">
          查看活动
        </van-button>
      </div>
    </template>
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

async function load() {
  try {
    await notice.loadNotices({ current: 1, size: 50 })
  } catch {
    // The store keeps the visible retry message.
  }
}

onMounted(load)
</script>

<style scoped>
.notice-summary {
  margin-bottom: 10px;
  color: #5f6b7a;
  font-size: 13px;
  text-align: right;
}

.notice-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-item .activity-meta {
  overflow-wrap: anywhere;
}

.notice-item.unread {
  border-left: 3px solid #4b5563;
}
</style>
