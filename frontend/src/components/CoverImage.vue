<template>
  <!--
    外面套一层裁剪框：
    - 没有裁剪参数时，img 就是普通的 100% + object-fit: cover（和原来一模一样）
    - 有裁剪参数时，img 会被放大并用 translate 定位，尺寸大于容器，
      超出容器的部分由这个框 overflow: hidden 裁掉
    这样封面图永远被限制在它自己的区域内，不会溢出盖住下面的书名、作者、收藏等信息

    图片地址失效（文件被删 / 路径错误）时，浏览器会把 alt 文字画在左上角、
    卡片上方留一块白板，看起来就是「卡片布局坏了」。
    所以这里在 error 时回退成和「无封面」一致的居中占位，保证卡片高度和信息对齐不变。
  -->
  <div class="cover-box">
    <img
      v-if="!failed"
      :src="src"
      :alt="alt"
      :style="style"
      loading="lazy"
      @error="failed = true"
    />
    <div v-else class="cover-fallback">
      <span class="fb-title">{{ title || alt }}</span>
      <span v-if="author" class="fb-author">{{ author }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { cropImageStyle } from '@/utils/coverCrop'

const props = defineProps({
  /** 封面图地址 */
  src: { type: String, default: '' },
  /** 裁剪参数（JSON 字符串），为空表示不裁剪 */
  crop: { type: String, default: '' },
  alt: { type: String, default: '封面' },
  /** 图片加载失败时占位显示的书名（不传则用 alt） */
  title: { type: String, default: '' },
  /** 图片加载失败时占位显示的作者 */
  author: { type: String, default: '' }
})

/** 图片是否加载失败：失败后回退到占位布局 */
const failed = ref(false)

/** 换图（比如裁剪后重新生成封面）时重置失败状态，重新尝试加载 */
watch(
  () => props.src,
  () => {
    failed.value = false
  }
)

const style = computed(() => cropImageStyle(props.crop))
</script>

<style scoped>
/* 裁剪框：撑满父容器，并把放大的封面图裁在自己的边界之内 */
.cover-box {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: inherit;
}

/* 未裁剪时：按原来的方式铺满显示 */
.cover-box > img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 加载失败：和「没有封面」时的占位保持一致（居中书名 + 作者） */
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  text-align: center;
  background: linear-gradient(150deg, var(--card-bg-soft) 0%, var(--fill) 100%);
}

.fb-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cls-text-main);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fb-author {
  font-size: 12px;
  color: var(--cls-text-secondary);
}
</style>
