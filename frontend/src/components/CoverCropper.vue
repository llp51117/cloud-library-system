<template>
  <el-dialog
    v-model="visible"
    title="编辑封面"
    width="660px"
    class="cropper-dialog"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mode-alert"
      title="两种展示尺寸分开调整"
      description="首页卡片是横向框，详情页封面是竖向框，同一张图两边想露出的位置不一样，所以分别保存两套裁剪参数。没有设置的那个位置会按原图自动居中。"
    />

    <div v-loading="loading" class="body">
      <el-tabs v-model="activeTab" class="mode-tabs" @tab-change="handleTabChange">
        <el-tab-pane name="card">
          <template #label>
            <span>卡片模式</span>
            <el-tag size="small" effect="plain" type="primary">首页 3:2</el-tag>
          </template>
        </el-tab-pane>
        <el-tab-pane name="detail">
          <template #label>
            <span>详情模式</span>
            <el-tag size="small" effect="plain" type="success">详情页 3:4</el-tag>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="stage-wrap">
        <canvas
          ref="canvasRef"
          class="stage"
          :width="stageSize.width"
          :height="stageSize.height"
          @mousedown="onMouseDown"
          @wheel.prevent="onWheel"
        ></canvas>
      </div>

      <div class="tip">
        <span>按住鼠标拖动移动画面，滚动滚轮缩放</span>
        <el-tag v-if="currentState.cleared" size="small" type="info" effect="dark">已清除，保存后按原图展示</el-tag>
      </div>

      <div class="controls">
        <div class="control-row">
          <span class="label">缩放</span>
          <el-slider v-model="currentState.zoom" :min="1" :max="4" :step="0.01" class="zoom-slider" />
          <span class="zoom-value">{{ Math.round(currentState.zoom * 100) }}%</span>
        </div>
        <div class="control-row">
          <el-button size="small" @click="resetMode">重置当前模式</el-button>
          <el-button size="small" type="warning" plain @click="clearMode">清除当前模式裁剪</el-button>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!image || loading" :loading="saving" @click="handleConfirm">
        保存裁剪
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { parseCrop } from '@/utils/coverCrop'
import { cssVar } from '@/utils/themes'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 封面图原图地址 */
  src: { type: String, default: '' },
  /** 首页卡片模式的已有裁剪参数 */
  cardCrop: { type: String, default: '' },
  /** 详情页模式的已有裁剪参数 */
  detailCrop: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/** 两种模式对应画布尺寸：卡片横向 3:2，详情竖向 3:4 */
const MODES = {
  card: { width: 420, height: 280 },
  detail: { width: 300, height: 400 }
}
const MIN_ZOOM = 1
const MAX_ZOOM = 4

const canvasRef = ref()
const image = ref(null)
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('card')

/** 每个模式各自的画面状态，互不干扰 */
const modes = reactive({
  card: { zoom: 1, offsetX: 0, offsetY: 0, cleared: false },
  detail: { zoom: 1, offsetX: 0, offsetY: 0, cleared: false }
})

const currentState = computed(() => modes[activeTab.value])
const stageSize = computed(() => MODES[activeTab.value])

watch(
  () => props.modelValue,
  (val) => {
    if (val) loadImage()
  }
)
watch(() => currentState.value.zoom, redraw)

/** 打开时加载原图，并按已有的裁剪参数还原 */
function loadImage() {
  if (!props.src) {
    ElMessage.warning('请先上传封面图')
    return
  }
  loading.value = true
  const img = new Image()
  img.onload = () => {
    image.value = img
    restoreMode('card', props.cardCrop)
    restoreMode('detail', props.detailCrop)
    activeTab.value = 'card'
    nextTick(redraw)
    loading.value = false
  }
  img.onerror = () => {
    loading.value = false
    ElMessage.error('封面加载失败，请重新上传图片')
  }
  img.src = props.src
}

/** 图片铺满画布所需的缩放（zoom = 1 时刚好铺满，也就是「自动居中裁剪」的效果） */
function baseScale(mode) {
  const img = image.value
  if (!img) return 1
  const size = MODES[mode]
  return Math.max(size.width / img.naturalWidth, size.height / img.naturalHeight)
}

/** 当前模式下图片的显示尺寸 */
function displaySize(mode) {
  const img = image.value
  if (!img) return { width: 0, height: 0 }
  const scale = baseScale(mode) * modes[mode].zoom
  return { width: img.naturalWidth * scale, height: img.naturalHeight * scale, scale }
}

/** 拖动范围限制：保证画面始终盖住整个裁剪框，不会出现空白边 */
function clampOffset(mode) {
  const size = MODES[mode]
  const { width, height } = displaySize(mode)
  const maxX = Math.max(0, (width - size.width) / 2)
  const maxY = Math.max(0, (height - size.height) / 2)
  const st = modes[mode]
  st.offsetX = Math.min(maxX, Math.max(-maxX, st.offsetX))
  st.offsetY = Math.min(maxY, Math.max(-maxY, st.offsetY))
}

function redraw() {
  const canvas = canvasRef.value
  const img = image.value
  if (!canvas || !img) return
  const mode = activeTab.value
  const { width: cw, height: ch } = MODES[mode]
  if (canvas.width !== cw) {
    canvas.width = cw
    canvas.height = ch
  }
  clampOffset(mode)
  const st = modes[mode]
  const { scale } = displaySize(mode)
  const ctx = canvas.getContext('2d')

  ctx.clearRect(0, 0, cw, ch)
  ctx.fillStyle = cssVar('--card-bg-soft', '#f2f4fa')
  ctx.fillRect(0, 0, cw, ch)

  ctx.save()
  ctx.translate(cw / 2 + st.offsetX, ch / 2 + st.offsetY)
  ctx.scale(scale, scale)
  ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2)
  ctx.restore()

  // 参考线 + 选中框：看到的这一块就是最终在这些位置上展示的内容
  ctx.save()
  ctx.strokeStyle = cssVar('--border-strong', 'rgba(255, 255, 255, 0.55)')
  ctx.lineWidth = 1
  ctx.setLineDash([4, 4])
  for (let i = 1; i < 3; i++) {
    ctx.beginPath()
    ctx.moveTo((cw / 3) * i, 0)
    ctx.lineTo((cw / 3) * i, ch)
    ctx.moveTo(0, (ch / 3) * i)
    ctx.lineTo(cw, (ch / 3) * i)
    ctx.stroke()
  }
  ctx.restore()

  ctx.save()
  ctx.strokeStyle = st.cleared ? cssVar('--cls-warning', '#f0a020') : cssVar('--cls-primary', '#4f6ef7')
  ctx.lineWidth = 2
  ctx.strokeRect(1, 1, cw - 2, ch - 2)
  ctx.restore()
}

/** 用已有的裁剪参数还原画面 */
function restoreMode(mode, cropJson) {
  const st = modes[mode]
  const crop = parseCrop(cropJson)
  const img = image.value
  if (!img || !crop || Math.abs(crop.naturalWidth - img.naturalWidth) > 1) {
    resetState(mode) // 没参数、或图片已经换过（旧参数作废）
    return
  }
  const size = MODES[mode]
  const scale = Math.max(size.width / crop.width, size.height / crop.height)
  st.zoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, scale / baseScale(mode)))
  const actual = baseScale(mode) * st.zoom
  st.offsetX = -crop.x * actual - size.width / 2 + (img.naturalWidth * actual) / 2
  st.offsetY = -crop.y * actual - size.height / 2 + (img.naturalHeight * actual) / 2
  st.cleared = false
  clampOffset(mode)
}

function resetState(mode) {
  modes[mode].zoom = 1
  modes[mode].offsetX = 0
  modes[mode].offsetY = 0
  modes[mode].cleared = false
}

function resetMode() {
  resetState(activeTab.value)
  redraw()
}

function clearMode() {
  resetState(activeTab.value)
  currentState.value.cleared = true
  redraw()
}

function handleTabChange() {
  nextTick(redraw)
}

let dragging = false
let startX = 0
let startY = 0
let startOffsetX = 0
let startOffsetY = 0
/** 画布 CSS 显示尺寸可能被缩小，「1 个屏幕像素」对应多少画布像素 */
let pixelRatio = 1

/** 计算显示尺寸与画布像素尺寸的比例，保证缩小显示时拖动依然跟手 */
function updatePixelRatio() {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  pixelRatio = rect.width ? canvas.width / rect.width : 1
}

function onMouseDown(e) {
  if (!image.value) return
  updatePixelRatio()
  dragging = true
  startX = e.clientX
  startY = e.clientY
  startOffsetX = currentState.value.offsetX
  startOffsetY = currentState.value.offsetY
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e) {
  if (!dragging) return
  currentState.value.offsetX = startOffsetX + (e.clientX - startX) * pixelRatio
  currentState.value.offsetY = startOffsetY + (e.clientY - startY) * pixelRatio
  currentState.value.cleared = false
  redraw()
}

function onMouseUp() {
  dragging = false
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
}

function onWheel(e) {
  if (!image.value) return
  const st = currentState.value
  const step = e.deltaY < 0 ? 0.06 : -0.06
  st.zoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, Number((st.zoom + step).toFixed(2))))
  st.cleared = false
  redraw()
}

/**
 * 把当前画面换算成「原图像素」的裁剪框
 * @returns {string} JSON 字符串；被清除的模式返回空字符串
 */
function buildCrop(mode) {
  const st = modes[mode]
  const img = image.value
  if (!img || st.cleared) return ''
  const size = MODES[mode]
  const { width, height, scale } = displaySize(mode)
  const left = size.width / 2 + st.offsetX - width / 2
  const top = size.height / 2 + st.offsetY - height / 2
  const r = (v) => Math.round(v * 100) / 100
  return JSON.stringify({
    x: r(-left / scale),
    y: r(-top / scale),
    width: r(size.width / scale),
    height: r(size.height / scale),
    naturalWidth: img.naturalWidth,
    naturalHeight: img.naturalHeight,
    zoom: r(st.zoom)
  })
}

async function handleConfirm() {
  if (!image.value) {
    ElMessage.warning('请先上传封面图')
    return
  }
  saving.value = true
  try {
    emit('confirm', {
      coverCardCrop: buildCrop('card'),
      coverDetailCrop: buildCrop('detail')
    })
  } finally {
    saving.value = false
  }
}

function handleClosed() {
  onMouseUp()
  image.value = null
  resetState('card')
  resetState('detail')
}

onBeforeUnmount(onMouseUp)
</script>

<style scoped>
/* ========== 弹窗尺寸：始终不超出一屏 ========== */
/* 整个弹窗用纵向 flex：高度超限时只有中间内容可能滚动，
   页脚的「取消 / 保存裁剪」永远留在可视范围内 */
.cropper-dialog {
  --el-dialog-margin-top: 4vh;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
}

.cropper-dialog :deep(.el-dialog__header) {
  padding: 12px 20px;
  margin-right: 0;
}

.cropper-dialog :deep(.el-dialog__body) {
  flex: 1;
  min-height: 0;
  padding: 10px 20px 8px;
  overflow-y: auto;
}

.cropper-dialog :deep(.el-dialog__footer) {
  padding: 8px 20px 12px;
}

.mode-alert {
  margin-bottom: 10px;
}

/* 说明文字压缩一点，少占竖向空间 */
.mode-alert :deep(.el-alert__title) {
  font-size: 13px;
  font-weight: 600;
}

.mode-alert :deep(.el-alert__description) {
  font-size: 12px;
  line-height: 1.5;
}

.body {
  min-height: 0;
}

.mode-tabs :deep(.el-tabs__item) {
  font-size: 14px;
}

.mode-tabs .el-tag {
  margin-left: 6px;
  transform: scale(0.9);
}

/* 预览区最大高度：跟着屏幕高度走，屏幕矮就把画布整个缩小，
   保证滑块、重置 / 清除 / 取消 / 保存都在可视范围内
   （420px 是「弹窗里除画布以外所有内容 + 外边距」的估算值，
     画布内部仍是原像素尺寸，只按比例显示，不影响裁剪精度） */
.stage-wrap {
  --stage-max-height: clamp(150px, calc(100vh - 420px), 360px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px 0 8px;
}

.stage {
  border-radius: 10px;
  cursor: move;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: var(--stage-max-height);
  box-shadow: var(--shadow);
  background: var(--cls-card-soft);
}

.tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12.5px;
  color: var(--cls-text-secondary);
  margin-bottom: 10px;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 6px;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.control-row .label {
  width: 34px;
  font-size: 13px;
  color: var(--cls-text-regular);
  flex-shrink: 0;
}

.zoom-slider {
  flex: 1;
}

.zoom-value {
  width: 48px;
  text-align: right;
  font-size: 13px;
  color: var(--cls-text-secondary);
}
</style>
