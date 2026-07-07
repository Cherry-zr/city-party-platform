<template>
  <van-nav-bar title="用户主页" left-arrow @click-left="$router.back()" />
  <div v-if="profile" class="mobile-content">
    <div class="plain-panel">
      <van-space>
        <van-image round width="64" height="64" :src="profile.avatarUrl || avatarFallback" />
        <div>
          <div class="activity-title">{{ profile.nickname }}</div>
          <div class="activity-meta">{{ profile.city }} · 信用分 {{ profile.creditScore }}</div>
        </div>
      </van-space>
      <p>{{ profile.bio }}</p>
      <div class="tag-row">
        <van-tag v-for="tag in profile.interestTags" :key="tag" plain>{{ tag }}</van-tag>
      </div>
      <van-grid :column-num="2">
        <van-grid-item title="发起活动" :text="String(profile.createdActivityCount)" />
        <van-grid-item title="参与活动" :text="String(profile.joinedActivityCount)" />
      </van-grid>
      <van-button block plain type="primary" style="margin-top: 12px">申请成为固定搭子</van-button>
    </div>
    <div class="section-title">公开发起的活动</div>
    <ActivityCard v-for="item in profile.publicActivities" :key="item.id" :activity="item" @click="$router.push(`/activities/${item.id}`)" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ActivityCard from '../../components/ActivityCard.vue'
import { getPublicProfile } from '../../api/user'

const route = useRoute()
const profile = ref(null)
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'

onMounted(async () => {
  profile.value = await getPublicProfile(route.params.id)
})
</script>
