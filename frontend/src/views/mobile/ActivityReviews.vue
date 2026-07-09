<template>
  <van-nav-bar title="活动评价" left-arrow @click-left="$router.back()" />
  <van-tabs v-model:active="activeTab" sticky offset-top="46" @change="handleTabChange">
    <van-tab title="评价成员" name="targets">
      <div class="mobile-content">
        <van-empty v-if="!loadingTargets && targets.length === 0" description="暂无可评价成员" />
        <div v-for="target in targets" :key="target.userId" class="plain-panel review-target">
          <van-space align="center" fill>
            <van-image round width="48" height="48" :src="target.avatarUrl || avatarFallback" />
            <div class="review-target-info">
              <div class="activity-title">{{ target.nickname }}</div>
              <div class="activity-meta">信用分 {{ target.creditScore }}</div>
            </div>
            <van-button
              size="small"
              :type="target.reviewed ? 'default' : 'primary'"
              :disabled="target.reviewed"
              @click="openReview(target)"
            >
              {{ target.reviewed ? '已评价' : '去评价' }}
            </van-button>
          </van-space>
        </div>
      </div>
    </van-tab>
    <van-tab title="评价记录" name="records">
      <div class="mobile-content">
        <van-empty v-if="!loadingRecords && reviews.length === 0" description="暂无评价记录" />
        <div v-for="review in reviews" :key="review.id" class="plain-panel">
          <van-space align="center" fill>
            <van-image round width="42" height="42" :src="review.reviewerAvatarUrl || avatarFallback" />
            <div class="review-card-main">
              <div class="activity-title">{{ review.reviewerNickname }} → {{ review.targetNickname }}</div>
              <van-rate :model-value="review.rating" readonly size="16" />
            </div>
          </van-space>
          <p v-if="review.content" class="review-content">{{ review.content }}</p>
          <div v-if="review.tags?.length" class="tag-row">
            <van-tag v-for="tag in review.tags" :key="tag" plain>{{ tag }}</van-tag>
          </div>
          <div class="activity-meta">{{ formatDateTime(review.createdAt) }}</div>
        </div>
      </div>
    </van-tab>
  </van-tabs>

  <van-popup v-model:show="showForm" position="bottom" round closeable :style="{ minHeight: '68%' }">
    <div class="review-form">
      <div class="section-title">评价 {{ selectedTarget?.nickname }}</div>
      <div class="form-label">评分</div>
      <van-rate v-model="form.rating" size="28" />
      <div class="form-label">评价内容</div>
      <van-field
        v-model="form.content"
        type="textarea"
        rows="3"
        maxlength="500"
        show-word-limit
        placeholder="可以填写到场、沟通和参与体验"
      />
      <div class="form-label">评价标签（最多 5 个）</div>
      <div class="tag-row">
        <van-tag
          v-for="tag in presetTags"
          :key="tag"
          size="medium"
          :type="form.tags.includes(tag) ? 'primary' : 'default'"
          :plain="!form.tags.includes(tag)"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </van-tag>
      </div>
      <van-field
        v-model="customTag"
        maxlength="20"
        placeholder="输入自定义标签"
        label="自定义"
        clearable
      >
        <template #button>
          <van-button size="small" plain type="primary" @click="addCustomTag">添加</van-button>
        </template>
      </van-field>
      <div v-if="customTags.length" class="tag-row selected-tags">
        <van-tag
          v-for="tag in customTags"
          :key="tag"
          closeable
          type="primary"
          plain
          @close="removeCustomTag(tag)"
        >
          {{ tag }}
        </van-tag>
      </div>
      <van-button block type="primary" :loading="submitting" @click="submitReview">提交评价</van-button>
    </div>
  </van-popup>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import {
  getActivityReviews,
  getReviewTargets,
  submitActivityReview
} from '../../api/review'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const activityId = route.params.id
const avatarFallback = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80'
const presetTags = ['准时', '友好', '好沟通', '守信', '积极参与']
const activeTab = ref('targets')
const targets = ref([])
const reviews = ref([])
const loadingTargets = ref(false)
const loadingRecords = ref(false)
const showForm = ref(false)
const selectedTarget = ref(null)
const submitting = ref(false)
const customTag = ref('')
const customTags = ref([])
const form = reactive({
  rating: 5,
  content: '',
  tags: []
})

async function loadTargets() {
  loadingTargets.value = true
  try {
    targets.value = await getReviewTargets(activityId)
  } catch {
    targets.value = []
  } finally {
    loadingTargets.value = false
  }
}

async function loadReviews() {
  loadingRecords.value = true
  try {
    const data = await getActivityReviews(activityId, { current: 1, size: 50 })
    reviews.value = data.records || []
  } catch {
    reviews.value = []
  } finally {
    loadingRecords.value = false
  }
}

function openReview(target) {
  selectedTarget.value = target
  form.rating = 5
  form.content = ''
  form.tags = []
  customTag.value = ''
  customTags.value = []
  showForm.value = true
}

function toggleTag(tag) {
  const index = form.tags.indexOf(tag)
  if (index >= 0) {
    form.tags.splice(index, 1)
    return
  }
  if (allTags().length >= 5) {
    showToast('最多选择 5 个标签')
    return
  }
  form.tags.push(tag)
}

function addCustomTag() {
  const tag = customTag.value.trim()
  if (!tag) return
  if (tag.includes(',')) {
    showToast('标签不能包含逗号')
    return
  }
  if (allTags().includes(tag)) {
    showToast('标签已存在')
    return
  }
  if (allTags().length >= 5) {
    showToast('最多选择 5 个标签')
    return
  }
  customTags.value.push(tag)
  customTag.value = ''
}

function removeCustomTag(tag) {
  customTags.value = customTags.value.filter((item) => item !== tag)
}

function allTags() {
  return [...form.tags, ...customTags.value]
}

async function submitReview() {
  if (!selectedTarget.value) return
  submitting.value = true
  try {
    await submitActivityReview(activityId, {
      targetUserId: selectedTarget.value.userId,
      rating: form.rating,
      content: form.content.trim() || null,
      tags: allTags()
    })
    showSuccessToast('评价提交成功')
    showForm.value = false
    await Promise.all([loadTargets(), loadReviews()])
  } catch {
    // The request interceptor already presents the backend error message.
  } finally {
    submitting.value = false
  }
}

function handleTabChange(name) {
  if (name === 'records' && reviews.value.length === 0) {
    loadReviews()
  }
}

onMounted(() => {
  loadTargets()
})
</script>

<style scoped>
.review-target-info,
.review-card-main {
  flex: 1;
  min-width: 0;
}

.review-content {
  margin: 12px 0 6px;
  color: #303943;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.review-form {
  padding: 18px 16px 28px;
}

.form-label {
  margin: 18px 0 8px;
  color: #303943;
  font-size: 14px;
  font-weight: 600;
}

.selected-tags {
  margin-bottom: 18px;
}
</style>
