<template>
  <van-nav-bar title="系统通知" left-arrow @click-left="$router.back()" />
  <div class="mobile-content">
    <van-empty v-if="!loading && items.length === 0" description="暂无系统通知" />
    <div v-for="item in items" :key="item.id" class="plain-panel" @click="read(item)">
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
import { onMounted, ref } from 'vue'
import { myNotices, markNoticeRead } from '../../api/notice'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const items = ref([])

async function load() {
  loading.value = true
  try {
    const data = await myNotices({ current: 1, size: 50 })
    items.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function read(item) {
  if (item.read) return
  await markNoticeRead(item.id)
  item.read = true
}

onMounted(load)
</script>
