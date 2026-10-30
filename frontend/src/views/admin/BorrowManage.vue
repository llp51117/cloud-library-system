<template>
  <div class="borrow-manage">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>借阅管理</span>
          <span class="cls-sub-title">共 {{ total }} 条借阅记录</span>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="cls-toolbar">
        <el-select v-model="searchType" style="width: 120px" @change="handleSearch">
          <el-option label="按用户" value="user" />
          <el-option label="按图书" value="book" />
          <el-option label="按平台" value="platform" />
        </el-select>
        <el-input
          v-model="keyword"
          :placeholder="searchPlaceholder"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>
        <!-- 一键催还：批量给「逾期」和「3 天内到期」的借阅记录发催还通知 -->
        <el-button type="warning" :loading="remindAllLoading" @click="handleRemindAll">
          <el-icon><Bell /></el-icon>
          <span>一键催还</span>
        </el-button>

        <div class="flex-1"></div>

        <el-radio-group v-model="status" @change="handleSearch">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">借阅中</el-radio-button>
          <el-radio-button :value="1">已归还</el-radio-button>
          <el-radio-button :value="2">逾期</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column v-if="searchType === 'user'" label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" :src="row.userAvatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span>{{ row.userNickname || row.userEmail || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-else-if="searchType === 'book'" label="书名" min-width="200">
          <template #default="{ row }">
            <div class="book-cell">
              <img v-if="row.bookCover" :src="row.bookCover" class="cover-thumb" alt="封面" />
              <div v-else class="cover-thumb placeholder">{{ row.bookTitle?.slice(0, 2) }}</div>
              <span>{{ row.bookTitle || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-else label="平台" min-width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.bookPlatform || '未知' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="searchType !== 'user'" label="借阅人" min-width="180">
          <template #default="{ row }">
            <span>{{ row.userNickname || row.userEmail || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="searchType !== 'book'" prop="bookTitle" label="书名" min-width="170" show-overflow-tooltip />
        <el-table-column v-if="searchType !== 'platform'" prop="bookPlatform" label="平台" width="100" />

        <el-table-column label="借阅时间" width="160">
          <template #default="{ row }">{{ fmt(row.borrowDate) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'due-overdue': row.status === 2 }">{{ fmt(row.dueDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="160">
          <template #default="{ row }">{{ fmt(row.returnDate) }}</template>
        </el-table-column>
        <el-table-column label="续借" width="80" align="center">
          <template #default="{ row }">{{ row.renewCount ?? 0 }} 次</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusMeta(row.status).type" effect="dark">
              {{ statusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <!-- 「强制归还」已下线：已归还只由用户在用户端自己操作，这里只保留催还 -->
            <template v-if="row.status === 1">
              <span class="cls-muted">{{ statusMeta(row.status).text }}</span>
            </template>
            <el-button v-else type="primary" size="small" link @click="handleRemind(row)">催还</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="cls-empty">暂无借阅记录</div>
        </template>
      </el-table>

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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminGetBorrows, adminRemind, adminRemindAll } from '@/api'

const route = useRoute()

const loading = ref(false)
const remindAllLoading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref(null)
const searchType = ref('user')
const keyword = ref('')

/**
 * 支持从数据概览的卡片直接进入：?status=0（借阅中）/ ?status=2（逾期）
 * 没有带参数或值非法时，默认「全部」
 */
function initStatusFromQuery() {
  const raw = route.query.status
  if (raw === undefined || raw === null || raw === '' || raw === 'all') return
  const value = Number(raw)
  if ([0, 1, 2].includes(value)) {
    status.value = value
  }
}

const searchPlaceholder = computed(() => {
  if (searchType.value === 'book') return '输入书名关键词'
  if (searchType.value === 'platform') return '输入平台关键词'
  return '输入用户邮箱关键词'
})

onMounted(() => {
  initStatusFromQuery()
  loadList()
})

/**
 * 状态展示：1 已归还（绿，只可能由用户在用户端归还产生）/ 2 逾期（红）/ 其余借阅中（蓝）
 */
function statusMeta(value) {
  if (value === 1) return { text: '已归还', type: 'success' }
  if (value === 2) return { text: '逾期', type: 'danger' }
  return { text: '借阅中', type: 'primary' }
}

function fmt(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminGetBorrows({
      page: page.value,
      size: size.value,
      status: status.value === null ? undefined : status.value,
      searchType: searchType.value,
      keyword: keyword.value
    })
    list.value = res.data.list || []
    total.value = Number(res.data.total || 0)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
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

/**
 * 一键催还：给所有「逾期」或「3 天内即将逾期」的借阅记录批量发送催还通知
 */
async function handleRemindAll() {
  try {
    await ElMessageBox.confirm('确认向所有逾期用户发送催还通知吗？', '一键催还', {
      confirmButtonText: '确认发送',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  remindAllLoading.value = true
  try {
    const res = await adminRemindAll()
    const count = Number(res?.data || 0)
    ElMessage.success(`已发送 ${count} 条催还通知`)
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    remindAllLoading.value = false
  }
}

async function handleRemind(row) {
  try {
    await ElMessageBox.confirm(
      `确定向 ${row.userNickname || row.userEmail} 发送《${row.bookTitle}》的催还提醒吗？`,
      '催还确认',
      { confirmButtonText: '确认发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch (e) {
    return
  }
  try {
    await adminRemind(row.id)
    ElMessage.success('催还提醒已发送')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}


</script>

<style scoped>
.user-cell,
.book-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover-thumb {
  width: 32px;
  height: 42px;
  border-radius: 5px;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.cover-thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-main);
  background: linear-gradient(150deg, var(--card-bg-soft), var(--fill));
}

.due-overdue {
  color: var(--cls-danger);
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
