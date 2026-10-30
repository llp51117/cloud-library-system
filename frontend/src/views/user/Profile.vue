<template>
  <div class="profile-page">
    <!-- 左侧栏 -->
    <aside class="sidebar">
      <!-- 头像卡片 -->
      <div class="cls-card avatar-card">
        <div class="avatar-box">
          <!-- 外圈彩色环：普通用户主题色（浅蓝 → 淡紫渐变）；点头像本身也能打开更换头像弹窗 -->
          <div
            class="avatar-ring clickable-avatar"
            title="点击更换头像"
            @click="cropperVisible = true"
          >
            <el-avatar :size="110" :src="profile.avatar" fit="cover" :style="avatarStyle">
              <span class="avatar-letter">{{ avatarLetter }}</span>
            </el-avatar>
          </div>
        </div>
        <div class="nickname">
          <span>{{ profile.nickname }}</span>
          <el-icon class="edit-icon" :size="13" @click="openNicknameEdit"><EditPen /></el-icon>
        </div>
        <div class="role-row">
          <el-tag :type="roleTagType" size="small" effect="light" class="role-tag">{{ roleText }}</el-tag>
          <!-- 管理员封号：红色小标签，由最高管理员设置 -->
          <el-tag v-if="profile.adminTitle" type="danger" size="small" effect="light">
            {{ profile.adminTitle }}
          </el-tag>
        </div>

        <!-- 待激活管理员：显示还书截止时间 -->
        <div v-if="isPendingAdmin" class="pending-admin-tip">
          <el-icon><WarningFilled /></el-icon>
          <span>
            管理员权限待激活：请在
            <b>{{ formatDateTime(profile.pendingAdminDeadline) }}</b>
            前归还全部图书，归还完成后自动转正；逾期未还完将恢复为普通用户。
          </span>
        </div>

        <el-button type="primary" plain class="upload-btn" @click="cropperVisible = true">
          <el-icon><Upload /></el-icon>
          <span>更换头像</span>
        </el-button>
        <div class="avatar-tip">支持旋转、缩放与选区裁剪</div>
      </div>

      <!-- 账号信息 -->
      <div class="cls-card account-card">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>账号信息</span>
          </div>
        </div>

        <div class="info-list">
          <div class="info-row">
            <span class="info-label">用户ID</span>
            <span class="info-value">{{ profile.id }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ profile.emailMasked || profile.email }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ formatDateTime(profile.createdAt) }}</span>
          </div>
          <div class="info-row last">
            <span class="info-label">账号状态</span>
            <el-tag :type="statusType" size="small" effect="light">{{ statusText }}</el-tag>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧栏 -->
    <div class="main">
      <!-- 修改邮箱 -->
      <div class="cls-card email-card">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>修改邮箱</span>
            <span class="cls-sub-title">需验证登录密码 + 当前邮箱验证码</span>
          </div>
        </div>

        <div class="email-current">
          <span class="email-label">当前邮箱</span>
          <span class="email-value">{{ verifiedEmail || profile.emailMasked || profile.email }}</span>
          <el-tag v-if="!verifiedEmail" type="info" size="small" effect="light">已打码</el-tag>
        </div>

        <!-- 第一步：验证登录密码 -->
        <el-form
          v-if="!verifiedEmail"
          ref="emailVerifyRef"
          :model="emailVerifyForm"
          :rules="emailVerifyRules"
          label-width="90px"
          class="email-form"
        >
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="emailVerifyForm.password"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入登录密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="verifying" @click="handleVerifyPassword">
              验证身份
            </el-button>
            <span class="form-tip">验证通过后才会显示完整邮箱</span>
          </el-form-item>
        </el-form>

        <!-- 第二步：填写新邮箱与验证码 -->
        <el-form
          v-else
          ref="emailChangeRef"
          :model="emailChangeForm"
          :rules="emailChangeRules"
          label-width="90px"
          class="email-form"
        >
          <el-form-item label="新邮箱" prop="newEmail">
            <el-input v-model="emailChangeForm.newEmail" placeholder="请输入新邮箱" />
          </el-form-item>
          <el-form-item label="邮箱验证码" prop="code">
            <div class="code-input">
              <el-input v-model="emailChangeForm.code" placeholder="请输入验证码" maxlength="6" />
              <el-button
                :disabled="sendCodeDisabled"
                :loading="sendingCode"
                @click="handleSendEmailCode"
              >
                {{ sendCodeText }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="emailSubmitting" @click="handleUpdateEmail">
              确认修改邮箱
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码 -->
      <div class="cls-card pwd-card">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>修改密码</span>
            <span class="cls-sub-title">修改成功后需重新登录</span>
          </div>
          <router-link to="/forgot" class="forgot-link">忘记密码？用邮箱验证码重置</router-link>
        </div>

        <el-form
          ref="pwdRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-width="90px"
          class="pwd-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入原密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请设置 6-20 位新密码"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="pwdForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入新密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="pwdSubmitting" @click="handleUpdatePassword">
              确认修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 我的书籍：管理员的历史借阅记录，放在个人信息页最下面，仅自己可见；默认收起，点「展开」才显示 -->
    <div v-if="isAdminUser" class="cls-card my-books-card">
      <div class="cls-section-title" :class="{ 'no-body': !booksExpanded }">
        <div class="title-left">
          <span class="bar"></span>
          <span>我的书籍</span>
          <span class="cls-sub-title">借阅记录，仅自己可见（共 {{ bookGroups.length }} 本）</span>
        </div>
        <div class="books-actions">
          <el-button plain size="small" @click="toggleBooks">
            <el-icon>
              <component :is="booksExpanded ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
            <span>{{ booksExpanded ? '收起' : '展开' }}</span>
          </el-button>
          <el-button v-if="booksExpanded" plain size="small" @click="loadMyBooks">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
        </div>
      </div>

      <el-collapse-transition>
        <div v-show="booksExpanded">
          <el-table v-loading="booksLoading" :data="bookGroups" stripe>
            <el-table-column prop="bookTitle" label="书名" min-width="220" show-overflow-tooltip />
            <el-table-column label="作者" min-width="140">
              <template #default="{ row }">{{ row.bookAuthor || '-' }}</template>
            </el-table-column>
            <el-table-column label="最早借阅时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.firstBorrowDate) }}</template>
            </el-table-column>
            <el-table-column label="最早归还时间" width="170">
              <template #default="{ row }">
                {{ row.firstReturnDate ? formatDateTime(row.firstReturnDate) : '未归还' }}
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">暂无借阅记录</div>
            </template>
          </el-table>
        </div>
      </el-collapse-transition>
    </div>

    <!-- 修改昵称 -->
    <el-dialog v-model="nicknameVisible" title="修改昵称" width="420px" :close-on-click-modal="false">
      <el-form ref="nicknameRef" :model="nicknameForm" :rules="nicknameRules" label-width="70px">
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="nicknameForm.nickname"
            maxlength="50"
            show-word-limit
            placeholder="留空则恢复为邮箱前缀"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nicknameVisible = false">取消</el-button>
        <el-button type="primary" :loading="nicknameSubmitting" @click="handleUpdateNickname">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 头像裁剪 -->
    <AvatarCropper v-model="cropperVisible" @confirm="handleAvatarConfirm" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import AvatarCropper from '@/components/AvatarCropper.vue'
import {
  getProfile,
  getMyBorrows,
  updateNickname,
  verifyEmailPassword,
  sendEmailChangeCode,
  updateEmail,
  updatePassword,
  uploadAvatar
} from '@/api'

const router = useRouter()
const userStore = useUserStore()

const profile = ref({})

/* ==================== 通用 ==================== */

/** 与管理端和其他页面保持一致的时间格式：YYYY-MM-DD HH:mm */
function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(String(value).trim().replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return '-'
  const p = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}`
}

async function loadProfile() {
  try {
    const res = await getProfile()
    profile.value = res.data || {}
  } catch (e) {
    /* 错误由拦截器统一提示 */
  }
}

/* ==================== 我的书籍（管理员专属，仅自己可见） ==================== */

/** 原始借阅记录（含已归还），按借阅时间倒序 */
const myBooks = ref([])
const booksLoading = ref(false)
/** 借阅记录默认收起，点「展开」才加载列表，保持页面整洁 */
const booksExpanded = ref(false)

/**
 * 按书名去重：同一本书借过多次只保留一条，
 * 取最早一次借阅时间、最早一次归还时间
 */
const bookGroups = computed(() => {
  const map = new Map()
  for (const item of myBooks.value) {
    const key = String(item.bookTitle || '').trim() || `book-${item.bookId ?? item.id}`
    const exist = map.get(key)
    if (!exist) {
      map.set(key, {
        key,
        bookTitle: item.bookTitle || '未知图书',
        bookAuthor: item.bookAuthor,
        firstBorrowDate: item.borrowDate || '',
        firstReturnDate: item.returnDate || ''
      })
      continue
    }
    if (item.borrowDate && (!exist.firstBorrowDate || String(item.borrowDate) < String(exist.firstBorrowDate))) {
      exist.firstBorrowDate = item.borrowDate
    }
    if (item.returnDate && (!exist.firstReturnDate || String(item.returnDate) < String(exist.firstReturnDate))) {
      exist.firstReturnDate = item.returnDate
    }
  }
  return [...map.values()].sort((a, b) =>
    String(b.firstBorrowDate || '').localeCompare(String(a.firstBorrowDate || ''))
  )
})

async function loadMyBooks() {
  if (!isAdminUser.value) return
  booksLoading.value = true
  try {
    const res = await getMyBorrows({ page: 1, size: 100 })
    myBooks.value = res.data.list || []
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    booksLoading.value = false
  }
}

function toggleBooks() {
  booksExpanded.value = !booksExpanded.value
  // 万一首次加载失败，展开时补拉一次
  if (booksExpanded.value && !myBooks.value.length && !booksLoading.value) {
    loadMyBooks()
  }
}

onMounted(async () => {
  await loadProfile()
  // 先拉一次借阅记录，标题栏「共 N 本」才有数据（列表默认收起，不占版面）
  loadMyBooks()
})

/** 身份文案：最高管理员 / 管理员 / 待激活管理员 / 普通用户 */
const roleText = computed(() => {
  if (profile.value.isSuperAdmin === 1) return '最高管理员'
  if (profile.value.role === 1) {
    return profile.value.pendingAdmin === 1 ? '待激活管理员' : '管理员'
  }
  return '普通用户'
})

const roleTagType = computed(() => {
  if (profile.value.isSuperAdmin === 1) return 'danger'
  if (profile.value.role === 1) return 'warning'
  return 'primary'
})

/** 待激活管理员：提示还书截止时间 */
const isPendingAdmin = computed(
  () => profile.value.role === 1 && profile.value.pendingAdmin === 1
)

/** 管理员（含待激活）才有「我的书籍」，且只在自己个人信息页可见 */
const isAdminUser = computed(() => profile.value.role === 1)

const statusText = computed(() => {
  const map = { 0: '注销', 1: '正常', 2: '禁用' }
  return map[profile.value.status] ?? '未知'
})

const statusType = computed(() => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[profile.value.status] ?? 'info'
})

/* ==================== 头像 ==================== */

/**
 * 字母头像配色池：按昵称稳定取色，每个用户颜色不同（类似 Gmail 字母头像）。
 * 上传过头像的用户显示图片，没上传的显示首字母 + 渐变底色，不再是灰色默认头像。
 */
const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #4f6ef7, #6a8bff)', // 靛蓝
  'linear-gradient(135deg, #8b5cf6, #a78bfa)', // 紫
  'linear-gradient(135deg, #2fb87f, #5ed6a5)', // 绿
  'linear-gradient(135deg, #f5a623, #ffc766)', // 橙
  'linear-gradient(135deg, #f0513f, #ff8a7a)', // 红
  'linear-gradient(135deg, #2f7ff0, #6ab7ff)', // 蓝
  'linear-gradient(135deg, #ec4899, #f9a8d4)', // 粉
  'linear-gradient(135deg, #14b8a6, #5eead4)' //  青
]

function hashString(text) {
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = (hash << 5) - hash + text.charCodeAt(i)
    hash |= 0
  }
  return hash
}

/** 没上传头像时展示的首字母（取昵称首字符，没有昵称则取邮箱首字符） */
const avatarLetter = computed(() => {
  const name = String(profile.value.nickname || profile.value.email || 'U').trim()
  return name.charAt(0).toUpperCase()
})

/** 没上传头像时的彩色渐变底色，同一个用户始终同色 */
const avatarStyle = computed(() => {
  const seed = String(profile.value.nickname || profile.value.email || 'user')
  const gradient = AVATAR_GRADIENTS[Math.abs(hashString(seed)) % AVATAR_GRADIENTS.length]
  return { background: gradient }
})

const cropperVisible = ref(false)

async function handleAvatarConfirm(file) {
  try {
    await uploadAvatar(file)
    ElMessage.success('头像上传成功')
    // 上传成功后自动关闭裁剪弹窗，不用再手动点 ×
    cropperVisible.value = false
    await userStore.fetchProfile()
    await loadProfile()
  } catch (e) {
    /* 错误由拦截器统一提示，弹窗保持打开方便重试 */
  }
}

/* ==================== 昵称 ==================== */

const nicknameVisible = ref(false)
const nicknameSubmitting = ref(false)
const nicknameRef = ref()
const nicknameForm = reactive({ nickname: '' })
const nicknameRules = {
  nickname: [{ max: 50, message: '昵称不能超过 50 个字符', trigger: 'blur' }]
}

function openNicknameEdit() {
  nicknameForm.nickname = profile.value.nickname || ''
  nicknameVisible.value = true
}

async function handleUpdateNickname() {
  try {
    await nicknameRef.value.validate()
  } catch (e) {
    return
  }
  nicknameSubmitting.value = true
  try {
    await updateNickname(nicknameForm.nickname)
    ElMessage.success('昵称已更新')
    nicknameVisible.value = false
    await userStore.fetchProfile()
    await loadProfile()
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    nicknameSubmitting.value = false
  }
}

/* ==================== 修改邮箱 ==================== */

const emailVerifyRef = ref()
const emailVerifyForm = reactive({ password: '' })
const emailVerifyRules = {
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

const verifiedEmail = ref('')
const verifying = ref(false)

async function handleVerifyPassword() {
  try {
    await emailVerifyRef.value.validate()
  } catch (e) {
    return
  }
  verifying.value = true
  try {
    const res = await verifyEmailPassword(emailVerifyForm.password)
    verifiedEmail.value = res.data || ''
    ElMessage.success('身份验证通过')
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    verifying.value = false
  }
}

const emailChangeRef = ref()
const emailChangeForm = reactive({ newEmail: '', code: '' })
const emailChangeRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

const sendCodeDisabled = computed(() => sendingCode.value || codeCountdown.value > 0)
const sendCodeText = computed(() => (codeCountdown.value > 0 ? `${codeCountdown.value}s 后重试` : '获取验证码'))

async function handleSendEmailCode() {
  sendingCode.value = true
  try {
    await sendEmailChangeCode()
    ElMessage.success(`验证码已发送至 ${verifiedEmail.value}`)
    codeCountdown.value = 60
    codeTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(codeTimer)
        codeTimer = null
      }
    }, 1000)
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    sendingCode.value = false
  }
}

const emailSubmitting = ref(false)

async function handleUpdateEmail() {
  try {
    await emailChangeRef.value.validate()
  } catch (e) {
    return
  }
  emailSubmitting.value = true
  try {
    await updateEmail({ email: emailChangeForm.newEmail, code: emailChangeForm.code })
    ElMessage.success('邮箱修改成功')
    emailChangeForm.newEmail = ''
    emailChangeForm.code = ''
    verifiedEmail.value = ''
    emailVerifyForm.password = ''
    await userStore.fetchProfile()
    await loadProfile()
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    emailSubmitting.value = false
  }
}

/* ==================== 修改密码 ==================== */

const pwdRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdSubmitting = ref(false)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleUpdatePassword() {
  try {
    await pwdRef.value.validate()
  } catch (e) {
    return
  }
  pwdSubmitting.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clear()
    router.replace('/login')
  } catch (e) {
    /* 错误由拦截器统一提示 */
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
  /* 两列等高拉伸，底部卡片再各自撑满本列剩余高度，
     这样「账号信息」（左）与「修改密码」（右）的底部始终对齐 */
  align-items: stretch;
}

.sidebar,
.main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 每列最后一张卡片撑满剩余高度 → 左右两列底部对齐 */
.sidebar > .account-card,
.main > .pwd-card {
  flex: 1 1 auto;
}

/* 我的书籍：横跨左右两列，排在个人信息页最下面 */
.my-books-card {
  grid-column: 1 / -1;
}

/* 标题栏右侧按钮组：展开/收起 + 刷新 */
.books-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 收起状态：标题栏不再留 16px 下边距，卡片只剩一行标题 */
.cls-section-title.no-body {
  margin-bottom: 0;
}

/* 待激活管理员提示 */
.pending-admin-tip {
  margin-top: 12px;
  padding: 8px 10px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
  font-size: 12px;
  line-height: 1.7;
  color: var(--cls-warning);
  background: var(--cls-warning-soft);
  border: 1px solid var(--cls-warning-border);
  border-radius: var(--cls-radius-sm);
}

.pending-admin-tip b {
  color: var(--cls-danger);
}

/* 头像卡片 */
.avatar-card {
  text-align: center;
}

.avatar-box {
  display: flex;
  justify-content: center;
}

/* 头像外圈彩色环：普通用户主题色（浅蓝 → 淡紫渐变） */
.avatar-ring {
  width: 126px;
  height: 126px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--cls-primary-light) 0%, var(--cls-primary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-ring :deep(.el-avatar) {
  border: 3px solid var(--card-bg-solid);
}

/* 点头像可打开「更换头像」弹窗：手型 + 轻微提亮（.clickable-avatar 的手势在 global.css） */
.avatar-ring.clickable-avatar {
  transition: filter 0.2s ease;
}

.avatar-ring.clickable-avatar:hover {
  filter: brightness(1.06);
}

/* 未上传头像时展示的彩色字母（渐变底色由 avatarStyle 按昵称生成） */
.avatar-letter {
  font-size: 42px;
  font-weight: 600;
  color: var(--btn-text);
  line-height: 1;
  user-select: none;
}

.nickname {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--cls-text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.edit-icon {
  color: var(--cls-primary-ink);
  cursor: pointer;
}

.edit-icon:hover {
  color: var(--cls-primary-ink);
}

.role-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.role-tag {
  margin-top: 0;
}

.upload-btn {
  margin-top: 16px;
  width: 100%;
}

.avatar-tip {
  margin-top: 10px;
  font-size: 12px;
  color: var(--cls-text-secondary);
}

/* 账号信息 */
.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--cls-border);
  font-size: 13.5px;
}

.info-row.last {
  border-bottom: none;
}

.info-label {
  width: 70px;
  flex-shrink: 0;
  color: var(--cls-text-secondary);
}

.info-value {
  flex: 1;
  color: var(--cls-text-main);
  word-break: break-all;
}

/* 修改邮箱 */
.email-current {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--cls-card-soft);
  padding: 8px 14px;
  border-radius: 0;
  font-size: 13.5px;
  margin-bottom: 4px;
}

.email-label {
  color: var(--cls-text-secondary);
}

.email-value {
  color: var(--cls-text-main);
  font-weight: 500;
}

.email-form,
.pwd-form {
  max-width: 520px;
  margin-top: 12px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12.5px;
  color: var(--cls-text-secondary);
}

.code-input {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input .el-input {
  flex: 1;
}

/* 修改密码 */
.pwd-card .cls-section-title {
  align-items: flex-start;
}

.forgot-link {
  font-size: 13px;
  color: var(--cls-primary-ink);
}

.forgot-link:hover {
  color: var(--cls-primary-ink);
}

@media (max-width: 900px) {
  .profile-page {
    grid-template-columns: 1fr;
  }
}
</style>
