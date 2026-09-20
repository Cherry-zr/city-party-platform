<template>
  <div>
    <div class="admin-page-heading">
      <div>
        <h1>活动管理</h1>
        <p>审核用户发布的活动，并查看活动、报名及候补详情</p>
      </div>
    </div>
    <div class="admin-card">
      <el-form inline @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="标题或活动描述" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="query.category" placeholder="活动分类" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 160px">
            <el-option v-for="item in activityStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" clearable placeholder="全部审核状态" style="width: 160px">
            <el-option v-for="item in auditStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="rows" border empty-text="暂无活动数据">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="creatorNickname" label="发起人" min-width="120" />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="活动时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="地点" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.city }} {{ row.address }}</template>
        </el-table-column>
        <el-table-column label="人数" width="100">
          <template #default="{ row }">{{ row.approvedCount }}/{{ row.maxParticipants }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="110">
          <template #default="{ row }">
            <el-tag :type="auditStatusType(row.auditStatus)">
              {{ auditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.auditStatus !== 'APPROVED'"
              link
              type="success"
              :loading="auditLoadingId === row.id"
              @click="audit(row, 'APPROVED')"
            >
              通过
            </el-button>
            <el-button
              v-if="row.auditStatus !== 'REJECTED'"
              link
              type="danger"
              :loading="auditLoadingId === row.id"
              @click="audit(row, 'REJECTED')"
            >
              拒绝
            </el-button>
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

    <el-drawer v-model="drawer" title="活动详情" size="80%">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="活动 ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">{{ auditStatusText(detail.auditStatus) }}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="发起人">{{ detail.creatorNickname }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="报名截止">{{ formatDateTime(detail.signupDeadline) }}</el-descriptions-item>
            <el-descriptions-item label="人数">{{ detail.approvedCount }}/{{ detail.maxParticipants }}</el-descriptions-item>
            <el-descriptions-item label="地点" :span="2">{{ detail.city }} {{ detail.address }}</el-descriptions-item>
            <el-descriptions-item label="活动说明" :span="2">{{ detail.description }}</el-descriptions-item>
            <el-descriptions-item label="注意事项" :span="2">{{ detail.notes || '-' }}</el-descriptions-item>
            <el-descriptions-item label="拒绝原因" :span="2">{{ detail.rejectReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ formatDateTime(detail.auditTime) }}</el-descriptions-item>
            <el-descriptions-item label="审核管理员 ID">{{ detail.reviewerId || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="admin-detail-actions">
            <el-button
              v-if="detail.auditStatus !== 'APPROVED'"
              type="success"
              :loading="auditLoadingId === detail.id"
              @click="audit(detail, 'APPROVED')"
            >
              审核通过
            </el-button>
            <el-button
              v-if="detail.auditStatus !== 'REJECTED'"
              type="danger"
              plain
              :loading="auditLoadingId === detail.id"
              @click="audit(detail, 'REJECTED')"
            >
              审核拒绝
            </el-button>
          </div>

          <el-tabs v-model="detailTab" class="admin-detail-tabs">
            <el-tab-pane :label="`报名用户 (${signupRows.length})`" name="signups">
              <el-table :data="signupRows" border empty-text="暂无报名记录">
                <el-table-column prop="id" label="报名 ID" width="90" />
                <el-table-column prop="user.id" label="用户 ID" width="90" />
                <el-table-column prop="user.nickname" label="用户" min-width="120" />
                <el-table-column prop="user.phone" label="手机号" min-width="130" />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">{{ signupStatusText(row.status) }}</template>
                </el-table-column>
                <el-table-column prop="applyMessage" label="报名说明" min-width="160" show-overflow-tooltip />
                <el-table-column label="报名时间" min-width="150">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="`候补用户 (${waitlistRows.length})`" name="waitlist">
              <el-table :data="waitlistRows" border empty-text="暂无候补记录">
                <el-table-column prop="queueNo" label="队列序号" width="100" />
                <el-table-column prop="user.id" label="用户 ID" width="90" />
                <el-table-column prop="user.nickname" label="用户" min-width="120" />
                <el-table-column prop="user.phone" label="手机号" min-width="130" />
                <el-table-column prop="status" label="状态" width="110" />
                <el-table-column label="加入时间" min-width="150">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminActivities,
  adminAuditActivity,
  adminActivityDetail,
  adminActivitySignups,
  adminActivityWaitlist
} from '../../api/admin'
import { formatDateTime } from '../../utils/format'

const activityStatuses = [
  { label: '报名中', value: 'SIGNING' },
  { label: '已满员', value: 'FULL' },
  { label: '即将开始', value: 'UPCOMING' },
  { label: '进行中', value: 'ONGOING' },
  { label: '已结束', value: 'FINISHED' },
  { label: '已取消', value: 'CANCELLED' }
]
const auditStatuses = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' }
]
const query = reactive({ keyword: '', category: '', status: '', auditStatus: 'PENDING', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drawer = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailTab = ref('signups')
const signupRows = ref([])
const waitlistRows = ref([])
const auditLoadingId = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await adminActivities(query)
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
  Object.assign(query, { keyword: '', category: '', status: '', auditStatus: '', current: 1 })
  load()
}

async function audit(activity, auditStatus) {
  let rejectReason = null
  if (auditStatus === 'REJECTED') {
    try {
      const result = await ElMessageBox.prompt(
        '请填写拒绝原因，用户修改活动时会看到该说明。',
        '拒绝活动',
        {
          confirmButtonText: '确认拒绝',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputValidator: (value) => Boolean(value && value.trim()) || '拒绝原因不能为空'
        }
      )
      rejectReason = result.value.trim()
    } catch {
      return
    }
  }
  auditLoadingId.value = activity.id
  try {
    const updated = await adminAuditActivity(activity.id, { auditStatus, rejectReason })
    if (detail.value?.id === activity.id) {
      detail.value = updated
    }
    ElMessage.success(auditStatus === 'APPROVED' ? '活动已通过审核' : '活动已拒绝')
    await load()
  } finally {
    auditLoadingId.value = null
  }
}

async function showDetail(id) {
  drawer.value = true
  detailLoading.value = true
  detail.value = null
  signupRows.value = []
  waitlistRows.value = []
  detailTab.value = 'signups'
  try {
    const [activity, signups, waitlist] = await Promise.all([
      adminActivityDetail(id),
      adminActivitySignups(id, { current: 1, size: 100 }),
      adminActivityWaitlist(id, { current: 1, size: 100 })
    ])
    detail.value = activity
    signupRows.value = signups.records || []
    waitlistRows.value = waitlist.records || []
  } finally {
    detailLoading.value = false
  }
}

function statusText(status) {
  return activityStatuses.find((item) => item.value === status)?.label || status
}

function statusType(status) {
  if (status === 'FINISHED' || status === 'CANCELLED') return 'info'
  if (status === 'FULL') return 'warning'
  if (status === 'ONGOING') return 'success'
  return 'primary'
}

function auditStatusText(status) {
  return auditStatuses.find((item) => item.value === status)?.label || status || '待审核'
}

function auditStatusType(status) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

function signupStatusText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '报名成功',
    REJECTED: '已拒绝',
    WAITING: '候补中',
    PROMOTED: '候补转正',
    CANCELLED: '已取消',
    COMPLETED: '已完成',
    ABSENT: '缺席'
  }
  return map[status] || status
}

onMounted(load)
</script>
