<template>
  <!--
    注意：组件是多根节点（齿轮 / 魂环头像 / 普通头像三选一），
    Vue 不会把父组件传下来的 class、title、@click 自动合并到根节点，
    所以这里统一用 inheritAttrs: false + v-bind="$attrs" 手动透传，
    保证「点击头像跳转」和外部样式都能生效。
  -->
  <!-- 最高管理员（is_super_admin=1）：旋转齿轮 + 魂环 -->
  <GearAvatar v-if="isSuper" v-bind="$attrs" :size="size" />

  <!-- 正式管理员（role=1 且 pending_admin=0）：个人头像 + 红魂环 -->
  <span
    v-else-if="isRingAdmin"
    class="avatar-box ring-box"
    :style="ringBoxStyle"
    title="管理员"
    v-bind="$attrs"
  >
    <span class="avatar-inner" :style="ringGapStyle">
      <img v-if="showImg" :src="avatar" alt="" class="avatar-img" @error="broken = true" />
      <span v-else class="avatar-letter" :style="[ringLetterStyle, letterBgStyle]">{{ letter }}</span>
    </span>
  </span>

  <!-- 普通用户 / 待激活管理员：个人头像（无魂环，待激活期间与普通用户一致） -->
  <span
    v-else
    class="avatar-box plain-box"
    :style="boxStyle"
    :title="pendingAdmin ? '待激活管理员' : '普通用户'"
    v-bind="$attrs"
  >
    <img v-if="showImg" :src="avatar" alt="" class="avatar-img" @error="broken = true" />
    <span v-else class="avatar-letter" :style="[letterStyle, letterBgStyle]">{{ letter }}</span>
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import GearAvatar from './GearAvatar.vue'

/** 多根节点组件：手动透传 $attrs（class / title / @click 等） */
defineOptions({ inheritAttrs: false })

const props = defineProps({
  /** 头像地址 */
  avatar: { type: String, default: '' },
  /** 角色：0普通用户/1管理员 */
  role: { type: Number, default: 0 },
  /** 是否最高管理员 */
  superAdmin: { type: [Number, Boolean], default: 0 },
  /** 是否待激活管理员 */
  pendingAdmin: { type: [Number, Boolean], default: 0 },
  /** 昵称（无头像时取首字母） */
  nickname: { type: String, default: '' },
  /** 头像尺寸（px） */
  size: { type: Number, default: 36 }
})

const isSuper = computed(() => Number(props.superAdmin) === 1)
const pendingAdmin = computed(() => Number(props.pendingAdmin) === 1)

/**
 * 只有「正式管理员」才加红魂环：role=1 且 pending_admin=0 且不是超管。
 * 待激活管理员（role=1 且 pending_admin=1）与普通用户一样，不加魂环。
 */
const isRingAdmin = computed(() => Number(props.role) === 1 && !isSuper.value && !pendingAdmin.value)

/** 头像加载失败（历史数据里的失效地址）时回退成彩色字母头像 */
const broken = ref(false)
watch(
  () => props.avatar,
  () => {
    broken.value = false
  }
)
const showImg = computed(() => !!props.avatar && !broken.value)

const boxStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))
const letterStyle = computed(() => ({ fontSize: `${Math.max(10, Math.round(props.size * 0.42))}px` }))

/* ---------- 魂环头像（正式管理员）----------
 * 1. 整体比普通头像略小一圈；
 * 2. 头像与魂环之间留一圈白色空隙，形成「镂空」效果。
 */
const RING_SCALE = 0.9 // 整体缩放
const RING_RATIO = 0.06 // 魂环厚度 / 空隙宽度（相对原尺寸）

/** 魂环头像外圈直径 */
const ringSize = computed(() => Math.max(14, Math.round(props.size * RING_SCALE)))
/** 魂环厚度 */
const ringPad = computed(() => Math.max(1, Math.round(props.size * RING_RATIO)))
/** 头像与魂环之间的空隙宽度 */
const ringGap = computed(() => Math.max(1, Math.round(props.size * RING_RATIO)))
/** 头像实际直径（用于算首字母字号） */
const ringInnerSize = computed(() =>
  Math.max(10, ringSize.value - (ringPad.value + ringGap.value) * 2)
)

const ringBoxStyle = computed(() => ({
  width: `${ringSize.value}px`,
  height: `${ringSize.value}px`,
  padding: `${ringPad.value}px`
}))
const ringGapStyle = computed(() => ({ borderWidth: `${ringGap.value}px` }))
const ringLetterStyle = computed(() => ({
  fontSize: `${Math.max(9, Math.round(ringInnerSize.value * 0.42))}px`
}))

/** 没有头像时的首字母 */
const letter = computed(() => {
  const name = (props.nickname || '').trim()
  return name ? name.charAt(0).toUpperCase() : 'U'
})

/* ---------- 默认头像：彩色渐变字母头像 ----------
 * 按昵称哈希从固定色板里挑一组渐变色，同一个人颜色始终一致。
 */
const LETTER_GRADIENTS = [
  'linear-gradient(135deg, #60a5fa, #a78bfa)',
  'linear-gradient(135deg, #f472b6, #c084fc)',
  'linear-gradient(135deg, #34d399, #22d3ee)',
  'linear-gradient(135deg, #fbbf24, #f97316)',
  'linear-gradient(135deg, #818cf8, #38bdf8)',
  'linear-gradient(135deg, #fb7185, #f59e0b)'
]

const letterBg = computed(() => {
  const name = props.nickname || ''
  let hash = 0
  for (let i = 0; i < name.length; i += 1) {
    hash = (hash * 31 + name.charCodeAt(i)) % 100000
  }
  return LETTER_GRADIENTS[hash % LETTER_GRADIENTS.length]
})

const letterBgStyle = computed(() => ({ background: letterBg.value }))
</script>

<style scoped>
.avatar-box {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  box-sizing: border-box;
}

/* 魂环：正红渐变描边（亮红 → 大红 → 深红，内边距厚度由内联样式按尺寸给出） */
.ring-box {
  padding: 2px;
  background: linear-gradient(135deg, var(--cls-danger) 0%, var(--cls-danger) 100%);
  transition: filter 0.25s ease;
}

.ring-box:hover {
  filter: brightness(1.2) drop-shadow(0 0 4px rgba(var(--cls-danger-rgb), 0.7));
}

/* 头像：白色描边把头像从魂环上「割开」，形成镂空效果 */
.avatar-inner {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border-style: solid;
  border-color: var(--card-bg-solid);
  border-width: 2px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg-solid);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 50%;
}

.plain-box {
  overflow: hidden;
}

/* 默认头像：彩色渐变 + 首字母（渐变色由内联样式按昵称哈希给出） */
.avatar-letter {
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--btn-text);
  background: var(--btn-bg);
}
</style>
