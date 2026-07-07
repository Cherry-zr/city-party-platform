<template>
  <div class="admin-layout">
    <div class="admin-shell">
      <aside class="admin-sidebar">
        <div class="admin-brand">City Party Admin</div>
        <el-menu router background-color="#1f2933" text-color="#d8dee6" active-text-color="#ffffff" :default-active="$route.path">
          <el-menu-item index="/admin/dashboard">数据看板</el-menu-item>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/activities">活动管理</el-menu-item>
          <el-menu-item index="/admin/signups">报名管理</el-menu-item>
          <el-menu-item index="/admin/credits">信用分管理</el-menu-item>
          <el-menu-item index="/admin/reports">举报管理</el-menu-item>
        </el-menu>
      </aside>
      <main class="admin-main">
        <header class="admin-topbar">
          <strong>同城活动发现与陌生人组局平台</strong>
          <el-space>
            <span>{{ auth.user?.nickname }}</span>
            <el-button size="small" @click="$router.push('/')">用户端</el-button>
            <el-button size="small" type="danger" plain @click="logout">退出</el-button>
          </el-space>
        </header>
        <section class="admin-content">
          <router-view />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>
