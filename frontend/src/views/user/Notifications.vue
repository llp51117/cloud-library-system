<template>
  <div class="notifications">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>消息通知</span>
        </div>
        <!-- 两个 Tab 各自独立：哪个 Tab 有未读，就在哪个 Tab 显示「全部标记已读」 -->
        <el-button v-if="showReadAll" type="primary" plain size="small" @click="handleReadAll">
          <el-icon><Select /></el-icon>
          <span>全部标记已读</span>
        </el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane name="announcement">
          <template #label>
            <span>系统公告</span>
            <el-badge
              v-if="announcementUnread > 0"
              :value="announcementUnread"
              :max="99"
              class="tab-badge"
            />
          </template>
        </el-tab-pane>
        <el-tab-pane name="personal">
          <template #label>
            <span>我的通知</span>
            <el-badge v-if="personalUnread > 0" :value="personalUnread" :max="99" class="tab-badge" />
          </template>
        </el-tab-pane>
        <el-tab-pane name="feedback" label="意见反馈" />
      </el-tabs>

      <!-- ============ 系统公告 ============ -->
      <div v-if="activeTab === 'announcement'" v-loading="loading" class="notice-list">
        <div
          v-for="item in pagedAnnouncements"
          :key="item.id"
          class="notice-item notice-announcement"
          :class="{ unread: !item.read }"
          @click="handleReadAnnouncement(item)"
        >
          <div class="notice-icon"><el-icon :size="20"><Bell /></el-icon></div>
          <div class="notice-body">
            <div class="notice-head">
              <h3>{{ item.title }}</h3>
              <span class="time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <p class="notice-content">{{ item.content }}</p>
            <div class="notice-footer">
              <!-- 发布人：超管显示「最高管理员」，普通管理员显示昵称，一律不显示邮箱 -->
              <span class="publisher">发布人：{{ publisherName(item) }}</span>
            </div>
          </div>
          <div v-if="!item.read" class="unread-dot"></div>
        </div>
        <div v-if="!loading && announcements.length === 0" class="cls-empty">暂无系统公告</div>

        <!-- 系统公告分页：每页 10 条 -->
        <div v-if="announcements.length > ANN_SIZE" class="pager">
          <el-pagination
            layout="prev, pager, next, total"
            :total="announcements.length"
            :page-size="ANN_SIZE"
            v-model:current-page="annPage"
            background
          />
        </div>
      </div>

      <!-- ============ 我的通知 ============ -->
      <div v-else-if="activeTab === 'personal'">
        <!-- 类型筛选：选项带各类通知数量，切换后列表实时刷新 -->
        <div class="filter-bar">
          <span class="filter-label">类型筛选：</span>
          <el-select
            v-model="typeFilter"
            style="width: 220px"
            placeholder="请选择通知类型"
            @change="handleFilterChange"
          >
            <el-option
              v-for="opt in filterOptions"
              :key="opt.value"
              :label="`${opt.label} (${opt.count})`"
              :value="opt.value"
            />
          </el-select>
        </div>

        <div v-loading="loading" class="notice-list">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notice-item"
            :class="{ unread: item.isRead === 0, 'alert-notice': isAlertNotice(item.type) }"
            @click="handleRead(item)"
          >
            <div class="notice-icon" :style="{ background: typeColor(item.type) }">
              <el-icon :size="18"><component :is="typeIcon(item.type)" /></el-icon>
            </div>
            <div class="notice-body">
              <div class="notice-head">
                <h3>
                  {{ noticeTitle(item) }}
                  <el-tag size="small" :color="typeColor(item.type)" effect="dark" class="type-tag">
                    {{ typeName(item.type) }}
                  </el-tag>
                </h3>
                <span class="time">{{ formatTime(item.createdAt) }}</span>
              </div>
              <p class="notice-content">{{ item.content }}</p>
            </div>
            <div v-if="item.isRead === 0" class="unread-dot"></div>
          </div>
          <div v-if="!loading && notifications.length === 0" class="cls-empty">暂无新消息</div>

          <div v-if="total > size" class="pager">
            <el-pagination
              layout="prev, pager, next, total"
              :total="total"
              :page-size="size"
              :current-page="page"
              background
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>

      <!-- ============ 意见反馈 ============ -->
      <div v-else-if="activeTab === 'feedback'" class="feedback-pane">
        <!-- 提交反馈表单 -->
        <div class="feedback-block">
          <div class="pane-sub-title">提交反馈</div>
          <el-form
            ref="feedbackRef"
            :model="feedbackForm"
            :rules="feedbackRules"
            label-width="90px"
            class="feedback-form"
          >
            <el-form-item label="反馈类型" prop="type">
              <el-select v-model="feedbackForm.type" placeholder="请选择反馈类型" style="width: 100%">
                <el-option
                  v-for="(name, index) in feedbackTypes"
                  :key="index"
                  :label="name"
                  :value="index"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="标题" prop="title">
              <el-input
                v-model="feedbackForm.title"
                maxlength="50"
                show-word-limit
                placeholder="请简要描述问题"
              />
            </el-form-item>
            <el-form-item label="详细描述" prop="content">
              <el-input
                v-model="feedbackForm.content"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                resize="none"
                placeholder="请详细描述你遇到的问题或建议，便于我们改进"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="feedbackSubmitting" @click="handleSubmitFeedback">
                提交反馈
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 我的反馈列表：待回复 / 已回复 + 管理员回复内容 -->
        <div class="feedback-block">
          <div class="pane-sub-title">我的反馈记录</div>
          <div v-loading="feedbackLoading" class="feedback-list">
            <div v-for="item in feedbackList" :key="item.id" class="feedback-item">
              <div class="fb-head">
                <span class="fb-type">{{ feedbackTypes[item.type] || '其他' }}</span>
                <span class="fb-title">{{ item.title }}</span>
                <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small">
                  {{ item.status === 1 ? '已回复' : '待回复' }}
                </el-tag>
              </div>
              <p class="fb-content">{{ item.content }}</p>
              <div v-if="item.reply" class="fb-reply">
                <el-icon><ChatDotSquare /></el-icon>
                <span>管理员回复：{{ item.reply }}</span>
              </div>
            </div>
            <div v-if="!feedbackLoading && feedbackList.length === 0" class="cls-empty">
              暂无反馈记录，遇到问题欢迎提交反馈
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { publisherName } from '@/utils/publisher'
import {
  getAnnouncements,
  getNotifications,
  getNotificationTypeCounts,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationRead,
  markAnnouncementRead,
  markAllAnnouncementRead,
  submitFeedback,
  getMyFeedbacks
} from '@/api'

const emit = defineEmits(['refresh-unread'])

const activeTab = ref('announcement')
const loading = ref(false)
const announcements = ref([])
const notifications = ref([])
/** 两个 Tab 各自的未读数量，全部由后端实时统计 */
const announcementUnread = ref(0)
const personalUnread = ref(0)
const total = ref(0)
const page = ref(1)
const size = ref(10)
/** 系统公告的页码与每页条数（公告接口一次性返回全部，这里在前端切片） */
const annPage = ref(1)
const ANN_SIZE = 10

/* ==================== 我的通知：类型筛选 ==================== */

/**
 * 筛选下拉的选项定义（value 用于 el-select，types 是对应后端通知类型）：
 * 预约相关 = 预约轮到(2) + 预约超时(3)；全部通知 = 不筛类型
 */
const TYPE_FILTERS = [
  { label: '全部通知', value: 'all', types: null },
  { label: '借阅成功', value: '1', types: [1] },
  { label: '归还成功', value: '4', types: [4] },
  { label: '强制归还', value: '8', types: [8] },
  { label: '预约相关', value: '2,3', types: [2, 3] },
  { label: '逾期提醒', value: '5', types: [5] },
  { label: '催还提醒', value: '7', types: [7] },
  { label: '反馈回复', value: '6', types: [6] },
  /* 管理员相关：待激活 / 转正 / 转换失败 / 取消管理员 / 操作申请审批结果 */
  { label: '管理员相关', value: '9,10,11,13,14,12', types: [9, 10, 11, 12, 13, 14] }
]
const typeFilter = ref('all')
/** 各通知类型的条数（type → 数量），由后端一次性统计 */
const typeCounts = ref({})

/** 带数量后缀的筛选项，如「借阅成功 (5)」；数量为全部通知的实时统计，不随筛选变化 */
const filterOptions = computed(() =>
  TYPE_FILTERS.map((f) => {
    const count = f.types
      ? f.types.reduce((sum, t) => sum + Number(typeCounts.value[t] || 0), 0)
      : Object.values(typeCounts.value).reduce((sum, c) => sum + Number(c || 0), 0)
    return { ...f, count }
  })
)

/* ==================== 意见反馈 ==================== */

const feedbackTypes = ['功能建议', '举报用户', '内容错误', '系统BUG', '其他']
const feedbackRef = ref()
const feedbackList = ref([])
const feedbackLoading = ref(false)
const feedbackSubmitting = ref(false)
const feedbackForm = reactive({ type: 0, title: '', content: '' })
const feedbackRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

const TYPE_NAMES = [
  '系统公告', '借阅成功', '预约轮到', '预约超时', '还书成功', '逾期提醒', '反馈回复', '催还提醒', '强制归还',
  '管理员待激活', '管理员开通', '管理员转换失败', '申请审批结果', '转换失败提醒', '取消管理员', '操作申请'
]
const TYPE_ICONS = [
  'Bell', 'CircleCheck', 'Star', 'Clock', 'Box', 'WarningFilled', 'ChatDotSquare', 'AlarmClock', 'Management',
  'Management', 'CircleCheck', 'WarningFilled', 'DocumentChecked', 'WarningFilled', 'CircleClose', 'DocumentAdd'
]
/** 后端没给标题时的兜底（正常由后端按类型下发，这里只是保险） */
const TYPE_TITLES = [
  '系统公告', '借阅成功', '预约轮到', '预约超时', '归还成功', '逾期提醒', '反馈回复', '催还提醒', '管理员强制归还',
  '管理员权限待激活', '管理员权限已开通', '管理员转换失败', '操作申请审批结果', '管理员转换失败提醒', '管理员权限已取消', '有新的操作申请'
]
/**
 * 各类型配色（图标底色 + 标签底色共用一套，改一处即可）
 * 借阅成功=蓝色系、归还成功=绿色系、管理员强制归还=橙色系；其余类型沿用原色并保持互不撞色：
 * 公告蓝紫、预约轮到橙、预约超时灰、逾期红、反馈回复紫、催还提醒橙
 */
const TYPE_COLORS = [
  '#4f6ef7', // 0 系统公告
  '#2f7ff0', // 1 借阅成功（蓝）
  '#f5a623', // 2 预约轮到
  '#b9c3d4', // 3 预约超时
  '#2fb87f', // 4 归还成功（绿）
  '#f0513f', // 5 逾期提醒
  '#8b5cf6', // 6 反馈回复
  '#f5a623', // 7 催还提醒
  '#fa8c16', // 8 管理员强制归还（橙）
  '#ef4444', // 9 管理员待激活（红，需重点提醒）
  '#2f7ff0', // 10 管理员开通（蓝）
  '#ef4444', // 11 管理员转换失败（红）
  '#8b5cf6', // 12 申请审批结果（紫）
  '#ef4444', // 13 转换失败提醒（红，发给超管）
  '#f5a623', // 14 取消管理员
  '#4f6ef7' // 15 新的操作申请
]

/** 需要「红色加粗」重点展示的通知类型：待激活、转换失败（本人 / 超管） */
const ALERT_TYPES = [9, 11, 13]

function isAlertNotice(type) {
  return ALERT_TYPES.includes(Number(type))
}

/**
 * 时间显示：后端给的是 2026-10-22T10:30:00 这种格式，
 * 中间那个大写 T 不能给用户看，统一换成空格并去掉秒：2026-10-22 10:30
 */
function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 16)
}

/**
 * 是否显示「全部标记已读」：只取决于当前 Tab 自己的未读数，
 * 公告和我的通知互不影响（公告清零不会动通知，反之亦然）
 */
const showReadAll = computed(
  () =>
    (activeTab.value === 'announcement' && announcementUnread.value > 0) ||
    (activeTab.value === 'personal' && personalUnread.value > 0)
)

/** 当前页的系统公告 */
const pagedAnnouncements = computed(() => {
  const start = (annPage.value - 1) * ANN_SIZE
  return announcements.value.slice(start, start + ANN_SIZE)
})

onMounted(() => {
  loadAnnouncements()
  // 进页面先取一次未读数，两个 Tab 的红点才能立刻显示数字；
  // 这里不自动清零，用户要能先看到各自有多少条未读
  loadUnreadCounts()
})

async function onTabChange(name) {
  // 切换 Tab 只负责加载列表，不做任何已读标记：
  // 未读必须一条一条点开才算已读，避免「一切 Tab 数字就清零」
  if (name === 'personal') {
    page.value = 1
    await Promise.all([loadNotifications(), loadTypeCounts()])
  } else if (name === 'feedback') {
    await loadFeedbacks()
  } else {
    await loadAnnouncements()
  }
}

/** 未读数量一律实时查数据库，不用列表本地统计（列表只加载当前页，统计出来会偏小） */
async function loadUnreadCounts() {
  try {
    const res = await getUnreadCount()
    const data = res.data || {}
    announcementUnread.value = Number(data.announcement || 0)
    personalUnread.value = Number(data.notification || 0)
  } catch (e) {
    /* 忽略未读数量加载失败 */
  }
  emit('refresh-unread')
}

/** 点开一条公告：只标记这一条已读，红点消失、Tab 数字减 1 */
async function handleReadAnnouncement(item) {
  if (item.read) return
  try {
    await markAnnouncementRead(item.id)
    item.read = true
    // 重新查库拿最新未读数，数字逐条递减
    await loadUnreadCounts()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function loadAnnouncements() {
  loading.value = true
  try {
    const res = await getAnnouncements()
    announcements.value = res.data || []
    annPage.value = 1
  } finally {
    loading.value = false
  }
}

async function loadNotifications() {
  loading.value = true
  try {
    // 预约相关等筛选项对应多个后端类型，用逗号拼接传给后端（types=2,3）
    const filter = TYPE_FILTERS.find((f) => f.value === typeFilter.value)
    const types = filter?.types ? filter.types.join(',') : undefined
    const res = await getNotifications({ page: page.value, size: size.value, types })
    notifications.value = res.data.list || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

/** 各类型通知数量（筛选下拉的选项数量） */
async function loadTypeCounts() {
  try {
    const res = await getNotificationTypeCounts()
    typeCounts.value = res.data || {}
  } catch (e) {
    /* 数量加载失败不影响列表展示 */
  }
}

/** 切换筛选：回到第 1 页并实时刷新列表，不需要手动点查询 */
function handleFilterChange() {
  page.value = 1
  loadNotifications()
}

function handlePageChange(current) {
  page.value = current
  loadNotifications()
}

async function handleRead(item) {
  if (item.isRead === 1) return
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
    await loadUnreadCounts()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 「全部标记已读」：按当前 Tab 分发，两个 Tab 的未读各清各的 */
async function handleReadAll() {
  if (activeTab.value === 'announcement') {
    await handleReadAllAnnouncements()
    return
  }
  try {
    await markAllNotificationRead()
    notifications.value.forEach((n) => {
      n.isRead = 1
    })
    personalUnread.value = 0
    emit('refresh-unread')
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 系统公告全部标记已读：红点消失、公告未读清零，个人通知的未读数不受影响 */
async function handleReadAllAnnouncements() {
  try {
    await markAllAnnouncementRead()
    announcements.value.forEach((a) => {
      a.read = true
    })
    announcementUnread.value = 0
    // 重新查库拿最新未读数（公告 = 0，个人通知保持原样）
    await loadUnreadCounts()
    ElMessage.success('系统公告已全部标记为已读')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

function typeName(type) {
  return TYPE_NAMES[type] || '通知'
}

/**
 * 标题由后端按归还方式下发（用户自己归还 / 管理员强制归还），
 * 这里按 type 兜底，保证老数据或空标题也能显示对应文案
 */
function noticeTitle(item) {
  return item.title || TYPE_TITLES[item.type] || '通知'
}

function typeIcon(type) {
  return TYPE_ICONS[type] || 'Bell'
}

function typeColor(type) {
  return TYPE_COLORS[type] || '#4f6ef7'
}

/* ==================== 意见反馈 ==================== */

async function loadFeedbacks() {
  feedbackLoading.value = true
  try {
    const res = await getMyFeedbacks()
    feedbackList.value = res.data || []
  } catch (e) {
    feedbackList.value = []
  } finally {
    feedbackLoading.value = false
  }
}

function resetFeedbackForm() {
  feedbackForm.type = 0
  feedbackForm.title = ''
  feedbackForm.content = ''
  feedbackRef.value?.clearValidate()
}

async function handleSubmitFeedback() {
  try {
    await feedbackRef.value.validate()
  } catch (e) {
    return
  }
  feedbackSubmitting.value = true
  try {
    await submitFeedback(feedbackForm)
    ElMessage.success('反馈提交成功，我们会尽快处理')
    resetFeedbackForm()
    loadFeedbacks()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    feedbackSubmitting.value = false
  }
}
</script>

<style scoped>
.tab-badge {
  margin-left: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-8px);
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

/* 我的通知：类型筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.filter-label {
  font-size: 13.5px;
  color: var(--cls-text-secondary);
}

/* 意见反馈 Tab */
.feedback-pane {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feedback-block {
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius);
  padding: 16px 18px;
  background: var(--cls-card);
}

.pane-sub-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cls-text-main);
  margin-bottom: 14px;
}

.feedback-form {
  max-width: 620px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 60px;
}

/* 每条反馈记录：和通知项一样带外边框，列表视觉统一（边框色已过色差保护） */
.feedback-item {
  padding: 12px 14px;
  border-radius: 0;
  background: var(--cls-card-soft);
  border: 1px solid var(--card-border);
}

.fb-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fb-type {
  flex-shrink: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--cls-primary-ink);
  background: rgba(var(--cls-primary-rgb), 0.14);
  padding: 2px 8px;
  border-radius: 0;
}

.fb-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--cls-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fb-content {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 管理员回复：紫色系与「反馈回复」通知类型同色 */
.fb-reply {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 0;
  background: var(--fill);
  color: var(--text-highlight);
  font-size: 13px;
  line-height: 1.7;
}

/* 所有通知项统一外边框（--card-border 已过色差保护），
   不再出现「有的有框、有的没框」的情况 */
.notice-item {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--cls-radius);
  border: 1px solid var(--card-border);
  background: var(--cls-card);
  transition: all 0.2s ease;
  cursor: pointer;
}

.notice-item:hover {
  border-color: var(--card-border-hover);
  box-shadow: var(--shadow);
}

/* 公告：卡片底色 + 主题色淡染，深色主题下不再用浅色渐变 */
.notice-item.notice-announcement {
  cursor: pointer;
  background: linear-gradient(90deg, rgba(var(--cls-primary-rgb), 0.12) 0%, var(--cls-card) 60%);
}

/* 未读：主色淡染底 + 主色描边，跟已读区分开（描边给足不透明度，保证看得出框） */
.notice-item.unread {
  background: linear-gradient(90deg, rgba(var(--cls-primary-rgb), 0.14) 0%, var(--cls-card) 70%);
  border-color: rgba(var(--cls-primary-rgb), 0.85);
}

/* 管理员待激活 / 转换失败等需要重点提醒的通知：红色加粗 */
.notice-item.alert-notice {
  background: linear-gradient(90deg, var(--cls-danger-soft) 0%, var(--card-bg) 65%);
  border-color: var(--cls-danger-border);
}

.notice-item.alert-notice .notice-head h3,
.notice-item.alert-notice .notice-content {
  color: var(--cls-danger);
  font-weight: 700;
}

.notice-icon {
  width: 38px;
  height: 38px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--btn-text);
  background: var(--btn-bg);
}

.notice-announcement .notice-icon {
  background: var(--btn-hover-bg);
}

/* 各类型颜色由 TYPE_COLORS 通过行内样式下发，这里不再按 type 写死 */

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.notice-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-head .time {
  font-size: 12px;
  color: var(--cls-text-secondary);
  flex-shrink: 0;
}

.notice-content {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.notice-footer {
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--cls-text-secondary);
}

.unread-dot {
  position: absolute;
  top: 18px;
  right: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--cls-danger);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
