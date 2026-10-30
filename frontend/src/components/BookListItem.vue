<template>
  <div class="book-list-item cls-hover-card" @click="goDetail">
    <!-- 封面 -->
    <div class="cover-wrap">
      <img
        v-if="book.coverUrl && !coverFailed"
        :src="book.coverUrl"
        :alt="book.title"
        class="cover"
        @error="coverFailed = true"
      />
      <div v-else class="cover cover-placeholder">
        <span class="ph-title">{{ book.title }}</span>
        <span class="ph-author">{{ book.author }}</span>
      </div>
    </div>

    <!-- 主体信息 -->
    <div class="main">
      <div class="line-1">
        <h3 class="title" :title="book.title">{{ book.title }}</h3>
        <el-tag v-if="book.recentBorrowed" size="small" type="danger" effect="light" round>最近借阅</el-tag>
        <el-tag size="small" :type="book.available ? 'success' : 'info'" effect="plain" round>
          {{ book.available ? '可借阅' : '暂无库存' }}
        </el-tag>
      </div>

      <!-- 信息行：作者 · 出版社 [完结] ……（最右侧）库存，单行不换行 -->
      <p class="meta">
        <span class="author">{{ book.author }}</span>
        <template v-if="book.publisher">
          <span class="split">·</span>
          <span class="publisher">{{ book.publisher }}</span>
        </template>
        <el-tag size="small" :type="isFinished ? 'success' : 'warning'" effect="plain" class="finish-tag">
          {{ finishText }}
        </el-tag>
        <span class="stock" :class="{ empty: !hasStock }">库存：{{ stock }} 本</span>
      </p>

      <p class="desc">{{ book.description || '暂无简介' }}</p>
    </div>

    <!-- 右侧：热度与操作 -->
    <div class="side">
      <div class="hot">
        <el-rate :model-value="book.hotStars || 0" disabled size="small" />
        <span class="hot-text">{{ hotText }}</span>
      </div>

      <div class="actions">
        <!-- 管理员（含待激活）不能借阅、库存为 0 时：按钮置灰并给出原因 -->
        <el-tooltip :content="noBorrowReason" placement="top" :disabled="borrowedByMe || canBorrowNow">
          <span class="borrow-slot">
            <el-button
              :type="borrowBtnType"
              size="small"
              :disabled="!borrowedByMe && !canBorrowNow"
              @click.stop="borrowedByMe ? handleReturn() : emit('borrow', book)"
            >
              <el-icon>
                <RefreshLeft v-if="borrowedByMe" />
                <Reading v-else-if="hasStock" />
                <Goods v-else />
              </el-icon>
              <span>{{ borrowText }}</span>
            </el-button>
          </span>
        </el-tooltip>

        <!-- 没库存时借阅按钮置灰，这里保留「预约」入口 -->
        <el-tooltip v-if="canReserve" content="暂无库存，可先预约，图书归还后系统会通知你" placement="top">
          <el-button size="small" type="warning" plain @click.stop="handleReserve">
            <el-icon><Clock /></el-icon>
            <span>预约</span>
          </el-button>
        </el-tooltip>

        <el-tooltip :content="book.favorited ? '取消收藏' : '加入收藏'" placement="top">
          <button
            type="button"
            class="fav-btn"
            :class="{ active: book.favorited }"
            :aria-label="book.favorited ? '取消收藏' : '加入收藏'"
            @click.stop="emit('favorite', book)"
          >
            <el-icon :size="18">
              <StarFilled v-if="book.favorited" />
              <Star v-else />
            </el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Goods, Reading, RefreshLeft, Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { reserveBook, returnBook } from '@/api'

const props = defineProps({
  book: { type: Object, required: true }
})

const emit = defineEmits(['borrow', 'favorite', 'reserve', 'return'])
const router = useRouter()
const userStore = useUserStore()

/** 库存余量（后端没给就当 0，即暂无库存） */
const stock = computed(() => (props.book.stock == null ? 0 : props.book.stock))
/** 还有没有库存：有库存才能借 */
const hasStock = computed(() => stock.value > 0)

/** 管理员 / 待激活管理员不能借阅，按钮置灰 */
const canBorrow = computed(() => userStore.canBorrow)
/** 封面图是否加载失败：失败时回退到「书名 + 作者」占位，避免白块和 alt 文字破坏布局 */
const coverFailed = ref(false)
/** 当前用户已借阅这本书且还没还（借阅中 / 逾期都算） */
const borrowedByMe = computed(() => !!props.book.borrowedByMe)
/** 已借阅且已逾期 */
const overdue = computed(() => borrowedByMe.value && !!props.book.myBorrowOverdue)
/** 真正能点「借阅」：不是管理员 + 还有库存 */
const canBorrowNow = computed(() => canBorrow.value && hasStock.value)
/** 没库存时仍然可以预约（不是管理员、自己也没借这本书） */
const canReserve = computed(() => canBorrow.value && !hasStock.value && !borrowedByMe.value)

const borrowText = computed(() => {
  if (borrowedByMe.value) return '去归还'
  if (!canBorrow.value) return '不可借阅'
  return hasStock.value ? '借阅' : '暂无库存'
})
const borrowBtnType = computed(() => {
  if (borrowedByMe.value) return overdue.value ? 'danger' : 'warning'
  return hasStock.value ? 'primary' : 'info'
})
const noBorrowReason = computed(() => {
  if (borrowedByMe.value || canBorrowNow.value) return ''
  if (!canBorrow.value) {
    return userStore.isPendingAdmin
      ? '管理员权限待激活：请在 10 天内归还全部图书，暂时不能借阅'
      : '管理员账号不能借阅图书'
  }
  return '该图书暂无库存，请先预约'
})

/** 去归还：已借未还时按钮直接变成「去归还」 */
async function handleReturn() {
  const borrowId = props.book.myBorrowId
  if (!borrowId) {
    ElMessage.info('请到「我的借阅」中归还这本书')
    router.push('/user/books')
    return
  }
  try {
    await ElMessageBox.confirm(
      overdue.value
        ? `《${props.book.title}》已逾期，确定现在归还吗？`
        : `确定要归还《${props.book.title}》吗？`,
      overdue.value ? '逾期归还' : '归还确认',
      { confirmButtonText: '确认归还', cancelButtonText: '再想想', type: overdue.value ? 'warning' : 'info' }
    )
  } catch (e) {
    return
  }
  try {
    await returnBook(borrowId)
    ElMessage.success('归还成功，感谢使用云图书管理系统')
    emit('return', props.book)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 预约：库存为 0 时的兜底入口 */
async function handleReserve() {
  try {
    await ElMessageBox.confirm(
      `《${props.book.title}》当前暂无库存，确定要预约排队吗？图书归还后系统会第一时间通知你。`,
      '预约确认',
      { confirmButtonText: '提交预约', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await reserveBook(props.book.id)
    ElMessage.success(res?.message || '预约成功，图书归还后会通知你')
    emit('reserve', props.book)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

const hotText = computed(() => {
  const count = props.book.favoriteCount || 0
  return count > 0 ? `${count} 人收藏` : '暂无收藏'
})

/** 是否完结：后端 isFinished 为 1 表示已完结 */
const isFinished = computed(() => props.book.isFinished === 1)
const finishText = computed(() => (isFinished.value ? '已完结' : '连载中'))

function goDetail() {
  router.push(`/user/book/${props.book.id}`)
}
</script>

<style scoped>
/* 列表（详情模式）每行：跟卡片同一套风格 —— 深色半透明底 + 主题色发光边框，
   悬停时整行高亮、边框更亮、轻微上浮 */
.book-list-item {
  background-color: var(--cls-card);
  background-image: var(--cls-card-inner);
  border: 1px solid var(--cls-card-border);
  border-radius: 0;
  box-shadow: var(--cls-shadow);
  cursor: pointer;
  display: flex;
  gap: 14px;
  padding: 14px;
  align-items: stretch;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease,
    background-color 0.22s ease;
}

.book-list-item:hover {
  transform: translateY(-2px);
  border-color: var(--cls-card-border-hover);
  background-color: rgba(var(--cls-primary-rgb), 0.12);
  box-shadow: var(--cls-shadow-hover);
}

/* 封面 */
/* 书封：跟卡片封面一致，深色底 + 主色描边 */
.cover-wrap {
  width: 84px;
  height: 116px;
  flex-shrink: 0;
  border-radius: 0;
  overflow: hidden;
  background: var(--cls-card-soft);
  border: 1px solid rgba(var(--cls-primary-rgb), 0.3);
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 无封面占位：深色半透明 + 主色淡染 */
.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  text-align: center;
  background: linear-gradient(150deg, var(--card-bg-soft) 0%, var(--fill) 100%);
}

.ph-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--cls-text-main);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ph-author {
  font-size: 11px;
  color: var(--cls-text-secondary);
}

/* 主体 */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.line-1 {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--cls-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.meta {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--cls-text-regular);
  /* 单行布局：库存固定在最右侧，其余内容过长省略号截断，绝不换行 */
  flex-wrap: nowrap;
  white-space: nowrap;
  overflow: hidden;
}

.split {
  flex-shrink: 0;
  color: var(--cls-text-secondary);
}

/* 作者：优先完整显示；出版社：吃掉剩余宽度，过长省略 */
.author {
  min-width: 0;
  flex: 0 0 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.publisher {
  min-width: 0;
  flex: 0 1 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.desc {
  margin: 0;
  font-size: 13px;
  color: var(--cls-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 右侧 */
.side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  gap: 10px;
}

.hot {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.hot :deep(.el-rate) {
  height: 20px;
}

.hot-text {
  font-size: 12px;
  color: var(--cls-text-secondary);
}

/* 库存：固定在信息行最右侧（margin-left:auto），不参与压缩、绝不换行 */
.stock {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.stock.empty {
  color: var(--cls-danger);
  font-weight: 600;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 收藏：纯五角星图标按钮（灰色空心星 / 收藏后金色实心星）。
   不用 el-button circle：全局「直角」主题会把圆形按钮压成方框，观感像一块方块 */
.fav-btn {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--cls-text-secondary);
  transition: color 0.2s ease, transform 0.2s ease;
}

.fav-btn:hover {
  transform: scale(1.15);
  color: var(--cls-warning);
}

.fav-btn.active {
  color: var(--cls-warning);
}

.borrow-slot {
  display: inline-flex;
}

@media (max-width: 640px) {
  .book-list-item {
    flex-direction: column;
  }

  .cover-wrap {
    width: 100%;
    height: 150px;
  }

  .side {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }
}
</style>
