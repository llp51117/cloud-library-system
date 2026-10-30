<template>
  <span class="gear-avatar-box" :style="boxStyle" title="管理员">
    <svg class="gear-svg" viewBox="0 0 100 100" role="img" aria-label="管理员">
      <defs>
        <!-- 齿轮：红色系（用主题的 danger 色），亮红 → 大红 → 深红，渐变过渡 -->
        <linearGradient :id="`clsAdminGear-${uid}`" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="var(--cls-danger)" stop-opacity="0.72" />
          <stop offset="45%" stop-color="var(--cls-danger)" stop-opacity="1" />
          <stop offset="100%" stop-color="var(--cls-danger)" stop-opacity="0.6" />
        </linearGradient>
        <!-- 魂环：同系红色渐变，比齿轮更亮更鲜艳，末端压到深红拉开层次 -->
        <linearGradient :id="`clsAdminRing-${uid}`" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="var(--cls-danger)" stop-opacity="1" />
          <stop offset="50%" stop-color="var(--cls-danger)" stop-opacity="1" />
          <stop offset="100%" stop-color="var(--cls-danger)" stop-opacity="0.62" />
        </linearGradient>
      </defs>

      <!-- 魂环：与齿轮之间留一圈空隙 -->
      <circle
        class="ring"
        cx="50"
        cy="50"
        r="45"
        fill="none"
        :stroke="`url(#clsAdminRing-${uid})`"
        stroke-width="7"
      />

      <!-- 实心齿轮：中心不挖空 -->
      <path class="gear" :d="gearPath" :fill="`url(#clsAdminGear-${uid})`" />
    </svg>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 头像尺寸（px） */
  size: {
    type: Number,
    default: 36
  }
})

/** 同页可能渲染多个齿轮头像，渐变 id 加后缀避免重复 */
const uid = Math.random().toString(36).slice(2, 8)

const boxStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))

/**
 * 实心齿轮的 SVG path（viewBox 100×100，圆心 50,50）：
 * 8 个齿在齿顶 / 齿根之间来回，闭合后即为实心齿轮，中心不挖空。
 */
const gearPath = (() => {
  const TEETH = 8
  const OUTER = 33 // 齿顶半径
  const ROOT = 25 // 齿根半径
  const step = (Math.PI * 2) / TEETH
  const pt = (r, a) => `${(50 + r * Math.cos(a)).toFixed(2)} ${(50 + r * Math.sin(a)).toFixed(2)}`

  let d = ''
  for (let i = 0; i < TEETH; i++) {
    const c = i * step - Math.PI / 2 // 从第 1 个齿居中于正上方开始
    d +=
      `${i === 0 ? 'M' : 'L'}${pt(ROOT, c - step * 0.5)}` +
      `L${pt(OUTER, c - step * 0.18)}` +
      `L${pt(OUTER, c + step * 0.18)}` +
      `L${pt(ROOT, c + step * 0.5)}`
  }
  d += 'Z' // 闭合成实心齿轮
  return d
})()
</script>

<style scoped>
.gear-avatar-box {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.gear-svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

/* 齿轮绕自身中心旋转：SVG 元素需要 fill-box 才能按自身中心变换 */
.gear {
  transform-box: fill-box;
  transform-origin: center;
  /* 常驻缓慢旋转，2.5s 一圈 */
  animation: gear-spin 2.5s linear infinite;
}

/* 悬停时只提亮魂环，旋转不依赖悬停 */
.gear-avatar-box:hover .ring {
  filter: brightness(1.2) drop-shadow(0 0 4px rgba(var(--cls-danger-rgb), 0.7));
}

.ring {
  transition: filter 0.25s ease;
}

@keyframes gear-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
