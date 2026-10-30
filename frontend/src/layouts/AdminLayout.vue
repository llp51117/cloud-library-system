<template>
  <div class="admin-layout">
    <!-- 顶部导航栏 -->
    <header class="topbar">
      <div class="topbar-inner cls-container">
        <div class="brand">
          <div class="logo">
            <el-icon :size="20"><DataBoard /></el-icon>
          </div>
          <div class="brand-text">
            <div class="name">云图书管理系统</div>
            <div class="slogan">管理员控制台</div>
          </div>
        </div>

        <div class="user-area">
          <el-tag size="small" effect="dark" round :type="isSuperAdmin ? 'danger' : 'warning'">
            {{ isSuperAdmin ? '最高管理员' : '管理员' }}
          </el-tag>

          <!-- 超管：旋转齿轮 + 魂环；普通管理员：个人头像 + 魂环 -->
          <RoleAvatar
            :size="30"
            class="clickable-avatar"
            :avatar="userStore.avatar"
            :role="userStore.userInfo?.role || 1"
            :super-admin="userStore.userInfo?.isSuperAdmin || 0"
            :nickname="displayName"
            :title="isSuperAdmin ? '最高管理员' : '点击进入个人信息'"
            @click="handleAvatarClick"
          />

          <!-- 普通管理员：高风险操作需要提交申请，由超管审批 -->
          <el-button v-if="!isSuperAdmin" text class="apply-btn" @click="applyVisible = true">
            <el-icon><DocumentAdd /></el-icon>
            <span>操作申请</span>
          </el-button>

          <span class="email">{{ displayName }}</span>
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </div>
    </header>

    <!-- Tab 导航栏 -->
    <nav class="tabbar">
      <div class="tabbar-inner cls-container">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="tab-item"
          :class="{ active: route.path === tab.path }"
          @click="router.push(tab.path)"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <!-- 待处理红点：用户管理 = 待回复反馈数；通知公告 = 待审批操作申请数 -->
          <el-badge
            v-if="tabBadge(tab.path) > 0"
            :value="tabBadge(tab.path)"
            :max="99"
            class="tab-badge"
          />
        </div>
      </div>
    </nav>

    <!-- 内容区域：子页面处理完待办后 emit refresh-admin-badges，这里立刻重算红点 -->
    <main class="content cls-container">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" @refresh-admin-badges="loadBadgeCounts" />
        </transition>
      </router-view>
    </main>

    <!-- 操作申请弹窗（普通管理员专用） -->
    <ApplyDialog v-model="applyVisible" @submitted="onApplied" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout as logoutApi, adminGetPendingFeedbackCount, adminGetPendingApplicationCount } from '@/api'
import { applyTheme } from '@/utils/themes'
import RoleAvatar from '@/components/RoleAvatar.vue'
import ApplyDialog from '@/components/ApplyDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 顶栏展示的用户名：没有昵称时兜底显示 admin */
const displayName = computed(() => userStore.nickname || 'admin')

/** 是否最高管理员（超管）：能设置/取消管理员、审批操作申请 */
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

/** 操作申请弹窗（只有普通管理员看得到入口） */
const applyVisible = ref(false)

/* ==================== Tab 待处理红点 ====================
 * 用户管理 → 待回复的用户反馈数（feedback.status = 0）
 * 通知公告 → 待审批的操作申请数（application.status = 0）
 * 两个数量都实时查库，页面上处理完一条会 emit refresh-admin-badges 重新统计
 */
/** 待回复反馈数（用户管理 Tab 红点） */
const pendingFeedback = ref(0)
/** 待审批操作申请数（通知公告 Tab 红点） */
const pendingApplication = ref(0)

/** 每个 Tab 对应的红点数量，没有就返回 0（el-badge 用 v-if 收掉） */
function tabBadge(path) {
  if (path === '/admin/users') return pendingFeedback.value
  if (path === '/admin/announcements') return pendingApplication.value
  return 0
}

async function loadBadgeCounts() {
  try {
    const res = await adminGetPendingFeedbackCount()
    pendingFeedback.value = Number(res.data) || 0
  } catch (e) {
    /* 忽略数量加载失败，不影响页面使用 */
  }
  try {
    const res = await adminGetPendingApplicationCount()
    pendingApplication.value = Number(res.data) || 0
  } catch (e) {
    /* 忽略数量加载失败，不影响页面使用 */
  }
}

// 换页面时顺手刷新一次，进来就能看到最新的待处理数量
watch(() => route.path, loadBadgeCounts)

/**
 * 点击自己的头像：普通管理员直接进「个人信息」页；
 * 超管不公开个人信息（也没有个人信息 Tab），不做跳转。
 */
function handleAvatarClick() {
  if (isSuperAdmin.value) return
  router.push('/admin/profile')
}

const ALL_TABS = [
  { path: '/admin/dashboard', label: '数据概览', icon: 'DataLine' },
  { path: '/admin/books', label: '图书管理', icon: 'Reading' },
  { path: '/admin/users', label: '用户管理', icon: 'UserFilled' },
  { path: '/admin/borrows', label: '借阅管理', icon: 'Tickets' },
  { path: '/admin/comments', label: '评论管理', icon: 'ChatLineSquare' },
  { path: '/admin/announcements', label: '通知公告', icon: 'Bell' },
  { path: '/admin/profile', label: '个人信息', icon: 'User' }
]

/** 「个人信息」只对普通管理员显示：超管是系统内置账号，不需要维护个人资料 */
const tabs = computed(() =>
  isSuperAdmin.value ? ALL_TABS.filter((tab) => tab.path !== '/admin/profile') : ALL_TABS
)

/** 申请提交后：顺便刷新 Tab 上的待审批红点数量 */
function onApplied() {
  loadBadgeCounts()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await logoutApi()
  } catch (e) {
    /* 忽略退出接口异常 */
  }
  userStore.clear()
  ElMessage.success('已安全退出')
  router.replace('/login')
}

/* ==================== 管理端固定皮肤：极光圣殿白 ====================
 * 管理端不跟随用户端主题，也不提供任何换肤入口，永远保持这一套：
 *   纯白背景 + 纯白卡片 / 表格 + 浅灰边框 + 深灰文字 + 系统蓝按钮 / 选中态，
 *   没有深色渐变、没有半透明毛玻璃、没有发光特效。
 * 做法是：进管理端时直接把「极光圣殿白」这套变量铺到 <html> 上（只改样式，
 * 不动用户端保存的主题数据），离开管理端时再把用户端那套主题还原回去。
 */
const ADMIN_THEME_KEY = 'aurora-white'
/** 点缀色（按钮 / Tab 选中 / 高亮 / 图表）：系统蓝 */
const ADMIN_ACCENT = '#3B82F6'

/** 在「极光圣殿白」基础上，按管理端要求再收一遍的几个色（其余全部沿用主题本身） */
const ADMIN_VARS = {
  /* 页面 / 卡片 / 表格：纯白（表头留一点浅灰区分） */
  '--cls-bg': '#FFFFFF',
  '--bg': '#FFFFFF',
  '--bg-page': 'linear-gradient(135deg, #FFFFFF 0%, #FFFFFF 100%)',
  '--cls-page-bg': 'linear-gradient(135deg, #FFFFFF 0%, #FFFFFF 100%)',
  '--cls-card': '#FFFFFF',
  '--cls-card-soft': '#FFFFFF',
  '--cls-card-solid': '#FFFFFF',
  '--card-inner': 'none',
  '--cls-card-inner': 'none',
  '--table-bg': '#FFFFFF',
  '--table-header-bg': '#F5F7FA',
  '--cls-bg-soft': '#FFFFFF',
  '--cls-topbar-bg': '#FFFFFF',

  /* 边框：浅灰 */
  '--cls-card-border': '#E5E7EB',
  '--cls-border': '#E5E7EB',
  '--border': '#E5E7EB',
  '--card-border': '#E5E7EB',
  '--border-soft': '#F1F3F5',
  '--input-border': '#E5E7EB',
  '--el-border-color': '#E5E7EB',
  '--el-border-color-light': '#EEF1F5',
  '--el-border-color-lighter': '#F1F3F5',
  '--el-border-color-extra-light': '#F7F8FA',

  /* 文字：主标题 / 正文深灰，辅助文字中灰 */
  '--cls-text-main': '#333333',
  '--text-main': '#333333',
  '--cls-text-regular': '#444444',
  '--text-regular': '#444444',
  '--cls-text-secondary': '#666666',
  '--text-sub': '#666666',
  '--cls-topbar-text': '#333333',
  '--cls-bg-soft-text': '#333333',
  '--title-color': '#333333',
  '--el-text-color-primary': '#333333',
  '--el-text-color-regular': '#444444',
  '--el-text-color-secondary': '#666666',

  /* 按钮 / 选中态：系统蓝底配白字（蓝底上白字比深字清楚） */
  '--cls-primary-ink': '#1D4ED8',
  '--text-highlight': '#1D4ED8',
  '--btn-text': '#FFFFFF',
  '--cls-on-primary': '#FFFFFF',
  '--tab-active-text': '#FFFFFF'
}

/** 把管理端这套固定配色铺到 <html> 上 */
function applyAdminTheme() {
  applyTheme(ADMIN_THEME_KEY, { accent: ADMIN_ACCENT })
  const root = document.documentElement
  Object.keys(ADMIN_VARS).forEach((name) => root.style.setProperty(name, ADMIN_VARS[name]))
}

onMounted(() => {
  /* 标记当前在管理端：全局「全站直角」规则只在 data-area="user" 时生效，
     管理端保持它原来的圆角和独立配色 */
  document.documentElement.setAttribute('data-area', 'admin')
  // 管理端固定「极光圣殿白」，不看用户端选了哪套皮肤
  applyAdminTheme()

  // 顶栏头像 / 身份标签依赖最新用户信息，进管理端时拉一次
  if (userStore.isLogin) {
    // 只拉个人信息（头像 / 身份）；拉完再刷一次管理端配色，防止被用户端主题覆盖
    userStore
      .fetchProfile()
      .catch(() => {})
      .finally(() => applyAdminTheme())
  }
  // 待处理红点：待回复反馈 / 待审批操作申请
  loadBadgeCounts()
})

/* 万一用户端在别处改了主题（比如个人信息里带回来的），管理端立刻刷回自己的固定配色 */
watch(
  () => userStore.themeKey,
  () => applyAdminTheme()
)

onBeforeUnmount(() => {
  // 离开管理端：把用户端自己的主题还原回去（用户端页面挂载时也会再拉一次，这里双保险）
  if (userStore.isLogin) {
    userStore.setThemeLocal(userStore.theme)
  }
  document.documentElement.removeAttribute('data-area')
})
</script>

<style scoped>
/* ============================================================
   管理端：固定「极光圣殿白」——纯白背景 / 纯白卡片 / 浅灰边框 / 深灰文字 /
   系统蓝按钮，不跟随用户端主题，也没有任何换肤入口（配色在 AdminLayout 的
   applyAdminTheme() 里铺到 <html> 上）。
   这里只保留「管理端自己的圆角」这一个与用户端不同的风格差异。
   ============================================================ */
.admin-layout {
  min-height: 100vh;

  /* 管理端保持原来的圆角（用户端是全站直角，这里单独恢复） */
  --cls-radius-sm: 8px;
  --cls-radius: 12px;
  --cls-radius-lg: 16px;
  --el-border-radius-base: 4px;
  --el-border-radius-small: 2px;
  --el-border-radius-round: 20px;

  /* 跟用户端同一套：主题色渐变底（--cls-page-bg 由主题实时生成） */
  background-color: var(--cls-bg);
  background-image: var(--cls-page-bg);
  background-attachment: fixed;
}

/* 卡片内部渐变（用户端那套横向渐变）在管理端关掉 */
.admin-layout :deep(.cls-card) {
  background-image: none;
}

/* 顶部导航栏：顶栏渐变 + 文字色都跟着主题走（--cls-topbar-bg / --cls-topbar-text） */
.topbar {
  background: var(--topbar-bg);
  color: var(--cls-topbar-text);
  border-bottom: 1px solid var(--card-border);
  box-shadow: var(--shadow);
}

/* 宽度由 .cls-container 控制（百分比 + 居中），与下方内容左右对齐 */
.topbar-inner {
  height: 64px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--fill);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 品牌名：用主题的标题色 */
.brand-text .name {
  font-size: 17px;
  font-weight: 600;
  color: var(--title-color);
}

.brand-text .slogan {
  font-size: 12px;
  color: var(--text-sub);
  opacity: 0.9;
}

.logo {
  background: var(--fill);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-area .email {
  font-size: 13px;
  opacity: 0.9;
}

/* 头像点击进个人信息（超管不公开个人信息，点击无动作），
   .clickable-avatar 的鼠标手势样式在 global.css 里统一定义 */
/* ===== 管理员齿轮头像样式已抽到公共组件 components/GearAvatar.vue ===== */

.user-area :deep(.el-button) {
  color: var(--cls-topbar-text);
  opacity: 0.9;
}

.user-area :deep(.el-button:hover) {
  opacity: 1;
  background: var(--fill);
}

.tabbar {
  background: var(--cls-bg-soft);
  color: var(--cls-bg-soft-text);
  box-shadow: var(--shadow);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.tabbar-inner {
  padding: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
  font-size: 14px;
  color: var(--cls-bg-soft-text);
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.tab-item:hover {
  color: var(--cls-primary);
  background: rgba(var(--cls-primary-rgb), 0.08);
}

/* 选中态：系统蓝 + 淡蓝底，干净利落，不加发光特效 */
.tab-item.active {
  color: var(--cls-primary);
  font-weight: 600;
  border-bottom-color: var(--cls-primary);
  background: rgba(var(--cls-primary-rgb), 0.1);
}

/* Tab 右上角的待处理红点 */
.tab-badge {
  margin-left: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-8px);
}

/* 内容区：百分比宽度由 .cls-container 给出（小屏 95% / 中屏 88% / 大屏 85%），居中显示 */
.content {
  padding: 20px 0 40px;
}

/* 区块之间留出明显间距 */
.content :deep(.cls-card + .cls-card) {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .brand-text .slogan,
  .user-area .email {
    display: none;
  }
}
</style>
