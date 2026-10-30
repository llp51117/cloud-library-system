<template>
  <div class="hot-rank">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>图书排行榜</span>
          <span class="cls-sub-title">实时统计前 10 名</span>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="loadRank">
        <el-tab-pane label="热门排行" name="hot" />
        <el-tab-pane label="收藏排行" name="favorite" />
        <el-tab-pane label="活跃排行" name="active" />
      </el-tabs>

      <div class="rank-toolbar">
        <p class="rank-tip">{{ tipText }}</p>
        <div class="toolbar-actions">
          <el-select
            v-if="activeTab !== 'active'"
            v-model="dim"
            size="small"
            class="dim-select"
            @change="loadRank"
          >
            <el-option label="按书籍" value="book" />
            <el-option label="按作者" value="author" />
            <el-option label="按平台" value="platform" />
          </el-select>

          <el-radio-group
            v-if="activeTab === 'active'"
            v-model="activeMode"
            size="small"
            class="view-switch"
            @change="handleModeChange"
          >
            <el-radio-button value="list">列表</el-radio-button>
            <el-radio-button value="chart">折线图</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-loading="loading" class="rank-body">
        <!-- ===================== 活跃排行（真实用户） ===================== -->
        <template v-if="activeTab === 'active'">
          <div v-show="activeMode === 'list'" class="rank-list">
            <div
              v-for="(u, index) in activeListRows"
              :key="u.userId"
              class="rank-item user-item clickable"
              :title="`可点击查看 ${userName(u)} 的个人主页`"
              @click="goUserHomepage(u)"
            >
              <div class="rank-no">
                <span class="no-text">{{ index + 1 }}</span>
              </div>
              <div class="user-avatar">
                <img v-if="u.avatar" :src="u.avatar" alt="头像" />
                <span v-else>{{ avatarText(u) }}</span>
              </div>
              <div class="rank-info">
                <div class="rank-title">{{ userName(u) }}</div>
                <div class="rank-meta">
                  <span>借阅 {{ u.borrowCount }}</span>
                  <span>评论 {{ u.commentCount }}</span>
                  <span>收藏 {{ u.favoriteCount }}</span>
                  <span>注册于 {{ formatDate(u.createdAt) }}</span>
                </div>
              </div>
              <div class="rank-value">
                <el-tag size="small" effect="plain" type="warning">活跃第 {{ u.activeRank }} 名</el-tag>
                <span class="value">{{ u.value }}</span>
                <span class="unit">活跃度</span>
              </div>
            </div>
            <div v-if="!loading && activeListRows.length === 0" class="cls-empty">暂无排行数据</div>
          </div>
          <div v-show="activeMode === 'chart'" ref="chartRef" class="rank-chart" />
          <div v-if="!loading && list.length === 0 && activeMode === 'chart'" class="cls-empty">暂无排行数据</div>
        </template>

        <!-- ===================== 热门 / 收藏排行 ===================== -->
        <template v-else>
          <!-- 左列第 1-5 名，右列第 6-10 名 -->
          <div v-if="list.length" class="rank-columns">
            <div v-for="(col, ci) in rankColumns" :key="ci" class="rank-col">
              <div
                v-for="item in col"
                :key="item.bookId || item.name"
                class="book-row clickable"
                :class="[rowToneClass(item.rank), { 'no-thumb': !isBookDim }]"
                @click="goDetail(item)"
              >
                <!-- 只有按书籍才有封面：作者 / 平台维度没有图片，不显示缩略图 -->
                <div v-if="isBookDim" class="row-thumb">
                  <img v-if="item.coverUrl" :src="item.coverUrl" :alt="displayName(item)" />
                  <span v-else class="row-thumb-ph">{{ thumbText(item) }}</span>
                </div>

                <div class="row-info">
                  <div class="row-line">
                    <span class="row-no">{{ item.rank }}</span>
                    <span class="row-title">{{ displayName(item) }}</span>
                  </div>
                  <div v-if="isBookDim" class="row-sub">{{ displaySub(item) }}</div>
                </div>

                <span class="row-value">{{ item.value }}<span class="unit">{{ unitText }}</span></span>
              </div>
            </div>
          </div>

          <div v-else-if="!loading" class="cls-empty">暂无排行数据</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getRanks } from '@/api'
import { cssVar } from '@/utils/themes'
import { useUserHomepageNav } from '@/utils/userHome'

const router = useRouter()
/** 个人主页跳转：与评论区点头像/昵称完全同一套规则 */
const { openUserHome } = useUserHomepageNav()
const activeTab = ref('hot')
const list = ref([])
const loading = ref(false)

/** 统计维度：book-按书籍 / author-按作者 / platform-按平台 */
const dim = ref('book')
/** 活跃榜展示模式：list-列表 / chart-折线图 */
const activeMode = ref('list')

const chartRef = ref(null)
let chart = null

const isBookDim = computed(() => dim.value === 'book')

const tipText = computed(() => {
  if (activeTab.value === 'active') {
    return '统计真实用户的活跃度（借阅 ×3 + 评论 ×2 + 收藏），取前 10 名后按注册时间从早到晚排列'
  }
  const dimText = isBookDim.value ? '图书' : dim.value === 'author' ? '作者名下图书' : '平台内图书'
  const metric = activeTab.value === 'favorite' ? '被收藏的人数' : '被搜索的次数'
  return `按${dimText}${metric}排序`
})

const unitText = computed(() => {
  if (activeTab.value === 'hot') return '次搜索'
  if (activeTab.value === 'favorite') return '人收藏'
  return '活跃度'
})

/**
 * 热门 / 收藏排行：左列第 1-5 名，右列第 6-10 名。
 * 每行额外带上 rank，供模板渲染名次与配色。
 */
const rankColumns = computed(() => [
  list.value.slice(0, 5).map((item, i) => ({ ...item, rank: i + 1 })),
  list.value.slice(5, 10).map((item, i) => ({ ...item, rank: i + 6 }))
])

/** 前三名金色，4-10 名灰褐色（名次与名称同色） */
function rowToneClass(rank) {
  return rank <= 3 ? 'tone-gold' : 'tone-gray'
}

/**
 * 活跃用户公共数据：只保留真实的注册用户——没有账号邮箱的行直接丢弃，
 * 绝不渲染成「未知用户」。顺带算出每个人的「活跃度排名」。
 */
const activeUsers = computed(() => {
  const rows = list.value.filter((u) => userName(u))
  const sorted = [...rows].sort((a, b) => (b.value || 0) - (a.value || 0))
  const rankMap = new Map()
  sorted.forEach((u, i) => {
    if (!rankMap.has(u.userId)) rankMap.set(u.userId, i + 1)
  })
  return rows.map((u) => ({ ...u, activeRank: rankMap.get(u.userId) || 0 }))
})

/** 列表模式：按活跃度从高到低 */
const activeListRows = computed(() => [...activeUsers.value].sort((a, b) => (b.value || 0) - (a.value || 0)))

/** 折线图模式：按注册时间从早到晚 */
const activeChartRows = computed(() =>
  [...activeUsers.value].sort((a, b) => timeValue(a.createdAt) - timeValue(b.createdAt))
)

onMounted(() => {
  loadRank()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

async function loadRank() {
  loading.value = true
  try {
    const res = await getRanks({ type: activeTab.value, dim: dim.value, limit: 10 })
    list.value = res.data || []
    if (activeTab.value === 'active' && activeMode.value === 'chart') {
      await nextTick()
      renderChart()
    }
  } finally {
    loading.value = false
  }
}

function handleModeChange() {
  if (activeTab.value === 'active' && activeMode.value === 'chart') {
    nextTick(renderChart)
  }
}

function handleResize() {
  if (chart) chart.resize()
}

function renderChart() {
  if (!chartRef.value) return
  // 切换 Tab 会重建容器，旧实例绑定到已销毁的 DOM 时需要释放
  if (chart && chart.getDom() !== chartRef.value) {
    chart.dispose()
    chart = null
  }
  chart = chart || echarts.init(chartRef.value)

  // 折线图按注册时间从早到晚，同样只画真实用户
  const rows = activeChartRows.value
  chart.setOption({
    // top 留出空间，最高的头像不会被裁掉
    grid: { left: 52, right: 26, top: 56, bottom: 58 },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        // 折线 + 头像两条 series 都会命中，取折线那条即可（dataIndex 与 rows 一一对应）
        const list = Array.isArray(params) ? params : [params]
        const p = list.find((x) => x.seriesType === 'line') || list[0]
        const u = rows[p.dataIndex] || {}
        return [
          `<b>${userName(u)}</b>`,
          `活跃度：${u.value || 0}`,
          `借阅 ${u.borrowCount || 0} · 评论 ${u.commentCount || 0} · 收藏 ${u.favoriteCount || 0}`,
          `注册时间：${formatDate(u.createdAt)}`
        ].join('<br/>')
      }
    },
    xAxis: {
      type: 'category',
      data: rows.map((u) => shortName(u)),
      axisLabel: { interval: 0, rotate: 24, color: cssVar('--text-sub', '#8a94a6') },
      axisLine: { lineStyle: { color: cssVar('--border', '#e4e8f0') } }
    },
    yAxis: {
      type: 'value',
      name: '活跃度',
      nameTextStyle: { color: cssVar('--text-sub', '#8a94a6') },
      splitLine: { lineStyle: { color: cssVar('--border-soft', '#f0f2f7') } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: rows.map((u) => u.value || 0),
        lineStyle: { width: 3, color: cssVar('--cls-primary', '#4f6ef7') },
        itemStyle: { color: cssVar('--cls-primary', '#4f6ef7') },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `rgba(${cssVar('--cls-primary-rgb', '79,110,247')},0.26)` },
            { offset: 1, color: `rgba(${cssVar('--cls-primary-rgb', '79,110,247')},0.02)` }
          ])
        }
      },
      {
        // 每个数据点正上方的圆形头像；symbolOffset 让头像浮在点上方，不压住折线
        name: 'avatar',
        type: 'scatter',
        clip: false,
        z: 10,
        symbolSize: 24,
        symbolOffset: [0, -22],
        data: rows.map((u, i) => ({
          value: [i, u.value || 0],
          symbol: `image://${DEFAULT_AVATAR}`
        }))
      }
    ]
  })
  loadAvatars(rows)
}

/* ---------------- 数据点头像 ---------------- */

/** 头像绘制尺寸（canvas 分辨率） */
const AVATAR_CANVAS = 48
/** 头像 dataURL 缓存：同一个 URL 只画一次 */
const avatarCache = new Map()

/** 没有头像时的灰色圆形占位图 */
const DEFAULT_AVATAR = createGrayAvatar()

function createGrayAvatar() {
  const canvas = document.createElement('canvas')
  canvas.width = AVATAR_CANVAS
  canvas.height = AVATAR_CANVAS
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = cssVar('--border', '#d8dde6')
  ctx.beginPath()
  ctx.arc(AVATAR_CANVAS / 2, AVATAR_CANVAS / 2, AVATAR_CANVAS / 2, 0, Math.PI * 2)
  ctx.fill()
  return canvas.toDataURL('image/png')
}

/**
 * 把头像裁成圆形，返回可直接给 ECharts 用的 dataURL。
 * 没有头像、加载失败或跨域导致 canvas 被污染时，统一回退到灰色圆形占位图。
 */
function circleAvatar(url) {
  if (!url) return Promise.resolve(DEFAULT_AVATAR)
  if (avatarCache.has(url)) return Promise.resolve(avatarCache.get(url))
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = AVATAR_CANVAS
        canvas.height = AVATAR_CANVAS
        const ctx = canvas.getContext('2d')
        ctx.beginPath()
        ctx.arc(AVATAR_CANVAS / 2, AVATAR_CANVAS / 2, AVATAR_CANVAS / 2, 0, Math.PI * 2)
        ctx.clip()
        // 居中裁剪成正方形再缩放，避免头像被拉变形
        const side = Math.min(img.width, img.height)
        ctx.drawImage(
          img,
          (img.width - side) / 2,
          (img.height - side) / 2,
          side,
          side,
          0,
          0,
          AVATAR_CANVAS,
          AVATAR_CANVAS
        )
        const dataUrl = canvas.toDataURL('image/png')
        avatarCache.set(url, dataUrl)
        resolve(dataUrl)
      } catch (e) {
        resolve(DEFAULT_AVATAR)
      }
    }
    img.onerror = () => resolve(DEFAULT_AVATAR)
    img.src = url
  })
}

/** 头像异步加载完成后，只更新头像 series 的 symbol，不动折线 */
async function loadAvatars(rows) {
  const dom = chartRef.value
  const symbols = await Promise.all(rows.map((u) => circleAvatar(u.avatar)))
  if (!chart || chart.getDom() !== dom) return
  chart.setOption({
    series: [
      {},
      {
        data: rows.map((u, i) => ({
          value: [i, u.value || 0],
          symbol: `image://${symbols[i]}`
        }))
      }
    ]
  })
}

/** 缩略图占位文字：取名称前两个字 */
function thumbText(item) {
  return displayName(item).slice(0, 2)
}

function displayName(item) {
  return isBookDim.value ? item.bookTitle : item.name
}

function displaySub(item) {
  return isBookDim.value ? item.bookAuthor : `${item.bookCount} 本图书`
}

/** 显示名：优先用昵称；没有昵称就用邮箱前缀（@ 前面的部分），不展示完整邮箱 */
function userName(u) {
  if (u.nickname) return String(u.nickname)
  const email = u.userEmail || u.email || ''
  return email ? String(email).split('@')[0] : ''
}

/** 折线图 X 轴用邮箱前缀，避免标签过长 */
function shortName(u) {
  const email = userName(u)
  return email ? String(email).split('@')[0] : ''
}

function avatarText(u) {
  const name = userName(u)
  return name ? name.slice(0, 1).toUpperCase() : ''
}

/**
 * 点击活跃排行里的用户：进个人主页。
 * 规则与评论区一致——点自己进「个人信息」，点别人进「用户主页」，超管提示不公开。
 */
function goUserHomepage(u) {
  return openUserHome({ id: u.userId, isSuperAdmin: u.isSuperAdmin || 0 })
}

/** 后端返回的日期可能是数组 [y,m,d,...] 也可能是字符串，统一转成时间戳用于排序 */
function timeValue(value) {
  if (!value) return 0
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1] = value
    return new Date(y || 0, (m || 1) - 1, d || 1).getTime()
  }
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isNaN(t) ? 0 : t
}

function formatDate(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d] = value
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return String(value).slice(0, 10)
}

/**
 * 点击某一行：
 * - 按书籍 → 该书详情页
 * - 按作者 → 「该作者的书」列表页
 * - 按平台 → 「该平台的书」列表页（按热度排序）
 */
function goDetail(item) {
  if (!item) return
  if (isBookDim.value) {
    if (item.bookId) router.push(`/user/book/${item.bookId}`)
    return
  }
  const name = item.name
  if (!name) return
  router.push({
    path: '/user/rank-books',
    query: { dim: dim.value, name, from: activeTab.value }
  })
}
</script>

<style scoped>
.rank-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.rank-tip {
  margin: 0;
  font-size: 13px;
  color: var(--cls-text-secondary);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.dim-select {
  width: 108px;
}

.rank-body {
  min-height: 120px;
}

/* ============ 热门 / 收藏排行：左 5 右 5 两列 ============ */
.rank-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
  align-items: start;
}

.rank-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

/* 每行卡片：深底 + 主题色淡染渐变 + 轻微阴影 */
.book-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px;
  border: 1px solid var(--cls-border);
  border-radius: 0;
  background: linear-gradient(90deg, var(--cls-card) 0%, var(--cls-card-soft) 100%);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.book-row.clickable {
  cursor: pointer;
}

.book-row.clickable:hover {
  border-color: var(--card-border-hover);
  box-shadow: var(--shadow-hover);
  transform: translateY(-1px);
}

/* 60×80 竖版缩略图，直角（不做圆角） */
.row-thumb {
  flex-shrink: 0;
  width: 60px;
  height: 80px;
  border-radius: 0;
  overflow: hidden;
  background: var(--card-bg-soft);
  box-shadow: var(--shadow);
}

.row-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0;
}

/* 没有封面时的渐变占位图 */
.row-thumb-ph {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: var(--btn-bg);
  color: var(--btn-text);
  font-size: 16px;
  font-weight: 700;
}

.row-info {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
}

.row-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.row-no {
  flex-shrink: 0;
  min-width: 22px;
  text-align: center;
  font-size: 15px;
  font-weight: 800;
}

.row-title {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 15px;
  font-weight: 600;
}

.row-sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--cls-text-secondary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 名次 + 名称统一配色：前三名纯金色，4-10 名灰褐色，都不用渐变 */
.tone-gold .row-no,
.tone-gold .row-title {
  color: var(--cls-warning);
}

.tone-gray .row-no {
  color: var(--text-sub);
}

.tone-gray .row-title {
  color: var(--text-regular);
}

/* 按作者 / 按平台：没有缩略图，名字放大一号，颜色跟随名次（金 / 灰褐） */
.book-row.no-thumb {
  padding: 10px 16px;
}

.book-row.no-thumb .row-title {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

/* 右侧数据：浅蓝色小字 */
.row-value {
  flex-shrink: 0;
  margin-left: auto;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-highlight);
}

.row-value .unit {
  margin-left: 3px;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-sub);
}

@media (max-width: 860px) {
  .rank-columns {
    grid-template-columns: 1fr;
  }
}

/* ============ 列表 ============ */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  border-radius: var(--cls-radius);
  background: var(--cls-card-soft);
  border: 1px solid var(--cls-border);
  transition: all 0.2s ease;
}

.rank-item.clickable {
  cursor: pointer;
}

.rank-item.clickable:hover {
  background: var(--fill);
  border-color: var(--card-border-hover);
  transform: translateX(3px);
}

.rank-no {
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.no-text {
  font-size: 17px;
  font-weight: 700;
  color: var(--cls-text-secondary);
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cls-text-main);
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--cls-text-regular);
}

.rank-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-shrink: 0;
}

.rank-value .value {
  font-size: 19px;
  font-weight: 700;
  color: var(--cls-primary-ink);
}

.rank-value .unit {
  font-size: 12px;
  color: var(--cls-text-secondary);
}

/* ============ 活跃用户 ============ */
.user-item {
  background: var(--cls-card-soft);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--btn-bg);
  color: var(--btn-text);
  font-size: 17px;
  font-weight: 700;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rank-chart {
  width: 100%;
  height: 360px;
}
</style>
