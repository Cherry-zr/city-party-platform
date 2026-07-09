<template>
  <div>
    <div class="admin-page-heading">
      <div>
        <h1>用户管理</h1>
        <p>查看用户基础资料与信用分，不提供删除或禁用操作</p>
      </div>
    </div>
    <div class="admin-card">
      <el-form inline @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号、昵称或手机号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="130" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="90" />
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.current"
        class="admin-pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawer" title="用户详情" size="480px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="用户 ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="城市">{{ detail.city || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ detail.role }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="信用分">{{ detail.creditScore }}</el-descriptions-item>
          <el-descriptions-item label="个人简介">{{ detail.bio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="兴趣标签">
            <el-space wrap>
              <el-tag v-for="tag in detail.interestTags || []" :key="tag" type="info">{{ tag }}</el-tag>
              <span v-if="!detail.interestTags?.length">-</span>
            </el-space>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminUserDetail, adminUsers } from '../../api/admin'
import { formatDateTime } from '../../utils/format'

const query = reactive({ keyword: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const drawer = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminUsers(query)
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.keyword = ''
  search()
}

async function showDetail(id) {
  drawer.value = true
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await adminUserDetail(id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>
