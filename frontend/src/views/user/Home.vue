<template>
  <div class="home">
    <!-- ============ AI 智能搜索区 ============ -->
    <section class="search-card">
      <div class="search-head">
        <div class="head-left">
          <div class="head-icon">
            <el-icon :size="22"><MagicStick /></el-icon>
          </div>
          <div>
            <h2>AI 智能搜索</h2>
            <p>输入即搜索，实时显示结果；输入一句话后按回车，可走 AI 语义匹配</p>
          </div>
        </div>
      </div>

      <div class="search-bar">
        <el-input
          v-model="query"
          size="large"
          placeholder="搜索书名、作者、平台、简介内容，或输入「连载中 / 已完结」"
          clearable
          @keyup.enter="handleEnter"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="statusFilter" size="large" class="status-select" placeholder="完结状态">
          <el-option label="全部" value="" />
          <el-option label="连载中" value="0" />
          <el-option label="已完结" value="1" />
        </el-select>
      </div>

      <div v-if="hotTagList.length" class="quick-tags">
        <span class="tag-label">热门搜索：</span>
        <el-tag
          v-for="tag in hotTagList"
          :key="tag"
          class="quick-tag"
          :class="{ personalized: isPersonalTag(tag) }"
          effect="plain"
          round
          @click="quickSearch(tag)"
        >
          <el-icon v-if="isPersonalTag(tag)" class="tag-icon"><User /></el-icon>
          {{ tag }}
        </el-tag>
      </div>
    </section>

    <!-- ============ 猜你喜欢（未搜索时展示） ============ -->
    <section v-if="!searched" class="cls-card guess-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>猜你喜欢</span>
          <span class="cls-sub-title">全部馆藏 {{ guessBooks.length }} 本 · 最近借阅优先</span>
        </div>
        <div class="title-actions">
          <el-tooltip content="刷新推荐" placement="top">
            <el-button size="small" circle :loading="guessLoading" @click="loadGuess">
              <el-icon v-if="!guessLoading"><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-radio-group v-model="viewMode" size="small" class="view-switch">
            <el-radio-button value="card">卡片</el-radio-button>
            <el-radio-button value="list">列表</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-loading="guessLoading" class="guess-body">
        <!-- 卡片图片模式：每页 8 本，两排每排 4 本 -->
        <div v-if="viewMode === 'card'" class="book-grid guess-grid">
          <BookCard
            v-for="book in pagedGuessBooks"
            :key="book.id"
            :book="book"
            @borrow="handleBorrow"
            @favorite="handleFavorite"
            @return="patchBook"
            @reserve="patchBook"
          />
        </div>

        <!-- 列表模式：每页 10 本 -->
        <div v-else class="book-list">
          <BookListItem
            v-for="book in pagedGuessBooks"
            :key="book.id"
            :book="book"
            @borrow="handleBorrow"
            @favorite="handleFavorite"
            @return="patchBook"
            @reserve="patchBook"
          />
        </div>

        <div v-if="!guessLoading && guessBooks.length === 0" class="cls-empty">暂无图书，请先在管理端添加图书</div>
      </div>

      <div v-if="guessBooks.length > guessPageSize" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="guessBooks.length"
          :page-size="guessPageSize"
          :current-page="guessPage"
          background
          @current-change="handleGuessPageChange"
        />
      </div>
    </section>

    <!-- ============ 搜索结果区 ============ -->
    <section v-else class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>搜索结果</span>
          <span class="cls-sub-title">
            <template v-if="lastQuery">关键词「{{ lastQuery }}」</template>
            <template v-else>全部馆藏</template>
            <template v-if="statusLabel">· {{ statusLabel }}</template>
            共 {{ total }} 本
            <template v-if="semanticMode">（AI 语义匹配）</template>
          </span>
        </div>
        <div class="title-actions">
          <el-radio-group v-model="resultViewMode" size="small" class="view-switch">
            <el-radio-button value="card">卡片</el-radio-button>
            <el-radio-button value="list">列表</el-radio-button>
          </el-radio-group>
          <el-button text @click="resetSearch">
            <el-icon><Close /></el-icon>
            <span>清空搜索</span>
          </el-button>
        </div>
      </div>

      <div v-loading="searching" class="result-body">
        <!-- 卡片图片模式 -->
        <div v-if="resultViewMode === 'card'" class="book-grid">
          <BookCard
            v-for="book in books"
            :key="book.id"
            :book="book"
            @borrow="handleBorrow"
            @favorite="handleFavorite"
            @return="patchBook"
            @reserve="patchBook"
          />
        </div>

        <!-- 列表模式（默认） -->
        <div v-else class="book-list">
          <BookListItem
            v-for="book in books"
            :key="book.id"
            :book="book"
            @borrow="handleBorrow"
            @favorite="handleFavorite"
            @return="patchBook"
            @reserve="patchBook"
          />
        </div>

        <div v-if="!searching && books.length === 0" class="cls-empty">
          <el-icon :size="42"><Search /></el-icon>
          <p>没有找到相关图书，换个关键词或筛选条件试试吧</p>
        </div>
      </div>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          background
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import BookListItem from '@/components/BookListItem.vue'
import {
  getBooks,
  getBookDetail,
  semanticSearch,
  guessYouLike,
  borrowBook,
  toggleFavorite,
  hotKeywords,
  recordSearch
} from '@/api'

/** 输入即搜索的防抖时长（毫秒） */
const SEARCH_DEBOUNCE = 350
/** 停止输入多久后，把当前关键词记为一次有效搜索（避免记录中间输入片段） */
const SEARCH_LOG_DELAY = 1500
/** 搜索结果每页条数卡片模式 8 本 / 列表模式 10 本（随展示模式变化，改模式会重新拉取） */
const PAGE_SIZE_CARD = 8
const PAGE_SIZE_LIST = 10
/** 借阅期限（天）：与后端 application.yml 的 cloud-library.borrow.days 保持一致 */
const BORROW_DAYS = 10

const query = ref('')
const lastQuery = ref('')
const books = ref([])
const guessBooks = ref([])
const total = ref(0)
const page = ref(1)
const searching = ref(false)
const guessLoading = ref(false)
const searched = ref(false)
const semanticMode = ref(false)
/** 猜你喜欢的展示方式：card 卡片图片模式 / list 列表模式 */
const viewMode = ref('card')
/** 搜索结果的展示方式：card 卡片图片模式 / list 列表模式（默认列表模式） */
const resultViewMode = ref('list')
/** 完结状态筛选：'' 全部 / '0' 连载中 / '1' 已完结 */
const statusFilter = ref('')

/** 当前完结状态筛选值，'' 表示不过滤（不向后端传该参数） */
const isFinishedParam = computed(() =>
  statusFilter.value === '' ? undefined : Number(statusFilter.value)
)

/** 当前展示模式下的每页条数：卡片 8 / 列表 10 */
const pageSize = computed(() => (resultViewMode.value === 'card' ? PAGE_SIZE_CARD : PAGE_SIZE_LIST))

/** 猜你喜欢的页码，以及当前模式下的每页条数（卡片 8 / 列表 10） */
const guessPage = ref(1)
const guessPageSize = computed(() => (viewMode.value === 'card' ? PAGE_SIZE_CARD : PAGE_SIZE_LIST))

/** 猜你喜欢当前页数据：接口一次返回全部，这里在前端切片 */
const pagedGuessBooks = computed(() => {
  const start = (guessPage.value - 1) * guessPageSize.value
  return guessBooks.value.slice(start, start + guessPageSize.value)
})

/** 完结状态筛选的中文说明，展示在结果标题里 */
const statusLabel = computed(() => {
  if (statusFilter.value === '0') {
    return '连载中'
  }
  if (statusFilter.value === '1') {
    return '已完结'
  }
  return ''
})

/** 热门搜索词（前几个来自当前用户，其余来自全平台） */
const hotTagList = ref([])
const personalTags = ref([])

// 请求序号：输入很快时先发出的请求可能后返回，用它丢弃过期结果
let searchSeq = 0
let searchTimer = null
let logTimer = null

onMounted(() => {
  loadGuess()
  loadHotKeywords()
})

onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  clearTimeout(logTimer)
})

/* ==================== 热门搜索 ==================== */

/** 加载热门搜索词：个人高频词优先，不足时用全平台高频词补足 */
async function loadHotKeywords() {
  try {
    const res = await hotKeywords()
    const data = res.data || {}
    personalTags.value = data.personal || []
    hotTagList.value = data.keywords || []
  } catch (e) {
    personalTags.value = []
    hotTagList.value = []
  }
}

function isPersonalTag(tag) {
  return personalTags.value.includes(tag)
}

/* ==================== 猜你喜欢 ==================== */

/** 加载全部图书，其中最近借阅过的排在最前面 */
async function loadGuess() {
  guessLoading.value = true
  try {
    // limit 传 0 表示返回全部图书
    const res = await guessYouLike({ limit: 0 })
    guessBooks.value = res.data || []
    /* 借阅 / 归还 / 收藏后刷新时保留当前页码：
       只有图书变少了（当前页超出范围）才回退到最后一页 */
    const maxPage = Math.max(1, Math.ceil(guessBooks.value.length / guessPageSize.value))
    if (guessPage.value > maxPage) {
      guessPage.value = maxPage
    }
  } catch (e) {
    guessBooks.value = []
  } finally {
    guessLoading.value = false
  }
}

/* ==================== 输入即搜索 ==================== */

watch([query, statusFilter], () => {
  clearTimeout(searchTimer)
  clearTimeout(logTimer)

  const keyword = (query.value || '').trim()
  if (!keyword) {
    // 没输入关键词但选了完结状态时，仍然按状态筛选展示全部馆藏
    if (statusFilter.value) {
      searchTimer = setTimeout(() => runKeywordSearch(''), SEARCH_DEBOUNCE)
    } else {
      backToGuess()
    }
    return
  }

  // 打字过程中实时搜索（走本地关键词检索，响应快）
  searchTimer = setTimeout(() => runKeywordSearch(keyword), SEARCH_DEBOUNCE)
  // 停止输入后才记一次有效搜索，避免把「科」「科幻」这类中间输入片段写进搜索记录
  logTimer = setTimeout(() => submitSearchLog(keyword), SEARCH_LOG_DELAY)
})

/**
 * 切换卡片 / 列表模式：两种模式每页条数不同（卡片 8 / 列表 10），
 * 所以回到第 1 页并按新的每页条数重新查询
 */
watch(resultViewMode, () => {
  if (!searched.value) return
  page.value = 1
  if (semanticMode.value) {
    runSemanticSearch(lastQuery.value)
  } else {
    runKeywordSearch(lastQuery.value)
  }
})

/** 猜你喜欢切换卡片 / 列表模式：两种模式每页条数不同，先回到第 1 页 */
watch(viewMode, () => {
  guessPage.value = 1
})

/** 猜你喜欢翻页 */
function handleGuessPageChange(current) {
  guessPage.value = current
}

/** 清空输入：回到猜你喜欢 */
function backToGuess() {
  searchSeq++
  searched.value = false
  semanticMode.value = false
  books.value = []
  total.value = 0
  page.value = 1
  lastQuery.value = ''
  loadGuess()
}

/**
 * 关键词检索（实时搜索走这条路径）
 *
 * @param keyword  关键词，空字符串表示只按完结状态筛选
 * @param keepPage true = 保留当前页码（借阅后刷新用），false = 新搜索回到第 1 页
 */
async function runKeywordSearch(keyword, keepPage = false) {
  const seq = ++searchSeq
  lastQuery.value = keyword
  searched.value = true
  semanticMode.value = false
  if (!keepPage) {
    page.value = 1
  }
  searching.value = true
  try {
    const res = await getBooks({
      page: page.value,
      size: pageSize.value,
      keyword,
      isFinished: isFinishedParam.value
    })
    if (seq !== searchSeq) {
      return
    }
    books.value = res.data.list || []
    total.value = Number(res.data.total || 0)
  } catch (e) {
    if (seq === searchSeq) {
      books.value = []
      total.value = 0
    }
  } finally {
    if (seq === searchSeq) {
      searching.value = false
    }
  }
}

/**
 * AI 语义搜索（自然语言描述）
 *
 * @param keepPage true = 回退到关键词检索时保留当前页码（借阅后刷新用）
 */
async function runSemanticSearch(keyword, keepPage = false) {
  const seq = ++searchSeq
  lastQuery.value = keyword
  searched.value = true
  semanticMode.value = true
  searching.value = true
  let hit = false
  try {
    const res = await semanticSearch({ query: keyword, limit: pageSize.value })
    if (seq !== searchSeq) {
      return
    }
    // 语义接口不支持状态筛选，这里在前端过滤，保证结果与筛选条件一致
    const list = (res.data || []).filter(
      (book) => !statusFilter.value || String(book.isFinished ?? 0) === statusFilter.value
    )
    if (list.length > 0) {
      books.value = list
      total.value = list.length
      hit = true
    }
  } catch (e) {
    /* 语义接口异常时静默回退到关键词检索 */
  } finally {
    if (seq === searchSeq) {
      searching.value = false
    }
  }
  // 语义未命中：回退到普通关键词检索
  if (!hit && seq === searchSeq) {
    await runKeywordSearch(keyword, keepPage)
  }
}

/** 是否像自然语言（含空格、标点或较长） */
function isNaturalLanguage(text) {
  return text.length > 6 && /[，,、。？?\s]/.test(text)
}

/** 回车：视为明确搜索意图，立即执行并记录 */
async function handleEnter() {
  const keyword = query.value.trim()
  if (!keyword) {
    return
  }
  clearTimeout(searchTimer)
  clearTimeout(logTimer)
  submitSearchLog(keyword)

  if (isNaturalLanguage(keyword)) {
    await runSemanticSearch(keyword)
  } else {
    await runKeywordSearch(keyword)
  }
}

/** 点击热门搜索词 */
function quickSearch(tag) {
  if (query.value === tag) {
    runKeywordSearch(tag)
    return
  }
  // 赋值后由 watch 统一触发防抖搜索
  query.value = tag
}

/** 上报一次有效搜索，并刷新热门搜索词 */
async function submitSearchLog(keyword) {
  try {
    await recordSearch(keyword)
    loadHotKeywords()
  } catch (e) {
    /* 记录失败不影响搜索本身 */
  }
}

/** 分页：沿用上一次的关键词 */
async function loadByKeyword() {
  const res = await getBooks({
    page: page.value,
    size: pageSize.value,
    keyword: lastQuery.value,
    isFinished: isFinishedParam.value
  })
  books.value = res.data.list || []
  total.value = Number(res.data.total || 0)
}

function handlePageChange(current) {
  page.value = current
  searching.value = true
  loadByKeyword().finally(() => {
    searching.value = false
  })
}

function resetSearch() {
  // 置空后由 watch 自动回到猜你喜欢
  query.value = ''
  statusFilter.value = ''
}

/* ==================== 借阅 / 收藏 ==================== */

/** 借阅 / 预约 */
async function handleBorrow(book) {
  // 没库存时借阅按钮已经置灰，能走到这里说明有库存，直接确认借阅
  const tip = `确定要借阅《${book.title}》吗？借阅期限 ${BORROW_DAYS} 天。`
  try {
    await ElMessageBox.confirm(tip, '借阅确认', {
      confirmButtonText: '确认借阅',
      cancelButtonText: '再想想',
      type: 'info'
    })
  } catch (e) {
    return
  }
  try {
    const res = await borrowBook(book.id)
    ElMessage.success(res.data)
    // 只原地刷新这一本书（库存、「去归还」状态），列表顺序 / 页码 / 滚动位置都不动
    patchBook(book)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 收藏 / 取消收藏 */
async function handleFavorite(book) {
  try {
    const res = await toggleFavorite(book.id)
    book.favorited = res.data
    book.favoriteCount = Math.max(0, (book.favoriteCount || 0) + (res.data ? 1 : -1))
    ElMessage.success(res.message || '操作成功')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/**
 * 借阅 / 归还 / 预约成功后：只刷新这一本书，列表绝不动。
 *
 * <p>详情接口会返回最新的库存、「borrowedByMe / myBorrowId」等展示字段，
 * 用 Object.assign 原地合并到列表里那本书上：卡片 key 不变、顺序不变、页码不变，
 * 用户翻页或重新搜索时才会重新拉取整页数据。</p>
 */
async function patchBook(book) {
  try {
    const res = await getBookDetail(book.id)
    if (res.data) {
      Object.assign(book, res.data)
    }
  } catch (e) {
    /* 静默失败：列表刷新留给用户翻页时进行 */
  }
}
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 搜索区 */
.search-card {
  background: var(--cls-card);
  border-radius: var(--cls-radius);
  padding: 24px 24px 20px;
  box-shadow: var(--cls-shadow);
}

.search-head {
  margin-bottom: 18px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.head-icon {
  width: 46px;
  height: 46px;
  border-radius: 0;
  background: var(--btn-bg);
  color: var(--btn-text);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.head-left h2 {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
}

.head-left p {
  margin: 0;
  font-size: 13px;
  color: var(--cls-text-secondary);
}

.search-bar {
  display: flex;
  gap: 12px;
}

.status-select {
  width: 140px;
  flex-shrink: 0;
}

.quick-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.tag-label {
  font-size: 13px;
  color: var(--cls-text-secondary);
}

/*
 * 热门搜索词条：常态 = 白底（浅色主题）/ 浅底（深色主题）+ 主题边框 + 正文色；
 * 悬停 = 底色只压深一点点 + 文字用最强对比色（--chip-hover-text），
 * 保证任何主题下都不会出现「同色底 + 同色字」看不清的情况。
 */
.quick-tag {
  cursor: pointer;
  background: var(--chip-bg);
  border-color: var(--chip-border);
  color: var(--chip-text);
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.quick-tag:hover {
  background: var(--chip-hover-bg);
  border-color: var(--chip-hover-border);
  color: var(--chip-hover-text);
}

/* 当前用户自己的高频搜索词，与全平台热词做视觉区分 */
.quick-tag.personalized {
  border-color: var(--cls-warning-border);
  color: var(--cls-warning);
  background: var(--cls-warning-soft);
}

/* 悬停：底色保持浅橙，文字压到最强对比色（浅色主题深灰 / 深色主题纯白），看得清 */
.quick-tag.personalized:hover {
  border-color: var(--cls-warning-border);
  background: var(--cls-warning-soft);
  color: var(--chip-hover-text);
}

.tag-icon {
  margin-right: 3px;
  vertical-align: -2px;
}

/* 猜你喜欢 */
.guess-body {
  min-height: 140px;
}

/* 搜索结果 */
.result-body {
  min-height: 120px;
}

.title-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.view-switch {
  flex-shrink: 0;
}

/* 图书网格（卡片模式） */
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
  min-height: 120px;
}

/* 猜你喜欢：固定 4 列，每页 8 本正好两排 */
.guess-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

/* 图书列表（列表模式） */
.book-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cls-empty p {
  margin: 10px 0 0;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

/* 屏幕变窄时依次降为 3 列、2 列，卡片不会挤成一条 */
@media (max-width: 1200px) {
  .guess-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .guess-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .search-bar {
    flex-direction: column;
  }

  .status-select {
    width: 100%;
  }

  .cls-section-title {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
