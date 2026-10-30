<template>
  <div class="cls-page homepage-page" v-loading="loading">
    <!-- 基本信息：超管只有头像 / 昵称 / 身份 / 注册时间，普通管理员多一块「管理操作记录」 -->
    <div class="cls-card hero-card">
      <RoleAvatar
        :size="isRingIdentity ? 64 : 72"
        :avatar="info.avatar"
        :role="info.role"
        :super-admin="info.isSuperAdmin"
        :pending-admin="info.pendingAdmin"
        :nickname="info.nickname"
      />

      <div class="hero-info">
        <div class="name-row">
          <h2 class="nickname">{{ info.nickname }}</h2>
          <el-tag :type="identityTagType" effect="dark" round size="small">{{ info.identity }}</el-tag>
          <!-- 管理员封号：红色小标签，跟在身份标签旁边 -->
          <el-tag v-if="info.adminTitle" type="danger" effect="plain" round size="small">
            {{ info.adminTitle }}
          </el-tag>
        </div>
        <div class="meta-row">
          <span class="meta-item">
            <el-icon><Calendar /></el-icon>
            注册时间：{{ formatDateTime(info.createdAt) }}
          </span>
          <span v-if="showAccountStatus" class="meta-item">
            <el-icon><CircleCheck /></el-icon>
            账号状态：{{ statusText }}
          </span>
        </div>
      </div>

      <el-button class="back-btn" text @click="goBack">
        <el-icon><Back /></el-icon>
        <span>返回</span>
      </el-button>
    </div>

    <!-- 管理操作记录：只有「普通管理员主页」有，且只对最高管理员展示 -->
    <div v-if="showOpsCard" class="cls-card ops-card">
      <div class="cls-section-title" :class="{ 'no-body': !ops.total }">
        <div class="title-left">
          <span class="bar"></span>
          管理操作记录
          <span class="cls-sub-title">仅最高管理员可见</span>
        </div>
        <span v-if="!ops.total" class="empty-inline">暂无操作记录</span>
      </div>

      <template v-if="ops.total">
        <div class="ops-stats">
          <div class="ops-stat">
            处理举报
            <b class="ops-num">{{ ops.reportCount || 0 }}</b>
            条
          </div>
          <div class="ops-stat">
            删除评论
            <b class="ops-num">{{ ops.commentDeleteCount || 0 }}</b>
            条
          </div>
          <div class="ops-stat">
            编辑图书
            <b class="ops-num">{{ ops.bookEditCount || 0 }}</b>
            本
          </div>
        </div>

        <div v-if="opsRecent.length" class="ops-list">
          <div v-for="(item, index) in opsRecent" :key="index" class="ops-row">
            <span class="ops-type">{{ item.typeText }}</span>
            <span class="ops-detail" :title="item.detail">{{ item.detail }}</span>
            <span class="ops-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- 正在阅读：正式管理员不能借书，主页不展示 -->
    <div v-if="!hideBookBlocks" class="cls-card">
      <div class="cls-section-title" :class="{ 'no-body': !reading.length }">
        <div class="title-left">
          <span class="bar"></span>
          正在阅读
          <span class="cls-sub-title">（{{ reading.length }}）</span>
        </div>
        <span v-if="!reading.length" class="empty-inline">暂无记录</span>
      </div>
      <div v-if="reading.length" class="book-grid">
        <div
          v-for="book in reading"
          :key="book.id"
          class="mini-book cls-hover-card"
          @click="goBook(book.id)"
        >
          <div class="mini-cover">
            <CoverImage
              v-if="book.coverUrl"
              :src="book.coverUrl"
              :crop="book.coverCardCrop"
              :alt="book.title"
              :title="book.title"
            />
            <div v-else class="cover-placeholder">{{ book.title }}</div>
          </div>
          <div class="mini-info">
            <h4 class="mini-title" :title="book.title">{{ book.title }}</h4>
            <p class="mini-author">{{ book.author }}</p>
            <el-tag :type="book.borrowStatus === 2 ? 'danger' : 'primary'" size="small" effect="plain">
              {{ book.borrowStatus === 2 ? '已逾期' : '借阅中' }}
            </el-tag>
            <span class="mini-time">应还：{{ formatDateTime(book.dueDate) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 已收藏：正式管理员不能借书，主页不展示 -->
    <div v-if="!hideBookBlocks" class="cls-card">
      <div class="cls-section-title" :class="{ 'no-body': !favorites.length }">
        <div class="title-left">
          <span class="bar"></span>
          已收藏
          <span class="cls-sub-title">（{{ favorites.length }}）</span>
        </div>
        <span v-if="!favorites.length" class="empty-inline">暂无记录</span>
      </div>
      <div v-if="favorites.length" class="book-grid">
        <div
          v-for="book in favorites"
          :key="book.id"
          class="mini-book cls-hover-card"
          @click="goBook(book.id)"
        >
          <div class="mini-cover">
            <CoverImage
              v-if="book.coverUrl"
              :src="book.coverUrl"
              :crop="book.coverCardCrop"
              :alt="book.title"
              :title="book.title"
            />
            <div v-else class="cover-placeholder">{{ book.title }}</div>
          </div>
          <div class="mini-info">
            <h4 class="mini-title" :title="book.title">{{ book.title }}</h4>
            <p class="mini-author">{{ book.author }}</p>
            <el-tag size="small" effect="plain" type="warning">已收藏</el-tag>
            <span class="mini-time">收藏于：{{ formatDateTime(book.favoriteTime) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminGetAdminOperations, getUserHomepage } from '@/api'
import { useUserStore } from '@/store/user'
import RoleAvatar from '@/components/RoleAvatar.vue'
import CoverImage from '@/components/CoverImage.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const info = ref({})

const reading = computed(() => info.value.reading || [])
const favorites = computed(() => info.value.favorites || [])

/**
 * 头像尺寸：只有「齿轮 / 魂环」头像（超管、正式管理员）才用 64，
 * 待激活管理员没有魂环，和普通用户一样用 72。
 */
const isRingIdentity = computed(() => info.value.role === 1 && info.value.pendingAdmin !== 1)

/**
 * 正式管理员（含超管）：不能借书，主页不展示「正在阅读 / 已收藏」；
 * 待激活管理员还要还书，保留这两块。
 */
const hideBookBlocks = computed(() => info.value.role === 1 && info.value.pendingAdmin !== 1)

/** 普通管理员（role=1、非超管、非待激活）：主页才有「管理操作记录」 */
const isNormalAdmin = computed(
  () => info.value.role === 1 && info.value.isSuperAdmin !== 1 && info.value.pendingAdmin !== 1
)

/** 管理操作记录只对最高管理员展示 */
const showOpsCard = computed(() => isNormalAdmin.value && userStore.isSuperAdmin)

/** 账号状态：只对普通用户展示（管理员的账号状态恒为正常） */
const showAccountStatus = computed(() => info.value.role !== 1)

const statusText = computed(() => {
  const status = info.value.status
  if (status === 0) return '已注销'
  if (status === 2) return '已禁用'
  return '正常'
})

const identityTagType = computed(() => {
  if (info.value.isSuperAdmin === 1) return 'danger'
  if (info.value.role === 1) return 'warning'
  return 'info'
})

/* ---------------- 管理操作记录 ---------------- */
const ops = ref({ total: 0, reportCount: 0, commentDeleteCount: 0, bookEditCount: 0, recent: [] })
const opsRecent = computed(() => ops.value.recent || [])

/** 管理端布局下的图书详情路径不同，按当前路由前缀判断 */
const isAdminSide = computed(() => route.path.startsWith('/admin'))

async function load() {
  loading.value = true
  try {
    const res = await getUserHomepage(route.params.id)
    info.value = res.data || {}
    ops.value = { total: 0, reportCount: 0, commentDeleteCount: 0, bookEditCount: 0, recent: [] }
    if (showOpsCard.value) {
      loadOps()
    }
  } finally {
    loading.value = false
  }
}

async function loadOps() {
  try {
    const res = await adminGetAdminOperations(info.value.id)
    ops.value = res.data || {}
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

function goBook(bookId) {
  router.push(isAdminSide.value ? `/admin/book/${bookId}` : `/user/book/${bookId}`)
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(isAdminSide.value ? '/admin/dashboard' : '/user/home')
  }
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(String(value).replace('T', ' ').replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<style scoped>
.homepage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.nickname {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--cls-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--cls-text-regular);
}

/* 没有数据时：区块只留标题行，右侧一行小字提示「暂无记录」 */
.cls-section-title.no-body {
  margin-bottom: 0;
}

.empty-inline {
  font-size: 13px;
  font-weight: 400;
  color: var(--cls-text-secondary);
}

/* ---------------- 管理操作记录 ---------------- */
.ops-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ops-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 10px 16px;
  font-size: 13px;
  color: var(--cls-text-regular);
  background: var(--cls-card-soft);
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius-sm);
}

.ops-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--cls-primary-ink);
}

.ops-list {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--cls-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ops-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--cls-text-regular);
}

.ops-type {
  flex-shrink: 0;
  padding: 1px 8px;
  font-size: 12px;
  border-radius: 0;
  background: var(--cls-primary-light);
  color: var(--cls-primary-ink);
}

.ops-detail {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ops-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.mini-book {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius);
  background: var(--cls-card);
  cursor: pointer;
}

.mini-cover {
  width: 72px;
  height: 96px;
  flex-shrink: 0;
  border-radius: var(--cls-radius-sm);
  overflow: hidden;
  background: var(--cls-primary-light);
}

.mini-cover :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-main);
  background: linear-gradient(150deg, var(--card-bg-soft) 0%, var(--fill) 100%);
}

.mini-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.mini-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--cls-text-main);
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-author {
  margin: 0;
  font-size: 12px;
  color: var(--cls-text-regular);
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-time {
  margin-top: auto;
  font-size: 12px;
  color: var(--cls-text-secondary);
}
</style>
