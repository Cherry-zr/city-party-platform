<template>
  <div class="admin-card">
    <el-form inline>
      <el-form-item label="关键词"><el-input v-model="query.keyword" placeholder="账号或手机号" clearable /></el-form-item>
      <el-button type="primary" @click="load">搜索</el-button>
    </el-form>
    <el-table :data="rows" border>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="city" label="城市" />
      <el-table-column prop="creditScore" label="信用分" />
      <el-table-column label="说明" value="第一阶段只展示，人工调整第二阶段实现" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminCredits } from '../../api/admin'

const query = reactive({ keyword: '', current: 1, size: 10 })
const rows = ref([])

async function load() {
  const data = await adminCredits(query)
  rows.value = data.records
}

onMounted(load)
</script>
