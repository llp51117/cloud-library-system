<template>
  <div class="dashboard">
    <!-- ============ 关键数据卡片 ============ -->
    <section class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card cls-hover-card"
        :title="`查看${card.label}明细`"
        @click="goDetail(card)"
      >
        <div class="stat-icon" :style="{ background: card.gradient }">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">
            {{ overview[card.key] ?? 0 }}
            <span class="stat-unit">{{ card.unit }}</span>
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
        <el-icon class="stat-arrow" :size="16"><ArrowRight /></el-icon>
      </div>
    </section>

    <!-- ============ 图表区 ============ -->
    <section class="chart-grid">
      <div class="cls-card">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>热门图书 TOP5</span>
            <span class="cls-sub-title">按借阅次数统计</span>
          </div>
        </div>
        <div ref="barRef" v-loading="chartLoading" class="chart"></div>
      </div>

      <div class="cls-card">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>借阅趋势</span>
            <span class="cls-sub-title">近 {{ trendDays }} 天</span>
          </div>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="lineRef" v-loading="chartLoading" class="chart"></div>
      </div>
    </section>

    <!-- ============ AI 分析报告 ============ -->
    <section class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>AI 运营分析报告</span>
          <span class="cls-sub-title">基于实时数据自动生成</span>
        </div>
        <el-button type="primary" :loading="reportLoading" @click="loadReport">
          <el-icon><MagicStick /></el-icon>
          <span>生成分析报告</span>
        </el-button>
      </div>

      <div v-if="report" class="report-content">
        <pre>{{ report }}</pre>
      </div>
      <div v-else class="cls-empty">
        点击右上角「生成分析报告」，AI 将根据借阅数据给出运营建议
      </div>
    </section>

    <!-- ============ 用户活跃排行 ============ -->
    <section class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>用户活跃排行</span>
          <span class="cls-sub-title">活跃度 = 借阅 ×3 + 评论 ×2 + 收藏</span>
        </div>
      </div>

      <el-table v-loading="activeLoading" :data="activeUsers" stripe>
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <el-tag v-if="$index < 3" :type="['danger', 'warning', 'success'][$index]" size="small" effect="dark">
              TOP{{ $index + 1 }}
            </el-tag>
            <span v-else class="rank-text">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div
              class="user-cell clickable"
              :title="`可点击查看 ${row.userNickname || row.userEmail} 的个人主页`"
              @click="goUserHomepage(row)"
            >
              <el-avatar :size="30" :src="row.avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span>{{ row.userNickname || row.userEmail }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="borrowCount" label="借阅次数" width="110" align="center" />
        <el-table-column prop="commentCount" label="评论数" width="100" align="center" />
        <el-table-column prop="favoriteCount" label="收藏数" width="100" align="center" />
        <el-table-column label="活跃度" width="110" align="center">
          <template #default="{ row }">
            <span class="active-value">{{ row.value }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="cls-empty">暂无活跃用户数据</div>
        </template>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { adminGetOverview, adminGetHotBooks, adminGetTrend, adminGetActiveUsers, adminGetAiReport } from '@/api'
import { cssVar } from '@/utils/themes'
import { useUserHomepageNav } from '@/utils/userHome'

/** 个人主页跳转：与评论区点头像/昵称完全同一套规则 */
const { openUserHome } = useUserHomepageNav()
const overview = ref({})
const hotBooks = ref([])
const trend = ref([])
const activeUsers = ref([])
const trendDays = ref(7)
const chartLoading = ref(false)
const activeLoading = ref(false)
const reportLoading = ref(false)
const report = ref('')

const barRef = ref()
const lineRef = ref()
let barChart = null
let lineChart = null

/** 图标用明亮的色系（清爽蓝 / 薄荷绿 / 暖橙 / 珊瑚红），点卡片可跳到对应的明细页 */
const statCards = [
  {
    key: 'totalBooks',
    label: '总藏书量',
    unit: '本',
    icon: 'Reading',
    gradient: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    path: '/admin/books'
  },
  {
    key: 'totalUsers',
    label: '注册用户',
    unit: '人',
    icon: 'UserFilled',
    gradient: 'linear-gradient(135deg,#34d399,#10b981)',
    path: '/admin/users'
  },
  {
    key: 'borrowingCount',
    label: '当前借阅中',
    unit: '本',
    icon: 'Tickets',
    gradient: 'linear-gradient(135deg,#fbbf24,#f59e0b)',
    path: '/admin/borrows',
    query: { status: 0 }
  },
  {
    key: 'overdueCount',
    label: '逾期未还',
    unit: '本',
    icon: 'WarningFilled',
    gradient: 'linear-gradient(135deg,#f87171,#ef4444)',
    path: '/admin/borrows',
    query: { status: 2 }
  }
]

const router = useRouter()

/** 点数据卡片 → 跳到对应的管理页，借阅类带上状态筛选 */
function goDetail(card) {
  router.push({ path: card.path, query: card.query })
}

onMounted(() => {
  loadOverview()
  loadHotBooks()
  loadTrend()
  loadActiveUsers()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
})

function handleResize() {
  barChart?.resize()
  lineChart?.resize()
}

async function loadOverview() {
  try {
    const res = await adminGetOverview()
    overview.value = res.data || {}
  } catch (e) {
    /* 忽略 */
  }
}

async function loadHotBooks() {
  chartLoading.value = true
  try {
    const res = await adminGetHotBooks({ limit: 5 })
    hotBooks.value = res.data || []
    await nextTick()
    renderBar()
  } finally {
    chartLoading.value = false
  }
}

async function loadTrend() {
  chartLoading.value = true
  try {
    const res = await adminGetTrend({ days: trendDays.value })
    trend.value = res.data || []
    await nextTick()
    renderLine()
  } finally {
    chartLoading.value = false
  }
}

async function loadActiveUsers() {
  activeLoading.value = true
  try {
    const res = await adminGetActiveUsers({ limit: 10 })
    activeUsers.value = res.data || []
  } finally {
    activeLoading.value = false
  }
}

/**
 * 点击活跃排行里的用户：进个人主页。
 * 规则与评论区一致——点自己进「个人信息」，点别人进「用户主页」，
 * 超管提示不公开（正式管理员本身已被活跃榜排除，这里只是兜底）。
 */
function goUserHomepage(row) {
  return openUserHome({ id: row.userId, isSuperAdmin: row.isSuperAdmin || 0 })
}

async function loadReport() {
  reportLoading.value = true
  try {
    const res = await adminGetAiReport({ days: trendDays.value })
    report.value = res.data || ''
    ElMessage.success('分析报告生成成功')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    reportLoading.value = false
  }
}

function renderBar() {
  if (!barRef.value) return
  barChart = barChart || echarts.init(barRef.value)

  if (!hotBooks.value.length) {
    barChart.clear()
    barChart.setOption({
      title: { text: '暂无借阅数据', left: 'center', top: 'middle', textStyle: { color: cssVar('--text-sub', '#96a3b4'), fontSize: 13, fontWeight: 'normal' } }
    })
    return
  }

  barChart.setOption({
    grid: { left: 50, right: 24, top: 30, bottom: 56 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: hotBooks.value.map((b) => b.bookTitle),
      axisLabel: {
        interval: 0,
        rotate: 18,
        color: cssVar('--text-sub', '#8a93ad'),
        formatter: (value) => (value.length > 7 ? value.slice(0, 7) + '…' : value)
      },
      axisLine: { lineStyle: { color: cssVar('--border', '#e8ecf3') } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: cssVar('--border-soft', '#f0f3f9') } },
      axisLabel: { color: cssVar('--text-sub', '#96a3b4') }
    },
    series: [
      {
        name: '借阅次数',
        type: 'bar',
        barWidth: '46%',
        data: hotBooks.value.map((b) => b.value),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: cssVar('--cls-primary-light', '#93c5fd') },
            { offset: 1, color: cssVar('--cls-primary', '#3b82f6') }
          ])
        },
        label: { show: true, position: 'top', color: cssVar('--text-highlight', '#2563eb'), fontWeight: 600 }
      }
    ]
  }, true)
}

function renderLine() {
  if (!lineRef.value) return
  lineChart = lineChart || echarts.init(lineRef.value)

  lineChart.setOption({
    grid: { left: 46, right: 24, top: 30, bottom: 42 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((t) => String(t.day).slice(5)),
      axisLine: { lineStyle: { color: cssVar('--border', '#2a3358') } },
      axisLabel: { color: cssVar('--text-sub', '#8a93ad') }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: cssVar('--border-soft', '#f0f3f9') } },
      axisLabel: { color: cssVar('--text-sub', '#96a3b4') }
    },
    series: [
      {
        name: '借阅量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.value.map((t) => t.count),
        lineStyle: { width: 3, color: cssVar('--cls-primary', '#3b82f6') },
        itemStyle: { color: cssVar('--cls-primary', '#3b82f6') },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `rgba(${cssVar('--cls-primary-rgb', '59,130,246')},0.28)` },
            { offset: 1, color: `rgba(${cssVar('--cls-primary-rgb', '59,130,246')},0.02)` }
          ])
        }
      }
    ]
  }, true)
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 数据卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.stat-card {
  background: var(--cls-card);
  border-radius: var(--cls-radius);
  padding: 20px 22px;
  box-shadow: var(--cls-shadow);
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.stat-arrow {
  margin-left: auto;
  color: var(--cls-text-secondary);
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.stat-card:hover .stat-arrow {
  opacity: 1;
  transform: translateX(3px);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--btn-text);
  flex-shrink: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--cls-text-main);
  line-height: 1.2;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--cls-text-secondary);
  margin-left: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--cls-text-secondary);
  margin-top: 4px;
}

/* 图表 */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart {
  height: 300px;
  width: 100%;
}

/* 报告 */
.report-content {
  background: var(--cls-card-soft);
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius-sm);
  padding: 18px 20px;
  max-height: 460px;
  overflow-y: auto;
}

.report-content pre {
  margin: 0;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.9;
  color: var(--cls-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 用户排行 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 用户可点击进个人主页：手型 + 悬浮变主色 */
.user-cell.clickable {
  cursor: pointer;
}

.user-cell.clickable:hover {
  color: var(--cls-primary);
}

.rank-text {
  font-weight: 600;
  color: var(--cls-text-secondary);
}

.active-value {
  font-weight: 700;
  color: var(--cls-primary);
}

@media (max-width: 1024px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
