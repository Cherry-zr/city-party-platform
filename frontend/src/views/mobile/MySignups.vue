<template>
  <van-nav-bar title="我的报名" />
  <div class="mobile-content">
    <van-empty v-if="items.length === 0" description="暂无报名记录" />
    <div v-for="item in items" :key="item.id" class="plain-panel">
      <div class="activity-title">{{ item.activity?.title }}</div>
      <div class="activity-meta">{{ item.activity?.city }} · {{ item.activity?.address }}</div>
      <van-space style="margin-top: 8px">
        <van-tag type="primary">{{ item.status }}</van-tag>
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

onMounted(load)
</script>
