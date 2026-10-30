import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { applyGuestTheme } from '@/utils/themes'

const routes = [
  { path: '/', redirect: '/login' },

  /* ---------- 登录认证页面（3 个独立页面） ---------- */
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { guest: true } },
  { path: '/forgot', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { guest: true } },

  /* ---------- 用户端（顶部 7 个 Tab + 图书详情） ---------- */
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    redirect: '/user/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/user/Home.vue'), meta: { title: '首页搜索' } },
      { path: 'ai-chat', name: 'AiChat', component: () => import('@/views/user/AiChat.vue'), meta: { title: '智能问答' } },
      { path: 'hot', name: 'HotRank', component: () => import('@/views/user/HotRank.vue'), meta: { title: '热门书籍' } },
      { path: 'my-books', name: 'MyBooks', component: () => import('@/views/user/MyBooks.vue'), meta: { title: '我的书籍' } },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/user/Notifications.vue'), meta: { title: '消息通知' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人信息' } },
      /* 更换皮肤（主题选择，主题存数据库、跟账号绑定） */
      { path: 'theme', name: 'ThemeSwitch', component: () => import('@/views/user/ThemeSwitch.vue'), meta: { title: '更换皮肤' } },
      { path: 'book/:id', name: 'BookDetail', component: () => import('@/views/user/BookDetail.vue'), meta: { title: '图书详情', hideTab: true } },
      { path: 'rank-books', name: 'RankBooks', component: () => import('@/views/user/RankBooks.vue'), meta: { title: '维度书单', hideTab: true } },
      /* 用户个人主页（评论区点头像 / 昵称进入，只展示公开信息） */
      { path: 'u/:id', name: 'UserHomepage', component: () => import('@/views/user/UserHomepage.vue'), meta: { title: '个人主页', hideTab: true } }
    ]
  },

  /* ---------- 管理端（顶部 6 个 Tab） ---------- */
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { admin: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据概览' } },
      { path: 'books', name: 'BookManage', component: () => import('@/views/admin/BookManage.vue'), meta: { title: '图书管理' } },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'borrows', name: 'BorrowManage', component: () => import('@/views/admin/BorrowManage.vue'), meta: { title: '借阅管理' } },
      { path: 'comments', name: 'CommentManage', component: () => import('@/views/admin/CommentManage.vue'), meta: { title: '评论管理' } },
      { path: 'book/:id', name: 'AdminBookDetail', component: () => import('@/views/user/BookDetail.vue'), meta: { title: '评论详情', hideTab: true } },
      { path: 'announcements', name: 'AnnouncementManage', component: () => import('@/views/admin/AnnouncementManage.vue'), meta: { title: '通知公告' } },
      /* 管理员的「个人信息」（含最下面的「我的书籍」，仅自己可见） */
      { path: 'profile', name: 'AdminProfile', component: () => import('@/views/user/Profile.vue'), meta: { title: '个人信息' } },
      /* 管理端内查看用户个人主页 */
      { path: 'u/:id', name: 'AdminUserHomepage', component: () => import('@/views/user/UserHomepage.vue'), meta: { title: '个人主页', hideTab: true } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 全局前置守卫：登录校验 + 角色校验
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLogin = userStore.isLogin

  // 登录 / 注册 / 忘记密码：用户还没登录，不知道他的主题，一律用初始色（量子脉冲蓝）
  if (to.meta.guest) {
    applyGuestTheme()
  }

  // 已登录用户访问登录/注册页 → 回到各自的首页
  if (to.meta.guest && isLogin) {
    return next(userStore.isAdmin ? '/admin/dashboard' : '/user/home')
  }

  // 需要登录的页面
  if (!to.meta.guest && !isLogin) {
    return next('/login')
  }

  // 管理端页面需要管理员角色
  if (to.matched.some((r) => r.meta.admin) && !userStore.isAdmin) {
    return next('/user/home')
  }

  // 「个人信息」只对普通管理员开放：超管访问时回到数据概览
  if (to.path === '/admin/profile' && userStore.isSuperAdmin) {
    return next('/admin/dashboard')
  }

  next()
})

export default router
