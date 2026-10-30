<template>
  <div class="book-manage">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>图书管理</span>
          <span class="cls-sub-title">共 {{ total }} 本图书</span>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="cls-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索书名 / 作者 / 平台"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>

        <div class="flex-1"></div>

        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          <span>新增图书</span>
        </el-button>
        <el-button plain @click="importVisible = true">
          <el-icon><Upload /></el-icon>
          <span>批量导入</span>
        </el-button>
        <el-button plain @click="coverVisible = true">
          <el-icon><Picture /></el-icon>
          <span>批量上传封面</span>
        </el-button>
        <el-button plain @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          <span>下载模板</span>
        </el-button>
        <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          <span>{{ isSuperAdmin ? '批量删除' : '申请批量删除' }}{{ selectedIds.length ? `(${selectedIds.length})` : '' }}</span>
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        class="book-table"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="封面" width="96">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="row.coverUrl" class="cover-thumb" alt="封面" />
            <div v-else class="cover-thumb placeholder">{{ row.title?.slice(0, 2) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="书名" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="book-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" min-width="120" show-overflow-tooltip />
        <!-- 平台不截断，宽度不够时换行也要完整显示 -->
        <el-table-column prop="platform" label="平台" min-width="130" />
        <el-table-column prop="publisher" label="出版社" min-width="150" show-overflow-tooltip />
        <el-table-column label="价格" width="90">
          <template #default="{ row }">
            {{ row.price != null ? '¥' + row.price : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90">
          <template #default="{ row }">
            <span :class="{ 'stock-empty': !(row.stock > 0) }">
              {{ row.stock == null ? 10 : row.stock }} 本
            </span>
          </template>
        </el-table-column>
        <el-table-column label="连载状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isFinished === 1 ? 'success' : 'warning'" effect="plain">
              {{ row.isFinished === 1 ? '已完结' : '连载中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="borrowCount" label="借阅" width="90" align="center" />
        <el-table-column prop="favoriteCount" label="收藏" width="90" align="center" />
        <el-table-column label="录入时间" min-width="165">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="cls-empty">暂无图书数据，点击「新增图书」开始录入</div>
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

    <!-- ============ 新增 / 编辑对话框 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑图书' : '新增图书'"
      width="720px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" maxlength="60" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台" prop="platform">
              <el-input v-model="form.platform" placeholder="如：起点 / 晋江 / 番茄" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版时间" prop="publishDate">
              <el-date-picker
                v-model="form.publishDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连载状态" prop="isFinished">
              <el-radio-group v-model="form.isFinished">
                <el-radio :value="0">连载中</el-radio>
                <el-radio :value="1">已完结</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :max="9999"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图">
              <div class="cover-upload">
                <div v-if="form.coverUrl" class="cover-preview-box">
                  <CoverImage :src="form.coverUrl" :crop="form.coverDetailCrop" class="cover-preview" />
                </div>
                <div class="cover-actions">
                  <el-upload
                    :show-file-list="false"
                    :auto-upload="false"
                    accept="image/*"
                    :on-change="handleCoverChange"
                  >
                    <el-button size="small" plain>上传封面</el-button>
                  </el-upload>
                  <el-button
                    size="small"
                    plain
                    type="primary"
                    :disabled="!form.coverUrl"
                    @click="cropperVisible = true"
                  >
                    <el-icon><Crop /></el-icon>
                    <span>编辑封面</span>
                  </el-button>
                  <el-button v-if="form.coverUrl" size="small" text type="danger" @click="handleRemoveCover">
                    移除
                  </el-button>
                </div>
              </div>
              <!-- 两套裁剪参数的设置情况 -->
              <div class="crop-status">
                <el-tag size="small" :type="form.coverCardCrop ? 'primary' : 'info'" effect="plain">
                  首页卡片裁剪：{{ form.coverCardCrop ? '已设置' : '未设置（自动居中）' }}
                </el-tag>
                <el-tag size="small" :type="form.coverDetailCrop ? 'success' : 'info'" effect="plain">
                  详情页裁剪：{{ form.coverDetailCrop ? '已设置' : '未设置（自动居中）' }}
                </el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图书简介" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                resize="none"
                maxlength="600"
                show-word-limit
                placeholder="请输入图书简介，或点击下方按钮由 AI 自动生成"
              />
              <el-button class="ai-btn" size="small" type="primary" plain :loading="aiLoading" @click="handleAiSummary">
                <el-icon><MagicStick /></el-icon>
                <span>AI 生成简介</span>
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ form.id ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 封面裁剪对话框 ============ -->
    <CoverCropper
      v-model="cropperVisible"
      :src="form.coverUrl"
      :card-crop="form.coverCardCrop"
      :detail-crop="form.coverDetailCrop"
      @confirm="handleCoverCropSave"
    />

    <!-- ============ 批量导入对话框 ============ -->
    <el-dialog v-model="importVisible" title="批量导入图书" width="880px" @closed="resetImport">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="操作步骤"
        description="1. 点击「下载模板」获取标准 Excel 模板；2. 按模板填写图书信息（含「库存」列，不填默认 10 本）；3. 上传文件后系统自动解析并预览；4. 确认无误后点击「确认导入」。系统按「书名 + 作者」查重，重复的书会自动跳过。"
        class="import-alert"
      />

      <div class="import-actions">
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          accept=".xlsx,.xls"
          :on-change="handleImportFile"
        >
          <el-button type="primary" :loading="parsing">
            <el-icon><Upload /></el-icon>
            <span>选择 Excel 文件</span>
          </el-button>
        </el-upload>
        <el-button plain @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          <span>下载模板</span>
        </el-button>
        <span v-if="previewList.length" class="parsed-tip">
          已解析 {{ previewList.length }} 条数据，请确认后导入
        </span>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" class="cover-result">
        <div class="result-line">
          <el-tag type="success" size="small" effect="dark">成功导入 {{ importResult.imported || 0 }}</el-tag>
          <el-tag v-if="importResult.updatedCount" type="primary" size="small" effect="dark">
            已更新 {{ importResult.updatedCount }} 条空字段
          </el-tag>
          <el-tag type="warning" size="small" effect="dark">
            已跳过 {{ importResult.duplicatedCount || 0 }} 条重复图书
          </el-tag>
          <el-tag v-if="importResult.invalid" type="info" size="small" effect="dark">
            数据不完整 {{ importResult.invalid }}
          </el-tag>
        </div>

        <div v-if="importResult.updated?.length" class="result-block">
          <div class="result-title">数据库已存在，已补齐空字段（原有内容不会覆盖）：</div>
          <div class="result-tags">
            <el-tag v-for="(name, i) in importResult.updated" :key="`u-${i}`" size="small" type="primary" effect="plain">
              {{ name }}
            </el-tag>
          </div>
        </div>

        <div v-if="importResult.duplicated?.length" class="result-block">
          <div class="result-title">数据库中已存在且无空字段可补，已跳过：</div>
          <div class="result-tags">
            <el-tag v-for="(name, i) in importResult.duplicated" :key="`d-${i}`" size="small" type="warning" effect="plain">
              {{ name }}
            </el-tag>
          </div>
        </div>

        <div v-if="importResult.repeatedInBatch?.length" class="result-block">
          <div class="result-title">同一批数据内重复，已跳过：</div>
          <div class="result-tags">
            <el-tag
              v-for="(name, i) in importResult.repeatedInBatch"
              :key="`r-${i}`"
              size="small"
              type="warning"
              effect="plain"
            >
              {{ name }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-table v-if="previewList.length && !importResult" :data="previewList" height="320" stripe size="small">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="书名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" min-width="110" />
        <el-table-column prop="platform" label="平台" width="100" />
        <el-table-column prop="publisher" label="出版社" min-width="130" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="90" />
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="{ row }">
            <span :class="{ 'stock-empty': !(row.stock > 0) }">{{ row.stock == null ? 10 : row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isFinished === 1 ? 'success' : 'warning'" effect="plain">
              {{ row.isFinished === 1 ? '已完结' : '连载中' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="importVisible = false">{{ importResult ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!importResult"
          type="primary"
          :disabled="previewList.length === 0"
          :loading="importing"
          @click="handleConfirmImport"
        >
          确认导入（{{ previewList.length }} 本）
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 批量上传封面对话框 ============ -->
    <el-dialog
      v-model="coverVisible"
      title="批量上传封面"
      width="680px"
      :close-on-click-modal="false"
      @closed="resetCoverBatch"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="匹配规则"
        description="图片名（不含扩展名）会与书名比对，例如「三体.jpg」匹配《三体》；单张图片不超过 10MB，支持 jpg / png / gif / webp / bmp。"
        class="import-alert"
      />

      <el-upload
        ref="coverUploadRef"
        v-model:file-list="coverFileList"
        :auto-upload="false"
        :on-change="handleCoverFilesChange"
        multiple
        accept="image/*"
        list-type="picture"
      >
        <el-button type="primary" plain>
          <el-icon><Upload /></el-icon>
          <span>选择图片（可多选）</span>
        </el-button>
        <template #tip>
          <div class="cls-muted cover-tip">已选 {{ coverFileList.length }} 张，建议一次不超过 50 张</div>
        </template>
      </el-upload>

      <!-- 上传结果 -->
      <div v-if="coverResult" class="cover-result">
        <div class="result-line">
          <el-tag type="success" size="small" effect="dark">匹配成功 {{ coverResult.matched?.length || 0 }}</el-tag>
          <el-tag type="warning" size="small" effect="dark">未匹配 {{ coverResult.unmatched?.length || 0 }}</el-tag>
          <el-tag type="danger" size="small" effect="dark">上传失败 {{ coverResult.failed?.length || 0 }}</el-tag>
        </div>

        <div v-if="coverResult.matched?.length" class="result-block">
          <div class="result-title">已更新封面：</div>
          <div class="result-tags">
            <el-tag v-for="(name, i) in coverResult.matched" :key="`m-${i}`" size="small" type="success" effect="plain">
              {{ name }}
            </el-tag>
          </div>
        </div>

        <div v-if="coverResult.unmatched?.length" class="result-block">
          <div class="result-title">未匹配到图书（请核对书名后重新上传）：</div>
          <div class="result-tags">
            <el-tag v-for="(name, i) in coverResult.unmatched" :key="`u-${i}`" size="small" type="warning" effect="plain">
              {{ name }}
            </el-tag>
          </div>
        </div>

        <div v-if="coverResult.failed?.length" class="result-block">
          <div class="result-title">上传失败：</div>
          <ul class="result-list">
            <li v-for="(item, i) in coverResult.failed" :key="`f-${i}`">{{ item }}</li>
          </ul>
        </div>
      </div>

      <template #footer>
        <el-button @click="coverVisible = false">{{ coverResult ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!coverResult"
          type="primary"
          :disabled="coverFileList.length === 0"
          :loading="coverUploading"
          @click="handleConfirmCoverUpload"
        >
          开始上传（{{ coverFileList.length }} 张）
        </el-button>
      </template>
    </el-dialog>

    <!-- 操作申请弹窗：普通管理员批量删除图书需超管审批 -->
    <ApplyDialog
      v-model="applyVisible"
      preset-type="batch_delete_book"
      :preset-target-id="applyPresetTarget"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import CoverImage from '@/components/CoverImage.vue'
import CoverCropper from '@/components/CoverCropper.vue'
import ApplyDialog from '@/components/ApplyDialog.vue'
import {
  adminGetBooks,
  adminAddBook,
  adminUpdateBook,
  adminUpdateCoverCrop,
  adminDeleteBook,
  adminBatchDeleteBooks,
  adminImportBooks,
  adminImportExcel,
  adminDownloadTemplate,
  adminAiSummary,
  adminBatchUploadCovers,
  uploadCover
} from '@/api'

const loading = ref(false)
const list = ref([])

/* ---------------- 权限与操作申请 ---------------- */
const userStore = useUserStore()
/** 只有最高管理员（超管）能直接批量删除图书，普通管理员需提交操作申请 */
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const applyVisible = ref(false)
const applyPresetTarget = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const aiLoading = ref(false)
/* 封面裁剪 */
const cropperVisible = ref(false)

const importVisible = ref(false)
const parsing = ref(false)
const importing = ref(false)
const previewList = ref([])
const importResult = ref(null)

/* 批量上传封面 */
const coverVisible = ref(false)
const coverUploading = ref(false)
const coverUploadRef = ref()
const coverFileList = ref([])
const coverResult = ref(null)
/** 单张图片大小上限，与单个封面上传、后端 FileService 保持一致 */
const MAX_COVER_SIZE = 10 * 1024 * 1024

const defaultForm = () => ({
  id: null,
  title: '',
  author: '',
  publisher: '',
  platform: '',
  price: 0,
  publishDate: '',
  coverUrl: '',
  coverCardCrop: '',
  coverDetailCrop: '',
  description: '',
  isFinished: 0,
  // 库存：新增默认 10 本（与后端 BookSaveDTO.DEFAULT_STOCK 一致）
  stock: 10
})

const form = reactive(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  stock: [{ type: 'number', min: 0, message: '库存不能为负数', trigger: 'change' }]
}

const pad = (n) => String(n ?? '').padStart(2, '0')

/**
 * 点击整行 = 打开编辑弹窗
 * 勾选列（多选）不触发；「删除」按钮自身已 stop 冒泡
 */
function handleRowClick(row, column, event) {
  if (!row || column?.type === 'selection') return
  // 勾选框、按钮等交互元素有自己的点击行为，不打开编辑弹窗
  const el = event?.target
  if (el?.closest?.('.el-checkbox, .el-button, .el-link')) return
  openEditDialog(row)
}

/**
 * 录入时间：2026-09-14 23:00:21
 * 后端返回的是带 T 的 ISO 串（如 2026-09-14T23:00:21），这里去掉 T 并截到秒；
 * 万一返回的是 [年,月,日,时,分,秒] 数组也能正常显示
 */
function formatDateTime(value) {
  if (!value) return '—'
  if (Array.isArray(value)) {
    const [y, m, d, hh, mm, ss] = value
    return `${pad(y)}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}:${pad(ss)}`
  }
  const text = String(value).replace('T', ' ')
  return text.length > 19 ? text.slice(0, 19) : text
}

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const res = await adminGetBooks({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data.list || []
    total.value = Number(res.data.total || 0)
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

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

/* ---------------- 新增 / 编辑 ---------------- */

function openAddDialog() {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row) {
  Object.assign(form, {
    id: row.id,
    title: row.title,
    author: row.author,
    publisher: row.publisher,
    platform: row.platform,
    price: row.price != null ? Number(row.price) : 0,
    publishDate: row.publishDate || '',
    coverUrl: row.coverUrl || '',
    coverCardCrop: row.coverCardCrop || '',
    coverDetailCrop: row.coverDetailCrop || '',
    description: row.description || '',
    isFinished: row.isFinished ?? 0,
    // 老数据可能没有库存字段，打开编辑时补默认值
    stock: row.stock == null ? 10 : row.stock
  })
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
      await adminUpdateBook(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await adminAddBook(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

async function handleCoverChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  try {
    const res = await uploadCover(raw)
    form.coverUrl = res.data
    // 换了图，之前的裁剪参数是按老图算的，直接作废
    form.coverCardCrop = ''
    form.coverDetailCrop = ''
    ElMessage.success('封面上传成功，可点击「编辑封面」调整展示区域')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

function handleRemoveCover() {
  form.coverUrl = ''
  form.coverCardCrop = ''
  form.coverDetailCrop = ''
}

/**
 * 保存封面裁剪参数：已经存在的图书立刻写库，新增中的图书先记在表单里，随「确认新增」一起提交
 */
async function handleCoverCropSave({ coverCardCrop, coverDetailCrop }) {
  if (!form.id) {
    form.coverCardCrop = coverCardCrop || ''
    form.coverDetailCrop = coverDetailCrop || ''
    cropperVisible.value = false
    ElMessage.success('裁剪已记录，保存图书时一并生效')
    return
  }
  try {
    await adminUpdateCoverCrop(form.id, {
      coverCardCrop: coverCardCrop || '',
      coverDetailCrop: coverDetailCrop || ''
    })
    form.coverCardCrop = coverCardCrop || ''
    form.coverDetailCrop = coverDetailCrop || ''
    cropperVisible.value = false
    ElMessage.success('封面裁剪已保存')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleAiSummary() {
  if (!form.title) {
    ElMessage.warning('请先输入书名')
    return
  }
  aiLoading.value = true
  try {
    const res = await adminAiSummary({
      title: form.title,
      author: form.author,
      description: form.description
    })
    form.description = res.data
    ElMessage.success('AI 简介生成成功，可手动调整')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    aiLoading.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${row.title}》吗？该操作会同时清除关联的借阅、预约、收藏与评论数据。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await adminDeleteBook(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  // 普通管理员：批量删除图书属于高风险操作，需提交操作申请由超管审批
  if (!isSuperAdmin.value) {
    applyPresetTarget.value = selectedIds.value.join(',')
    applyVisible.value = true
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 本图书吗？`,
      '批量删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    const res = await adminBatchDeleteBooks(selectedIds.value)
    ElMessage.success(res.message || '批量删除成功')
    selectedIds.value = []
    page.value = 1
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ---------------- 批量导入 ---------------- */

function resetImport() {
  previewList.value = []
  importResult.value = null
}

async function handleImportFile(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  parsing.value = true
  try {
    const res = await adminImportExcel(raw)
    previewList.value = res.data || []
    if (previewList.value.length === 0) {
      ElMessage.warning('未解析到有效数据')
    }
  } catch (e) {
    previewList.value = []
  } finally {
    parsing.value = false
  }
}

async function handleConfirmImport() {
  if (!previewList.value.length) return
  importing.value = true
  try {
    const res = await adminImportBooks(previewList.value)
    // 后端返回查重结果，弹窗里展示「成功导入 / 已跳过 X 条重复图书 + 书名清单」
    importResult.value = res.data || null
    ElMessage.success(res.message || '导入成功')
    page.value = 1
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    importing.value = false
  }
}

/* ---------------- 批量上传封面 ---------------- */

function resetCoverBatch() {
  coverFileList.value = []
  coverResult.value = null
  coverUploadRef.value?.clearFiles()
}

/** 选文件时先卡一次 10MB，避免把超限文件提交到后端 */
function handleCoverFilesChange(file, fileList) {
  const oversize = fileList.filter((f) => f.raw && f.raw.size > MAX_COVER_SIZE)
  if (oversize.length) {
    ElMessage.error(`「${oversize.map((f) => f.name).join('、')}」超过 10MB，已移除`)
    coverFileList.value = fileList.filter((f) => !f.raw || f.raw.size <= MAX_COVER_SIZE)
  }
}

async function handleConfirmCoverUpload() {
  const files = coverFileList.value.map((f) => f.raw).filter(Boolean)
  if (!files.length) {
    ElMessage.warning('请先选择图片')
    return
  }
  coverUploading.value = true
  try {
    const res = await adminBatchUploadCovers(files)
    coverResult.value = res.data || null
    ElMessage.success(res.message || '上传完成')
    // 上传完立刻刷新列表，新封面马上能看到
    loadList()
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    coverUploading.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const res = await adminDownloadTemplate()
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '图书导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}
</script>

<style scoped>
.cover-thumb {
  width: 60px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
  box-shadow: var(--shadow);
}

.cover-thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--text-main);
  background: linear-gradient(150deg, var(--card-bg-soft), var(--fill));
}

/* 整行可点击：鼠标手型 + 悬停浅灰底 */
.book-table :deep(.el-table__body tr) {
  cursor: pointer;
}

.book-table {
  --el-table-row-hover-bg-color: var(--table-row-hover);
}

/* 行悬停：底色保持行本身的不透明底色，只叠加半透明主题色。
   直接写 background-color（半透明）会让 sticky 的固定列透出被盖住的单元格，
   横向滚动时看起来就像右侧内容在闪，所以跟 global.css 保持一致用 background-image。 */
.book-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: inherit !important;
  background-image: linear-gradient(var(--table-row-hover), var(--table-row-hover)) !important;
}

/* 书名：普通文字，不蓝、不带下划线 */
.book-title {
  color: var(--text-main);
  text-decoration: none;
}

/* 库存为 0：醒目标红 */
.stock-empty {
  color: var(--cls-danger);
  font-weight: 600;
}

/* 表格不再强行压缩：列宽总和超出卡片时，交给 Element Plus 自己的横向滚动。
   注意这里不要再给 .el-table__body-wrapper 加 overflow-x:auto —— 那会让它变成
   第二个滚动容器，sticky 的固定列会跟着错位（表现为悬停时右侧内容闪出来）。 */

/* 只有时间这种必须一行的才禁止换行，其他列舒展开 */
.time-cell {
  display: inline-block;
  white-space: nowrap;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* 表单内封面 */
.cover-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover-preview-box {
  position: relative;
  width: 54px;
  height: 72px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 6px;
  border: 1px solid var(--cls-border);
  background: var(--cls-primary-light);
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.crop-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.cover-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.ai-btn {
  margin-top: 8px;
}

/* 导入 */
.import-alert {
  margin-bottom: 16px;
  border-radius: var(--cls-radius-sm);
}

/* 批量上传封面 */
.cover-tip {
  font-size: 12px;
  margin-top: 8px;
}

.cover-result {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid var(--cls-border);
  border-radius: var(--cls-radius-sm);
  background: var(--cls-card-soft);
}

.result-line {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.result-block {
  margin-top: 12px;
}

.result-title {
  font-size: 13px;
  color: var(--cls-text-regular);
  margin-bottom: 8px;
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.result-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--cls-danger);
}

.import-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.parsed-tip {
  font-size: 13px;
  color: var(--cls-success);
}
</style>
