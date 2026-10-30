<template>
  <div class="user-manage">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>用户管理</span>
          <span class="cls-sub-title">账号管理 · 用户反馈处理</span>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <!-- ==================== 用户列表 ==================== -->
        <el-tab-pane label="用户列表" name="users">
          <el-alert
            v-if="!isSuperAdmin"
            type="info"
            :closable="false"
            show-icon
            class="perm-tip"
            title="您不是最高管理员：设置/取消管理员只有最高管理员可以操作；删除用户、注销用户、批量删除图书需要提交「操作申请」由最高管理员审批。"
          />

          <div class="cls-toolbar">
            <el-input
              v-model="userKeyword"
              placeholder="按邮箱或昵称搜索用户"
              clearable
              style="width: 260px"
              @keyup.enter="handleUserSearch"
              @clear="handleUserSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="handleUserSearch">
              <el-icon><Search /></el-icon>
              <span>查询</span>
            </el-button>
            <div class="flex-1"></div>
            <el-button v-if="!isSuperAdmin" type="primary" plain @click="openApplyDialog()">
              <el-icon><DocumentAdd /></el-icon>
              <span>操作申请</span>
            </el-button>
            <el-button plain @click="loadUsers">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>

          <el-table v-loading="userLoading" :data="userList" stripe>
            <el-table-column label="用户" min-width="180">
              <template #default="{ row }">
                <div class="user-cell">
                  <!-- 超管：齿轮 + 魂环；正式管理员：个人头像 + 魂环；普通用户 / 待激活管理员：个人头像 -->
                  <RoleAvatar
                    :size="34"
                    class="clickable-avatar"
                    :avatar="row.avatar"
                    :role="row.role || 0"
                    :super-admin="row.isSuperAdmin || 0"
                    :pending-admin="row.pendingAdmin || 0"
                    :nickname="row.nickname || row.email"
                    @click="openUserHome(row)"
                  />
                  <span
                    class="email"
                    :title="`查看 ${row.nickname || row.email} 的个人主页`"
                    @click="openUserHome(row)"
                  >
                    {{ row.nickname || row.email }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="身份" width="170" align="center">
              <template #default="{ row }">
                <div class="identity-cell">
                  <el-tag size="small" :type="identityMeta(row).type" effect="dark">
                    {{ identityMeta(row).text }}
                  </el-tag>
                  <!-- 管理员封号：红色小标签，紧跟身份标签 -->
                  <el-tag v-if="row.adminTitle" size="small" type="danger" effect="plain">
                    {{ row.adminTitle }}
                  </el-tag>
                  <div v-if="isPendingAdmin(row)" class="pending-tip">
                    截止 {{ fmt(row.pendingAdminDeadline) }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMeta(row.status).type" effect="dark">
                  {{ statusMeta(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="禁用信息" min-width="200">
              <template #default="{ row }">
                <div v-if="row.status === 2" class="ban-info">
                  <div>原因：{{ row.banReason || '—' }}</div>
                  <div class="cls-muted">
                    {{ row.banDays }} 天 · 自 {{ fmt(row.banStartTime) }}
                  </div>
                </div>
                <span v-else class="cls-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="160">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="270" fixed="right">
              <template #default="{ row }">
                <!-- 最高管理员：功能按钮全部置灰 -->
                <template v-if="row.isSuperAdmin === 1">
                  <span class="cls-muted op-muted">最高管理员，不可操作</span>
                </template>

                <!-- 普通管理员 / 待激活管理员 -->
                <template v-else-if="row.role === 1">
                  <el-button
                    v-if="isSuperAdmin"
                    type="warning"
                    size="small"
                    link
                    @click="handleCancelAdmin(row)"
                  >
                    取消管理员
                  </el-button>
                  <el-button
                    v-if="isSuperAdmin && !isPendingAdmin(row)"
                    type="primary"
                    size="small"
                    link
                    @click="openTitleDialog(row)"
                  >
                    设置封号
                  </el-button>
                  <el-button type="warning" size="small" link disabled>禁用</el-button>
                  <el-button type="danger" size="small" link disabled>注销</el-button>
                </template>

                <!-- 普通用户 -->
                <template v-else-if="row.status === 1">
                  <el-button
                    v-if="isSuperAdmin"
                    type="primary"
                    size="small"
                    link
                    @click="handleSetAdmin(row)"
                  >
                    设为管理员
                  </el-button>
                  <el-button type="warning" size="small" link @click="openBanDialog(row)">禁用</el-button>
                  <el-button
                    type="danger"
                    size="small"
                    link
                    @click="isSuperAdmin ? handleCancelUser(row) : openApplyDialog('cancel_user', row.id)"
                  >
                    {{ isSuperAdmin ? '注销' : '申请注销' }}
                  </el-button>
                </template>

                <template v-else>
                  <el-button type="primary" size="small" link @click="handleEnableUser(row)">启用</el-button>
                </template>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">暂无用户数据</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="prev, pager, next, total, sizes"
              :total="userTotal"
              :page-size="userSize"
              :current-page="userPage"
              :page-sizes="[10, 20, 50]"
              background
              @current-change="handleUserPageChange"
              @size-change="handleUserSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- ==================== 用户反馈 ==================== -->
        <el-tab-pane label="用户反馈" name="feedback">
          <div class="cls-toolbar">
            <el-radio-group v-model="feedbackStatus" @change="handleFeedbackSearch">
              <el-radio-button :value="null">全部</el-radio-button>
              <el-radio-button :value="0">待回复</el-radio-button>
              <el-radio-button :value="1">已回复</el-radio-button>
            </el-radio-group>
            <div class="flex-1"></div>
            <el-button plain @click="loadFeedbacks">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>

          <el-table v-loading="feedbackLoading" :data="feedbackList" stripe>
            <el-table-column label="用户" min-width="200">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="30" :src="row.userAvatar">
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <span class="email">{{ row.userNickname || row.userEmail }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="105" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ FEEDBACK_TYPES[row.type] || '其他' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="content" label="反馈内容" min-width="220" show-overflow-tooltip />
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'" effect="dark">
                  {{ row.status === 1 ? '已回复' : '待回复' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="openReplyDialog(row)">
                  {{ row.status === 1 ? '查看回复' : '回复' }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">暂无反馈数据</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="prev, pager, next, total, sizes"
              :total="feedbackTotal"
              :page-size="feedbackSize"
              :current-page="feedbackPage"
              :page-sizes="[10, 20, 50]"
              background
              @current-change="handleFeedbackPageChange"
              @size-change="handleFeedbackSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============ 设置管理员封号对话框（仅超管） ============ -->
    <el-dialog v-model="titleVisible" title="设置管理员封号" width="440px" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="封号会显示在该管理员的个人主页、评论区与用户列表中；清空内容表示取消封号。"
        class="perm-tip"
      />
      <el-form label-width="72px">
        <el-form-item label="管理员">
          <span class="title-target">{{ titleForm.nickname || titleForm.email }}</span>
        </el-form-item>
        <el-form-item label="封号">
          <el-select
            v-model="titleForm.title"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择或输入封号，留空表示取消"
            style="width: 100%"
          >
            <el-option v-for="item in ADMIN_TITLES" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="titleVisible = false">取消</el-button>
        <el-button type="primary" :loading="titleSubmitting" @click="handleTitleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 禁用用户对话框 ============ -->
    <el-dialog
      v-model="banVisible"
      title="禁用用户"
      width="480px"
      :close-on-click-modal="false"
      @closed="resetBanForm"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        :title="`即将禁用账号：${banForm.email}`"
        description="禁用期间用户仍可登录，但无法借阅图书与发表评论；到期后系统自动恢复正常，同时会向用户邮箱发送通知。"
        class="ban-alert"
      />
      <el-form ref="banFormRef" :model="banForm" :rules="banRules" label-width="90px">
        <el-form-item label="禁用天数" prop="banDays">
          <el-input-number v-model="banForm.banDays" :min="1" :max="3650" style="width: 100%" />
        </el-form-item>
        <el-form-item label="禁用原因" prop="banReason">
          <el-input
            v-model="banForm.banReason"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="100"
            show-word-limit
            placeholder="请填写禁用原因，将在用户登录时提示"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="primary" :loading="banSubmitting" @click="handleBanSubmit">确认禁用</el-button>
      </template>
    </el-dialog>

    <!-- ============ 反馈回复对话框 ============ -->
    <el-dialog v-model="replyVisible" title="回复用户反馈" width="560px" :close-on-click-modal="false">
      <div v-if="replyRow" class="feedback-detail">
        <div class="detail-line">
          <span class="detail-label">用户</span>
          <span>{{ replyRow.userNickname || replyRow.userEmail }}</span>
        </div>
        <div class="detail-line">
          <span class="detail-label">类型</span>
          <span>{{ FEEDBACK_TYPES[replyRow.type] || '其他' }}</span>
        </div>
        <div class="detail-line">
          <span class="detail-label">标题</span>
          <span>{{ replyRow.title }}</span>
        </div>
        <div class="detail-line">
          <span class="detail-label">内容</span>
          <span class="detail-content">{{ replyRow.content }}</span>
        </div>
        <div class="detail-line">
          <span class="detail-label">提交时间</span>
          <span>{{ fmt(replyRow.createdAt) }}</span>
        </div>
        <div v-if="replyRow.status === 1" class="detail-line">
          <span class="detail-label">已回复</span>
          <span class="detail-content replied">{{ replyRow.reply }}</span>
        </div>
        <div v-if="replyRow.status === 1" class="detail-line">
          <span class="detail-label">回复时间</span>
          <span>{{ fmt(replyRow.replyTime) }}</span>
        </div>
      </div>

      <el-form v-if="replyRow && replyRow.status !== 1" label-width="90px" class="reply-form">
        <el-form-item label="回复内容">
          <el-input
            v-model="replyText"
            type="textarea"
            :rows="4"
            resize="none"
            maxlength="200"
            show-word-limit
            placeholder="请输入回复内容，提交后用户将在消息通知中收到提醒"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyVisible = false">关闭</el-button>
        <el-button
          v-if="replyRow && replyRow.status !== 1"
          type="primary"
          :loading="replySubmitting"
          @click="handleReplySubmit"
        >
          提交回复
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 操作申请弹窗（普通管理员） ============ -->
    <ApplyDialog
      v-model="applyVisible"
      :preset-type="applyPresetType"
      :preset-target-id="applyPresetTargetId"
      @submitted="onApplied"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useUserHomepageNav } from '@/utils/userHome'
import RoleAvatar from '@/components/RoleAvatar.vue'
import ApplyDialog from '@/components/ApplyDialog.vue'
import {
  adminGetUsers,
  adminBanUser,
  adminEnableUser,
  adminCancelUser,
  adminSetUserAdmin,
  adminCancelUserAdmin,
  adminSetAdminTitle,
  adminGetUnreturnedCount,
  adminGetFeedbacks,
  adminReplyFeedback
} from '@/api'

/** 待办变化（回复反馈 / 提交操作申请）时通知 AdminLayout 重算 Tab 红点 */
const emit = defineEmits(['refresh-admin-badges'])

const FEEDBACK_TYPES = ['功能建议', '举报用户', '内容错误', '系统BUG', '其他']

/** 管理员封号预设选项（可下拉选，也可以自己输入） */
const ADMIN_TITLES = ['图书管理专员', '用户管理专员', '评论管理专员', '借阅管理专员', '通知公告专员']

const userStore = useUserStore()
/** 点击头像 / 昵称的统一跳转逻辑 */
const { openUserHome } = useUserHomepageNav()

const activeTab = ref('users')

/** 当前登录者是否最高管理员：只有超管能设置/取消管理员 */
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

/** 操作申请弹窗 */
const applyVisible = ref(false)
const applyPresetType = ref('')
const applyPresetTargetId = ref('')

/* ---------------- 用户列表 ---------------- */
const userLoading = ref(false)
const userList = ref([])
const userKeyword = ref('')
const userPage = ref(1)
const userSize = ref(10)
const userTotal = ref(0)

const banVisible = ref(false)
const banFormRef = ref()
const banSubmitting = ref(false)
const banForm = reactive({ id: null, email: '', banDays: 7, banReason: '' })

const banRules = {
  banDays: [{ required: true, message: '请填写禁用天数', trigger: 'change' }],
  banReason: [{ required: true, message: '请填写禁用原因', trigger: 'blur' }]
}

/* ---------------- 用户反馈 ---------------- */
const feedbackLoading = ref(false)
const feedbackList = ref([])
const feedbackStatus = ref(null)
const feedbackPage = ref(1)
const feedbackSize = ref(10)
const feedbackTotal = ref(0)

const replyVisible = ref(false)
const replyRow = ref(null)
const replyText = ref('')
const replySubmitting = ref(false)

onMounted(() => {
  loadUsers()
  loadFeedbacks()
})

/* ==================== 用户列表逻辑 ==================== */

function statusMeta(status) {
  if (status === 0) return { text: '已注销', type: 'info' }
  if (status === 2) return { text: '已禁用', type: 'danger' }
  return { text: '正常', type: 'success' }
}

/** 是否待激活管理员：role=1 且 pendingAdmin=1 */
function isPendingAdmin(row) {
  return row.role === 1 && row.pendingAdmin === 1
}

/** 身份标签：最高管理员 / 管理员 / 待激活管理员 / 普通用户 */
function identityMeta(row) {
  if (row.isSuperAdmin === 1) return { text: '最高管理员', type: 'danger' }
  if (row.role === 1) {
    return row.pendingAdmin === 1
      ? { text: '待激活管理员', type: 'warning' }
      : { text: '管理员', type: 'warning' }
  }
  return { text: '普通用户', type: 'info' }
}

/* ---------------- 管理员封号 ---------------- */
const titleVisible = ref(false)
const titleSubmitting = ref(false)
const titleForm = reactive({ id: null, nickname: '', email: '', title: '' })

function openTitleDialog(row) {
  titleForm.id = row.id
  titleForm.nickname = row.nickname
  titleForm.email = row.email
  titleForm.title = row.adminTitle || ''
  titleVisible.value = true
}

async function handleTitleSubmit() {
  titleSubmitting.value = true
  try {
    await adminSetAdminTitle(titleForm.id, (titleForm.title || '').trim())
    ElMessage.success(
      (titleForm.title || '').trim() ? '已设置管理员封号' : '已清除该管理员的封号'
    )
    titleVisible.value = false
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    titleSubmitting.value = false
  }
}

/** 打开操作申请弹窗（普通管理员） */
function openApplyDialog(presetType = '', presetTargetId = '') {
  applyPresetType.value = presetType
  applyPresetTargetId.value = presetTargetId ? String(presetTargetId) : ''
  applyVisible.value = true
}

/** 申请提交成功：通知顶栏刷新「通知公告」Tab 的待审批红点 */
function onApplied() {
  emit('refresh-admin-badges')
}

/** 设为管理员：有未归还图书时先弹窗确认，确认后设为「待激活管理员」 */
async function handleSetAdmin(row) {
  let unreturned = 0
  try {
    const res = await adminGetUnreturnedCount(row.id)
    unreturned = Number(res.data) || 0
  } catch (e) {
    return
  }
  if (unreturned > 0) {
    try {
      await ElMessageBox.confirm(
        `该用户还有未归还图书，是否继续设为管理员？\n（将继续设为「待激活管理员」，需在 10 天内归还全部图书才能行使管理员权限，否则自动恢复为普通用户）`,
        '设为管理员',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch (e) {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(
        `确定要将 ${row.nickname || row.email} 设为管理员吗？该用户将无法再借阅图书。`,
        '设为管理员',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
    } catch (e) {
      return
    }
  }
  try {
    const res = await adminSetUserAdmin(row.id)
    const pending = res.data === true
    ElMessage.success(
      pending
        ? '已设为待激活管理员，10 天内归还全部图书即可转正'
        : '已设为正式管理员'
    )
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 取消管理员权限 */
async function handleCancelAdmin(row) {
  try {
    await ElMessageBox.confirm(
      `确定要取消 ${row.nickname || row.email} 的管理员权限吗？取消后该用户恢复为普通用户。`,
      '取消管理员',
      { confirmButtonText: '确定取消', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminCancelUserAdmin(row.id)
    ElMessage.success('已取消管理员权限')
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

function fmt(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await adminGetUsers({
      page: userPage.value,
      size: userSize.value,
      keyword: userKeyword.value
    })
    userList.value = res.data.list || []
    userTotal.value = Number(res.data.total || 0)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    userLoading.value = false
  }
}

function handleUserSearch() {
  userPage.value = 1
  loadUsers()
}

function handleUserPageChange(current) {
  userPage.value = current
  loadUsers()
}

function handleUserSizeChange(val) {
  userSize.value = val
  userPage.value = 1
  loadUsers()
}

function openBanDialog(row) {
  banForm.id = row.id
  banForm.email = row.email
  banForm.banDays = 7
  banForm.banReason = ''
  banVisible.value = true
}

function resetBanForm() {
  banFormRef.value?.clearValidate()
}

async function handleBanSubmit() {
  try {
    await banFormRef.value.validate()
  } catch (e) {
    return
  }
  banSubmitting.value = true
  try {
    await adminBanUser(banForm.id, {
      banDays: banForm.banDays,
      banReason: banForm.banReason
    })
    ElMessage.success('已禁用该用户，通知邮件已发送')
    banVisible.value = false
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    banSubmitting.value = false
  }
}

async function handleEnableUser(row) {
  try {
    await adminEnableUser(row.id)
    ElMessage.success('已恢复该用户为正常状态')
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleCancelUser(row) {
  try {
    await ElMessageBox.confirm(
      `确定要注销账号 ${row.email} 吗？注销后该用户将无法登录系统。`,
      '注销确认',
      { confirmButtonText: '确认注销', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminCancelUser(row.id)
    ElMessage.success('已注销该用户')
    loadUsers()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ==================== 用户反馈逻辑 ==================== */

async function loadFeedbacks() {
  feedbackLoading.value = true
  try {
    const res = await adminGetFeedbacks({
      page: feedbackPage.value,
      size: feedbackSize.value,
      status: feedbackStatus.value === null ? undefined : feedbackStatus.value
    })
    feedbackList.value = res.data.list || []
    feedbackTotal.value = Number(res.data.total || 0)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    feedbackLoading.value = false
  }
}

function handleFeedbackSearch() {
  feedbackPage.value = 1
  loadFeedbacks()
}

function handleFeedbackPageChange(current) {
  feedbackPage.value = current
  loadFeedbacks()
}

function handleFeedbackSizeChange(val) {
  feedbackSize.value = val
  feedbackPage.value = 1
  loadFeedbacks()
}

function openReplyDialog(row) {
  replyRow.value = row
  replyText.value = ''
  replyVisible.value = true
}

async function handleReplySubmit() {
  if (!replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySubmitting.value = true
  try {
    await adminReplyFeedback(replyRow.value.id, replyText.value.trim())
    ElMessage.success('回复成功，已通知用户')
    replyVisible.value = false
    loadFeedbacks()
    // 待回复少一条：顶栏「用户管理」Tab 的红点立刻减少
    emit('refresh-admin-badges')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    replySubmitting.value = false
  }
}
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.user-cell .email {
  flex: 1;
  min-width: 0;
  color: var(--cls-text-main);
  cursor: pointer;
  /* 昵称/邮箱过长时省略，避免把「用户」列撑宽 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell .email:hover {
  color: var(--cls-primary);
}

.perm-tip {
  margin-bottom: 14px;
  border-radius: var(--cls-radius-sm);
}

/* 身份列：身份标签 + 管理员封号标签（换行居中） */
.identity-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.identity-cell .el-tag + .el-tag {
  margin-left: 0;
}

.title-target {
  font-size: 13px;
  color: var(--cls-text-main);
}

/* 待激活管理员的截止时间：固定一行显示，不换行 */
.pending-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--cls-warning);
  white-space: nowrap;
}

.op-muted {
  font-size: 13px;
}

.ban-info {
  font-size: 13px;
  line-height: 1.6;
  color: var(--cls-text-regular);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.ban-alert {
  margin-bottom: 16px;
  border-radius: var(--cls-radius-sm);
}

.feedback-detail {
  background: var(--cls-card-soft);
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius-sm);
  padding: 14px 16px;
}

.detail-line {
  display: flex;
  gap: 12px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--cls-text-regular);
}

.detail-label {
  width: 62px;
  flex-shrink: 0;
  color: var(--cls-text-secondary);
}

.detail-content {
  flex: 1;
  color: var(--cls-text-main);
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-content.replied {
  color: var(--cls-success);
}

.reply-form {
  margin-top: 16px;
}
</style>
