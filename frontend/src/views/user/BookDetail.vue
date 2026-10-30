<template>
  <div v-loading="loading" class="book-detail">
    <!-- 返回 -->
    <div class="back-row">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
    </div>

    <!-- 图书信息 -->
    <section class="cls-card info-card">
      <div class="cover-wrap">
        <!-- 详情页用竖向的 cover_detail_crop 渲染 -->
        <CoverImage
          v-if="book.coverUrl"
          :src="book.coverUrl"
          :crop="book.coverDetailCrop"
          :alt="book.title"
          :title="book.title"
          :author="book.author"
          class="cover"
        />
        <div v-else class="cover cover-placeholder">
          <span class="cover-title">{{ book.title }}</span>
          <span class="cover-author">{{ book.author }}</span>
        </div>
      </div>

      <div class="info-main">
        <div class="title-row">
          <h1>{{ book.title }}</h1>
          <el-tag :type="book.available ? 'success' : 'info'" effect="dark" round>
            {{ book.available ? '可借阅' : '暂无库存' }}
          </el-tag>
        </div>

        <p class="author-line">
          <span>{{ book.author }}</span>
          <el-tag v-if="book.platform" size="small" effect="plain">{{ book.platform }}</el-tag>
          <el-tag size="small" :type="book.isFinished === 1 ? 'success' : 'warning'" effect="plain">
            {{ book.isFinished === 1 ? '已完结' : '连载中' }}
          </el-tag>
        </p>

        <div class="hot-line">
          <span class="label">收藏热度</span>
          <el-rate :model-value="book.hotStars || 0" disabled />
          <span class="cls-muted">（{{ book.favoriteCount || 0 }} 人收藏）</span>
        </div>

        <el-descriptions :column="2" border size="small" class="desc-table">
          <el-descriptions-item label="出版社">{{ book.publisher || '—' }}</el-descriptions-item>
          <el-descriptions-item label="出版时间">{{ book.publishDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="价格">
            {{ book.price != null ? '¥ ' + book.price : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="借阅次数">{{ book.borrowCount || 0 }} 次</el-descriptions-item>
          <el-descriptions-item label="库存">
            <span :class="{ 'stock-empty': !hasStock }">{{ stock }} 本</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 待激活管理员虽然不能借书，但自己借过的书要能在这里直接还 -->
        <div v-if="!isAdmin || (isPendingAdmin && borrowedByMe)" class="action-row">
          <!-- 已借阅未归还：按钮直接变成「去归还」（逾期标红） -->
          <template v-if="borrowedByMe">
            <el-button :type="overdue ? 'danger' : 'warning'" size="large" @click="handleReturn">
              <el-icon><RefreshLeft /></el-icon>
              <span>去归还</span>
            </el-button>
          </template>
          <!-- 有库存：直接借阅；没库存：借阅按钮置灰显示「暂无库存」，另给预约入口 -->
          <template v-else-if="canBorrow">
            <el-button
              :type="hasStock ? 'primary' : 'info'"
              size="large"
              :disabled="!hasStock"
              @click="handleBorrow"
            >
              <el-icon>
                <Reading v-if="hasStock" />
                <Goods v-else />
              </el-icon>
              <span>{{ hasStock ? '立即借阅' : '暂无库存' }}</span>
            </el-button>
            <el-button v-if="!hasStock" size="large" type="warning" plain @click="handleReserve">
              <el-icon><Clock /></el-icon>
              <span>预约排队</span>
            </el-button>
          </template>
          <el-button v-else size="large" type="info" plain disabled>
            <el-icon><Lock /></el-icon>
            <span>{{ isPendingAdmin ? '待激活中，暂不能借阅' : '管理员不能借阅' }}</span>
          </el-button>
          <el-button
            size="large"
            :type="book.favorited ? 'warning' : 'default'"
            :plain="!book.favorited"
            @click="handleFavorite"
          >
            <el-icon>
              <StarFilled v-if="book.favorited" />
              <Star v-else />
            </el-icon>
            <span>{{ book.favorited ? '已收藏' : '收藏' }}</span>
          </el-button>
        </div>

        <!-- 管理员 / 待激活管理员：说明为什么不能借书 -->
        <el-alert
          v-if="!canBorrow"
          class="borrow-tip"
          :type="isPendingAdmin ? 'warning' : 'info'"
          :closable="false"
          show-icon
          :title="isPendingAdmin
            ? '您已被设为管理员，请在 10 天内归还所有图书，归还成功后即可行使管理员权限，暂时不能借阅。'
            : '管理员账号不能借阅图书，请前往管理端进行操作。'"
        />
      </div>
    </section>

    <!-- 图书简介 -->
    <section class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>图书简介</span>
        </div>
      </div>
      <p class="description">{{ book.description || '暂无简介信息。' }}</p>
    </section>

    <!-- 评论区 -->
    <section class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>读者评论</span>
          <span class="cls-sub-title">共 {{ commentTotal }} 条</span>
        </div>
      </div>

      <div class="comment-editor">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="300"
          show-word-limit
          placeholder="分享你的读书感受吧～（Enter 发送，Ctrl + Enter 换行；含敏感词的评论将被系统自动删除）"
          @keydown.enter.exact.prevent="onCommentEnter"
          @keydown.ctrl.enter.prevent="insertNewline('comment')"
        />
        <div class="editor-footer">
          <span class="tip">
            <el-icon><WarningFilled /></el-icon>
            请文明发言，违规评论将被处理
          </span>
          <el-button type="primary" :loading="submitting" @click="handleAddComment">发表评论</el-button>
        </div>
      </div>

      <div v-loading="commentLoading" class="comment-list">
        <div
          v-for="item in comments"
          :id="'comment-' + item.id"
          :key="item.id"
          class="comment-item"
          :class="{
            highlight: highlightId === item.id,
            reported: isAdmin && item.isReported === 1 && highlightId !== item.id
          }"
        >
          <!-- 头像：超管齿轮+魂环 / 正式管理员头像+魂环 / 普通用户与待激活管理员头像；点击进入个人主页 -->
          <RoleAvatar
            :size="38"
            class="clickable-avatar"
            :avatar="item.userAvatar"
            :role="item.userRole || 0"
            :super-admin="item.userSuperAdmin || 0"
            :pending-admin="item.userPendingAdmin || 0"
            :nickname="item.userNickname || maskEmail(item.userEmail)"
            title="查看个人主页"
            @click="goUserHomepage(item)"
          />

          <div class="comment-body">
            <div class="comment-head">
              <span
                class="email clickable-name"
                :title="`查看 ${item.userNickname || maskEmail(item.userEmail)} 的个人主页`"
                @click="goUserHomepage(item)"
              >
                {{ item.userNickname || maskEmail(item.userEmail) }}
              </span>
              <el-tag v-if="isAdminIdentity(item)" size="small" :type="item.userSuperAdmin === 1 ? 'danger' : 'warning'" effect="dark">
                {{ item.userSuperAdmin === 1 ? '最高管理员' : '管理员' }}
              </el-tag>
              <!-- 管理员封号：红色小标签 -->
              <el-tag v-if="item.userAdminTitle" size="small" type="danger" effect="plain">
                {{ item.userAdminTitle }}
              </el-tag>
              <span class="time">{{ formatTime(item.createdAt) }}</span>
            </div>

            <!-- 正常评论 -->
            <p v-if="item.status === 0 || item.status === 3" class="comment-text">
              {{ item.content }}
            </p>
            <!-- 系统已删 -->
            <p v-else-if="item.status === 1" class="comment-text deleted system">
              <el-icon><CircleClose /></el-icon>
              该评论包含敏感词，已被系统自动删除
            </p>
            <!-- 管理员删除 -->
            <p v-else class="comment-text deleted admin">
              <el-icon><CircleClose /></el-icon>
              该评论已被管理员删除
            </p>

            <div class="comment-actions">
              <el-button
                text
                size="small"
                class="like-btn"
                :class="{ liked: item.liked }"
                @click="handleLike(item)"
              >
                <el-icon>
                  <component :is="item.liked ? 'StarFilled' : 'Star'" />
                </el-icon>
                <span>点赞 {{ item.likeCount || 0 }}</span>
              </el-button>
              <!-- 踩：一人只能踩一次，与点赞互相独立 -->
              <el-button
                text
                size="small"
                class="dislike-btn"
                :class="{ disliked: item.disliked }"
                @click="handleDislike(item)"
              >
                <span class="dislike-icon">👎</span>
                <span>踩 {{ item.dislikeCount || 0 }}</span>
              </el-button>
              <!-- 回复：只支持一层楼中楼 -->
              <el-button
                v-if="item.status === 0 || item.status === 3"
                text
                size="small"
                @click="toggleReplyBox(item)"
              >
                <el-icon><ChatDotRound /></el-icon>
                <span>回复</span>
              </el-button>
              <el-button
                text
                size="small"
                class="report-btn"
                :class="{ reported: item.isReported === 1 }"
                @click="handleReport(item)"
              >
                <el-icon><Bell /></el-icon>
                <span>@管理员{{ item.isReported === 1 ? ' ' + (item.reportCount || 0) : '' }}</span>
              </el-button>
              <el-button
                v-if="isAdmin && item.status === 0"
                text
                size="small"
                type="danger"
                @click="handleAdminDelete(item)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>

            <!-- 回复输入框（点击「回复」展开，只支持一层） -->
            <div v-if="replyTargetId === item.id" class="reply-editor">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                resize="none"
                maxlength="300"
                show-word-limit
                :placeholder="`回复 ${item.userNickname || maskEmail(item.userEmail)}：（Enter 发送，Ctrl + Enter 换行）`"
                @keydown.enter.exact.prevent="onReplyEnter(item)"
                @keydown.ctrl.enter.prevent="insertNewline('reply')"
              />
              <div class="reply-editor-footer">
                <span class="reply-tip">回复不支持再次回复，也不能被 @管理员 举报</span>
                <div class="reply-editor-buttons">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button
                    type="primary"
                    size="small"
                    :loading="replySubmitting"
                    @click="submitReply(item)"
                  >
                    发送回复
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 楼中楼子回复：缩进显示，支持点赞/踩，不支持回复和 @管理员 -->
            <div v-if="item.replies && item.replies.length" class="reply-list">
              <div v-for="reply in item.replies" :key="reply.id" class="reply-item">
                <RoleAvatar
                  :size="28"
                  class="clickable-avatar"
                  :avatar="reply.userAvatar"
                  :role="reply.userRole || 0"
                  :super-admin="reply.userSuperAdmin || 0"
                  :pending-admin="reply.userPendingAdmin || 0"
                  :nickname="reply.userNickname || maskEmail(reply.userEmail)"
                  title="查看个人主页"
                  @click="goUserHomepage(reply)"
                />
                <div class="reply-body">
                  <div class="reply-head">
                    <span
                      class="email clickable-name"
                      @click="goUserHomepage(reply)"
                    >
                      {{ reply.userNickname || maskEmail(reply.userEmail) }}
                    </span>
                    <el-tag v-if="isAdminIdentity(reply)" size="small" :type="reply.userSuperAdmin === 1 ? 'danger' : 'warning'" effect="dark">
                      {{ reply.userSuperAdmin === 1 ? '最高管理员' : '管理员' }}
                    </el-tag>
                    <!-- 管理员封号：红色小标签 -->
                    <el-tag v-if="reply.userAdminTitle" size="small" type="danger" effect="plain">
                      {{ reply.userAdminTitle }}
                    </el-tag>
                    <span class="time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <p v-if="reply.status === 0 || reply.status === 3" class="comment-text">
                    {{ reply.content }}
                  </p>
                  <p v-else class="comment-text deleted">
                    <el-icon><CircleClose /></el-icon>
                    {{ reply.status === 1 ? '该评论包含敏感词，已被系统自动删除' : '该评论已被管理员删除' }}
                  </p>
                  <div class="comment-actions">
                    <el-button
                      text
                      size="small"
                      class="like-btn"
                      :class="{ liked: reply.liked }"
                      @click="handleLike(reply)"
                    >
                      <el-icon>
                        <component :is="reply.liked ? 'StarFilled' : 'Star'" />
                      </el-icon>
                      <span>点赞 {{ reply.likeCount || 0 }}</span>
                    </el-button>
                    <el-button
                      text
                      size="small"
                      class="dislike-btn"
                      :class="{ disliked: reply.disliked }"
                      @click="handleDislike(reply)"
                    >
                      <span class="dislike-icon">👎</span>
                      <span>踩 {{ reply.dislikeCount || 0 }}</span>
                    </el-button>
                    <el-button
                      v-if="isAdmin && reply.status === 0"
                      text
                      size="small"
                      type="danger"
                      @click="handleAdminDelete(reply)"
                    >
                      <el-icon><Delete /></el-icon>
                      <span>删除</span>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!commentLoading && comments.length === 0" class="cls-empty">
          还没有评论，快来抢沙发吧～
        </div>
      </div>

      <div v-if="commentTotal > commentSize" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="commentTotal"
          :page-size="commentSize"
          :current-page="commentPage"
          background
          @current-change="handleCommentPage"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Goods, Lock, Reading, RefreshLeft, Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useUserHomepageNav } from '@/utils/userHome'
import CoverImage from '@/components/CoverImage.vue'
import RoleAvatar from '@/components/RoleAvatar.vue'
import {
  getBookDetail,
  getComments,
  addComment,
  likeComment,
  dislikeComment,
  reportComment,
  borrowBook,
  reserveBook,
  returnBook,
  toggleFavorite,
  adminDeleteComment
} from '@/api'

/** 借阅期限（天）：与后端 application.yml 的 cloud-library.borrow.days 保持一致 */
const BORROW_DAYS = 10

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
/** 点击头像 / 昵称的统一跳转逻辑 */
const { openUserHome } = useUserHomepageNav()

/** 是否管理员视角（管理端「评论详情」页复用本组件） */
const isAdmin = computed(() => userStore.isAdmin)

/** 待激活管理员：停留在用户端，只能还书不能借书 */
const isPendingAdmin = computed(() => userStore.isPendingAdmin)

/** 能否借阅：管理员（含待激活）都不能借书 */
const canBorrow = computed(() => userStore.canBorrow)

/**
 * 点击评论头像 / 昵称的跳转：
 * 超管不公开个人信息 → 提示不跳转；点自己 → 个人信息页；点别人 → 用户主页
 */
function goUserHomepage(item) {
  return openUserHome({ id: item.userId, isSuperAdmin: item.userSuperAdmin })
}

const bookId = route.params.id
const book = reactive({})
const loading = ref(false)

const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(5)
const commentLoading = ref(false)
const commentContent = ref('')
const submitting = ref(false)
/** 楼中楼回复：当前展开输入框的评论ID、回复内容 */
const replyTargetId = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)

/** 从管理端举报列表「查看」跳转过来时，要定位并高亮的评论ID */
const highlightId = ref(route.query.commentId ? Number(route.query.commentId) : null)

/** 管理员核查跳转时一次加载更多评论，保证被举报的评论尽量都在本页可见 */
if (highlightId.value) {
  commentSize.value = 50
}

onMounted(() => {
  loadBook()
  loadComments()
})

async function loadBook() {
  loading.value = true
  try {
    const res = await getBookDetail(bookId)
    Object.assign(book, res.data || {})
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentLoading.value = true
  try {
    const res = await getComments({ bookId, page: commentPage.value, size: commentSize.value })
    comments.value = res.data.list || []
    commentTotal.value = Number(res.data.total || 0)
    await scrollToHighlight()
  } finally {
    commentLoading.value = false
  }
}

/**
 * 管理员核查跳转：
 * - 「查看」对应的那条评论 → 深红色边框高亮（highlight）
 * - 该书其他被举报的评论 → 浅红色边框标记（reported）
 * - 自动滚动到页面上最上面的那条被举报评论
 */
async function scrollToHighlight() {
  if (!highlightId.value) return
  await nextTick()
  const reportedEls = Array.from(
    document.querySelectorAll('.comment-item.highlight, .comment-item.reported')
  )
  if (reportedEls.length === 0) return
  reportedEls[0].scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function handleCommentPage(current) {
  commentPage.value = current
  loadComments()
}

/** 库存余量（后端没给就当 0） */
const stock = computed(() => (book.stock == null ? 0 : book.stock))
/** 还有没有库存：有库存才能借，没库存只能预约 */
const hasStock = computed(() => stock.value > 0)
/** 当前用户是否已借阅这本书且未归还 */
const borrowedByMe = computed(() => !!book.borrowedByMe)
/** 已借阅且已逾期 */
const overdue = computed(() => borrowedByMe.value && !!book.myBorrowOverdue)

async function handleBorrow() {
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
    loadBook()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 去归还：已借未还时直接归还（逾期会先提示） */
async function handleReturn() {
  const borrowId = book.myBorrowId
  if (!borrowId) {
    ElMessage.info('请到「我的借阅」中归还这本书')
    router.push('/user/books')
    return
  }
  try {
    await ElMessageBox.confirm(
      overdue.value
        ? `《${book.title}》已逾期，确定现在归还吗？`
        : `确定要归还《${book.title}》吗？`,
      overdue.value ? '逾期归还' : '归还确认',
      { confirmButtonText: '确认归还', cancelButtonText: '再想想', type: overdue.value ? 'warning' : 'info' }
    )
  } catch (e) {
    return
  }
  try {
    await returnBook(borrowId)
    ElMessage.success('归还成功，感谢使用云图书管理系统')
    loadBook()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 库存为 0 时的预约入口 */
async function handleReserve() {
  try {
    await ElMessageBox.confirm(
      `《${book.title}》当前暂无库存，确定要预约排队吗？图书归还后系统会第一时间通知你。`,
      '预约确认',
      { confirmButtonText: '提交预约', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await reserveBook(book.id)
    ElMessage.success(res?.message || '预约成功，图书归还后会通知你')
    loadBook()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleFavorite() {
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
 * 输入框 Enter 键发送（Ctrl + Enter 才换行）：
 * 内容为空时静默忽略，不弹「请输入评论内容」提示，避免误触。
 */
function onCommentEnter() {
  if (!commentContent.value.trim()) return
  handleAddComment()
}

function onReplyEnter(item) {
  if (!replyContent.value.trim()) return
  submitReply(item)
}

/** Ctrl + Enter 换行 */
function insertNewline(target) {
  if (target === 'reply') {
    replyContent.value += '\n'
  } else {
    commentContent.value += '\n'
  }
}

async function handleAddComment() {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    const res = await addComment({ bookId, content })
    commentContent.value = ''
    if (res.data?.status === 1) {
      ElMessage.warning('评论包含敏感词，已被系统自动删除')
    } else {
      ElMessage.success('评论发表成功')
    }
    commentPage.value = 1
    loadComments()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

/** 评论时间格式化：2026-09-15T23:01:46 → 2026-09-15 23:01:46 */
function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 点赞/取消点赞：一人对同一条评论只能赞一次，实时 +1 / -1 */
async function handleLike(item) {
  try {
    const res = await likeComment(item.id)
    item.liked = res.data.liked
    item.likeCount = res.data.likeCount
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 踩/取消踩：一人对同一条评论只能踩一次，与点赞互相独立，实时 +1 / -1 */
async function handleDislike(item) {
  try {
    const res = await dislikeComment(item.id)
    item.disliked = res.data.disliked
    item.dislikeCount = res.data.dislikeCount
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 展开 / 收起某条评论的回复输入框（只支持一层楼中楼） */
function toggleReplyBox(item) {
  if (replyTargetId.value === item.id) {
    cancelReply()
    return
  }
  replyTargetId.value = item.id
  replyContent.value = ''
}

function cancelReply() {
  replyTargetId.value = null
  replyContent.value = ''
}

/** 提交楼中楼回复：作为该评论的子回复，提交后重新加载评论列表 */
async function submitReply(item) {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySubmitting.value = true
  try {
    const res = await addComment({ bookId, content, parentId: item.id })
    if (res.data?.status === 1) {
      ElMessage.warning('回复包含敏感词，已被系统自动删除')
    } else {
      ElMessage.success('回复成功')
    }
    cancelReply()
    await loadComments()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    replySubmitting.value = false
  }
}

/** @管理员 举报：同一用户重复举报只计一次，举报次数实时更新 */
async function handleReport(item) {
  try {
    await ElMessageBox.confirm('确定要将该评论标记并通知管理员审核吗？', '举报确认', {
      confirmButtonText: '确定举报',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    const res = await reportComment(item.id)
    item.isReported = 1
    item.reportCount = res.data.reportCount
    if (res.data.alreadyReported) {
      ElMessage.info('你已举报过该评论，不重复计数')
    } else {
      ElMessage.success('已通知管理员审核')
    }
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 管理员删除任意评论 */
async function handleAdminDelete(item) {
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
    await adminDeleteComment(item.id)
    item.status = 2
    ElMessage.success('评论已删除')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

function maskEmail(email) {
  if (!email) return '匿名读者'
  const at = email.indexOf('@')
  if (at <= 2) return email
  return email.slice(0, 2) + '***' + email.slice(at)
}

/**
 * 评论 / 回复是否展示管理员身份标签：
 * 只有正式管理员（role=1 且 pending_admin=0）才挂「管理员 / 最高管理员」；
 * 待激活管理员（role=1 且 pending_admin=1）在用户端等同普通用户，不显示身份标签。
 */
function isAdminIdentity(user) {
  return Number(user?.userRole) === 1 && Number(user?.userPendingAdmin) !== 1
}
</script>

<style scoped>
.book-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.back-row {
  margin-bottom: -8px;
}

.info-card {
  display: flex;
  gap: 26px;
}

.cover-wrap {
  position: relative;
  width: 200px;
  height: 270px;
  flex-shrink: 0;
  border-radius: var(--cls-radius);
  overflow: hidden;
  background: var(--cls-primary-light);
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  text-align: center;
  background: linear-gradient(150deg, var(--card-bg-soft) 0%, var(--fill) 100%);
}

.cover-title {
  font-size: 19px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.4;
}

.cover-author {
  font-size: 13px;
  color: var(--text-sub);
}

.info-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-row h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.author-line {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--cls-text-regular);
}

.hot-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.hot-line .label {
  color: var(--cls-text-regular);
}

.desc-table {
  margin-top: 2px;
}

/* 库存为 0：醒目提示 */
.stock-empty {
  color: var(--cls-danger);
  font-weight: 600;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.description {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
}

/* 评论区 */
.comment-editor {
  margin-bottom: 22px;
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.editor-footer .tip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: var(--cls-warning);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 80px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--cls-border);
}

.comment-item:last-child {
  border-bottom: none;
}

/* 被举报评论定位高亮（管理员核查） */
.comment-item.highlight {
  border-radius: 0;
  border: 2px solid var(--cls-danger);
  background: var(--cls-danger-soft);
  padding: 14px 12px;
  animation: highlight-flash 1.2s ease-in-out 2;
}

@keyframes highlight-flash {
  0%, 100% { box-shadow: 0 0 0 0 rgba(var(--cls-danger-rgb), 0); }
  50% { box-shadow: 0 0 12px 2px rgba(var(--cls-danger-rgb), 0.45); }
}

/* 该书其他被举报的评论：浅红色边框标记，方便管理员一起看到 */
.comment-item.reported {
  border-radius: 0;
  border: 1.5px solid var(--cls-danger-border);
  background: var(--cls-danger-soft);
  padding: 13px 11px;
}

/* 管理员评论头像样式已抽到公共组件 components/GearAvatar.vue */

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-head .email {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--cls-text-main);
}

/* 头像 / 昵称可点击进入个人主页 */
.clickable-avatar,
.clickable-name {
  cursor: pointer;
}

.clickable-name:hover {
  color: var(--cls-primary-ink);
}

.clickable-avatar:hover {
  filter: brightness(1.08);
}

.borrow-tip {
  margin-top: 14px;
  border-radius: var(--cls-radius-sm);
}

.comment-head .time {
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.comment-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-text.deleted {
  display: flex;
  align-items: center;
  gap: 6px;
  font-style: italic;
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 0;
}

/* 被删评论的提示底色：深色半透明 + 亮色文字，暗色主题下不刺眼 */
.comment-text.deleted.system {
  color: var(--cls-warning);
  background: var(--cls-warning-soft);
}

.comment-text.deleted.admin {
  color: var(--cls-danger);
  background: var(--cls-danger-soft);
}

.comment-actions {
  margin-top: 6px;
  display: flex;
  gap: 4px;
  align-items: center;
}

/* 点赞按钮：已点赞变实心、高亮 */
.like-btn.liked {
  color: var(--cls-warning, #e6a23c);
  font-weight: 600;
}

/* 踩按钮：已踩变亮（灰底色不适合，用青灰色区分点赞的黄色） */
.dislike-btn {
  color: var(--cls-text-secondary);
}

.dislike-btn.disliked {
  color: var(--cls-info);
  font-weight: 600;
}

.dislike-btn.disliked .dislike-icon {
  filter: drop-shadow(0 0 4px rgba(var(--cls-info-rgb), 0.6));
}

.dislike-icon {
  font-size: 14px;
  line-height: 1;
  margin-right: 4px;
  filter: grayscale(1);
}

/* 回复输入框 */
.reply-editor {
  margin-top: 10px;
  padding: 12px;
  background: var(--cls-primary-light, #eff6ff);
  border-radius: var(--cls-radius-sm, 8px);
}

.reply-editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
}

.reply-tip {
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.reply-editor-buttons {
  display: flex;
  gap: 8px;
}

/* 楼中楼子回复：缩进显示 */
.reply-list {
  margin-top: 12px;
  padding: 2px 0 2px 14px;
  border-left: 2px solid var(--cls-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.reply-head .email {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--cls-text-main);
}

.reply-head .time {
  font-size: 11.5px;
  color: var(--cls-text-secondary);
}

.reply-body .comment-text {
  font-size: 13.5px;
}

/* @管理员 按钮：已举报变红变亮 */
.report-btn.reported {
  color: var(--cls-danger, #f56c6c);
  font-weight: 600;
}

.report-btn.reported :deep(.el-icon) {
  animation: none;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .info-card {
    flex-direction: column;
  }

  .cover-wrap {
    width: 100%;
    height: 220px;
  }
}
</style>
