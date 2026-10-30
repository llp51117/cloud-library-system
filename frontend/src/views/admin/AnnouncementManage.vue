<template>
  <div class="announcement-manage">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>通知公告</span>
          <span class="cls-sub-title">共 {{ list.length }} 条公告</span>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <!-- ==================== 系统公告 ==================== -->
        <el-tab-pane label="系统公告" name="announcements">
      <div class="cls-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索公告标题"
          clearable
          style="width: 260px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="flex-1"></div>
        <el-button plain @click="loadList">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openPublishDialog">
          <el-icon><Plus /></el-icon>
          <span>发布公告</span>
        </el-button>
      </div>

      <el-table v-loading="loading" :data="pagedList" stripe row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容摘要" min-width="300" show-overflow-tooltip />
        <el-table-column label="发布人" min-width="180">
          <template #default="{ row }">
            <!-- 发布人：超管显示「最高管理员」，普通管理员显示昵称，一律不显示邮箱 -->
            <span>{{ publisherName(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ fmt(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="dark">
              {{ row.status === 1 ? '已发布' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              link
              :disabled="row.status !== 1"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              link
              :disabled="row.status !== 1"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="cls-empty">暂无公告，点击「发布公告」创建第一条公告</div>
        </template>
      </el-table>

      <!-- 分页：与其它管理页保持一致（每页 10 条） -->
      <div v-if="total > 0" class="pager">
        <el-pagination
          layout="prev, pager, next, total, sizes"
          :total="total"
          :page-size="size"
          :current-page="page"
          :page-sizes="[10, 20, 50]"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
        </el-tab-pane>

        <!-- ==================== 操作申请（仅最高管理员可见） ==================== -->
        <el-tab-pane v-if="isSuperAdmin" name="applications">
          <template #label>
            <span class="tab-label">
              操作申请
              <el-badge v-if="pendingCount > 0" :value="pendingCount" class="tab-badge" />
            </span>
          </template>

          <div class="cls-toolbar">
            <el-radio-group v-model="appStatus" @change="handleAppSearch">
              <el-radio-button :value="null">全部</el-radio-button>
              <el-radio-button :value="0">待审批</el-radio-button>
              <el-radio-button :value="1">已同意</el-radio-button>
              <el-radio-button :value="2">已拒绝</el-radio-button>
            </el-radio-group>
            <div class="flex-1"></div>
            <el-button plain @click="loadApplications">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>

          <el-alert
            type="warning"
            :closable="false"
            show-icon
            class="app-tip"
            title="点「同意」后系统会立即执行该操作（删除用户 / 批量删除图书等不可恢复），请确认目标无误后再审批。"
          />

          <el-table v-loading="appLoading" :data="appList" stripe>
            <el-table-column label="申请人" min-width="180">
              <template #default="{ row }">
                <div class="applicant-cell">
                  <RoleAvatar
                    :size="30"
                    :avatar="row.applicantAvatar"
                    :role="1"
                    :nickname="row.applicantNickname || row.applicantEmail"
                  />
                  <span>{{ row.applicantNickname || row.applicantEmail }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="申请类型" width="130" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="appTypeMeta(row.type).type">
                  {{ appTypeMeta(row.type).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作目标" min-width="190">
              <template #default="{ row }">
                <div>{{ row.targetLabel || '—' }}</div>
                <div v-if="row.targetId" class="target-id">ID：{{ row.targetId }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="申请理由" min-width="200" show-overflow-tooltip />
            <el-table-column label="申请时间" width="150">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="appStatusMeta(row.status).type" effect="dark">
                  {{ appStatusMeta(row.status).text }}
                </el-tag>
                <div v-if="row.reply" class="reply-text" :title="row.reply">{{ row.reply }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button type="success" size="small" link @click="handleApprove(row)">同意</el-button>
                  <el-button type="danger" size="small" link @click="handleReject(row)">拒绝</el-button>
                </template>
                <span v-else class="cls-muted">{{ fmt(row.replyTime) }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">暂无操作申请</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination
              layout="prev, pager, next, total, sizes"
              :total="appTotal"
              :page-size="appSize"
              :current-page="appPage"
              :page-sizes="[10, 20, 50]"
              background
              @current-change="handleAppPageChange"
              @size-change="handleAppSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============ 发布 / 编辑对话框 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑公告' : '发布公告'"
      width="620px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="如：新书上架通知 / 系统升级公告"
            maxlength="60"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            resize="none"
            maxlength="1000"
            show-word-limit
            placeholder="请输入公告内容，发布后所有用户可在「消息通知 → 系统公告」中查看"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ form.id ? '保存修改' : '确认发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import RoleAvatar from '@/components/RoleAvatar.vue'
import { publisherName } from '@/utils/publisher'
import {
  adminGetAnnouncements,
  adminPublishAnnouncement,
  adminUpdateAnnouncement,
  adminDeleteAnnouncement,
  adminGetApplications,
  adminGetPendingApplicationCount,
  adminApproveApplication,
  adminRejectApplication
} from '@/api'

const userStore = useUserStore()

/** 审批完一条申请后通知 AdminLayout 重算 Tab 红点 */
const emit = defineEmits(['refresh-admin-badges'])

/** 当前 Tab：系统公告 / 操作申请 */
const activeTab = ref('announcements')

/** 只有最高管理员能审批操作申请 */
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

/* ---------------- 操作申请（超管审批） ---------------- */
const appLoading = ref(false)
const appList = ref([])
const appStatus = ref(null)
const appPage = ref(1)
const appSize = ref(10)
const appTotal = ref(0)
const pendingCount = ref(0)

const APP_TYPES = {
  delete_user: { text: '删除用户', type: 'danger' },
  cancel_user: { text: '注销用户', type: 'warning' },
  batch_delete_book: { text: '批量删除图书', type: 'danger' },
  other: { text: '其他', type: 'info' }
}

const loading = ref(false)
const list = ref([])
const keyword = ref('')
/** 分页：管理端公告为全量返回，这里在前端切片，默认每页 10 条 */
const page = ref(1)
const size = ref(10)

const dialogVisible = ref(false)
const formRef = ref()
const submitting = ref(false)

const form = reactive({ id: null, title: '', content: '' })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const filteredList = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return list.value
  return list.value.filter((item) => (item.title || '').includes(kw))
})

/** 搜索后的总条数 */
const total = computed(() => filteredList.value.length)

/** 当前页数据 */
const pagedList = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredList.value.slice(start, start + size.value)
})

// 搜索关键词变化后回到第 1 页，避免停留在空页
watch(keyword, () => {
  page.value = 1
})

function handlePageChange(current) {
  page.value = current
}

function handleSizeChange(val) {
  size.value = val
  page.value = 1
}

onMounted(() => {
  loadList()
  if (isSuperAdmin.value) {
    loadApplications()
    loadPendingCount()
  }
})

function fmt(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

/* ==================== 操作申请逻辑 ==================== */

function appTypeMeta(type) {
  return APP_TYPES[type] || APP_TYPES.other
}

function appStatusMeta(status) {
  if (status === 1) return { text: '已同意', type: 'success' }
  if (status === 2) return { text: '已拒绝', type: 'danger' }
  return { text: '待审批', type: 'warning' }
}

async function loadPendingCount() {
  try {
    const res = await adminGetPendingApplicationCount()
    pendingCount.value = Number(res.data) || 0
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function loadApplications() {
  appLoading.value = true
  try {
    const res = await adminGetApplications({
      page: appPage.value,
      size: appSize.value,
      status: appStatus.value === null ? undefined : appStatus.value
    })
    appList.value = res.data.list || []
    appTotal.value = Number(res.data.total || 0)
    loadPendingCount()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    appLoading.value = false
  }
}

function handleAppSearch() {
  appPage.value = 1
  loadApplications()
}

function handleAppPageChange(current) {
  appPage.value = current
  loadApplications()
}

function handleAppSizeChange(val) {
  appSize.value = val
  appPage.value = 1
  loadApplications()
}

/** 同意：系统会立即执行该操作 */
async function handleApprove(row) {
  const meta = appTypeMeta(row.type)
  try {
    await ElMessageBox.confirm(
      `确定同意【${meta.text}】申请吗？\n目标：${row.targetLabel || row.targetId || '无'}\n理由：${row.content || '—'}\n\n同意后系统将立即执行该操作，且不可恢复。`,
      '同意申请',
      { confirmButtonText: '同意并执行', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminApproveApplication(row.id, '同意执行')
    ElMessage.success('已同意，操作已执行')
    loadApplications()
    emit('refresh-admin-badges')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 拒绝：不执行操作，需填写拒绝理由 */
async function handleReject(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写拒绝理由，将通知申请人 ${row.applicantNickname || row.applicantEmail}。`,
      '拒绝申请',
      {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '如：目标信息不完整，请补充后重新提交',
        inputValidator: (val) => (val && val.trim() ? true : '请填写拒绝理由'),
        type: 'warning'
      }
    )
    reason = value
  } catch (e) {
    return
  }
  try {
    await adminRejectApplication(row.id, reason.trim())
    ElMessage.success('已拒绝该申请，操作未执行')
    loadApplications()
    emit('refresh-admin-badges')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminGetAnnouncements()
    list.value = res.data || []
    page.value = 1
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

function openPublishDialog() {
  form.id = null
  form.title = ''
  form.content = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  form.id = row.id
  form.title = row.title
  form.content = row.content
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await adminUpdateAnnouncement(form.id, { title: form.title, content: form.content })
      ElMessage.success('公告修改成功')
    } else {
      await adminPublishAnnouncement({ title: form.title, content: form.content })
      ElMessage.success('公告发布成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告《${row.title}》吗？删除后用户端将不再显示该公告。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminDeleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.expand-content {
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--cls-card-soft);
  border-radius: var(--cls-radius-sm);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-2px);
}

.app-tip {
  margin-bottom: 14px;
  border-radius: var(--cls-radius-sm);
}

.applicant-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--cls-text-main);
}

.target-id {
  margin-top: 2px;
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.reply-text {
  margin-top: 4px;
  font-size: 12px;
  color: var(--cls-text-secondary);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
