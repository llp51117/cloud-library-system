<template>
  <div class="theme-page">
    <div class="cls-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>更换皮肤</span>
          <span class="cls-sub-title">
            共 {{ THEMES.length }} 套内置主题，除极光圣殿白外都支持深 / 浅两种模式，与账号绑定，换设备登录自动同步
          </span>
        </div>
        <span class="current-tag">
          当前主题：<b :style="{ color: currentTheme.primary }">{{ currentTheme.name }}</b>
          <span v-if="currentModeText">{{ currentModeText }}</span>
        </span>
      </div>

      <div class="theme-grid">
        <div
          v-for="item in builtInThemes"
          :key="item.key"
          class="theme-item"
          :class="{ active: isActiveTheme(item) }"
          :style="cardStyle(item)"
          @click="handleSelect(item)"
        >
          <!-- 选中标记 -->
          <span v-if="isActiveTheme(item)" class="checked-badge" :style="badgeStyle(item)">
            <el-icon><Check /></el-icon>
          </span>

          <!-- 深浅状态展示（不可点）：太阳 = 浅色模式，月亮 = 深色模式。
               切换模式靠点整张卡片：第一次浅色 → 第二次深色 → 第三次浅色…（极光圣殿白不显示） -->
          <span
            v-if="item.modes.length > 1"
            class="mode-badge"
            :style="badgeStyle(item)"
            :title="`当前：${modeLabel(modeOf(item))}模式（点击卡片切换）`"
          >
            <el-icon>
              <Moon v-if="modeOf(item) === MODE_DARK" />
              <Sunny v-else />
            </el-icon>
          </span>

          <div class="theme-preview" :style="{ background: previewOf(item).page }"></div>

          <div class="theme-name">
            {{ item.name }}
            <span v-if="item.modes.length > 1" class="mode-tag">{{ modeLabel(modeOf(item)) }}</span>
          </div>
          <div class="theme-key">{{ themeKeyOf(item.key, modeOf(item)) }}</div>

          <!-- 三个色阶 -->
          <div class="swatches">
            <span class="swatch" :style="swatchStyle(item.primary)" title="主色 primary">
              <i>主</i>
            </span>
            <span class="swatch" :style="swatchStyle(item.dark)" title="深色 dark（主色加深 30%）">
              <i>深</i>
            </span>
            <span class="swatch" :style="swatchStyle(item.light)" title="浅色 light（主色变浅 40%）">
              <i>浅</i>
            </span>
          </div>

          <div class="apply-btn">{{ applyText(item) }}</div>
        </div>

        <!-- 第 12 套：光影幻境（magic-bg）
             背景 = 自己上传的图片（没传 = 白底黑星星），自动向上滚动 + 鼠标五彩光圈；
             没有固定的主 / 深 / 浅色块，配色由上传图片自动取色得到。 -->
        <div
          class="theme-item magic-item"
          :class="{ active: isMagicActive }"
          :style="magicCardStyle"
          @click="handleMagicSelect"
        >
          <span v-if="isMagicActive" class="checked-badge" :style="magicBadgeStyle">
            <el-icon><Check /></el-icon>
          </span>

          <!-- 卡片背景：传了图就直接铺这张图，没传图就是默认的「白底星空」 -->
          <div class="magic-card-bg">
            <div
              v-if="userStore.bgUrl"
              class="magic-card-img"
              :style="{ backgroundImage: `url(&quot;${userStore.bgUrl}&quot;)` }"
            ></div>
            <div v-else class="magic-card-stars">
              <span
                v-for="star in cardStars"
                :key="star.id"
                class="magic-card-star"
                :style="cardStarStyle(star)"
              ></span>
            </div>
          </div>

          <!-- 占位条：和前面 11 张卡的预览条同高，保证卡片排版一致 -->
          <div class="theme-preview magic-preview-strip"></div>

          <div class="theme-name">光影幻境</div>
          <div class="theme-key">magic-bg</div>

          <!-- 前面 11 张卡这里是「三个色阶」，光影幻境不需要，
               但要占同一行、同一高度，所以放成上传 / 删除两个按钮 -->
          <div class="swatches">
            <el-button class="swatch-btn" size="small" @click.stop="pickBgImage">
              <el-icon><Upload /></el-icon>
              <span>上传背景图</span>
            </el-button>
            <el-button
              v-if="userStore.bgUrl"
              class="swatch-btn"
              size="small"
              title="删除背景图，回到默认星空"
              @click.stop="handleClearBg"
            >
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </div>

          <div class="apply-btn">{{ isMagicActive ? '使用中' : '点击应用' }}</div>

          <!-- 隐藏的图片选择器：点「上传背景图」触发 -->
          <input
            ref="bgInput"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleBgFileChange"
          />
        </div>
      </div>

      <!-- 我的自定义颜色（图片取色，只属于当前账号，可无限添加） -->
      <div class="custom-section">
        <div class="cls-section-title">
          <div class="title-left">
            <span class="bar"></span>
            <span>我的自定义颜色</span>
            <span class="cls-sub-title">
              上传图片自动取色，可无限添加，只有你自己能看到（{{ customThemes.length }} 套）
            </span>
          </div>
        </div>

        <div class="theme-grid">
          <!-- 已保存的自定义颜色：样式跟内置主题一致（背景主色 / 边框深色 / 三个色块） -->
          <div
            v-for="item in customThemes"
            :key="item.key"
            class="theme-item custom-item"
            :class="{ active: item.key === userStore.themeKey }"
            :style="customCardStyle(item)"
            @click="handleSelect(item)"
            >
              <span
                v-if="item.key === userStore.themeKey"
                class="checked-badge"
                :style="checkedStyle(item)"
              >
              <el-icon><Check /></el-icon>
            </span>

            <!-- 删除按钮（右上角） -->
            <span class="delete-badge" title="删除这套颜色" @click.stop="handleDelete(item)">
              <el-icon><Delete /></el-icon>
            </span>

            <div class="theme-preview" :style="{ background: previewGradient(item) }"></div>

            <div class="theme-name">{{ item.name }}</div>
            <div class="theme-key">custom · {{ item.id }}</div>

            <div class="swatches">
              <span class="swatch" :style="swatchStyle(item.primary)" title="主色 primary">
                <i>主</i>
              </span>
              <span class="swatch" :style="swatchStyle(item.dark)" title="深色 dark">
                <i>深</i>
              </span>
              <span class="swatch" :style="swatchStyle(item.light)" title="浅色 light">
                <i>浅</i>
              </span>
            </div>

            <div class="apply-btn">
              {{ item.key === userStore.themeKey ? '使用中' : '点击应用' }}
            </div>
          </div>

          <!-- 上传入口：跟主题卡片一样大的「+」方框，点了就选图片，保存后后面还会再跟一个 -->
          <div class="theme-item add-tile" @click="pickImage">
            <div class="add-plus">+</div>
            <div class="add-text">上传图片取色</div>
          </div>
        </div>

        <!-- 隐藏的图片选择器：点上面的「+」方框触发 -->
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          class="hidden-input"
          @change="handleFileChange"
        />
      </div>
    </div>

    <!-- 自定义颜色：图片取色 → 随机配色 → 命名 → 保存 -->
    <el-dialog
      v-model="dialogVisible"
      title="自定义颜色（图片取色）"
      width="620px"
      :close-on-click-modal="false"
      @closed="resetDialog"
    >
      <div class="custom-dialog">
        <div class="upload-row">
          <div class="upload-box">
            <img v-if="previewUrl" :src="previewUrl" alt="预览图" />
            <div v-else class="upload-empty">
              <el-icon :size="26"><Picture /></el-icon>
              <span>还没有图片</span>
            </div>
          </div>

          <div class="upload-tip">
            <p>已从图片里提取出 <b>{{ paletteColors.length }}</b> 个主要颜色，随机组成下面这套配色。</p>
            <div class="tip-actions">
              <el-button link type="primary" @click="reshuffle">
                <el-icon><Refresh /></el-icon>
                <span>重新分析（换一套）</span>
              </el-button>
              <el-button link type="primary" @click="pickImage">
                <el-icon><Picture /></el-icon>
                <span>换一张图片</span>
              </el-button>
            </div>
            <p class="tip-hint">点下面的任意一个颜色，可以把它设成主色。</p>
          </div>
        </div>

        <!-- 图片里的所有主要颜色（8~12 个） -->
        <div v-if="paletteColors.length" class="palette-block">
          <div class="block-title">图片里的 {{ paletteColors.length }} 个主要颜色</div>
          <div class="palette-row">
            <span
              v-for="(color, index) in paletteColors"
              :key="`${color}-${index}`"
              class="palette-chip"
              :class="{ picked: isPicked(color) }"
              :style="{ background: color, color: textOnColor(color) }"
              :title="`${color}（点击设为主色）`"
              @click="setPrimary(color)"
            >
              {{ index + 1 }}
            </span>
          </div>
        </div>

        <!-- 随机分配结果：8 个部位各一个颜色 -->
        <div v-if="assignment.primary" class="result-block">
          <div class="block-title">随机分配到各处（点「重新分析」换一套）</div>
          <div class="role-grid">
            <div v-for="role in PALETTE_ROLES" :key="role.key" class="role-item">
              <span class="role-swatch" :style="{ background: assignment[role.key] }"></span>
              <span class="role-label">{{ role.label }}</span>
              <span class="role-value">{{ assignment[role.key] }}</span>
            </div>
          </div>
          <div class="result-row">
            <div class="result-preview" :style="{ background: previewGradient(assignment) }"></div>
            <el-button
              size="small"
              :style="{
                background: assignment.button,
                borderColor: assignment.cardBorder,
                color: textOnColor(assignment.button)
              }"
              @click="previewCustomTheme"
            >
              试穿一下
            </el-button>
          </div>
        </div>

        <el-form class="name-form" label-width="72px">
          <el-form-item label="主题名">
            <el-input
              v-model="form.name"
              maxlength="30"
              show-word-limit
              placeholder="给这套颜色起个名字，比如「我的夏日」"
            />
          </el-form-item>
        </el-form>

        <p v-if="analyzing" class="analyzing">正在分析配色…</p>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!assignment.primary"
          @click="handleSaveCustom"
        >
          保存这套颜色
        </el-button>
      </template>
    </el-dialog>

    <!-- 光影幻境：上传完背景图后确认配色（配色由图片自动取色得到，可以重新分析换一套） -->
    <el-dialog
      v-model="magicDialog"
      title="光影幻境 · 背景配色"
      width="520px"
      :close-on-click-modal="false"
    >
      <div class="magic-dialog">
        <div class="upload-row">
          <div class="upload-box">
            <img v-if="magicPreview" :src="magicPreview" alt="背景图" />
          </div>
          <div class="upload-tip">
            <p>图片已上传，配色是从这张图里自动取出来的（主色 / 深色 / 浅色）。</p>
            <div class="tip-actions">
              <el-button link type="primary" :disabled="!magicFile" @click="reshuffleMagic">
                <el-icon><Refresh /></el-icon>
                <span>重新分析（换一套）</span>
              </el-button>
              <el-button link type="primary" @click="pickBgImage">
                <el-icon><Picture /></el-icon>
                <span>换一张图片</span>
              </el-button>
            </div>
            <p class="tip-hint">点「应用这套」立刻生效：背景会向上滚动，鼠标移动带五彩光圈。</p>
          </div>
        </div>

        <div class="magic-colors">
          <span class="magic-chip" :style="magicChipStyle(magicColors.primary)">
            主 {{ magicColors.primary }}
          </span>
          <span class="magic-chip" :style="magicChipStyle(magicColors.dark)">
            深 {{ magicColors.dark }}
          </span>
          <span class="magic-chip" :style="magicChipStyle(magicColors.light)">
            浅 {{ magicColors.light }}
          </span>
        </div>

        <div
          class="magic-preview-bar"
          :style="{
            background: `linear-gradient(120deg, ${magicColors.light} 0%, ${magicColors.primary} 50%, ${magicColors.dark} 100%)`
          }"
        ></div>

        <p v-if="magicSaving" class="analyzing">正在应用…</p>
      </div>

      <template #footer>
        <el-button @click="magicDialog = false">取消</el-button>
        <el-button type="primary" :loading="magicSaving" @click="confirmMagic">
          应用这套
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { analyzeThemeImage } from '@/api'
import {
  THEMES,
  MODE_DARK,
  MODE_LIGHT,
  PALETTE_ROLES,
  PALETTE_KEYS,
  getTheme,
  textOnColor,
  applyTheme,
  modesOf,
  modeLabel,
  modePreview,
  themeKeyOf,
  parseThemeKey,
  MAGIC_BG_KEY
} from '@/utils/themes'

const userStore = useUserStore()

const currentTheme = computed(() => getTheme(userStore.themeKey))
const customThemes = computed(() => userStore.customThemes || [])
/** 前 11 套常规内置主题（第 12 套「光影幻境」单独渲染成特效卡片） */
const builtInThemes = computed(() => THEMES.filter((item) => !item.magicBg))

/* ==================== 深 / 浅模式 ==================== */

/** 当前正在用的模式（极光圣殿白 / 自定义主题没有，返回空） */
const currentMode = computed(() => {
  const theme = currentTheme.value
  return modesOf(theme.baseKey || '').length > 1 ? theme.mode : ''
})

/** 顶部「当前主题」后面跟的模式说明 */
const currentModeText = computed(() =>
  currentMode.value ? ` · ${modeLabel(currentMode.value)}模式` : ''
)

/** 这套主题是不是正在用的那套（不管哪个模式） */
function isActiveTheme(item) {
  return parseThemeKey(userStore.themeKey).base === item.key
}

/** 这张卡片支持几种模式（白色 / 自定义主题只有 1 种） */
function modeCount(item) {
  return modesOf(item.key).length
}

/**
 * 这张卡片当前展示的模式：
 * - 没在用的主题 → 浅色（第一次点击就是浅色，卡片展示的即是点下去会得到的东西）
 * - 正在用的主题 → 实际模式（浅 → 深 → 浅 循环）
 * - 极光圣殿白（只有浅色）→ 浅色
 */
function modeOf(item) {
  if (modeCount(item) < 2) return MODE_LIGHT
  if (!isActiveTheme(item)) return MODE_LIGHT
  return currentMode.value || MODE_LIGHT
}

/** 预览配色的兜底值：任何异常都不许把整页渲染带崩 */
const FALLBACK_PREVIEW = {
  page: 'linear-gradient(135deg, #FFFFFF 0%, #E5E7EB 100%)',
  card: '#FFFFFF',
  cardSoft: '#F5F7FA',
  text: '#333333',
  border: '#E5E7EB'
}

/** 这张卡片按当前选中模式给出的预览配色（页面渐变 / 卡片 / 文字 / 边框） */
function previewOf(item) {
  try {
    return modePreview(item.key, modeOf(item)) || FALLBACK_PREVIEW
  } catch (e) {
    console.error('[主题预览失败]', item && item.key, e)
    return FALLBACK_PREVIEW
  }
}

/** 卡片底部的提示文案 */
function applyText(item) {
  if (modeCount(item) < 2) return isActiveTheme(item) ? '使用中' : '点击应用'
  if (!isActiveTheme(item)) return '点击应用（浅色）'
  return `点击切换到${modeLabel(currentMode.value === MODE_LIGHT ? MODE_DARK : MODE_LIGHT)}模式`
}

/** 自定义颜色弹窗 */
const dialogVisible = ref(false)
const analyzing = ref(false)
const saving = ref(false)
const previewUrl = ref('')
const currentFile = ref(null)
/** 图片里提取出来的所有主要颜色（8~12 个） */
const paletteColors = ref([])
const fileInput = ref(null)
const form = reactive({ name: '' })
/** 随机分配结果：主色 / 深色 / 浅色 / 按钮 / 边框 / Tab选中 / 文字高亮 / 卡片边框 */
const assignment = reactive({
  primary: '',
  dark: '',
  light: '',
  button: '',
  border: '',
  tab: '',
  highlight: '',
  cardBorder: ''
})

/* ==================== 卡片样式 ==================== */

/**
 * 内置主题卡片：整张卡片就是「该主题当前选中模式」的页面底色 ——
 * 深色模式 = 深X底 + 白字，浅色模式 = 浅X底 + 深灰字（文字颜色自适应，两种卡上都看得清）；
 * 边框 / 预览条 / 色阶也是这套配色，所见即所得。
 */
/**
 * 只有这一种情况才改预览卡片：主题是「光影幻境」+ 用户上传的是深色 / 黑色背景图。
 * 其余情况（没传图、传的是白色 / 浅色图、其他 11 套主题）预览卡片一律保持原样。
 */
const darkBgPreview = computed(
  // 必须同时满足：当前用的就是「光影幻境」+ 传了图 + 这张图实测是深色
  () => userStore.themeKey === MAGIC_BG_KEY && !!userStore.bgUrl && !!userStore.bgDark
)

const DARK_PREVIEW_TEXT = '#FFFFFF'
const DARK_PREVIEW_SHADOW = '0 1px 2px rgba(0, 0, 0, 0.95), 0 0 4px rgba(0, 0, 0, 0.8)'
const LIGHT_PREVIEW_SHADOW = '0 0 2px rgba(255, 255, 255, 0.98), 0 0 3px rgba(255, 255, 255, 0.9)'

/**
 * 阴影配对规则（光影幻境下所有卡片文字通用）：
 * 白色字体 → 黑色阴影；黑色（深色）字体 → 白色阴影。
 * 字和阴影必须反着来，才不会糊成一团。
 */
function shadowForText(color) {
  return isDarkColor(color) ? LIGHT_PREVIEW_SHADOW : DARK_PREVIEW_SHADOW
}

/** 这个底色偏暗吗（决定压多少蒙版：本来就暗的少压一点，亮的多压一点） */
function isDarkColor(color) {
  const value = String(color || '').replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(value)) return false
  const num = parseInt(value, 16)
  const linear = (c) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  }
  const l =
    0.2126 * linear((num >> 16) & 255) + 0.7152 * linear((num >> 8) & 255) + 0.0722 * linear(num & 255)
  return l < 0.35
}

/**
 * 深色背景图时：预览卡片压一层深色蒙版（主题色仍然透得出来），
 * 文字从原来的深色改成纯白 + 一点黑色阴影 —— 白字压深色底才看得清，不会糊。
 */
function darkPreviewBg(bg) {
  const alpha = isDarkColor(bg) ? 0.32 : 0.62
  return `linear-gradient(0deg, rgba(6, 8, 14, ${alpha}), rgba(6, 8, 14, ${alpha})), ${bg}`
}

function cardStyle(item) {
  const pv = previewOf(item)
  const style = {
    background: pv.bg,
    borderColor: pv.border,
    color: pv.text,
    boxShadow: isActiveTheme(item)
      ? `0 0 0 3px ${pv.border}, 0 6px 16px ${hexToRgba(pv.border, 0.35)}`
      : `0 4px 12px ${hexToRgba(pv.border, 0.22)}`
  }
  if (!darkBgPreview.value) {
    // 光影幻境的其他情况（没传图 / 浅色图）：文字颜色不变，但阴影按「黑白配对」规则给
    if (userStore.themeKey !== MAGIC_BG_KEY) return style
    return { ...style, textShadow: shadowForText(style.color) }
  }
  return {
    ...style,
    background: darkPreviewBg(pv.bg),
    color: DARK_PREVIEW_TEXT,
    textShadow: DARK_PREVIEW_SHADOW
  }
}

/** 角标（选中对勾 / 深浅状态）：跟卡片同一套配色，保证深浅卡片上都看得清 */
function badgeStyle(item) {
  const pv = previewOf(item)
  if (!darkBgPreview.value) {
    const style = { background: pv.card, color: pv.text }
    if (userStore.themeKey !== MAGIC_BG_KEY) return style
    return { ...style, textShadow: shadowForText(style.color) }
  }
  return {
    background: darkPreviewBg(pv.card),
    color: DARK_PREVIEW_TEXT,
    textShadow: DARK_PREVIEW_SHADOW
  }
}

/**
 * 「主 / 深 / 浅」三个色阶块的文字：文字颜色始终按底色深浅自适应（浅块黑字 / 深块白字），
 * 阴影按上面的配对规则走：黑字配白色阴影、白字配黑色阴影，保证三种块上都分得清。
 */
function swatchStyle(color) {
  const textColor = textOnColor(color)
  const style = { background: color, color: textColor }
  if (userStore.themeKey !== MAGIC_BG_KEY) return style
  return { ...style, textShadow: shadowForText(textColor) }
}

/**
 * 上传背景图弹窗里的「主 / 深 / 浅」色块：
 * 文字按底色取相反色，阴影再反一次 —— 黑字配白色阴影、白字配黑色阴影，
 * 保证浅色块和深色块上的字都分得清。
 */
function magicChipStyle(color) {
  const textColor = textOnColor(color)
  return { background: color, color: textColor, textShadow: shadowForText(textColor) }
}

/** 自定义卡片右上角的「选中对勾」：深色背景图时图标改白色，不然压在深色蒙版上看不见 */
function checkedStyle(item) {
  return darkBgPreview.value
    ? { color: DARK_PREVIEW_TEXT, textShadow: DARK_PREVIEW_SHADOW }
    : { color: item.dark }
}

/** 自定义主题卡片：不参加深浅切换，保持原来的样子（背景主色 / 边框深色 / 文字自适应） */
function customCardStyle(item) {
  const style = {
    background: item.primary,
    borderColor: item.dark,
    color: textOnColor(item.primary),
    boxShadow:
      item.key === userStore.themeKey
        ? `0 0 0 3px ${item.light}, 0 6px 16px ${hexToRgba(item.dark, 0.35)}`
        : `0 4px 12px ${hexToRgba(item.dark, 0.22)}`
  }
  if (!darkBgPreview.value) {
    // 光影幻境的其他情况：文字颜色不变，但阴影按「黑白配对」规则给
    if (userStore.themeKey !== MAGIC_BG_KEY) return style
    return { ...style, textShadow: shadowForText(style.color) }
  }
  return {
    ...style,
    background: darkPreviewBg(item.primary),
    color: DARK_PREVIEW_TEXT,
    textShadow: DARK_PREVIEW_SHADOW
  }
}

function hexToRgba(hex, alpha) {
  const value = String(hex).replace('#', '')
  const num = parseInt(value.length === 3 ? value.replace(/./g, '$&$&') : value, 16)
  return `rgba(${(num >> 16) & 255}, ${(num >> 8) & 255}, ${num & 255}, ${alpha})`
}

/** 顶部预览条：主色 → 深 → 浅 */
function previewGradient(item) {
  return `linear-gradient(120deg, ${item.light} 0%, ${item.primary} 50%, ${item.dark} 100%)`
}

/**
 * 点卡片应用主题（深浅模式的切换也走这里，图标不响应点击）：
 * - 没在用的主题 → 浅色模式
 * - 正在用的主题 → 浅 ⇄ 深 循环切换
 * 自定义主题没有模式，themeKeyOf 原样返回 custom-{id}
 */
async function handleSelect(item) {
  const multi = modeCount(item) > 1
  let mode = MODE_LIGHT
  if (multi && isActiveTheme(item)) {
    mode = currentMode.value === MODE_LIGHT ? MODE_DARK : MODE_LIGHT
  }
  const key = themeKeyOf(item.key, mode)
  const modeText = multi ? `${modeLabel(mode)}模式` : ''
  if (key === userStore.themeKey) {
    ElMessage.info(`当前已在使用「${item.name}」${modeText}`)
    return
  }
  try {
    // 保存到数据库 + 立即改 CSS 变量（页面不刷新）
    await userStore.saveTheme(key)
    ElMessage.success(`已切换为「${item.name}」${modeText}`)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ==================== 第 12 套：光影幻境（magic-bg） ==================== */

/** 「光影幻境」是不是正在用 */
const isMagicActive = computed(() => userStore.themeKey === MAGIC_BG_KEY)

/** 卡片配色：取到背景图配色就用它，没取到就用主题自带的蓝紫兜底 */
const magicPalette = computed(() => {
  const theme = THEMES.find((item) => item.magicBg)
  const p = userStore.bgPalette
  return {
    primary: p?.primary || theme?.primary || '#5B6BFF',
    dark: p?.dark || theme?.dark || '#3F3FB5',
    light: p?.light || theme?.light || '#9AA4FF'
  }
})

/**
 * 卡片背景：传了图 → 直接铺这张图；没传 → 纯白底（星星是画在上面的，见模板）。
 * 文字颜色按背景明暗自适应：白底星空 / 亮图 → 深色字；暗图 → 白字（再叠一层描边阴影保证可读）。
 */
const magicCardStyle = computed(() => {
  const p = magicPalette.value
  // 深色背景图 → 白色文字；白色 / 浅色背景图（或没传图）→ 深色文字（相反色）
  const light = !userStore.bgUrl || !userStore.bgDark
  const text = light ? '#0A0B0D' : '#FFFFFF'
  return {
    background: userStore.bgUrl ? '#f2f3f7' : '#ffffff',
    borderColor: isMagicActive.value ? p.primary : 'rgba(120, 130, 150, 0.55)',
    color: text,
    /* 深色图 → 纯白字 + 黑描边 + 右下发光；浅色 / 没传图 → 近黑字 + 白色紧致描边 */
    textShadow: light
      ? '0 0 2px rgba(255, 255, 255, 0.98), 0 0 3px rgba(255, 255, 255, 0.92)'
      : '0 1px 2px rgba(0, 0, 0, 0.98), 0 0 4px rgba(0, 0, 0, 0.9), 1px 1px 6px rgba(0, 0, 0, 0.6)',
    boxShadow: isMagicActive.value
      ? `0 0 0 3px ${p.primary}, 0 6px 16px ${hexToRgba(p.dark || p.primary, 0.35)}`
      : '0 4px 12px rgba(15, 23, 42, 0.18)'
  }
})

const magicBadgeStyle = computed(() => {
  const p = magicPalette.value
  return { background: p.primary, color: textOnColor(p.primary) }
})

/** 卡片上的星空点点（固定种子，位置散乱、大小不一） */
const cardStars = (() => {
  let seed = 777
  const random = () => {
    seed = (seed * 1103515245 + 12345) % 2147483648
    return seed / 2147483648
  }
  const list = []
  for (let i = 0; i < 22; i++) {
    list.push({
      id: i,
      left: `${(random() * 94).toFixed(2)}%`,
      top: `${(4 + random() * 92).toFixed(2)}%`,
      size: `${(2 + Math.round(random() * 5)).toFixed(0)}px`,
      opacity: (0.4 + random() * 0.5).toFixed(2),
      delay: `${(random() * 3).toFixed(2)}s`
    })
  }
  return list
})()

function cardStarStyle(star) {
  return {
    left: star.left,
    top: star.top,
    width: star.size,
    height: star.size,
    opacity: star.opacity,
    animationDelay: star.delay
  }
}

/** 点整张卡片应用「光影幻境」（没上传图也能用，是默认的白底星空） */
async function handleMagicSelect() {
  if (isMagicActive.value) {
    ElMessage.info('当前已在使用「光影幻境」')
    return
  }
  try {
    await userStore.applyMagicBg()
    ElMessage.success(
      userStore.bgUrl
        ? '已切换为「光影幻境」，背景图开始滚动'
        : '已切换为「光影幻境」（还没传图，当前是默认星空，点「上传背景图」换成自己的图）'
    )
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ---- 上传背景图 → 自动取色 → 确认配色 ---- */

const bgInput = ref(null)
const magicDialog = ref(false)
const magicSaving = ref(false)
const magicPreview = ref('')
const magicFile = ref(null)
const magicColors = reactive({ primary: '', dark: '', light: '' })

function pickBgImage() {
  bgInput.value?.click()
}

async function handleBgFileChange(event) {
  const file = event.target?.files?.[0]
  if (event.target) event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  magicFile.value = file
  if (magicPreview.value) URL.revokeObjectURL(magicPreview.value)
  magicPreview.value = URL.createObjectURL(file)
  try {
    const data = await userStore.uploadMagicBg(file)
    magicColors.primary = data.primaryColor || magicColors.primary
    magicColors.dark = data.darkColor || magicColors.dark
    magicColors.light = data.lightColor || magicColors.light
    magicDialog.value = true
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 重新分析：复用自定义主题那套「图片取色」逻辑，换一套配色 */
async function reshuffleMagic() {
  if (!magicFile.value) return
  try {
    const res = await analyzeThemeImage(magicFile.value)
    const data = res.data || {}
    magicColors.primary = data.primaryColor || magicColors.primary
    magicColors.dark = data.darkColor || magicColors.dark
    magicColors.light = data.lightColor || magicColors.light
    ElMessage.success('已换一套配色')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/** 确认配色 → 存库 → 应用主题（背景滚动 + 鼠标光圈立刻生效） */
async function confirmMagic() {
  magicSaving.value = true
  try {
    await userStore.saveBgPalette({ ...magicColors })
    await userStore.applyMagicBg()
    magicDialog.value = false
    ElMessage.success('「光影幻境」已应用，动动鼠标看看光圈')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    magicSaving.value = false
  }
}

/** 删除背景图：回落到默认的白底星空（主题仍是光影幻境） */
async function handleClearBg() {
  try {
    await userStore.clearMagicBg()
    ElMessage.success('背景图已删除，回到默认星空')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/* ==================== 自定义颜色（图片取色） ==================== */

/** 点「+」方框 → 弹出系统文件选择器 */
function pickImage() {
  fileInput.value?.click()
}

/** 选中图片 → 分析配色 → 打开弹窗 */
async function handleFileChange(event) {
  const file = event.target?.files?.[0]
  // 清空 value，保证同一张图再选一次也能触发
  if (event.target) {
    event.target.value = ''
  }
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  currentFile.value = file
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file)
  await analyzeImage(file)
  dialogVisible.value = true
}

/** 上传图片，让后端提取图片里的所有主要颜色（8~12 个） */
async function analyzeImage(file) {
  analyzing.value = true
  try {
    const res = await analyzeThemeImage(file)
    const data = res.data || {}
    paletteColors.value = data.colors || []
    // 前端从这些颜色里随机分配到 8 个部位
    Object.assign(assignment, assignRoles(paletteColors.value))
    if (!form.name && data.suggestName) {
      form.name = data.suggestName
    }
    ElMessage.success(`已提取 ${paletteColors.value.length} 个主要颜色`)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    analyzing.value = false
  }
}

/** 重新分析：把已提取的颜色重新随机分配到 8 个部位（不用重新上传） */
function reshuffle() {
  if (!paletteColors.value.length) {
    pickImage()
    return
  }
  Object.assign(assignment, assignRoles(paletteColors.value))
  ElMessage.info('换了一套随机配色')
}

/**
 * 把提取出的主要颜色随机分配到 8 个部位：
 * 主色 / 深色 / 浅色 / 按钮 / 边框 / Tab 选中 / 文字高亮 / 卡片边框。
 * 主 / 深 / 浅 随机挑 3 个后按明暗排成「深 → 主 → 浅」，其余部位从剩下的颜色里继续随机取。
 */
function assignRoles(colors) {
  const pool = shuffleColors([...colors])
  const next = {}
  const trio = pool.slice(0, 3).sort((a, b) => luminance(a) - luminance(b))
  next.dark = trio[0] || ''
  next.primary = trio[Math.min(1, Math.max(0, trio.length - 1))] || ''
  next.light = trio[trio.length - 1] || ''
  const rest = pool.slice(3)
  PALETTE_KEYS.slice(3).forEach((key, index) => {
    next[key] = rest.length ? rest[index % rest.length] : next.primary
  })
  return next
}

/** 洗牌：每次调用顺序都不一样 */
function shuffleColors(list) {
  const pool = [...list]
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[pool[i], pool[j]] = [pool[j], pool[i]]
  }
  return pool
}

/** 视亮度：数值越大越亮 */
function luminance(hex) {
  const num = parseInt(String(hex).replace('#', ''), 16)
  const r = (num >> 16) & 255
  const g = (num >> 8) & 255
  const b = num & 255
  return 0.299 * r + 0.587 * g + 0.114 * b
}

/** 某个颜色是否被选进了本次配色（8 个部位里有没有它） */
function isPicked(color) {
  return PALETTE_KEYS.some((key) => assignment[key] === color)
}

/** 点色板上的颜色 → 设为主色 */
function setPrimary(color) {
  assignment.primary = color
}

function resetDialog() {
  previewUrl.value = ''
  currentFile.value = null
  paletteColors.value = []
  form.name = ''
  PALETTE_KEYS.forEach((key) => {
    assignment[key] = ''
  })
}

/** 先拿这套颜色在整站试穿一下（只改 CSS 变量，不入库） */
function previewCustomTheme() {
  applyPreviewPalette(assignment)
  ElMessage.info('已临时应用这套颜色，满意就点「保存这套颜色」')
}

async function handleSaveCustom() {
  if (!assignment.primary) {
    ElMessage.warning('请先上传一张图片，让系统分析配色')
    return
  }
  const name = (form.name || '').trim()
  if (!name) {
    ElMessage.warning('请给这套颜色起个名字')
    return
  }
  saving.value = true
  try {
    const saved = await userStore.addCustomTheme({
      name,
      primaryColor: assignment.primary,
      darkColor: assignment.dark,
      lightColor: assignment.light,
      // 整套配色按 主色/深色/浅色/按钮/边框/Tab选中/文字高亮/卡片边框 的顺序存库
      palette: PALETTE_KEYS.map((key) => assignment[key])
    })
    // 保存后直接切到这套新主题（卡片列表刷新后，后面会自动再跟一个「+」方框）
    await userStore.saveTheme(saved.key || `custom-${saved.id}`)
    dialogVisible.value = false
    ElMessage.success(`已保存并应用「${name}」，可以继续添加下一套`)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    saving.value = false
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(
      `删除后这套颜色将不可恢复，确定删除「${item.name}」吗？`,
      '删除自定义颜色',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await userStore.removeCustomTheme(item.id)
    ElMessage.success('已删除这套自定义颜色')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  }
}

/**
 * 临时应用一套还没保存的配色（试穿）：8 个部位各用分配到的颜色。
 * 走 applyTheme()，保证整站变量（背景 / 卡片 / 按钮 / Tab / 文字 / 表格 …）一起换。
 */
function applyPreviewPalette(item) {
  const palette = [
    item.primary,
    item.dark,
    item.light,
    item.button,
    item.border,
    item.tab,
    item.highlight,
    item.cardBorder
  ].filter(Boolean)
  applyTheme(userStore.theme, {
    primary: item.primary,
    dark: item.dark,
    light: item.light,
    palette
  })
}

onMounted(async () => {
  // 进页面时以数据库为准刷新一次主题 + 自定义主题
  if (userStore.isLogin) {
    await userStore.loadTheme()
    await userStore.loadCustomThemes()
  }
})
</script>

<style scoped>
.theme-page {
  padding-bottom: 8px;
}

.current-tag {
  font-size: 13px;
  color: var(--cls-text-secondary);
}

.current-tag b {
  font-weight: 600;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 18px;
}

.custom-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--cls-border-soft);
}

.theme-item {
  position: relative;
  padding: 14px 14px 12px;
  border: 2px solid transparent;
  border-radius: 0;
  cursor: pointer;
  overflow: hidden;
  /* 背景 / 边框 / 文字色都由 cardStyle 按主题主色实时给出 */
  transition: transform 0.25s ease, box-shadow 0.25s ease, filter 0.25s ease;
}

.theme-item:hover {
  transform: translateY(-4px);
  filter: brightness(1.05);
}

/* 预览条：展示「这张卡片自己的模式」的页面渐变（不读全局变量，避免串色） */
.theme-preview {
  height: 62px;
  border-radius: 0;
  margin-bottom: 12px;
  /* 中性细线：不能用 var(--border)，那是「当前主题」的边框色，会把别的卡片染成当前主题色 */
  box-shadow: inset 0 0 0 1px rgba(128, 128, 128, 0.35);
}

.theme-name {
  font-size: 15px;
  font-weight: 600;
}

.theme-key {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 10px;
}

/* 三个色阶 */
.swatches {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.swatch {
  flex: 1;
  height: 26px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  /* 背景色与文字色由模板按该色阶实时给出，保证深色块上也能看清「深」字 */
  box-shadow: inset 0 0 0 1px rgba(128, 128, 128, 0.35);
}

.swatch i {
  font-style: normal;
  font-weight: 600;
}

.apply-btn {
  font-size: 13px;
  font-weight: 500;
}

/* 深浅状态展示（只读）：右上角方块，太阳 = 浅色，月亮 = 深色；不响应点击 */
.mode-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  pointer-events: none;
  user-select: none;
  box-shadow: var(--shadow);
  z-index: 1;
}

/* 选中角标：主题卡片底 + 该主题深色对勾（右上角留给了深 / 浅按钮，角标往左挪） */
.checked-badge {
  position: absolute;
  top: 10px;
  right: 44px;
  width: 24px;
  height: 24px;
  border-radius: 0;
  background: var(--card-bg-solid);
  color: var(--text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--shadow);
}

.custom-item .checked-badge {
  right: 44px;
}

/* 卡片标题后面的「深色 / 浅色」小标签 */
.mode-tag {
  margin-left: 6px;
  padding: 1px 5px;
  font-size: 11px;
  font-weight: 400;
  border: 1px solid currentColor;
  opacity: 0.75;
  vertical-align: middle;
}

/* 自定义主题卡片右上角的删除按钮 */
.delete-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 0;
  background: var(--card-bg-solid);
  color: var(--cls-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: var(--shadow);
  transition: background 0.2s ease;
  z-index: 1;
}

.delete-badge:hover {
  background: var(--cls-danger-soft);
  color: var(--cls-danger);
}

/* 「+」上传方框：尺寸、间距跟主题卡片完全一致 */
.add-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 216px;
  background: var(--cls-card-soft);
  border: 2px dashed var(--cls-primary-ink);
  color: var(--cls-primary-ink);
}

.add-tile:hover {
  filter: none;
  background: var(--cls-card);
  border-color: var(--cls-primary);
  color: var(--cls-primary);
}

.add-plus {
  font-size: 46px;
  font-weight: 300;
  line-height: 1;
}

.add-text {
  font-size: 13px;
}

.hidden-input {
  display: none;
}

/* ==================== 第 12 套：光影幻境卡片 ==================== */

.magic-item {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

/* 卡片背景层：整张卡铺满（图片 or 白底星空），内容浮在它上面 */
.magic-card-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.magic-card-img {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.magic-card-stars {
  position: absolute;
  inset: 0;
  background: #ffffff;
}

/* 星空点点：小黑点（白底上清楚），大小 / 位置 / 明暗都不一样 */
.magic-card-star {
  position: absolute;
  display: block;
  background: #111111;
  border-radius: 50%;
  animation: magic-card-star-blink 3s ease-in-out infinite;
}

@keyframes magic-card-star-blink {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }

  50% {
    opacity: 0.95;
    transform: scale(1.15);
  }
}

/* 卡上的文字 / 按钮压在背景层上面；预览条留空（透出卡片背景的那张图） */
.magic-item .theme-name,
.magic-item .theme-key,
.magic-item .apply-btn,
.magic-item .checked-badge,
.magic-item .swatches {
  position: relative;
  z-index: 1;
}

.magic-preview-strip {
  background: transparent;
  box-shadow: none;
}

/* 上传 / 删除按钮：占「三个色阶」那一行，高度与 .swatch（26px）完全一致，
   这样第 12 张卡和前 11 张卡的外框高度、内边距、文字位置对齐 */
.swatch-btn {
  flex: 1;
  height: 26px;
  padding: 0 4px;
  font-size: 12px;
  border-radius: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.magic-item :deep(.swatch-btn .el-icon) {
  font-size: 13px;
}

/* ==================== 光影幻境配色弹窗 ==================== */

.magic-dialog .magic-colors {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.magic-chip {
  flex: 1;
  padding: 8px 6px;
  font-size: 12px;
  text-align: center;
  border-radius: 2px;
}

.magic-preview-bar {
  height: 46px;
  margin-top: 14px;
  border: 1px solid var(--cls-border);
}

/* ==================== 自定义颜色弹窗 ==================== */

.custom-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-row {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.upload-box {
  width: 150px;
  height: 110px;
  border: 1px solid var(--cls-border);
  background: var(--cls-card-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--cls-text-secondary);
  font-size: 13px;
}

.upload-tip {
  flex: 1;
  font-size: 13px;
  color: var(--cls-text-regular);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-tip p {
  margin: 0;
}

.tip-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.tip-hint {
  font-size: 12px;
  color: var(--cls-text-secondary);
}

.block-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--cls-primary-ink);
  margin-bottom: 8px;
}

/* 图片里的所有主要颜色（8~12 个） */
.palette-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.palette-chip {
  width: 46px;
  height: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px var(--border);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.palette-chip:hover {
  transform: translateY(-2px);
}

.palette-chip.picked {
  box-shadow: 0 0 0 2px var(--cls-primary), inset 0 0 0 1px var(--border);
}

/* 8 个部位的分配结果 */
.result-block {
  border: 1px solid var(--cls-border);
  background: var(--cls-card-soft);
  padding: 12px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px 14px;
  margin-bottom: 12px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--cls-text-regular);
}

.role-swatch {
  width: 26px;
  height: 18px;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px var(--border);
}

.role-label {
  color: var(--cls-text-secondary);
  flex-shrink: 0;
}

.role-value {
  font-family: Consolas, Monaco, monospace;
}

.result-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.result-preview {
  width: 160px;
  height: 40px;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px var(--border);
}

.name-form {
  margin-bottom: 0;
}

.analyzing {
  margin: 0;
  font-size: 13px;
  color: var(--cls-primary-ink);
}

@media (max-width: 768px) {
  .theme-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }

  .upload-row {
    flex-direction: column;
  }

  .upload-box {
    width: 100%;
  }
}
</style>
