<template>
  <div class="user-layout">
    <!-- 第 12 套特殊主题「光影幻境」的动态背景（z-index -1，永远在内容下面，不挡任何东西） -->
    <DynamicBg v-if="isMagicBg" :image="userStore.bgUrl" />

    <!-- 顶部导航栏 -->
    <header class="topbar">
      <div class="topbar-inner cls-container">
        <div class="brand">
          <div class="logo">
            <el-icon :size="20"><Reading /></el-icon>
          </div>
          <div class="brand-text">
            <div class="name">云图书管理系统</div>
            <div class="slogan">AI 智慧图书服务平台</div>
          </div>
        </div>

        <div class="user-area">
          <!-- 点击自己的头像 / 昵称 → 个人信息页 -->
          <el-avatar :size="32" :src="avatarFull" class="user-avatar" @click="go('/user/profile')">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="email user-avatar" @click="go('/user/profile')">{{ userStore.nickname }}</span>
          <el-button text type="primary" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </div>
    </header>

    <!-- Tab 导航栏 -->
    <nav ref="tabbarRef" class="tabbar">
      <!-- 光影幻境：Tab 栏本身完全透明（直接透视背景图），
           但下面滚上来的内容会被这一层挡住 —— 它渲染的是与底层完全同步、对齐的
           同一张滚动背景，所以看起来依旧是透明的，Tab 文字也不会被内容文字打架 -->
      <div v-if="isMagicBg" class="tabbar-clip">
        <DynamicBg mode="bar" :image="userStore.bgUrl" :offset="tabbarOffset" />
      </div>
      <div class="tabbar-inner cls-container">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="tab-item"
          :class="{ active: isActive(tab.path) }"
          @click="go(tab.path)"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <el-badge
            v-if="tab.path === '/user/notifications' && unreadCount > 0"
            :value="unreadCount"
            :max="99"
            class="tab-badge"
          />
        </div>
      </div>
    </nav>

    <!-- 内容区域 -->
    <main class="content cls-container">
      <!-- 错误兜底：任何子页面渲染抛错，这里接住并给出提示 / 重试，而不是整块白屏 -->
      <div v-if="renderError" class="render-fallback">
        <p class="render-fallback-title">页面渲染出错了</p>
        <p class="render-fallback-msg">{{ renderError }}</p>
        <el-button type="primary" @click="retryRender">重新加载这个页面</el-button>
      </div>
      <router-view v-else v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="renderKey" @refresh-unread="loadUnread" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, onBeforeUnmount, onErrorCaptured, ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { MAGIC_BG_KEY } from '@/utils/themes'
import DynamicBg from '@/components/DynamicBg.vue'
import { getUnreadCount, logout as logoutApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

/* ==================== 页面级错误兜底 ====================
 * 子页面（router-view 里的组件）渲染时抛错，Vue 会中断这棵子树的渲染：
 * 之后路由切换去卸载它时就会报「can't access property "component", instance.subTree is null」，
 * 用户看到的就是整块白屏。这里把错误接住、显示出来，并提供「重新加载」按钮（重新挂载子页面）。 */
const renderError = ref('')
const renderKey = ref(0)

/** 当前是不是第 12 套特殊主题「光影幻境」：是才渲染动态背景层 */
const isMagicBg = computed(() => userStore.themeKey === MAGIC_BG_KEY)

/* ==================== 光影幻境：Tab 栏遮挡层的对齐 ====================
 * Tab 栏是 sticky 的：滚动后它贴在视口顶部（offset = 0），
 * 没滚动时它在顶栏下面（offset = 顶栏高度）。
 * 遮挡层里的背景必须和整屏背景严格对齐，否则会出现一条错位的接缝。 */
const tabbarRef = ref(null)
const tabbarOffset = ref(0)

function syncTabbarOffset() {
  if (!isMagicBg.value || !tabbarRef.value) return
  const rect = tabbarRef.value.getBoundingClientRect()
  tabbarOffset.value = Math.max(0, Math.round(rect.top))
}

onErrorCaptured((err) => {
  renderError.value = err && err.message ? err.message : String(err)
  console.error('[页面渲染出错]', err)
  return false
})

/* 换页面时清掉错误状态，正常跳转后自动恢复 */
watch(
  () => route.fullPath,
  () => {
    renderError.value = ''
  }
)

function retryRender() {
  renderError.value = ''
  renderKey.value += 1
}

const tabs = [
  { path: '/user/home', label: '首页搜索', icon: 'Search' },
  { path: '/user/ai-chat', label: '智能问答', icon: 'ChatDotRound' },
  { path: '/user/hot', label: '热门书籍', icon: 'Trophy' },
  { path: '/user/my-books', label: '我的书籍', icon: 'Collection' },
  { path: '/user/notifications', label: '消息通知', icon: 'Bell' },
  { path: '/user/profile', label: '个人信息', icon: 'User' },
  { path: '/user/theme', label: '更换皮肤', icon: 'Brush' }
]

const avatarFull = computed(() => {
  const url = userStore.avatar
  if (!url) return ''
  return url.startsWith('http') ? url : url
})

/* 标记当前在用户端：全局「全站直角」的样式只在 data-area="user" 时生效，
   管理端保持它原来的圆角与配色 */
onMounted(() => {
  document.documentElement.setAttribute('data-area', 'user')
})

onBeforeUnmount(() => {
  document.documentElement.removeAttribute('data-area')
})

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function go(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

async function loadUnread() {
  try {
    const res = await getUnreadCount()
    // 后端返回 { notification, announcement, total }，红点显示合计
    unreadCount.value = Number(res.data?.total ?? 0)
  } catch (e) {
    /* 忽略未读数量加载失败 */
  }
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

watch(() => route.path, loadUnread)

/** 每 30 秒重新查一次数据库，保证红点始终是最新未读数 */
let unreadTimer = null

onMounted(() => {
  loadUnread()
  unreadTimer = setInterval(loadUnread, 30000)
  // 登录后从数据库读取主题并应用（主题跟账号绑定，不存 localStorage）
  if (userStore.isLogin) {
    userStore.loadTheme()
  }
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.fetchProfile().catch(() => {})
  }
  // 光影幻境：刷新后也要重新采样一次背景图亮度，决定文字用白色还是深色
  userStore.measureBgBrightness().catch(() => {})
  // 光影幻境：Tab 栏遮挡层要跟着滚动 / 窗口变化对齐
  syncTabbarOffset()
  window.addEventListener('scroll', syncTabbarOffset, { passive: true })
  window.addEventListener('resize', syncTabbarOffset)
})

/* 切到 / 切出「光影幻境」时立刻重新对齐（DOM 更新后量高度） */
watch(isMagicBg, () => {
  nextTick(syncTabbarOffset)
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
  window.removeEventListener('scroll', syncTabbarOffset)
  window.removeEventListener('resize', syncTabbarOffset)
})
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  /* 页面底：跟 body 一样跟随主题（--cls-page-bg 由 applyTheme 整体重算），
     保证滚动区域也有层次，切主题时整屏一起换 */
  background-color: transparent;
  background-image: var(--cls-page-bg);
  background-attachment: fixed;
}

/* 顶部导航栏：顶栏渐变 + 文字色都由主题实时生成（--cls-topbar-bg / --cls-topbar-text） */
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
  border-radius: 0;
  background: var(--fill);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text .name {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--title-color);
}

.brand-text .slogan {
  font-size: 12px;
  opacity: 0.82;
  color: var(--text-sub);
}

.logo {
  background: var(--fill);
  box-shadow: 0 0 12px var(--card-glow);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-area .email {
  font-size: 13px;
  opacity: 0.95;
}

/* 点击自己的头像 / 昵称 → 个人信息页 */
.user-area .user-avatar {
  cursor: pointer;
}


.user-area :deep(.el-button) {
  color: var(--cls-topbar-text);
  opacity: 0.92;
}

.user-area :deep(.el-button:hover) {
  opacity: 1;
  color: var(--text-highlight);
  background: var(--fill);
}

/* 光影幻境：Tab 栏遮挡层 —— 铺满 Tab 栏、压在 Tab 文字之下（z-index -1），
   因为 Tab 栏自己 z-index 1300，这一层一定在所有页面内容之上 → 下面滚上来的内容
   会被它挡住（裁剪掉），而它显示的就是背景图本身，所以看着就是「完全透明」 */
.tabbar-clip {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

/* 光影幻境：Tab 栏彻底透明（不加底色、不加毛玻璃，背景图 100% 透出），
   但层级给到最高，保证 Tab 文字永远在最上面、不被任何内容压住 */
html[data-magic-bg='true'] .tabbar {
  background: transparent;
  box-shadow: none;
  border-bottom-color: transparent;
  z-index: 1300;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

html[data-magic-bg='true'] .tab-item {
  text-shadow: var(--magic-text-shadow, none);
}

/* Tab 导航栏：底色 + 文字色都由主题实时生成（--cls-bg-soft / --cls-bg-soft-text） */
.tabbar {
  background: var(--cls-bg-soft);
  color: var(--cls-bg-soft-text);
  box-shadow: var(--shadow);
  border-bottom: 1px solid var(--card-border);
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
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
  font-size: 14px;
  /* 未选中：次级文字色 */
  color: var(--tab-inactive-text);
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, background-color 0.2s, border-color 0.2s;
}

.tab-item:hover {
  color: var(--text-main);
  background: var(--tab-inactive-bg);
  background-color: var(--fill);
}

/* 选中态：Tab 选中色实心填充（默认主色，自定义主题用分配到的颜色）+ 文字色自动匹配 */
.tab-item.active {
  color: var(--tab-active-text);
  font-weight: 600;
  background: var(--tab-active-bg);
  border-bottom-color: var(--tab-active-bg);
  box-shadow: 0 0 12px var(--card-glow);
}

.tab-badge {
  margin-left: 6px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateX(0) translateY(-10px);
}

/* 内容区域：百分比宽度由 .cls-container 给出（小屏 95% / 中屏 88% / 大屏 85%），居中显示 */
.content {
  padding: 20px 0 40px;
}

/* 页面渲染出错时的兜底面板（替代白屏） */
.render-fallback {
  margin-top: 24px;
  padding: 32px 24px;
  text-align: center;
  border: 1px solid var(--cls-card-border);
  background: var(--cls-card-solid);
  color: var(--cls-text-main);
}

.render-fallback-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--cls-primary-ink);
}

.render-fallback-msg {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--cls-text-secondary);
  word-break: break-all;
}

@media (max-width: 768px) {
  .brand-text .slogan,
  .user-area .email {
    display: none;
  }
}
</style>
