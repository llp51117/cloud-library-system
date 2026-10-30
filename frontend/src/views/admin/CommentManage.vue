<template>
  <div class="comment-manage">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>评论管理</span>
          <span class="cls-sub-title">{{ tab === 'all' ? `共 ${total} 本图书` : `共 ${total} 条评论` }}</span>
        </div>
        <el-button plain @click="loadList">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>

      <el-tabs v-model="tab" @tab-change="handleTabChange">
        <el-tab-pane label="全部评论" name="all" />
        <el-tab-pane label="系统已删" name="system" />
        <el-tab-pane label="@管理员" name="reported" />
      </el-tabs>

      <!-- 全部评论：按图书聚合的列表 -->
      <template v-if="tab === 'all'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="按图书聚合展示评论：置顶的图书排最前，其次按被举报数、评论数从高到低排序。点击「评论详情」进入该书的评论管理页。"
          class="tab-alert"
        />
        <el-table v-loading="loading" :data="bookList" stripe>
          <el-table-column label="图书" min-width="300">
            <template #default="{ row }">
              <div class="book-cell">
                <img v-if="row.coverUrl" :src="row.coverUrl" class="book-cover" alt="" />
                <div v-else class="book-cover placeholder">
                  <el-icon><Reading /></el-icon>
                </div>
                <div class="book-meta">
                  <div class="book-title">
                    <el-icon v-if="row.commentPinned === 1" class="pin-icon"><TopFilled /></el-icon>
                    <span>{{ row.title }}</span>
                  </div>
                  <div class="book-author">{{ row.author || '—' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="点赞数" width="100" align="center">
            <template #default="{ row }">
              <span class="like-count">
                <el-icon><Star /></el-icon>
                {{ row.likeCount ?? 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="踩数" width="90" align="center">
            <template #default="{ row }">
              <span class="dislike-count">👎 {{ row.dislikeCount ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评论数" width="100" align="center">
            <template #default="{ row }">{{ row.commentCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="被举报数" width="110" align="center">
            <template #default="{ row }">
              <span :class="['report-count', { danger: (row.reportCount || 0) > 0 }]">
                <el-icon><Bell /></el-icon>
                {{ row.reportCount ?? 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="goBookDetail(row)">
                评论详情
              </el-button>
              <el-button
                :type="row.commentPinned === 1 ? 'warning' : 'default'"
                size="small"
                link
                @click="handlePin(row)"
              >
                {{ row.commentPinned === 1 ? '取消置顶' : '置顶' }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="cls-empty">暂无图书数据</div>
          </template>
        </el-table>
      </template>

      <!-- 系统已删 / @管理员：评论明细列表 -->
      <template v-else>
        <el-alert
          v-if="tab === 'system'"
          type="info"
          :closable="false"
          show-icon
          title="以下评论因包含敏感词被系统自动删除，确认无问题后可点击「恢复」重新展示。"
          class="tab-alert"
        />
        <el-alert
          v-else
          type="warning"
          :closable="false"
          show-icon
          title="以下评论被用户点击「@管理员」标记（多人举报已合并、次数累加）。点击「查看」跳到该书评论页，自动定位到最上面的被举报评论。"
          class="tab-alert"
        />

        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="list"
          stripe
          @selection-change="handleSelectionChange"
        >
          <!-- @管理员 列表：批量操作复选框（表头自带全选） -->
          <el-table-column v-if="tab === 'reported'" type="selection" width="45" />
          <el-table-column label="用户" min-width="190">
            <template #default="{ row }">
              <div class="user-cell">
                <!-- 只有正式管理员（role=1 且 pending_admin=0）显示旋转齿轮头像；待激活管理员等同普通用户，显示本人头像 -->
                <GearAvatar v-if="row.userRole === 1 && row.userPendingAdmin !== 1" :size="30" />
                <el-avatar v-else :size="30" :src="row.userAvatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <span>{{ row.userNickname || row.userEmail || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="bookTitle" label="图书" min-width="150" show-overflow-tooltip />
          <el-table-column label="评论内容" :min-width="tab === 'reported' ? 130 : 260">
            <template #default="{ row }">
              <!-- @管理员 列表：只显示前 5 个字 + 省略号，红色醒目 -->
              <div
                v-if="tab === 'reported'"
                class="comment-content reported-content"
                :title="row.content"
              >
                {{ shortContent(row.content) }}
              </div>
              <template v-else>
                <div class="comment-content">{{ row.content }}</div>
                <div v-if="row.deleteReason" class="reason system">
                  <el-icon><WarningFilled /></el-icon>
                  <span>{{ row.deleteReason }}</span>
                </div>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="点赞" width="80" align="center">
            <template #default="{ row }">
              <span class="like-count">
                <el-icon><Star /></el-icon>
                {{ row.likeCount ?? 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column v-if="tab === 'reported'" label="举报次数" width="100" align="center">
            <template #default="{ row }">
              <span class="report-count danger">{{ row.reportCount ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评论时间" width="160">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column v-if="tab === 'system'" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusMeta(row.status).type" effect="plain">
                {{ statusMeta(row.status).text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <template v-if="tab === 'system'">
                <el-button type="primary" size="small" link @click="handleRestore(row)">恢复</el-button>
              </template>
              <template v-else>
                <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
                <el-button type="success" size="small" link @click="handleKeep(row)">保留</el-button>
                <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
          <template #empty>
            <div class="cls-empty">{{ emptyText }}</div>
          </template>
        </el-table>

        <!-- @管理员 列表：批量操作栏 -->
        <div v-if="tab === 'reported'" class="batch-bar">
          <span class="selected-tip">已选 {{ selectedIds.length }} 条</span>
          <div class="batch-buttons">
            <el-button
              type="success"
              :disabled="selectedIds.length === 0"
              @click="handleBatchKeep"
            >
              <el-icon><CircleCheck /></el-icon>
              <span>批量保留</span>
            </el-button>
            <el-button
              type="danger"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon>
              <span>批量删除</span>
            </el-button>
          </div>
        </div>
      </template>

      <div class="pager">
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
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import GearAvatar from '@/components/GearAvatar.vue'
import {
  adminGetComments,
  adminGetCommentBooks,
  adminDeleteComment,
  adminRestoreComment,
  adminToggleCommentBookPin
} from '@/api'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const bookList = ref([])
const tab = ref('all')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const tableRef = ref()
/** @管理员 列表当前勾选的评论 */
const selected = ref([])

const selectedIds = computed(() => selected.value.map((row) => row.id))

const emptyText = computed(() => {
  if (tab.value === 'system') return '暂无被系统删除的评论'
  return '暂无被 @管理员 标记的评论'
})

onMounted(loadList)

function statusMeta(status) {
  if (status === 1) return { text: '系统已删', type: 'info' }
  if (status === 2) return { text: '管理员删除', type: 'danger' }
  if (status === 3) return { text: '管理员恢复', type: 'success' }
  return { text: '正常', type: 'primary' }
}

function fmt(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

/** @管理员 列表：评论内容只显示前 5 个字 + 省略号 */
function shortContent(content) {
  const text = content || ''
  return text.length > 5 ? text.slice(0, 5) + '…' : text
}

async function loadList() {
  loading.value = true
  try {
    if (tab.value === 'all') {
      const res = await adminGetCommentBooks({ page: page.value, size: size.value })
      bookList.value = res.data.list || []
      total.value = Number(res.data.total || 0)
    } else {
      const res = await adminGetComments({ tab: tab.value, page: page.value, size: size.value })
      list.value = res.data.list || []
      total.value = Number(res.data.total || 0)
    }
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  selected.value = []
  loadList()
}

function handlePageChange(current) {
  page.value = current
  loadList()
}

function handleSizeChange(val) {
  size.value = val
  page.value = 1
  loadList()
}

/** 进入该书的评论详情页（当前页内跳转，保持管理员顶栏） */
function goBookDetail(row) {
  router.push(`/admin/book/${row.id}`)
}

/** 置顶 / 取消置顶 */
async function handlePin(row) {
  try {
    const res = await adminToggleCommentBookPin(row.id)
    row.commentPinned = res.data
    ElMessage.success(res.data === 1 ? '已置顶，该图书将排在评论列表最前' : '已取消置顶')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 从举报列表查看：当前页内跳转，自动定位到最上面的被举报评论 */
function handleView(row) {
  router.push(`/admin/book/${row.bookId}?commentId=${row.id}`)
}

function handleSelectionChange(rows) {
  selected.value = rows
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      '确定删除该评论吗？删除后用户端将显示「该评论已被管理员删除」。',
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminDeleteComment(row.id)
    ElMessage.success('评论已删除')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleRestore(row) {
  try {
    await adminRestoreComment(row.id)
    ElMessage.success('评论已恢复')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleKeep(row) {
  try {
    await ElMessageBox.confirm(
      '确定保留该评论吗？确认后将清除「@管理员」标记，评论继续正常展示。',
      '保留确认',
      { confirmButtonText: '确认保留', cancelButtonText: '取消', type: 'info' }
    )
  } catch (e) {
    return
  }
  try {
    await adminRestoreComment(row.id)
    ElMessage.success('已保留该评论并清除举报标记')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 批量处理选中的评论（保留 / 删除），处理完刷新列表 */
async function batchProcess(action) {
  const isDelete = action === 'delete'
  const tip = isDelete
    ? `确定删除选中的 ${selectedIds.value.length} 条评论吗？删除后用户端将显示「该评论已被管理员删除」。`
    : `确定保留选中的 ${selectedIds.value.length} 条评论吗？确认后将清除这些评论的「@管理员」标记。`
  try {
    await ElMessageBox.confirm(tip, isDelete ? '批量删除确认' : '批量保留确认', {
      confirmButtonText: isDelete ? '确认删除' : '确认保留',
      cancelButtonText: '取消',
      type: isDelete ? 'warning' : 'info'
    })
  } catch (e) {
    return
  }
  const results = await Promise.allSettled(
    selectedIds.value.map((id) => (isDelete ? adminDeleteComment(id) : adminRestoreComment(id)))
  )
  const failed = results.filter((r) => r.status === 'rejected').length
  const success = results.length - failed
  if (failed > 0) {
    ElMessage.warning(`${isDelete ? '删除' : '保留'}完成：成功 ${success} 条，失败 ${failed} 条`)
  } else {
    ElMessage.success(`已批量${isDelete ? '删除' : '保留'} ${success} 条评论`)
  }
  selected.value = []
  loadList()
}

function handleBatchDelete() {
  batchProcess('delete')
}

function handleBatchKeep() {
  batchProcess('keep')
}
</script>

<style scoped>
.book-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.book-cover {
  width: 44px;
  height: 58px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  background: var(--cls-primary-light);
}

.book-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--cls-primary);
}

.book-meta {
  min-width: 0;
}

.book-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--cls-text-main);
}

.pin-icon {
  color: var(--cls-warning);
}

.book-author {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--cls-text-secondary);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-content {
  color: var(--cls-text-regular);
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

/* @管理员 列表：评论内容红色、截断显示 */
.reported-content {
  color: var(--cls-danger, #f56c6c);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: default;
}

.reason {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
}

.reason.system {
  color: var(--cls-warning);
}

.like-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--cls-warning);
}

.dislike-count {
  color: var(--cls-text-secondary);
  font-size: 13px;
}

.report-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--cls-text-secondary);
}

.report-count.danger {
  color: var(--cls-danger);
  font-weight: 600;
}

.tab-alert {
  margin-bottom: 16px;
  border-radius: var(--cls-radius-sm);
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 10px 14px;
  background: var(--cls-primary-light, #eff6ff);
  border-radius: var(--cls-radius-sm, 8px);
}

.selected-tip {
  font-size: 13px;
  color: var(--cls-text-regular);
}

.batch-buttons {
  display: flex;
  gap: 10px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
