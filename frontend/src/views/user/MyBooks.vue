<template>
  <div class="my-books">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>我的书籍</span>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <!-- ============ 当前借阅 ============ -->
        <el-tab-pane :label="`当前借阅 (${counts.current})`" name="current">
          <el-table v-loading="loading" :data="pagedCurrent" stripe>
            <el-table-column label="书名" width="200">
              <template #default="{ row }">
                <span class="book-title" :title="row.bookTitle">{{ shortTitle(row.bookTitle) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作者" min-width="100">
              <template #default="{ row }">
                <span class="book-author">{{ row.bookAuthor || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="平台" min-width="100">
              <template #default="{ row }">{{ row.bookPlatform || '-' }}</template>
            </el-table-column>
            <el-table-column label="借阅时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(row.borrowDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="应还时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(dueDateOf(row)) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <el-tag :type="borrowStatusType(row.status)" size="small">{{ borrowStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="剩余时间" min-width="100">
              <template #default="{ row }">
                <span :class="remainClass(row)">{{ remainText(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="cell-actions">
                  <el-button type="primary" size="small" @click="handleReturn(row)">归还</el-button>
                  <el-button size="small" plain @click="handleRenew(row)">续借</el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">当前没有借阅中的图书，去首页找找喜欢的书吧</div>
            </template>
          </el-table>

          <div v-if="currentBorrows.length > PAGE_SIZE" class="pager">
            <el-pagination
              layout="prev, pager, next, total"
              :total="currentBorrows.length"
              :page-size="PAGE_SIZE"
              v-model:current-page="pages.current"
              background
            />
          </div>
        </el-tab-pane>

        <!-- ============ 借阅历史 ============ -->
        <el-tab-pane :label="`借阅历史 (${counts.history})`" name="history">
          <el-table v-loading="loading" :data="pagedHistory" stripe>
            <el-table-column label="书名" width="200">
              <template #default="{ row }">
                <span class="book-title" :title="row.bookTitle">{{ shortTitle(row.bookTitle) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作者" min-width="100">
              <template #default="{ row }">
                <span class="book-author">{{ row.bookAuthor || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="平台" min-width="100">
              <template #default="{ row }">{{ row.bookPlatform || '-' }}</template>
            </el-table-column>
            <el-table-column label="借阅时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(row.borrowDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="归还时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(row.returnDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <!-- 归还只由用户在用户端触发，统一显示绿色「已归还」 -->
                <el-tag type="success" size="small">已归还</el-tag>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">暂无借阅历史</div>
            </template>
          </el-table>

          <div v-if="historyBorrows.length > PAGE_SIZE" class="pager">
            <el-pagination
              layout="prev, pager, next, total"
              :total="historyBorrows.length"
              :page-size="PAGE_SIZE"
              v-model:current-page="pages.history"
              background
            />
          </div>
        </el-tab-pane>

        <!-- ============ 我的收藏 ============ -->
        <el-tab-pane :label="`我的收藏 (${counts.favorite})`" name="favorite">
          <el-table v-loading="loading" :data="pagedFavorites" stripe>
            <el-table-column label="书名" width="200">
              <template #default="{ row }">
                <span class="book-title" :title="row.bookTitle">{{ shortTitle(row.bookTitle) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作者" min-width="100">
              <template #default="{ row }">
                <span class="book-author">{{ row.bookAuthor || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="平台" min-width="100">
              <template #default="{ row }">{{ row.bookPlatform || '-' }}</template>
            </el-table-column>
            <el-table-column label="收藏时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="cell-actions">
                  <el-tooltip :content="noBorrowReason" placement="top" :disabled="canBorrow">
                    <span>
                      <el-button
                        type="primary"
                        size="small"
                        :disabled="row.borrowing || !canBorrow"
                        @click="handleBorrowFavorite(row)"
                      >
                        {{ !canBorrow ? '不可借阅' : row.borrowing ? '已借阅' : '去借阅' }}
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-button size="small" plain @click="handleCancelFavorite(row)">取消收藏</el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">还没有收藏任何图书</div>
            </template>
          </el-table>

          <div v-if="favorites.length > PAGE_SIZE" class="pager">
            <el-pagination
              layout="prev, pager, next, total"
              :total="favorites.length"
              :page-size="PAGE_SIZE"
              v-model:current-page="pages.favorite"
              background
            />
          </div>
        </el-tab-pane>

        <!-- ============ 我的预约 ============ -->
        <el-tab-pane :label="`我的预约 (${counts.reservation})`" name="reservation">
          <el-table v-loading="loading" :data="pagedReservations" stripe>
            <el-table-column label="书名" width="200">
              <template #default="{ row }">
                <span class="book-title" :title="row.bookTitle">{{ shortTitle(row.bookTitle) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="作者" min-width="100">
              <template #default="{ row }">
                <span class="book-author">{{ row.bookAuthor || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="平台" min-width="100">
              <template #default="{ row }">{{ row.bookPlatform || '-' }}</template>
            </el-table-column>
            <el-table-column label="预约时间" width="150">
              <template #default="{ row }">
                <span class="cell-time">{{ formatDateTime(row.reservationTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="cell-actions">
                  <el-tooltip
                    v-if="row.status === 1"
                    :content="noBorrowReason"
                    placement="top"
                    :disabled="canBorrow"
                  >
                    <span>
                      <el-button type="primary" size="small" :disabled="!canBorrow" @click="handleBorrowReservation(row)">
                        {{ canBorrow ? '去借阅' : '不可借阅' }}
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-button
                    v-if="row.status === 0 || row.status === 1"
                    size="small"
                    plain
                    @click="handleCancelReservation(row)"
                  >
                    取消预约
                  </el-button>
                  <span v-if="row.status !== 0 && row.status !== 1" class="cls-muted">—</span>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <div class="cls-empty">
                暂无预约记录。预约功能正常——只有已被他人借出的图书才能预约，预约成功后会显示在这里
              </div>
            </template>
          </el-table>

          <div v-if="reservations.length > PAGE_SIZE" class="pager">
            <el-pagination
              layout="prev, pager, next, total"
              :total="reservations.length"
              :page-size="PAGE_SIZE"
              v-model:current-page="pages.reservation"
              background
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 意见反馈已移到「消息通知」页面的第三个 Tab，这里不再重复入口 -->
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getMyBorrows,
  returnBook,
  renewBook,
  getMyFavorites,
  toggleFavorite,
  getMyReservations,
  cancelReservation,
  borrowFromReservation,
  borrowBook,
  getMyBookCounts
} from '@/api'

// 借阅 / 还书都会生成新通知，操作完成后通知父布局刷新右上角红点
const emit = defineEmits(['refresh-unread'])

const activeTab = ref('current')
const loading = ref(false)
const borrows = ref([])
const favorites = ref([])
const reservations = ref([])

const userStore = useUserStore()

/** 管理员（含待激活）不能借书：收藏 / 预约里的「去借阅」按钮要置灰 */
const canBorrow = computed(() => userStore.canBorrow)
const noBorrowReason = computed(() =>
  userStore.isPendingAdmin
    ? '管理员权限待激活：请在 10 天内归还全部图书，暂时不能借阅'
    : '管理员账号不能借阅图书，请前往管理端操作'
)

const currentBorrows = computed(() => borrows.value.filter((b) => b.status === 0 || b.status === 2))
const historyBorrows = computed(() => borrows.value.filter((b) => b.status === 1))

/**
 * 四个子 Tab 的分页：每页 10 条，各自独立记页码。
 * 后端接口本身不分页（收藏/预约返回全量，借阅按 100 条取回后前端按状态拆分），
 * 所以这里统一在前端切片，切 Tab、刷新列表时页码都回到第 1 页。
 */
const PAGE_SIZE = 10
const pages = reactive({ current: 1, history: 1, favorite: 1, reservation: 1 })

function slicePage(list, key) {
  const start = (pages[key] - 1) * PAGE_SIZE
  return list.slice(start, start + PAGE_SIZE)
}

const pagedCurrent = computed(() => slicePage(currentBorrows.value, 'current'))
const pagedHistory = computed(() => slicePage(historyBorrows.value, 'history'))
const pagedFavorites = computed(() => slicePage(favorites.value, 'favorite'))
const pagedReservations = computed(() => slicePage(reservations.value, 'reservation'))

/**
 * 四个子 Tab 的数量，全部由后端 /borrows/counts 用 COUNT 实时统计，
 * 不依赖当前页列表长度（借阅列表是分页的，长度不等于总数）。
 */
const counts = reactive({ current: 0, history: 0, favorite: 0, reservation: 0 })

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadBorrows(), loadFavorites(), loadReservations()])
    await loadCounts()
  } finally {
    loading.value = false
  }
}

async function loadBorrows() {
  const res = await getMyBorrows({ page: 1, size: 100 })
  borrows.value = res.data.list || []
  pages.current = 1
  pages.history = 1
  loadCounts()
}

async function loadFavorites() {
  const res = await getMyFavorites()
  favorites.value = res.data || []
  pages.favorite = 1
  loadCounts()
}

async function loadReservations() {
  const res = await getMyReservations()
  reservations.value = res.data || []
  pages.reservation = 1
  loadCounts()
}

/** 同一批并发刷新只发一次统计请求 */
let countsTask = null

/** 刷新四个 Tab 的数量（借还、收藏、预约等操作后都会重新统计） */
function loadCounts() {
  if (countsTask) return countsTask
  countsTask = (async () => {
    try {
      const res = await getMyBookCounts()
      const data = res.data || {}
      counts.current = data.currentCount ?? 0
      counts.history = data.historyCount ?? 0
      counts.favorite = data.favoriteCount ?? 0
      counts.reservation = data.reservationCount ?? 0
    } catch (e) {
      /* 统计失败不影响列表展示，保持上一次的数量 */
    }
  })().finally(() => {
    countsTask = null
  })
  return countsTask
}

/* ---------------- 借阅操作 ---------------- */

/**
 * 把后端返回的日期统一解析成 Date。
 * 后端 LocalDateTime 会序列化成 "2026-09-14T16:00:00"，旧代码把它 replace 成
 * "2026/09/14T16:00:00" 这种非法格式，new Date() 得到 Invalid Date —— 这就是「剩余 NaN 天」的根因。
 * 另外早期接口还可能返回数组 [y,m,d,h,mi,s]，这里一并兼容。
 */
function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, mi = 0, s = 0] = value
    const date = new Date(y || 0, (m || 1) - 1, d || 1, h || 0, mi || 0, s || 0)
    return Number.isNaN(date.getTime()) ? null : date
  }
  const date = new Date(String(value).trim().replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

/** 一行显示完整时间：2026-09-14 16:00（去掉秒） */
function formatDateTime(value) {
  const date = toDate(value)
  if (!date) return '-'
  const p = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}`
}

/** 借阅状态文案：0借阅中 / 1已归还 / 2已逾期 */
function borrowStatusText(status) {
  return ['借阅中', '已归还', '已逾期'][status] ?? '借阅中'
}

/** 借阅中(蓝) / 已归还(绿) / 已逾期(红) */
function borrowStatusType(status) {
  return ['primary', 'success', 'danger'][status] ?? 'primary'
}

/** 借阅期限（天）：与后端 application.yml 的 cloud-library.borrow.days 保持一致 */
const BORROW_DAYS = 10
/** 书名最多显示的汉字数，超出用省略号截断 */
const TITLE_MAX = 7

function shortTitle(title) {
  if (!title) return '-'
  const text = String(title)
  return text.length > TITLE_MAX ? `${text.slice(0, TITLE_MAX)}…` : text
}

/**
 * 应还时间：统一按「借阅时间 + BORROW_DAYS(10) 天」重算。
 * 改配置之前产生的旧记录，后端 dueDate 仍是 30 天，直接读它就会显示 30 天，
 * 所以这里一律以借阅时间为准；若以后要严格沿用后端 dueDate（例如体现续借后的新期限），
 * 把下一行改成 `return toDate(row.dueDate) || fallback` 即可。
 */
function dueDateOf(row) {
  const start = toDate(row.borrowDate)
  if (!start) return toDate(row.dueDate)
  return new Date(start.getTime() + BORROW_DAYS * 24 * 3600 * 1000)
}

/** 剩余天数；已归还或无法推算应还时间时返回 null，避免显示 NaN */
function remainDays(row) {
  if (row.status === 1) return null
  const due = dueDateOf(row)
  if (!due) return null
  return Math.ceil((due.getTime() - Date.now()) / (24 * 3600 * 1000))
}

function remainText(row) {
  const days = remainDays(row)
  if (days === null) return '-'
  if (days < 0) return `逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今天到期'
  return `剩 ${days} 天`
}

function remainClass(row) {
  const days = remainDays(row)
  if (days === null) return 'cls-muted'
  if (days < 0) return 'danger-text'
  if (days <= 3) return 'warn-text'
  return 'ok-text'
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm(`确定要归还《${row.bookTitle}》吗？`, '归还确认', {
      confirmButtonText: '确认归还',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch (e) {
    return
  }
  try {
    await returnBook(row.id)
    ElMessage.success('归还成功')
    loadBorrows()
    // 还书成功会生成通知，立刻刷新红点，不用等轮询
    emit('refresh-unread')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleRenew(row) {
  try {
    await ElMessageBox.confirm(
      `确定要续借《${row.bookTitle}》吗？续借可延长 15 天，需在应还日前 3 天内办理。`,
      '续借确认',
      { confirmButtonText: '确认续借', cancelButtonText: '取消', type: 'info' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await renewBook(row.id)
    ElMessage.success(res.data)
    loadBorrows()
    emit('refresh-unread')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ---------------- 收藏操作 ---------------- */

async function handleCancelFavorite(item) {
  try {
    await toggleFavorite(item.bookId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleBorrowFavorite(item) {
  try {
    await borrowBook(item.bookId)
    ElMessage.success('操作成功，请到「当前借阅」查看结果')
    // 借阅会产生通知，刷新红点；收藏列表也要刷新，按钮才会变成「已借阅」
    loadBorrows()
    loadFavorites()
    emit('refresh-unread')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ---------------- 预约操作 ---------------- */

function statusText(status) {
  return ['等待借阅', '可借阅', '已借阅', '已取消', '超时取消'][status] || '未知'
}

function statusType(status) {
  return ['info', 'success', 'primary', 'info', 'danger'][status] || 'info'
}

async function handleCancelReservation(row) {
  try {
    await ElMessageBox.confirm(`确定要取消对《${row.bookTitle}》的预约吗？`, '取消预约', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await cancelReservation(row.id)
    ElMessage.success('已取消预约')
    loadReservations()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleBorrowReservation(row) {
  try {
    const res = await borrowFromReservation(row.id)
    ElMessage.success(res.data)
    loadReservations()
    loadBorrows()
    emit('refresh-unread')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

</script>

<style scoped>
/* 单元格一律单行显示，避免长词（如平台名）折行 */
:deep(.el-table .cell) {
  white-space: nowrap;
}

.book-title {
  font-weight: 600;
  color: var(--cls-text-main);
}

.book-author {
  display: block;
  font-size: 13px;
  color: var(--cls-text-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 时间列：一行完整显示 2026-09-14 16:00 */
.cell-time {
  font-size: 13px;
  white-space: nowrap;
}

/* 操作按钮强制同一行，不上下堆叠 */
.cell-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
}

.ok-text {
  color: var(--cls-success);
  font-weight: 500;
}

.warn-text {
  color: var(--cls-warning);
  font-weight: 500;
}

.danger-text {
  color: var(--cls-danger);
  font-weight: 600;
}

/* 分页：与管理端表格分页保持一致的样式 */
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
