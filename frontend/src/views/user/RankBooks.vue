<template>
  <div class="rank-books">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>{{ pageTitle }}</span>
          <span class="cls-sub-title">共 {{ books.length }} 本 · 按热度（搜索次数）排序</span>
        </div>
        <el-button size="small" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回排行榜</span>
        </el-button>
      </div>

      <div v-loading="loading" class="list-body">
        <div v-if="books.length" class="book-list">
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

        <div v-else-if="!loading" class="cls-empty">{{ emptyText }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { borrowBook, getBookDetail, getBooks, toggleFavorite } from '@/api'
import BookListItem from '@/components/BookListItem.vue'

const route = useRoute()
const router = useRouter()

const books = ref([])
const loading = ref(false)

/** 维度：author-按作者 / platform-按平台，由排行榜页通过 query 传入 */
const dim = computed(() => (route.query.dim === 'platform' ? 'platform' : 'author'))
const name = computed(() => String(route.query.name || '').trim())
const isPlatform = computed(() => dim.value === 'platform')

const pageTitle = computed(() => {
  if (!name.value) return '图书列表'
  return isPlatform.value ? `${name.value} · 平台图书` : `${name.value} · 全部作品`
})

const emptyText = computed(() =>
  name.value ? `未找到与「${name.value}」相关的图书` : '缺少查询条件，请从排行榜进入'
)

/** 一次取足数量后在前端精确过滤，避免分页导致作品被截断 */
const FETCH_SIZE = 200

/**
 * 加载该作者 / 该平台的图书：
 * 后端只支持关键词模糊检索，所以先用名称当关键词把候选捞回来，
 * 再按 author / platform 字段精确匹配（匹配不到时退化为包含匹配），最后按热度排序。
 */
async function loadBooks() {
  if (!name.value) {
    books.value = []
    return
  }
  loading.value = true
  try {
    const res = await getBooks({ keyword: name.value, page: 1, size: FETCH_SIZE })
    const list = res.data?.list || []
    const field = isPlatform.value ? 'platform' : 'author'
    const target = name.value.toLowerCase()

    let matched = list.filter((b) => String(b[field] || '').trim().toLowerCase() === target)
    if (matched.length === 0) {
      matched = list.filter((b) => String(b[field] || '').toLowerCase().includes(target))
    }

    // 按热度排序：搜索次数优先，其次收藏人数
    matched.sort(
      (a, b) =>
        (b.searchCount || 0) - (a.searchCount || 0) ||
        (b.favoriteCount || 0) - (a.favoriteCount || 0)
    )
    books.value = matched
  } catch (e) {
    books.value = []
  } finally {
    loading.value = false
  }
}

/* ==================== 借阅 / 收藏 ==================== */

/** 借阅（没库存时按钮已置灰，能走到这里说明有库存；预约走卡片上的「预约」按钮） */
async function handleBorrow(book) {
  const tip = `确定要借阅《${book.title}》吗？`
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
    // 只原地刷新这一本书（库存、「去归还」状态），列表顺序 / 滚动位置都不动
    patchBook(book)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/**
 * 借阅 / 归还 / 预约成功后：只刷新这一本书，列表绝不动。
 *
 * <p>详情接口会返回最新的库存、「borrowedByMe / myBorrowId」等展示字段，
 * 原地合并到列表里那本书上，用户离开再进来时才重新拉取整个列表。</p>
 */
async function patchBook(book) {
  try {
    const res = await getBookDetail(book.id)
    if (res.data) {
      Object.assign(book, res.data)
    }
  } catch (e) {
    /* 静默失败：列表刷新留给用户重新进入页面时进行 */
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

function goBack() {
  router.push('/user/hot')
}

watch([dim, name], loadBooks)
onMounted(loadBooks)
</script>

<style scoped>
.list-body {
  min-height: 120px;
}

.book-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
