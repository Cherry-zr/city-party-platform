<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="item in cards" :key="item.label" :span="6">
        <div class="admin-card">
          <div style="color:#667085">{{ item.label }}</div>
          <div style="font-size:30px;font-weight:700;margin-top:8px">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboard } from '../../api/admin'

const data = ref({ userCount: 0, activityCount: 0, signupCount: 0, favoriteCount: 0 })
const cards = computed(() => [
  { label: '用户数', value: data.value.userCount },
  { label: '活动数', value: data.value.activityCount },
  { label: '报名数', value: data.value.signupCount },
  { label: '收藏数', value: data.value.favoriteCount }
])

onMounted(async () => {
  data.value = await dashboard()
})
</script>
