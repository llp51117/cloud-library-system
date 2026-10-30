<template>
  <el-dialog
    v-model="visible"
    title="编辑头像"
    width="580px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <!-- 未选择图片 -->
    <div v-if="!sourceImg" class="picker">
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept="image/png,image/jpeg,image/jpg,image/gif,image/webp,image/bmp"
        :on-change="onFileChange"
      >
        <el-icon :size="46" class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">将图片拖到此处，或<em>点击选择图片</em></div>
        <div class="upload-tip">支持 jpg / png / gif / webp / bmp，大小不超过 10MB</div>
      </el-upload>
    </div>

    <!-- 裁剪区 -->
    <div v-else class="cropper">
      <canvas
        ref="canvasRef"
        class="stage"
        width="360"
        height="360"
        @mousedown="onMouseDown"
      ></canvas>
      <div class="tip">按住鼠标拖动可调整位置</div>

      <div class="controls">
        <div class="control-row">
          <span class="label">旋转</span>
          <el-button-group>
            <el-button size="small" @click="rotate(-90)">
              <el-icon><RefreshLeft /></el-icon>
              <span>左转 90°</span>
            </el-button>
            <el-button size="small" @click="rotate(90)">
              <el-icon><RefreshRight /></el-icon>
              <span>右转 90°</span>
            </el-button>
          </el-button-group>
        </div>
        <div class="control-row">
          <span class="label">缩放</span>
          <el-slider v-model="zoom" :min="1" :max="3" :step="0.01" class="zoom-slider" />
          <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="sourceImg" plain @click="reset">重新选择</el-button>
      <el-button type="primary" :loading="processing" @click="handleConfirm">
        确认并上传
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { cssVar } from '@/utils/themes'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const MAX_SIZE = 10 * 1024 * 1024

const canvasRef = ref()
const sourceImg = ref(null)
const zoom = ref(1)
const rotation = ref(0)
const offsetX = ref(0)
const offsetY = ref(0)
const processing = ref(false)

let dragging = false
let startX = 0
let startY = 0
let startOffsetX = 0
let startOffsetY = 0

watch(zoom, redraw)

/** 选择文件并加载图片 */
function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (raw.size > MAX_SIZE) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      sourceImg.value = img
      zoom.value = 1
      rotation.value = 0
      offsetX.value = 0
      offsetY.value = 0
      nextTick(redraw)
    }
    img.onerror = () => ElMessage.error('图片加载失败，请更换图片')
    img.src = e.target.result
  }
  reader.readAsDataURL(raw)
}

/** 基础缩放比例（保证图片铺满圆形区域） */
function baseScale() {
  const img = sourceImg.value
  if (!img) return 1
  return Math.max(360 / img.width, 360 / img.height)
}

/** 绘制到 canvas */
function redraw() {
  const canvas = canvasRef.value
  if (!canvas || !sourceImg.value) return
  const ctx = canvas.getContext('2d')
  const size = 360
  ctx.clearRect(0, 0, size, size)

  // 背景（跟着主题走）
  ctx.fillStyle = cssVar('--card-bg-soft', '#f4f6fb')
  ctx.fillRect(0, 0, size, size)

  ctx.save()
  ctx.translate(size / 2 + offsetX.value, size / 2 + offsetY.value)
  ctx.rotate((rotation.value * Math.PI) / 180)
  const scale = baseScale() * zoom.value
  ctx.scale(scale, scale)
  ctx.drawImage(sourceImg.value, -sourceImg.value.width / 2, -sourceImg.value.height / 2)
  ctx.restore()

  // 遮罩 + 圆形选区
  ctx.save()
  ctx.fillStyle = cssVar('--scrollbar-track', 'rgba(31, 45, 61, 0.42)')
  ctx.beginPath()
  ctx.rect(0, 0, size, size)
  ctx.arc(size / 2, size / 2, size / 2 - 12, 0, Math.PI * 2, true)
  ctx.fill()
  ctx.restore()

  ctx.save()
  ctx.strokeStyle = cssVar('--cls-primary', '#4f6ef7')
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(size / 2, size / 2, size / 2 - 12, 0, Math.PI * 2)
  ctx.stroke()
  ctx.restore()
}

function rotate(deg) {
  rotation.value = (rotation.value + deg + 360) % 360
  redraw()
}

function onMouseDown(e) {
  dragging = true
  startX = e.clientX
  startY = e.clientY
  startOffsetX = offsetX.value
  startOffsetY = offsetY.value
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e) {
  if (!dragging) return
  offsetX.value = startOffsetX + (e.clientX - startX)
  offsetY.value = startOffsetY + (e.clientY - startY)
  redraw()
}

function onMouseUp() {
  dragging = false
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
}

onBeforeUnmount(onMouseUp)

/** 生成 300x300 的头像 Blob */
function exportBlob() {
  return new Promise((resolve) => {
    const img = sourceImg.value
    const size = 300
    const out = document.createElement('canvas')
    out.width = size
    out.height = size
    const ctx = out.getContext('2d')

    const scale = Math.max(360 / img.width, 360 / img.height) * zoom.value * (size / 360)
    ctx.translate(size / 2 + offsetX.value * (size / 360), size / 2 + offsetY.value * (size / 360))
    ctx.rotate((rotation.value * Math.PI) / 180)
    ctx.scale(scale, scale)
    ctx.drawImage(img, -img.width / 2, -img.height / 2)

    out.toBlob((blob) => resolve(blob), 'image/png', 0.95)
  })
}

async function handleConfirm() {
  if (!sourceImg.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  processing.value = true
  try {
    const blob = await exportBlob()
    const file = new File([blob], `avatar_${Date.now()}.png`, { type: 'image/png' })
    emit('confirm', file)
  } catch (e) {
    ElMessage.error('头像处理失败，请重试')
  } finally {
    processing.value = false
  }
}

function reset() {
  sourceImg.value = null
  zoom.value = 1
  rotation.value = 0
  offsetX.value = 0
  offsetY.value = 0
}
</script>

<style scoped>
.picker :deep(.el-upload-dragger) {
  padding: 36px 20px;
  border-radius: var(--cls-radius);
}

.upload-icon {
  color: var(--text-placeholder);
  margin-bottom: 10px;
}

.upload-text {
  font-size: 14px;
  color: var(--cls-text-regular);
}

.upload-text em {
  color: var(--cls-primary-ink);
  font-style: normal;
}

.upload-tip {
  font-size: 12.5px;
  color: var(--cls-text-secondary);
  margin-top: 8px;
}

.cropper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stage {
  border-radius: 0;
  cursor: move;
  box-shadow: var(--shadow);
}

.tip {
  font-size: 12.5px;
  color: var(--cls-text-secondary);
  margin: 10px 0 16px;
}

.controls {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.control-row .label {
  width: 42px;
  font-size: 13px;
  color: var(--cls-text-regular);
  flex-shrink: 0;
}

.zoom-slider {
  flex: 1;
}

.zoom-value {
  width: 52px;
  font-size: 13px;
  color: var(--cls-text-secondary);
  text-align: right;
}
</style>
