<template>
  <!--
    光影幻境的特效层，分两层：
    1. .magic-bg（z-index -1）：滚动的背景图 / 白底，永远在内容下面；
    2. .magic-fx（高 z-index）：星空点点 + 鼠标五彩光圈，浮在页面最上方，
       但整层 pointer-events: none —— 绝不会挡住任何点击，也不影响阅读
       （光圈用 mix-blend-mode 与背景融合，是很淡的一层光）。
    移植自 ryanyu/whatToEat：滚动用两片完全相同的背景首尾相接 + translateY（无缝），
    光圈是它 FluidCursor（WebGL 流体）的轻量版：彩色径向渐变尾巴，不引入 WebGL 依赖。

    mode = 'bar' 时只渲染「一层滚动的背景」：给 Tab 栏做遮挡层用 ——
    Tab 栏完全透明（透视背景图），但这一层会挡住从下面滚上来的内容，
    保证 Tab 栏文字不会被内容文字打架（尺寸用 vh，与底层背景完全对齐、同步）。
  -->
  <div v-if="mode === 'bar'" class="bg-bar" aria-hidden="true">
    <div class="bg-track bar-track" :class="{ paused: reduceMotion }" :style="{ top: `-${offset}px` }">
      <div v-for="copy in [0, 1]" :key="copy" class="bg-tile bar-tile" :style="tileStyle"></div>
    </div>
  </div>

  <div v-else aria-hidden="true">
    <!-- 底层：背景图（没传图就是白底），自动向上无缝滚动 -->
    <div class="magic-bg">
      <div class="bg-track" :class="{ paused: reduceMotion }">
        <div v-for="copy in [0, 1]" :key="copy" class="bg-tile" :style="tileStyle"></div>
      </div>
    </div>

    <!-- 顶层：星空点点 + 鼠标光圈 -->
    <div class="magic-fx">
      <div class="fx-track" :class="{ paused: reduceMotion }">
        <div v-for="copy in [0, 1]" :key="copy" class="bg-tile">
          <span
            v-for="star in stars"
            :key="`${copy}-${star.id}`"
            class="fx-star"
            :style="starStyle(star)"
          ></span>
        </div>
      </div>
      <canvas ref="canvasRef" class="fx-halo" :style="haloStyle"></canvas>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/store/user'

const props = defineProps({
  /** 背景图 URL（空 = 用默认的白底） */
  image: { type: String, default: '' },
  /** full = 整屏特效层；bar = 只渲染一层滚动背景（给 Tab 栏做遮挡层） */
  mode: { type: String, default: 'full' },
  /** bar 模式用：Tab 栏当前距视口顶部的距离，用来让这层背景与底层背景严格对齐 */
  offset: { type: Number, default: 0 }
})

const userStore = useUserStore()

/** 系统开了「减少动效」就不滚了（光圈仍保留） */
const reduceMotion =
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const tileStyle = computed(() =>
  props.image
    ? { backgroundImage: `url("${props.image}")`, backgroundSize: 'cover', backgroundPosition: 'center' }
    : { background: '#ffffff' }
)

/** 背景是暗的才用 screen 混合（亮背景上 screen 会把光吃掉，改成很淡的正常叠加） */
const darkBg = computed(() => {
  const p = userStore.bgPalette
  if (!props.image || !p) return false
  const list = [p.primary, p.dark, p.light].filter((c) => typeof c === 'string' && c.startsWith('#'))
  if (!list.length) return false
  const avg = list.reduce((sum, c) => sum + luminance(c), 0) / list.length
  return avg < 0.42
})

const haloStyle = computed(() =>
  darkBg.value
    ? { mixBlendMode: 'screen', opacity: 0.95 }
    : { mixBlendMode: 'normal', opacity: 0.8 }
)

function luminance(hex) {
  const value = String(hex).replace('#', '')
  const num = parseInt(value.length === 3 ? value.replace(/./g, '$&$&') : value, 16)
  const r = ((num >> 16) & 255) / 255
  const g = ((num >> 8) & 255) / 255
  const b = (num & 255) / 255
  const linear = (c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
}

/* ==================== 星空点点（固定种子，两片一模一样 → 无缝） ==================== */
const stars = ref([])

function buildStars() {
  let seed = 20261022
  const random = () => {
    seed = (seed * 1103515245 + 12345) % 2147483648
    return seed / 2147483648
  }
  const list = []
  for (let i = 0; i < 26; i++) {
    const size = 2 + Math.round(random() * 5)
    list.push({
      id: i,
      left: `${(random() * 98).toFixed(2)}%`,
      top: `${(2 + random() * 94).toFixed(2)}%`,
      size: `${size}px`,
      opacity: (0.45 + random() * 0.4).toFixed(2),
      delay: `${(random() * 3).toFixed(2)}s`
    })
  }
  stars.value = list
}

function starStyle(star) {
  return {
    left: star.left,
    top: star.top,
    width: star.size,
    height: star.size,
    opacity: star.opacity,
    animationDelay: star.delay
  }
}

/* ==================== 鼠标光圈 ==================== */
const canvasRef = ref(null)
let ctx = null
let rafId = 0
let blobs = []
let hue = 0
let lastX = -999
let lastY = -999
let running = false

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(window.innerWidth * dpr)
  canvas.height = Math.floor(window.innerHeight * dpr)
  canvas.style.width = '100%'
  canvas.style.height = '100%'
  ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.globalCompositeOperation = 'lighter'
  }
}

function spawn(x, y) {
  if (Math.abs(x - lastX) < 12 && Math.abs(y - lastY) < 12) return
  lastX = x
  lastY = y
  hue = (hue + 24) % 360
  /* 更大更亮：起始半径更大、颜色更饱和，成为明确的视觉焦点 */
  blobs.push({ x, y, r: 22 + Math.random() * 14, life: 1, hue })
  if (blobs.length > 120) blobs.shift()
}

function onMouseMove(e) {
  spawn(e.clientX, e.clientY)
}

function onTouchMove(e) {
  const touch = e.touches && e.touches[0]
  if (touch) spawn(touch.clientX, touch.clientY)
}

function drawFrame() {
  rafId = requestAnimationFrame(drawFrame)
  if (!ctx) return
  ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
  const next = []
  for (const blob of blobs) {
    blob.r += 1.9 // 扩散更快 → 光晕更大
    blob.life *= 0.972 // 衰减更慢 → 尾巴更长
    if (blob.life < 0.03) continue
    /* 亮度拉满：中心近白的高光 + 高饱和彩色外圈 */
    const alpha = blob.life * 0.9
    const gradient = ctx.createRadialGradient(blob.x, blob.y, 0, blob.x, blob.y, blob.r)
    gradient.addColorStop(0, `hsla(${blob.hue}, 100%, 78%, ${alpha})`)
    gradient.addColorStop(0.28, `hsla(${blob.hue}, 100%, 62%, ${alpha * 0.85})`)
    gradient.addColorStop(0.62, `hsla(${(blob.hue + 45) % 360}, 100%, 56%, ${alpha * 0.5})`)
    gradient.addColorStop(1, `hsla(${(blob.hue + 90) % 360}, 100%, 52%, 0)`)
    ctx.fillStyle = gradient
    ctx.beginPath()
    ctx.arc(blob.x, blob.y, blob.r, 0, Math.PI * 2)
    ctx.fill()
    next.push(blob)
  }
  blobs = next
}

function start() {
  if (running) return
  running = true
  rafId = requestAnimationFrame(drawFrame)
}

function stop() {
  running = false
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
}

onMounted(() => {
  // bar 模式只是「一层滚动的背景」，不需要星空和光圈
  if (props.mode === 'bar') return
  buildStars()
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('touchmove', onTouchMove, { passive: true })
  start()
})

watch(
  () => props.image,
  () => {
    blobs = []
  }
)

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('touchmove', onTouchMove)
  blobs = []
  ctx = null
})
</script>

<style scoped>
/* 底层：滚动的背景（内容在它上面） */
.magic-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
  background: #ffffff;
}

/* 顶层：星空 + 光圈（浮在页面最上方，但不吃鼠标事件） */
.magic-fx {
  position: fixed;
  inset: 0;
  /* 高于页面内容（顶栏 / Tab 栏是 10），低于 Element Plus 弹窗（2000+），
     这样弹窗里的内容不会被光点盖住 */
  z-index: 1200;
  overflow: hidden;
  pointer-events: none;
}

/* Tab 栏遮挡层：只渲染滚动背景，尺寸用 vh —— 和整屏那一层一模一样，
   动画同时开始、同时长，所以看起来就是「Tab 栏完全透明」 */
.bg-bar {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bar-track {
  height: 200vh;
  width: 100vw;
}

.bar-tile {
  height: 100vh;
}

/* 两片完全相同的背景首尾相接，整体往上平移一片的高度 → 无缝循环 */
.bg-track,
.fx-track {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 200%;
  will-change: transform;
  animation: magic-bg-scroll 26s linear infinite;
}

.bg-track.paused,
.fx-track.paused {
  animation-play-state: paused;
}

@keyframes magic-bg-scroll {
  from {
    transform: translateY(0);
  }

  to {
    /* 200% 高的一半 = 一屏，滚满一屏后状态与起点完全一致 */
    transform: translateY(-50%);
  }
}

.bg-tile {
  position: relative;
  width: 100%;
  height: 50%;
  background-repeat: no-repeat;
  overflow: hidden;
}

/* 星空点点：小白点 + 一圈淡阴影，白底和深色图上都看得见，又不会挡字 */
.fx-star {
  position: absolute;
  display: block;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.55);
  animation: magic-star-blink 3.2s ease-in-out infinite;
}

@keyframes magic-star-blink {
  0%,
  100% {
    transform: scale(0.75);
    opacity: 0.35;
  }

  50% {
    transform: scale(1.15);
    opacity: 0.9;
  }
}

/* 鼠标光圈：模糊 + 很淡的一层，只做「光」，不遮内容 */
.fx-halo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  /* 轻一点模糊 + 更亮的色彩 = 光圈更抢眼 */
  filter: blur(10px) saturate(1.4);
}
</style>
