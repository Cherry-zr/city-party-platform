<template>
  <van-nav-bar title="我的收藏" />
  <div class="mobile-content">
    <van-empty v-if="items.length === 0" description="暂无收藏" />
    <ActivityCard v-for="item in items" :key="item.id" :activity="item.activity" @click="$router.push(`/activities/${item.activityId}`)" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ActivityCard from '../../components/ActivityCard.vue'
import { myFavorites } from '../../api/favorite'

const items = ref([])

async function load() {
  const data = await myFavorites({ current: 1, size: 50 })
  items.value = data.records
}

onMounted(load)
</script>
