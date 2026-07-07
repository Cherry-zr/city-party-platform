<template>
  <van-nav-bar title="我的活动" />
  <div class="mobile-content">
    <van-empty v-if="activities.length === 0" description="暂无发起活动" />
    <div v-for="item in activities" :key="item.id" class="plain-panel">
      <ActivityCard :activity="item" @click="$router.push(`/activities/${item.id}`)" />
      <van-button size="small" type="primary" plain @click="openSignups(item)">查看报名申请</van-button>
    </div>
    <van-popup v-model:show="show" position="bottom" round :style="{ height: '70%' }">
      <div class="mobile-content">
        <div class="section-title">报名申请</div>
        <van-empty v-if="signups.length === 0" description="暂无报名" />
        <van-cell v-for="signup in signups" :key="signup.id" :title="signup.user?.nickname" :label="signup.applyMessage || '无申请说明'" :value="signup.status">
          <template #right-icon>
            <van-space v-if="signup.status === 'PENDING'">
              <van-button size="mini" type="primary" @click="review(signup.id, 'APPROVED')">同意</van-button>
              <van-button size="mini" type="danger" plain @click="review(signup.id, 'REJECTED')">拒绝</van-button>
            </van-space>
          </template>
        </van-cell>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { showSuccessToast } from 'vant'
import ActivityCard from '../../components/ActivityCard.vue'
import { activitySignups, myActivities } from '../../api/activity'
import { reviewSignup } from '../../api/signup'

const activities = ref([])
const signups = ref([])
const show = ref(false)
const currentActivityId = ref(null)

async function load() {
  const data = await myActivities({ current: 1, size: 20 })
  activities.value = data.records
}

async function openSignups(activity) {
  currentActivityId.value = activity.id
  const data = await activitySignups(activity.id, { current: 1, size: 50 })
  signups.value = data.records
  show.value = true
}

async function review(id, status) {
  await reviewSignup(id, status)
  showSuccessToast('审核完成')
  const data = await activitySignups(currentActivityId.value, { current: 1, size: 50 })
  signups.value = data.records
  await load()
}

onMounted(load)
</script>
