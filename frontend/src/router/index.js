import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import MobileLayout from '../views/mobile/MobileLayout.vue'
import Login from '../views/mobile/Login.vue'
import Register from '../views/mobile/Register.vue'
import Home from '../views/mobile/Home.vue'
import ActivityDetail from '../views/mobile/ActivityDetail.vue'
import ActivityChat from '../views/mobile/ActivityChat.vue'
import PublishActivity from '../views/mobile/PublishActivity.vue'
import MyActivities from '../views/mobile/MyActivities.vue'
import MySignups from '../views/mobile/MySignups.vue'
import MyFavorites from '../views/mobile/MyFavorites.vue'
import Profile from '../views/mobile/Profile.vue'
import EditProfile from '../views/mobile/EditProfile.vue'
import UserProfile from '../views/mobile/UserProfile.vue'
import ActivityMap from '../views/mobile/ActivityMap.vue'
import Partner from '../views/mobile/Partner.vue'
import SystemNotices from '../views/mobile/SystemNotices.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import UserManage from '../views/admin/UserManage.vue'
import ActivityManage from '../views/admin/ActivityManage.vue'
import SignupManage from '../views/admin/SignupManage.vue'
import CreditManage from '../views/admin/CreditManage.vue'
import ReportManage from '../views/admin/ReportManage.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/register', component: Register, meta: { public: true } },
  {
    path: '/',
    component: MobileLayout,
    children: [
      { path: '', component: Home, meta: { public: true } },
      { path: 'activities/:id', component: ActivityDetail, meta: { public: true } },
      { path: 'activities/:id/chat', component: ActivityChat },
      { path: 'publish', component: PublishActivity },
      { path: 'my-activities', component: MyActivities },
      { path: 'my-signups', component: MySignups },
      { path: 'my-favorites', component: MyFavorites },
      { path: 'profile', component: Profile },
      { path: 'profile/edit', component: EditProfile },
      { path: 'users/:id', component: UserProfile },
      { path: 'map', component: ActivityMap },
      { path: 'notices', component: SystemNotices },
      { path: 'partner', component: Partner }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: Dashboard },
      { path: 'users', component: UserManage },
      { path: 'activities', component: ActivityManage },
      { path: 'signups', component: SignupManage },
      { path: 'credits', component: CreditManage },
      { path: 'reports', component: ReportManage }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLogin) {
    return '/login'
  }
  if (to.meta.admin && !auth.isAdmin) {
    return '/'
  }
  if ((to.path === '/login' || to.path === '/register') && auth.isLogin) {
    return auth.isAdmin ? '/admin/dashboard' : '/'
  }
  return true
})

export default router
