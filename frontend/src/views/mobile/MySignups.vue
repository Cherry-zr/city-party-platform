<template>
  <van-nav-bar title="我的报名" />
  <div class="mobile-content">
    <van-empty v-if="items.length === 0" description="暂无报名记录" />
    <div v-for="item in items" :key="item.id" class="plain-panel">
      <div class="activity-title">{{ item.activity?.title }}</div>
      <div class="activity-meta">{{ item.activity?.city }} · {{ item.activity?.address }}</div>
      <van-space style="margin-top: 8px">
        <van-tag :type="statusType(item.status)">{{ statusText(item.status) }}</van-tag>
        <van-button size="small" plain @click="$router.push(`/activities/${item.activityId}`)">详情</van-button>
      </van-space>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { mySignups } from '../../api/signup'

const items = ref([])

async function load() {
  const data = await mySignups({ current: 1, size: 50 })
  items.value = data.records
}

function statusText(status) {
  const map = {
    APPROVED: '报名成功',
    PROMOTED: '报名成功',
    PENDING: '待审核',
    WAITING: '候补中',
    CANCELLED: '已取消',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

function statusType(status) {
  if (status === 'APPROVED' || status === 'PROMOTED') return 'success'
  if (status === 'WAITING') return 'warning'
  if (status === 'CANCELLED' || status === 'REJECTED') return 'default'
  return 'primary'
}

onMounted(load)
</script>
