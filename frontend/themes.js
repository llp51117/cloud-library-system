/**
 * 主题（皮肤）
 *
 * 风格参考电竞比赛计分板：深色底（黑里带主色调）+ 灰色半透明卡片 + 亮色主题边框
 * + 白色正文 / 主色标题 + 主色小长方体装饰 + 主色按钮（深色文字）+ 直角。
 *
 * 每套主题只给 2 个基础色（primary 主色 + bg 该主题的深色底），
 * 其余（深色 / 浅色色阶、卡片、边框、表头、文字）全部由这两个色推导出来，
 * 所以切主题是「整站换色」，不是只换个框。
 *
 * 主题标识存数据库（user.theme），与后端 UserService.THEMES 一一对应，不用 localStorage 存。
 * 除了内置的 11 套，用户还能用「图片取色」保存自己的主题（标识 custom-{id}），见 setCustomThemes。
 */
/** 新用户默认主题：极光圣殿白（浅色），与后端 UserVO.DEFAULT_THEME / 数据库默认值一致 */
export const DEFAULT_THEME = 'aurora-white'

/** 登录 / 注册 / 忘记密码页永远用这套初始色（用户还没登录，不知道他的主题） */
export const GUEST_THEME = 'quantum-blue'

/**
 * 登录 / 注册 / 忘记密码页专用「浅色初始色」：
 * 浅灰白底 #F5F7FA + 白色卡片 + 浅灰边框 + 深色文字 #333333 + 量子脉冲蓝 #00D4FF 点缀。
 * 这三个页面用户还没登录，不知道他的主题，所以固定用这套浅色，跟用户端的深色皮肤解耦。
 */
export const AUTH_THEME = {
  key: 'aurora-white',
  name: '登录页浅色',
  primary: '#00D4FF', // 量子脉冲蓝：按钮 / 标题 / 底部链接
  dark: darken('#00D4FF', 0.3), // 主色加深：边框 / 悬停
  light: lighten('#00D4FF', 0.4), // 主色变浅：高亮
  bg: '#F5F7FA', // 浅灰白页面底
  lightMode: true,
  // 点缀色：浅色主题里 CSS 的真正「主色」就是它，登录页三个页面固定用蓝色点缀
  accent: '#00D4FF',
  ink: '#00D4FF' // 标题用主色
}

/** 自定义主题标识前缀：custom-{id} */
export const CUSTOM_PREFIX = 'custom-'

/**
 * 配色的 8 个角色：图片里提取出的 8~12 个主要颜色会「随机」分配到这些位置上，
 * 存库顺序（palette 字段）和这里保持一致。
 */
export const PALETTE_ROLES = [
  { key: 'primary', label: '主色' },
  { key: 'dark', label: '深色' },
  { key: 'light', label: '浅色' },
  { key: 'button', label: '按钮' },
  { key: 'border', label: '边框' },
  { key: 'tab', label: 'Tab 选中' },
  { key: 'highlight', label: '文字高亮' },
  { key: 'cardBorder', label: '卡片边框' }
]

/** palette 存库 / 读取的顺序 */
export const PALETTE_KEYS = PALETTE_ROLES.map((item) => item.key)

/** 校验并整理一套配色：只留合法的 #RRGGBB，最多 12 个 */
export function normalizePalette(list) {
  if (!Array.isArray(list)) return []
  return list
    .filter((c) => typeof c === 'string' && /^#[0-9a-fA-F]{6}$/.test(c.trim()))
    .map((c) => c.trim().toUpperCase())
    .slice(0, 12)
}

/**
 * 内置 11 套主题（已去掉「超新星爆闪黄」，它和「熔核爆裂橙」太像）
 * 排列顺序即「更换皮肤」页面的展示顺序：白 → 灰 → 蓝 → 青 → 绿 → 金 → 橙 → 红 → 粉 → 紫红 → 紫
 * 深色 / 浅色不用手写，统一由主色推导：深 = 主色加深 30%，浅 = 主色变浅 40%。
 */
const BASE_THEMES = [
  // 每套主题只给主色 primary，深色 / 浅色由它推导（深 = 主色加深 30%，浅 = 主色变浅 40%）；
  // 页面底色（--cls-bg / --bg-page / --cls-topbar-bg 等）不再写死，统一由「深色」现场算，
  // 见 deriveBackground()。
  /*
   * 极光圣殿白：白 / 浅灰 / 深灰一整套；accent 是它的「点缀色」（按钮 / Tab / 标题 / 高亮）。
   * flatWhite = 唯一一套「允许写死」的浅色主题：纯白底 + 纯白卡片 + #E5E7EB 边框 + #333333 文字。
   */
  {
    key: 'aurora-white',
    name: '极光圣殿白',
    primary: '#ECEFF1',
    accent: '#333333',
    lightMode: true,
    flatWhite: true
  }, // 浅灰白
  { key: 'ghost-gray', name: '暗夜幽灵灰', primary: '#607D8B' }, // 灰
  { key: 'quantum-blue', name: '量子脉冲蓝', primary: '#00D4FF', classic: true }, // 蓝（配色不动）
  { key: 'deep-cyan', name: '深空回响青', primary: '#00FFD1' }, // 青
  { key: 'matrix-green', name: '赛博矩阵绿', primary: '#00FF44', classic: true }, // 绿（配色不动）
  { key: 'gold-rhythm', name: '黄金律动金', primary: '#FFB300' }, // 金
  { key: 'core-orange', name: '熔核爆裂橙', primary: '#FF6B00' }, // 橙
  { key: 'flame-red', name: '赤焰脉冲红', primary: '#FF1744', classic: true }, // 红（配色不动）
  // 霓虹幻梦粉：浅色主题（主题色 → 浅色 的浅粉渐变 + 深灰文字），不走深色渐变
  { key: 'neon-pink', name: '霓虹幻梦粉', primary: '#FF4FA3', lightMode: true }, // 粉
  { key: 'nebula-magenta', name: '星云裂变紫红', primary: '#D500F9' }, // 紫红
  { key: 'void-purple', name: '虚空裂变紫', primary: '#7C4DFF' } // 紫
]

/** 加深：往黑色方向混合（0.3 = 加深 30%） */
function darken(hex, ratio) {
  return mix(hex, '#000000', ratio)
}

/** 变浅：往白色方向混合（0.4 = 变浅 40%） */
function lighten(hex, ratio) {
  return mix(hex, '#ffffff', ratio)
}

/** 内置 11 套主题：每套带 primary / dark / light 三个色阶 */
export const THEMES = BASE_THEMES.map((item) => ({
  ...item,
  dark: darken(item.primary, 0.3),
  light: lighten(item.primary, 0.4)
}))

const THEME_MAP = THEMES.reduce((map, item) => {
  map[item.key] = item
  return map
}, {})

/* ==================== 用户自定义主题（图片取色，存数据库） ==================== */

/** 当前登录用户的自定义主题：{ 'custom-3': { key, name, primary, dark, light, custom: true } } */
let CUSTOM_THEMES = {}

/** 自定义主题标识 */
export function customKey(id) {
  return `${CUSTOM_PREFIX}${id}`
}

/** 是否是自定义主题标识 */
export function isCustomKey(key) {
  return typeof key === 'string' && key.startsWith(CUSTOM_PREFIX)
}

/**
 * 把当前用户的自定义主题注册进来（登录后 / 新增 / 删除后调用）
 * @param {Array} list 后端 GET /api/user/themes 返回的列表
 */
export function setCustomThemes(list) {
  const map = {}
  ;(list || []).forEach((item) => {
    if (!item) return
    const key = item.key || customKey(item.id)
    map[key] = {
      key,
      id: item.id,
      name: item.name || '我的主题',
      primary: item.primaryColor || item.primary || THEME_MAP[DEFAULT_THEME].primary,
      dark: item.darkColor || item.dark,
      light: item.lightColor || item.light,
      // 整套配色（主色/深色/浅色/按钮/边框/Tab选中/文字高亮/卡片边框）
      palette: normalizePalette(item.palette),
      custom: true
    }
  })
  CUSTOM_THEMES = map
  return Object.values(map)
}

/** 已注册的自定义主题列表 */
export function getCustomThemes() {
  return Object.values(CUSTOM_THEMES)
}

/** 按标识取主题（内置 + 自定义），找不到（老数据 / 脏数据）时回退到默认主题 */
export function getTheme(key) {
  if (isCustomKey(key) && CUSTOM_THEMES[key]) {
    return CUSTOM_THEMES[key]
  }
  return THEME_MAP[key] || THEME_MAP[DEFAULT_THEME]
}

/** 已知主题标识才返回（自定义标识一律放行），否则返回默认主题标识 */
export function normalizeTheme(key) {
  if (isCustomKey(key)) {
    return CUSTOM_THEMES[key] ? key : DEFAULT_THEME
  }
  return THEME_MAP[key] ? key : DEFAULT_THEME
}

/* ==================== 颜色工具 ==================== */

/** #RRGGBB / #RGB → { r, g, b } */
function hexToRgb(hex) {
  let value = String(hex || '').trim().replace('#', '')
  if (value.length === 3) {
    value = value
      .split('')
      .map((c) => c + c)
      .join('')
  }
  if (value.length !== 6 || /[^0-9a-fA-F]/.test(value)) {
    return { r: 0, g: 212, b: 255 }
  }
  const num = parseInt(value, 16)
  return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 }
}

function rgbToHex({ r, g, b }) {
  const toHex = (n) => Math.round(Math.min(255, Math.max(0, n))).toString(16).padStart(2, '0')
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

/** 两个颜色按比例混合：t=0 取 c1，t=1 取 c2 */
function mix(c1, c2, t) {
  const a = hexToRgb(c1)
  const b = hexToRgb(c2)
  return rgbToHex({
    r: a.r + (b.r - a.r) * t,
    g: a.g + (b.g - a.g) * t,
    b: a.b + (b.b - a.b) * t
  })
}

/** #RRGGBB → "r, g, b"，供 CSS 的 rgba(var(--cls-primary-rgb), 0.x) 使用 */
function hexToRgbChannels(hex) {
  const { r, g, b } = hexToRgb(hex)
  return `${r}, ${g}, ${b}`
}

/** 0-1 的视觉亮度 */
function luminanceOf(hex) {
  const { r, g, b } = hexToRgb(hex)
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255
}

/**
 * 主色上叠的文字色：计分板风格统一用深色文字（12 套主色都是亮色 / 中间调，深字才清楚）
 */
function onPrimaryColor(hex) {
  return luminanceOf(hex) < 0.3 ? '#ffffff' : '#0b1020'
}

/* ==================== 对比度保护 ====================
 * 用户上传图片取色时，分到各个位置的颜色可能「撞车」：浅色字压在浅底上、
 * 边框和底色糊成一片。下面这几个函数是全站的兜底 —— 不管传什么图、算出什么色，
 * 文字一定和所在底色拉开对比度，边框一定和所在底色拉开色差。
 * ==================================================== */

/** 是不是合法的 #RRGGBB（半透明 / 渐变字符串直接放行，不参与计算） */
function isHexColor(value) {
  return typeof value === 'string' && /^#[0-9a-fA-F]{6}$/.test(value.trim())
}

/** WCAG 相对亮度（0~1），比简单加权更准 */
function relativeLuminance(hex) {
  const { r, g, b } = hexToRgb(hex)
  const ch = (v) => {
    const c = v / 255
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  }
  return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)
}

/** 两个颜色的对比度（1 ~ 21）：不算半透明 / 渐变，直接当「没问题」放行 */
export function contrastRatio(a, b) {
  if (!isHexColor(a) || !isHexColor(b)) return 21
  const la = relativeLuminance(a.trim())
  const lb = relativeLuminance(b.trim())
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05)
}

/**
 * 文字色保护：文字和底色的对比度不够，就把它往「远离底色」的方向推到达标为止
 * （底色亮 → 文字往黑里压；底色暗 → 文字往白里提）。
 * @param {string} text 文字色
 * @param {string} background 文字所在的底色
 * @param {number} min 最低对比度，正文 4.5、大字号 / 次要文字 3 起
 */
export function ensureTextContrast(text, background, min = 4.5) {
  if (!isHexColor(text) || !isHexColor(background)) return text
  if (contrastRatio(text, background) >= min) return text
  /* 底色亮 → 先把文字往黑里压；底色暗 → 先往白里提。
   * 底色是中间调时「只往一个方向推」可能越推越糟（白字在亮青底上怎么推都不够），
   * 所以主方向不行就试反方向，两条都不行就取对比度更高的那条。 */
  const dark = pushTowards(text, background, '#000000', min)
  const light = pushTowards(text, background, '#ffffff', min)
  const okDark = contrastRatio(dark, background) >= min
  const okLight = contrastRatio(light, background) >= min
  if (okDark && okLight) {
    return Math.abs(luminanceOf(dark) - luminanceOf(text)) <=
      Math.abs(luminanceOf(light) - luminanceOf(text))
      ? dark
      : light
  }
  if (okDark) return dark
  if (okLight) return light
  return contrastRatio(dark, background) >= contrastRatio(light, background) ? dark : light
}

/** 把颜色逐档往 target 推，第一档达标就返回 */
function pushTowards(color, background, target, min) {
  for (let i = 1; i <= 20; i++) {
    const next = mix(color.trim(), target, i / 20)
    if (contrastRatio(next, background) >= min) return next
  }
  return target
}

/**
 * 深色面（卡片 / 输入框 / 表头这类）：白字看不清就把「面」压暗，压到达标为止。
 * 深色主题永远是「深底 + 白字」，所以这里改的是底色而不是把字改成黑色。
 */
export function darkenForWhiteText(surface, min = 4) {
  if (!isHexColor(surface)) return surface
  if (contrastRatio('#ffffff', surface) >= min) return surface
  for (let i = 1; i <= 20; i++) {
    const next = mix(surface.trim(), '#000000', i / 20)
    if (contrastRatio('#ffffff', next) >= min) return next
  }
  return '#000000'
}

/**
 * 浅色面：深字看不清就把「面」提亮，提到达标为止。
 * 浅色主题永远是「浅底 + 深字」，同理改底色而不是把字改白。
 */
export function lightenForDarkText(surface, min = 4, text = '#333333') {
  if (!isHexColor(surface)) return surface
  if (contrastRatio(text, surface) >= min) return surface
  for (let i = 1; i <= 20; i++) {
    const next = mix(surface.trim(), '#ffffff', i / 20)
    if (contrastRatio(text, next) >= min) return next
  }
  return '#ffffff'
}

/**
 * 边框色保护：边框和底色的色差不够（糊在一起看不出框），
 * 就把边框往「远离底色」的方向推到达标为止（浅底 → 边框压深；深底 → 边框提亮）。
 */
export function ensureBorderContrast(border, background, min = 1.8) {
  if (!isHexColor(border) || !isHexColor(background)) return border
  if (contrastRatio(border, background) >= min) return border
  const target = luminanceOf(background) >= 0.5 ? '#000000' : '#ffffff'
  for (let i = 1; i <= 20; i++) {
    const next = mix(border.trim(), target, i / 20)
    if (contrastRatio(next, background) >= min) return next
  }
  return target
}

/**
 * 这个底色上放深字还是白字更清楚？按对比度算，不拍脑袋定阈值：
 * 白字对比度 1.05/(L+0.05)，深字对比度 (L+0.05)/0.0626，谁大用谁。
 */
function prefersDarkText(hex) {
  const l = luminanceOf(hex)
  return (l + 0.05) / 0.0626 >= 1.05 / (l + 0.05)
}

/**
 * 「某个底色上放什么文字才看得清」：亮底用深字、深底用白字。
 * 页面底 / 顶栏 / Tab 栏这些跟随主题变化的面，都用它算文字色，
 * 这样就算底色被推亮（青、绿、金这类亮色主题）也不会出现白底白字。
 */
function onSurfaceColor(hex) {
  return prefersDarkText(hex) ? '#111827' : '#ffffff'
}

/**
 * 渐变底上当作文字用的颜色：太暗（深底深字）就往白里提，保证「亮色文字 + 渐变底」看得清。
 * 标题 / 链接 / 高亮这些「彩色文字」都过一遍这个。
 */
function inkForDarkSurface(hex) {
  let color = hex
  // 逐步提亮，直到亮度够在渐变底 / 半透明卡片上当文字或描边（≈0.45）
  for (let i = 0; i < 5 && luminanceOf(color) < 0.45; i++) color = lighten(color, 0.3)
  return color
}

/**
 * 浅色底上当作文字用的颜色：太亮（浅底浅字）就往黑里压，压到亮度 ≈ 0.2（对比度约 4.2:1）。
 * 浅色主题的标题 / 链接 / 高亮都过一遍这个，保证浅色渐变底上看得清。
 */
function inkForLightSurface(hex) {
  const l = luminanceOf(hex)
  if (l <= 0.2) return hex
  const ratio = Math.min(0.75, 1 - 0.2 / l)
  return mix(hex, '#000000', ratio)
}

/**
 * 卡片的渐变描边：主题色的亮色 → 主题色 → 主题色的暗色（135°）。
 * 配合 CSS 里的 border-image 使用，让边框也带上主题渐变。
 */
function buildBorderGradient(primary, borderColor) {
  return `linear-gradient(135deg, ${lighten(primary, 0.55)} 0%, ${borderColor} 45%, ${darken(primary, 0.3)} 100%)`
}

/* ==================== HSL 工具（让「变深」往鲜艳方向走，不是往黑里走） ==================== */

/** #RRGGBB → { h, s, l }（0~1） */
function hexToHsl(hex) {
  const { r, g, b } = hexToRgb(hex)
  const rn = r / 255
  const gn = g / 255
  const bn = b / 255
  const max = Math.max(rn, gn, bn)
  const min = Math.min(rn, gn, bn)
  const l = (max + min) / 2
  const d = max - min
  let h = 0
  let s = 0
  if (d !== 0) {
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    if (max === rn) h = ((gn - bn) / d + (gn < bn ? 6 : 0)) / 6
    else if (max === gn) h = ((bn - rn) / d + 2) / 6
    else h = ((rn - gn) / d + 4) / 6
  }
  return { h, s, l }
}

/** { h, s, l }（0~1）→ #RRGGBB */
function hslToHex(h, s, l) {
  const pick = (p, q, t) => {
    let x = t
    if (x < 0) x += 1
    if (x > 1) x -= 1
    if (x < 1 / 6) return p + (q - p) * 6 * x
    if (x < 1 / 2) return q
    if (x < 2 / 3) return p + (q - p) * (2 / 3 - x) * 6
    return p
  }
  let r = l
  let g = l
  let b = l
  if (s !== 0) {
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s
    const p = 2 * l - q
    r = pick(p, q, h + 1 / 3)
    g = pick(p, q, h)
    b = pick(p, q, h - 1 / 3)
  }
  const two = (v) =>
    Math.round(Math.min(255, Math.max(0, v * 255)))
      .toString(16)
      .padStart(2, '0')
  return `#${two(r)}${two(g)}${two(b)}`.toUpperCase()
}

/**
 * 主题色的三档深浅（「变深」往鲜艳的方向走：亮度压一点、饱和度反而提上去，不是往黑里混）：
 * - lightest 主题最浅色 → 深色主题的边框
 * - deep     主题深色（亮度 80%）
 * - deepest  主题最深色 → 深色主题的卡片 / 浅色主题的边框
 * 例：#d58de0 的深色是 #C848DC 这种鲜艳紫，不是 #571761 这种发暗的紫。
 */
function shadeScale(P) {
  const { h, s, l } = hexToHsl(P)
  return {
    lightest: hslToHex(h, s, Math.min(0.93, l + (1 - l) * 0.4)),
    deep: hslToHex(h, Math.min(1, s + (1 - s) * 0.25), Math.max(0.1, l * 0.8)),
    deepest: hslToHex(h, Math.min(1, s + (1 - s) * 0.3), Math.max(0.08, l * 0.62))
  }
}

/** 赛博主题 = 内置主题且不是极光圣殿白（白色单独一套，不参加科技感） */
function isCyberTheme(theme) {
  return !String(theme.key).startsWith(CUSTOM_PREFIX) && !theme.flatWhite
}

/**
 * 赛博主题专用：把「深色档」提亮提饱和一点。
 * 原来的页面渐变最深档压到 0.62 / 0.8，蓝色全被压成墨蓝黑（用户：我的蓝色呢？）。
 * 提亮后仍保证白字够得着（亮档只当背景，不当卡片底）。
 */
function liftDeep(hex, factor) {
  const { h, s, l } = hexToHsl(hex)
  return hslToHex(h, Math.min(1, s * 1.06 + 0.04), Math.min(0.38, l * factor))
}

/**
 * 主题的四类：
 * - flat    极光圣殿白：唯一允许写死的纯白 / 浅灰 / 深灰一套
 * - classic 红 / 绿 / 蓝：这三套用户满意，配色保持原样不动
 * - light   浅色主题（霓虹幻梦粉 + 图片取色偏亮的自定义主题）
 * - dark    深色主题（青 / 金 / 橙 / 紫红 / 紫 / 灰 + 主色偏暗的自定义主题）
 */
function themeKind(theme) {
  if (theme.flatWhite) return 'flat'
  if (theme.classic) return 'classic'
  return theme.lightMode ? 'light' : 'dark'
}

/** 某个底色上放白字还是 #333333 深字（按对比度算，不拍脑袋） */
function textOnCard(hex) {
  return prefersDarkText(hex) ? '#333333' : '#ffffff'
}

/**
 * 背景五件套（--cls-bg / --cls-bg-soft / --bg-page / --cls-page-bg / --cls-topbar-bg）
 * 全部由当前主题的「主色 P」现场推导，CSS 里没有任何写死的底色。
 *
 * 浅色主题：页面底 = 主题色再浅一点点（135° 三层渐变），Tab 栏 = 主题色本身
 * 深色主题：页面底 = 主题色「加浅 + 加深」的 135° 四层渐变（最浅 → 主题色 → 深 → 最深）
 *           卡片用最深那一档（不超过它），边框用最浅那一档并带发光
 * 红 / 绿 / 蓝（classic）保持原样；极光圣殿白（flat）走纯白那一套。
 */
function deriveBackground(theme) {
  const P = theme.primary
  const kind = themeKind(theme)

  if (kind === 'flat') {
    return {
      base: '#FFFFFF',
      bg: '#FFFFFF',
      bgSoft: '#F7F8FA',
      pageStops: ['#FFFFFF', '#FFFFFF'],
      topbarTop: '#FFFFFF',
      topbarBottom: '#FFFFFF'
    }
  }

  /* 红 / 绿 / 蓝：保持用户满意的那一套，一个色阶都不动
   * （赛博提亮：原来 0.62 / 0.8 两档太黑，页面糊成墨蓝黑；整体上移，蓝色才看得出来） */
  if (kind === 'classic') {
    const c = isCyberTheme(theme)
    return {
      base: darken(P, c ? 0.42 : 0.5),
      bg: darken(P, c ? 0.5 : 0.62),
      bgSoft: darken(P, c ? 0.58 : 0.7),
      pageStops: [
        darken(P, c ? 0.08 : 0.15), // 第 1 层：主题色（亮）
        darken(P, c ? 0.3 : 0.4), // 第 2 层：中间色
        darken(P, c ? 0.48 : 0.62), // 第 3 层：偏暗
        darken(P, c ? 0.64 : 0.8) // 第 4 层：主题色的暗色
      ],
      topbarTop: darken(P, c ? 0.42 : 0.5),
      topbarBottom: darken(P, c ? 0.6 : 0.72)
    }
  }

  /* 浅色主题：背景 = 主题色浅一点点（渐变），Tab 栏 = 主题色，卡片 = 主题色 */
  if (kind === 'light') {
    return {
      base: lighten(P, 0.18),
      bg: lighten(P, 0.18),
      bgSoft: P, // 6 个 Tab 那条 = 主题色
      pageStops: [
        lighten(P, 0.3), // 第 1 层：最浅
        lighten(P, 0.18), // 第 2 层：浅一点点
        lighten(P, 0.06) // 第 3 层：贴近主题色
      ],
      topbarTop: lighten(P, 0.28),
      topbarBottom: lighten(P, 0.08)
    }
  }

  /* 深色主题：主题色「加浅 → 加深」四层渐变，最深一层给卡片、最浅一层给边框
   * 赛博主题：深色档整体提亮提饱和，页面才有「色」，不会糊成墨蓝黑 */
  const scale = shadeScale(P)
  const cyber = isCyberTheme(theme)
  return {
    base: cyber ? liftDeep(scale.deep, 0.85) : scale.deep,
    bg: cyber ? liftDeep(scale.deep, 0.85) : scale.deep,
    bgSoft: cyber ? liftDeep(scale.deepest, 0.72) : scale.deepest,
    pageStops: [
      scale.lightest, // 第 1 层：主题最浅色（边框用）
      P, // 第 2 层：主题色
      cyber ? liftDeep(scale.deep, 0.85) : scale.deep, // 第 3 层：主题深色
      cyber ? liftDeep(scale.deepest, 0.72) : scale.deepest // 第 4 层：最深（卡片用，不超过它）
    ],
    topbarTop: scale.lightest,
    topbarBottom: cyber ? liftDeep(scale.deep, 0.85) : scale.deep
  }
}

/** 给主题卡片 / 色块用：某个底色上放什么颜色的文字才看得清 */
export function textOnColor(hex) {
  return onPrimaryColor(hex)
}

/**
 * 读取当前主题的 CSS 变量值（ECharts / Canvas 这类只能吃具体颜色的地方用）
 * @param {string} name 变量名，如 '--text-sub'
 * @param {string} fallback 兜底值
 */
export function cssVar(name, fallback = '') {
  if (typeof window === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name)
  const text = String(value || '').trim()
  return text || fallback
}

/** 把渐变色标拼成 135° 的 linear-gradient（几层都能用） */
function buildGradient(stops) {
  const last = Math.max(1, stops.length - 1)
  const text = stops
    .map((color, index) => `${color} ${Math.round((index / last) * 100)}%`)
    .join(', ')
  return `linear-gradient(135deg, ${text})`
}

/**
 * 推导主题配色。先按「深色 / 浅色」分流：
 *
 * 浅色主题（lightMode：极光圣殿白、霓虹幻梦粉、以及图片取色偏亮的自定义主题）：
 * - 页面底：主题色 → 浅色 的浅色渐变（不给黑背景）
 * - 卡片：浅色（极光圣殿白是纯白）
 * - 文字：#333333 系深灰；标题 / 高亮 = 主色压暗后的版本
 *
 * 深色主题（其余内置主题 + 主色偏暗的自定义主题）：
 * - 页面底：135° 四层渐变，主题色（亮）→ 中间色 → 主题色（暗），全部由主色现场派生
 * - 卡片：中性半透明（透出背后的渐变）+ 毛玻璃，压得住亮色区域，白字才清楚
 * - 边框：主题色的亮色，并额外给一条「亮 → 暗」的渐变描边（--cls-card-border-gradient）
 * - 文字：白 / 亮色系；标题、高亮用主题色提亮后的版本
 */
function derivePalette(theme) {
  const primary = theme.primary
  const rgb = hexToRgbChannels(primary)

  /* ---------- 极光圣殿白：唯一一套允许写死的浅色主题（纯白 / 浅灰 / 深灰） ---------- */
  if (theme.lightMode && theme.flatWhite) {
    const lightGray = '#E5E7EB'
    return {
      bg: '#FFFFFF',
      bgSoft: '#F7F8FA',
      pageBg: 'linear-gradient(135deg, #FFFFFF 0%, #FFFFFF 100%)',
      topbar: '#FFFFFF',
      topbarText: '#333333',
      bgSoftText: '#333333',
      lightSurface: true,

      /* 卡片：浅灰（不是纯白），在纯白底上能看出层次 */
      card: '#F5F7FA',
      cardSoft: '#F7F8FA',
      cardInner: `linear-gradient(135deg, rgba(15, 23, 42, 0.02) 0%, rgba(15, 23, 42, 0.04) 100%)`,
      cardSolid: '#F5F7FA',
      cardSoftSolid: '#F7F8FA',

      cardBorder: lightGray,
      cardBorderHover: '#CBD5E1',
      border: lightGray,
      borderSoft: '#EEF1F5',
      fill: 'rgba(15, 23, 42, 0.06)',
      tableHeader: '#EEF1F5',

      input: '#FFFFFF',
      dialog: '#F5F7FA',

      textMain: '#333333',
      textRegular: '#4b5563',
      textSecondary: '#6b7280',
      textPlaceholder: '#9ca3af',
      ink: '#333333'
    }
  }

  /* 页面底色：由主色现场推导（见 deriveBackground） */
  const back = deriveBackground(theme)
  const bg = back.bg
  const pageBg = buildGradient(back.pageStops)
  const topbar = `linear-gradient(135deg, ${back.topbarTop} 0%, ${back.topbarBottom} 100%)`

  const kind = themeKind(theme)

  /* ---------- 浅色主题：背景 = 主题色浅一点点 / Tab 栏 = 主题色 / 卡片 = 主题色
   *            边框 = 主题最深色，并带发光 ---------- */
  if (kind === 'light') {
    const deepest = shadeScale(primary).deepest // 主题最深色 → 边框
    const deepestRgb = hexToRgbChannels(deepest)
    /* 卡片 = 主题色掺大量白 → 只有淡淡的主题色（比页面底更浅，靠最深色边框拉开）；
     * 同样按「深字对比度」提亮到刚好达标，浅色主题永远是「浅底 + 深字」 */
    const cardBase = lightenForDarkText(mix(primary, '#ffffff', 0.62), 4.2)
    const cardBaseSoft = lightenForDarkText(mix(primary, '#ffffff', 0.74), 4)
    const cardMid = lightenForDarkText(mix(cardBase, '#ffffff', 0.5), 3.6)
    /* 浅色主题 = 浅底 + 深字（上面已经把所有「面」提亮到深字看得清） */
    const cardText = '#333333'
    return {
      bg,
      bgSoft: back.bgSoft,
      pageBg,
      topbar,
      topbarText: onSurfaceColor(mix(back.topbarTop, back.topbarBottom, 0.5)),
      bgSoftText: onSurfaceColor(back.bgSoft),
      lightSurface: true,

      /* 卡片 = 淡淡的主题色（比页面底更浅）；
       * 卡片渐变：左右两边是淡主题色，中间一段是更浅的平色（中间不渐变） */
      card: cardBase,
      cardSoft: cardBaseSoft,
      cardInner:
        `linear-gradient(90deg, ${cardBase} 0%, ${cardMid} 32%, ` +
        `${cardMid} 68%, ${cardBase} 100%)`,
      cardSolid: cardBase,
      cardSoftSolid: cardBaseSoft,

      /* 边框 = 主题最深色 */
      cardBorder: deepest,
      cardBorderHover: darken(deepest, 0.15),
      border: mix(primary, '#000000', 0.25),
      borderSoft: `rgba(${deepestRgb}, 0.25)`,
      fill: `rgba(${rgb}, 0.16)`,
      /* 表头 / 输入框 / 弹窗：也都是深字面，同样提到看得清 */
      tableHeader: lightenForDarkText(mix(cardBase, primary, 0.3), 4),
      input: lightenForDarkText(mix(cardBase, '#ffffff', 0.45), 4.2),
      dialog: cardBase,

      /* 文字：按卡片底色算，亮底深字、深底白字 */
      textMain: cardText,
      textRegular: mix(cardText, cardBase, 0.22),
      textSecondary: mix(cardText, cardBase, 0.45),
      textPlaceholder: mix(cardText, cardBase, 0.62),

      /* 标题 / 高亮：最深色再压暗，保证在主题色卡片上看得清 */
      ink: inkForLightSurface(deepest),

      /* 发光：用主题最深色 */
      glow: `rgba(${deepestRgb}, 0.45)`,
      shadow: '0 4px 16px rgba(15, 23, 42, 0.12)',
      shadowHover: '0 10px 28px rgba(15, 23, 42, 0.18)',

      /* 赛博：浅色主题卡片也全透明，文字（深字）对比基准 = 页面渐变最亮一档 */
      cyberGlassBase: back.pageStops[0],
      cyberCardTint: null
    }
  }

  /* ---------- 深色主题：主题色「加浅 → 加深」渐变底 + 卡片最深色 + 边框最浅色（发光） ---------- */
  if (kind === 'dark') {
    const stops = back.pageStops
    const lightest = stops[0] // 主题最浅色 → 边框
    const deepest = stops[3] // 最深一档 → 卡片（不超过它）
    const lightestRgb = hexToRgbChannels(lightest)
    /* 深色主题 = 深底 + 白字（上面已经把所有「面」压暗到白字看得清） */
    const cardText = '#ffffff'
    /*
     * 卡片渐变的两端 / 中间：两端 = 主题色再黑一点，中间 = 整条渐变里最亮的一档。
     * 三档都要能承载白字（按「白字对比度」压暗到刚好达标），
     * 否则青色 / 绿色这类亮主题色下卡片太亮，白字会糊成一片。
     */
    const edgeRaw = mix(primary, deepest, 0.62)
    /* 两端压得更暗（白字 5.2:1），中间留亮一点（4.2:1）——
       这样「两端深、中间亮」的层次还在，白字在卡片任何位置都看得清 */
    const cardEdge = darkenForWhiteText(edgeRaw, 5.2)
    const cardMid = darkenForWhiteText(mix(edgeRaw, lightest, 0.45), 4.2)
    /* 卡片底色（渐变兜底 + Element Plus 用）：同样压到白字看得清 */
    const cardFace = darkenForWhiteText(deepest, 4.5)
    /* 页面背景：横向渐变，最深色 ↔ 最浅色 交替（一深一浅地横向变色） */
    /* 页面背景：横向渐变，最深色 ↔ 亮色 交替（一深一浅地横向变色）
     * 赛博主题：亮档从「全亮最浅色」压成「比最深档略亮」——
     * 卡片全透明后文字直接坐在渐变上，亮条不能太亮 */
    const waveLight = isCyberTheme(theme) ? lighten(deepest, 0.08) : lightest
    const pageBgWave =
      `linear-gradient(90deg, ${deepest} 0%, ${waveLight} 25%, ${deepest} 50%, ` +
      `${waveLight} 75%, ${deepest} 100%)`
    return {
      bg,
      bgSoft: back.bgSoft,
      pageBg: pageBgWave,
      topbar,
      topbarText: onSurfaceColor(mix(back.topbarTop, back.topbarBottom, 0.5)),
      bgSoftText: onSurfaceColor(back.bgSoft),
      lightSurface: false,
      /* 赛博：卡片全透明，文字的对比基准 = 页面波纹的亮档 */
      cyberGlassBase: isCyberTheme(theme) ? waveLight : null,

      /* 卡片 = 最深那一档颜色（不会比渐变最深那层更黑）；
       * 卡片渐变：左右两端 = 主题色再黑一点，向中间逐渐提亮，正中间最亮（水平对称） */
      card: cardFace,
      cardSoft: darkenForWhiteText(stops[2], 4),
      cardInner: `linear-gradient(90deg, ${cardEdge} 0%, ${cardMid} 50%, ${cardEdge} 100%)`,
      cardSolid: cardFace,
      cardSoftSolid: darkenForWhiteText(stops[2], 4),

      /* 边框 = 主题最浅色 */
      cardBorder: lightest,
      cardBorderHover: lighten(lightest, 0.2),
      border: lighten(primary, 0.25),
      borderSoft: `rgba(${lightestRgb}, 0.35)`,
      fill: `rgba(${rgb}, 0.18)`,
      /* 表头 / 输入框 / 弹窗：也都是白字面，同样压到看得清 */
      tableHeader: darkenForWhiteText(mix(deepest, primary, 0.25), 4),
      input: darkenForWhiteText(mix(deepest, '#ffffff', 0.12), 4.2),
      dialog: cardFace,

      /* 文字：按卡片底色算 */
      textMain: cardText,
      textRegular: mix(cardText, deepest, 0.22),
      textSecondary: mix(cardText, deepest, 0.45),
      textPlaceholder: mix(cardText, deepest, 0.62),

      /* 标题 / 高亮：最浅色，深卡片上最清楚 */
      ink: lightest,

      /* 发光：用主题最浅色 */
      glow: `rgba(${lightestRgb}, 0.5)`,
      shadow: '0 4px 18px rgba(0, 0, 0, 0.35)',
      shadowHover: '0 8px 26px rgba(0, 0, 0, 0.45)'
    }
  }

  /* ---------- 红 / 绿 / 蓝（classic）保持原样：渐变底 + 半透明卡片 + 亮色文字 ---------- */
  return {
    bg,
    bgSoft: back.bgSoft,
    pageBg,
    topbar,
    topbarText: onSurfaceColor(mix(back.topbarTop, back.topbarBottom, 0.5)),
    bgSoftText: onSurfaceColor(back.bgSoft),
    lightSurface: false,
    /* 赛博：卡片全透明，文字的对比基准 = 渐变第 2 档（页面常驻底色） */
    cyberGlassBase: isCyberTheme(theme) ? back.pageStops[1] : null,

    /* 卡片：中性半透明（透出页面渐变）+ 一层主题色微光 */
    card: 'rgba(0, 0, 0, 0.45)',
    cardSoft: 'rgba(0, 0, 0, 0.32)',
    cardInner: `linear-gradient(135deg, rgba(${rgb}, 0.22) 0%, rgba(255, 255, 255, 0.04) 45%, rgba(${rgb}, 0.1) 100%)`,
    // 需要实色的地方（Element Plus 底色变量）用等效色：半透明黑叠在该主题的中间底色上
    cardSolid: mix(bg, '#000000', 0.45),
    cardSoftSolid: mix(bg, '#000000', 0.32),

    /* 边框：主题色提亮后的亮色 + 亮 → 暗 渐变描边 */
    cardBorder: lighten(primary, 0.4),
    cardBorderHover: lighten(primary, 0.6),
    border: lighten(primary, 0.28),
    borderSoft: `rgba(${rgb}, 0.3)`,
    fill: `rgba(${rgb}, 0.18)`,
    tableHeader: mix(bg, primary, 0.18),

    /* 输入框 / 弹窗：比卡片略亮一点（同样是暗色半透明） */
    input: mix(bg, '#000000', 0.25),
    dialog: mix(bg, '#000000', 0.35),

    /* 文字：白 / 亮色，往底色方向递减 */
    textMain: '#ffffff',
    textRegular: mix('#ffffff', bg, 0.2),
    textSecondary: mix('#ffffff', bg, 0.42),
    textPlaceholder: mix('#ffffff', bg, 0.58),

    /* 标题 / 高亮：主题色提亮后的版本，暗底 / 半透明卡片上都清楚 */
    ink: lighten(primary, 0.35)
  }
}

/**
 * 应用主题：只改 CSS 变量，全站样式（背景 / 卡片 / 按钮 / 表格 / 文字）跟着变，页面不刷新
 */
/**
 * 补全主题信息：每套主题必须有 primary / dark / light 三档色。
 * 自定义主题（图片取色）的 dark 就是 palette 里分配到的「深色」，
 * 页面底色全部由它推导，所以自定义主题的背景也跟着图片走。
 * 注意：这里不再有 bg 字段——底色是算出来的，不是写死的。
 */
function resolveTheme(key) {
  const theme = getTheme(key)
  return {
    ...theme,
    dark: theme.dark || darken(theme.primary, 0.3),
    light: theme.light || lighten(theme.primary, 0.4)
  }
}

export function applyTheme(key, override) {
  const raw = override ? { ...resolveTheme(key), ...override } : resolveTheme(key)
  const root = document.documentElement
  /*
   * 先判断这套主题是「深色」还是「浅色」：
   *   1. 内置主题看 lightMode 标记（极光圣殿白 / 霓虹幻梦粉是浅色，其余深色）；
   *   2. 自定义主题（图片取色）没有标记，按主色亮度判断：亮度 > 60% → 浅色主题。
   * 无论深浅，页面底都走「主题色的亮色 → 主题色 → 暗色」渐变（见 deriveBackground），
   * 只是主色很暗的自定义主题才保留深色那一套，绝不给浅色主题加黑背景。
   */
  const isCustom = Array.isArray(raw.palette) && raw.palette.length >= PALETTE_KEYS.length
  const isLight = !!raw.lightMode || (isCustom && luminanceOf(raw.primary) > 0.6)
  /*
   * 浅色主题：CSS 里真正当「主色」用的是点缀色 accent（按钮 / Tab / 标题 / 高亮），
   * raw.primary（如极光圣殿白的 #ECEFF1）只用于「更换皮肤」页面的卡片展示。
   * 深色主题没有 accent，主色就是 primary。
   */
  const cssPrimary = isLight && raw.accent ? raw.accent : raw.primary
  const theme = {
    ...raw,
    primary: cssPrimary,
    lightMode: isLight,
    dark: isLight && raw.accent ? darken(cssPrimary, 0.3) : raw.dark,
    light: isLight && raw.accent ? lighten(cssPrimary, 0.4) : raw.light
  }
  const rgb = hexToRgbChannels(cssPrimary)
  /*
   * 这套主题的「面」是亮还是暗：亮面（极光圣殿白 / 浅色主题）用深字，
   * 暗面（深色主题、红绿蓝）用白字；高亮色 / 卡片边框 / data-light-theme 都跟着它走。
   */
  const lightSurfaceTheme = themeKind(theme) === 'flat' || themeKind(theme) === 'light'
  const p = derivePalette(theme)
  /*
   * ---- 内置 11 套主题专属的「赛博 / 电子科技」质感 ----
   * 只有内置主题加（自定义主题 data-cyber="false"，保持用户自己的样子）。
   * 卡片完全透明（背景的网格 / 波纹直接透上来），文字的对比基准 = 页面波纹的亮档。
   */
  const isBuiltIn = !String(theme.key).startsWith(CUSTOM_PREFIX)
  /* 赛博质感开关：内置主题开，但极光圣殿白除外 —— 它是单独一套纯白浅灰，不参加 */
  const cyberOn = isBuiltIn && !theme.flatWhite
  const builtInLightest = cyberOn ? shadeScale(cssPrimary).lightest : null
  const glassBase = p.cyberGlassBase || null
  /*
   * ---- 全局对比度兜底 ----
   * 自定义主题（图片取色）的颜色是随机分配的，可能「撞车」：
   * 浅色字压在浅底上、边框和底色糊成一片。这里统一以「卡片实色」为基准
   * （赛博内置主题卡片全透明，按页面波纹亮档算），
   * 正文 / 次要文字 / 占位文字 / 标题色 / 边框色 全部强制拉到达标对比度。
   * 赛博深色主题：卡片全透明后文字直接坐在页面渐变上，青 / 绿这类高亮度色相
   * 4.5 根本够不着——阈值压到 4.5 以下反而会被「暗方向」反超，白字变成深字糊成一团。
   * 所以放宽阈值保住白字，可读性由 global.css 的全局 text-shadow 兜底。
   */
  const contrastBase = cyberOn ? glassBase : p.cardSolid
  /* 赛博深色主题的放宽阈值（浅色主题 / 自定义主题维持原标准） */
  const gm = cyberOn && !lightSurfaceTheme
  p.textMain = ensureTextContrast(p.textMain, contrastBase, gm ? 2.6 : 4.5)
  p.textRegular = ensureTextContrast(p.textRegular, contrastBase, gm ? 2.4 : 4.2)
  p.textSecondary = ensureTextContrast(p.textSecondary, contrastBase, gm ? 2.0 : 3.5)
  p.textPlaceholder = ensureTextContrast(p.textPlaceholder, contrastBase, gm ? 1.6 : 2.8)
  p.ink = ensureTextContrast(p.ink, contrastBase, gm ? 2.6 : 4.5)
  p.border = ensureBorderContrast(p.border, contrastBase)
  p.cardBorder = ensureBorderContrast(p.cardBorder, contrastBase)
  p.cardBorderHover = ensureBorderContrast(p.cardBorderHover, contrastBase)
  /* 顶栏 / Tab 栏的文字：按各自底色再保一次（赛博深色主题同样放宽，保白字） */
  p.topbarText = ensureTextContrast(p.topbarText, p.bg, gm ? 2.6 : 4.5)
  p.bgSoftText = ensureTextContrast(p.bgSoftText, p.bgSoft, gm ? 2.6 : 4.5)
  /*
   * 深面主题（渐变底 + 半透明卡片）：阴影走主色光晕、遮罩更深、词条悬停用主色填充，
   * 这些细节都跟着这个标志走，保证白字始终看得清。
   */
  const lightSurface = !!p.lightSurface

  root.setAttribute('data-theme', theme.key)
  root.setAttribute('data-light-theme', lightSurfaceTheme ? 'true' : 'false')
  /* 赛博 / 电子科技质感：内置主题开（极光圣殿白除外），global.css 里按 data-cyber 生效 */
  root.setAttribute('data-cyber', cyberOn ? 'true' : 'false')

  /* ---- 主题主色 ---- */
  root.style.setProperty('--cls-primary', theme.primary)
  root.style.setProperty('--cls-primary-dark', theme.dark)
  root.style.setProperty('--cls-primary-light', theme.light)
  root.style.setProperty('--cls-primary-rgb', rgb)
  root.style.setProperty('--cls-primary-ink', theme.ink || p.ink)

  /* ---- 各部位颜色 ----
   * 自定义主题（图片取色）带 palette：主色 / 深色 / 浅色 / 按钮 / 边框 /
   * Tab 选中 / 文字高亮 / 卡片边框 各用随机分配到的那个颜色；
   * 内置主题没有 palette，全部由主色派生。 */
  const palette = Array.isArray(theme.palette) && theme.palette.length >= PALETTE_KEYS.length
    ? theme.palette
    : null
  const roleColor = (index, fallback) => (palette ? palette[index] : fallback)
  const btnColor = roleColor(3, theme.primary)
  /* 边框：和卡片底色色差不够就自动压深 / 提亮，绝不和背景糊在一起 */
  const borderColor = ensureBorderContrast(roleColor(4, p.border), contrastBase)
  /*
   * 文字高亮（标题 / 链接）和卡片边框：自定义主题取到的颜色不一定合适——
   * 深色主题里偏暗的提亮，浅色主题里偏亮的压暗，保证各自底上都看得清。
   */
  const rawHighlight = roleColor(6, theme.ink || p.ink)
  const tabColor = roleColor(5, theme.primary)
  const highlightColor = ensureTextContrast(
    lightSurfaceTheme
      ? inkForLightSurface(luminanceOf(rawHighlight) > 0.7 ? theme.primary : rawHighlight)
      : inkForDarkSurface(rawHighlight),
    contrastBase,
    4.5
  )
  /* 卡片边框：色差不够就自动压深 / 提亮 */
  const rawCardBorder = roleColor(7, p.cardBorder)
  const cardBorderColor = ensureBorderContrast(
    lightSurfaceTheme
      ? // 真的太暗（几乎看不出颜色）才混白，否则保留主题色本身，保证边框是主题色
        luminanceOf(rawCardBorder) < 0.28
        ? mix(rawCardBorder, '#ffffff', 0.6)
        : rawCardBorder
      : inkForDarkSurface(rawCardBorder),
    contrastBase
  )

  root.style.setProperty('--cls-btn-bg', btnColor)
  root.style.setProperty('--cls-btn-border', btnColor)
  /* 按钮上的文字色跟着按钮底色走（深色按钮用白字，亮色按钮用深字），
   * 对比度不够就再往黑 / 往白拉一把 */
  const onBtnColor = ensureTextContrast(onPrimaryColor(btnColor), btnColor, 4.5)
  root.style.setProperty('--cls-on-primary', onBtnColor)
  root.style.setProperty('--cls-tab-active-bg', tabColor)
  root.style.setProperty('--cls-primary-ink', highlightColor)
  root.style.setProperty('--cls-card-border', cardBorderColor)
  root.style.setProperty(
    '--cls-card-border-hover',
    ensureBorderContrast(palette ? lighten(cardBorderColor, 0.5) : p.cardBorderHover, contrastBase)
  )
  /* 渐变描边：亮色 → 主色 → 暗色（主题色派生，配合 .cls-card 的 border-image）
     极光圣殿白走纯白 / 浅灰这一套，不要渐变描边 */
  root.style.setProperty(
    '--cls-card-border-gradient',
    theme.flatWhite ? 'none' : buildBorderGradient(palette ? palette[0] : theme.primary, cardBorderColor)
  )

  /* ---- 整站底色：全部由主题主色动态推导，没有任何写死的底色
   * （body/html/#app 用 --bg / --bg-page，向下也保留 --cls-* 兼容旧引用）---- */
  root.style.setProperty('--bg', p.bg)
  root.style.setProperty('--cls-bg', p.bg)
  root.style.setProperty('--bg-soft', p.bgSoft)
  root.style.setProperty('--cls-bg-soft', p.bgSoft)
  /* 页面底：主题色 → 中间色 → 暗色 的四层 135° 渐变（不是平色） */
  root.style.setProperty('--bg-page', p.pageBg)
  root.style.setProperty('--cls-page-bg', p.pageBg)
  root.style.setProperty('--topbar-bg', p.topbar)
  root.style.setProperty('--cls-topbar-bg', p.topbar)
  /* 顶栏 / Tab 栏底色也跟着变，文字色按各自底色的明暗自动取黑或白，保证可读 */
  root.style.setProperty('--cls-topbar-text', p.topbarText)
  root.style.setProperty('--cls-bg-soft-text', p.bgSoftText)
  root.style.setProperty('--cls-card', p.card)
  root.style.setProperty('--cls-card-soft', p.cardSoft)
  root.style.setProperty('--cls-card-inner', p.cardInner)
  root.style.setProperty('--cls-card-solid', p.cardSolid)
  root.style.setProperty('--cls-card-soft-solid', p.cardSoftSolid)
  // 边框：自定义主题用分配到的边框色，内置主题用主色半透明
  root.style.setProperty('--cls-border', borderColor)
  root.style.setProperty('--el-border-color', borderColor)
  root.style.setProperty('--cls-border-soft', p.borderSoft)
  root.style.setProperty('--cls-fill', p.fill)
  /* 输入框 / 弹窗：允许比卡片亮一点（但不是白色） */
  root.style.setProperty('--cls-input-bg', p.input)
  root.style.setProperty('--cls-dialog-bg', p.dialog)

  /* ---- 文字色（深色主题白色、浅色主题深色）---- */
  root.style.setProperty('--cls-text-main', p.textMain)
  root.style.setProperty('--cls-text-regular', p.textRegular)
  root.style.setProperty('--cls-text-secondary', p.textSecondary)
  root.style.setProperty('--cls-text-placeholder', p.textPlaceholder)

  /* ---- 发光：浅色主题用「主题最深色」发光，深色主题用「主题最浅色」发光
   * （红 / 绿 / 蓝 classic 保持原来的阴影，不动）---- */
  let shadowValue
  let shadowHoverValue
  let glowValue
  if (p.glow) {
    // 深色主题：用「主题最浅色」发光；浅色主题：用「主题最深色」发光
    shadowValue = `${p.shadow}, 0 0 12px ${p.glow}`
    shadowHoverValue = `${p.shadowHover}, 0 0 18px ${p.glow}`
    glowValue = p.glow
  } else if (lightSurface) {
    // 卡片「浮」在浅色渐变底上：轻微阴影，不做发光
    shadowValue = '0 4px 16px rgba(15, 23, 42, 0.1)'
    shadowHoverValue = '0 10px 28px rgba(15, 23, 42, 0.16)'
    glowValue = `rgba(${rgb}, 0.2)`
  } else {
    shadowValue = `0 4px 18px rgba(0, 0, 0, 0.35), 0 0 14px rgba(${rgb}, 0.1)`
    shadowHoverValue = `0 8px 26px rgba(${rgb}, 0.28), 0 0 18px rgba(${rgb}, 0.2)`
    glowValue = `rgba(${rgb}, 0.22)`
  }
  root.style.setProperty('--cls-shadow', shadowValue)
  root.style.setProperty('--cls-shadow-hover', shadowHoverValue)
  root.style.setProperty('--cls-card-glow', glowValue)
  /* 卡片边框 = 主色的浅色细线，悬停更亮（都过过色差保护） */
  root.style.setProperty('--cls-card-border', cardBorderColor)
  root.style.setProperty('--cls-card-border-hover', ensureBorderContrast(p.cardBorderHover, contrastBase))

  /* ---- Element Plus 主色 ---- */
  root.style.setProperty('--el-color-primary', theme.primary)
  root.style.setProperty('--el-color-primary-dark-2', theme.dark)
  root.style.setProperty('--el-color-primary-light-3', theme.primary)
  root.style.setProperty('--el-color-primary-light-5', theme.light)
  root.style.setProperty('--el-color-primary-light-7', theme.light)
  root.style.setProperty('--el-color-primary-light-8', `rgba(${rgb}, 0.28)`)
  root.style.setProperty('--el-color-primary-light-9', `rgba(${rgb}, 0.12)`)

  /* ---- Element Plus 深色底色（卡片用等效实色）---- */
  root.style.setProperty('--el-bg-color', p.cardSolid)
  root.style.setProperty('--el-bg-color-page', p.bg)
  root.style.setProperty('--el-bg-color-overlay', p.cardSolid)
  root.style.setProperty('--el-fill-color', p.cardSoftSolid)
  root.style.setProperty('--el-fill-color-light', p.borderSoft)
  root.style.setProperty('--el-fill-color-lighter', p.cardSoftSolid)
  /* 输入框填色：允许比卡片亮一点（不是纯白） */
  root.style.setProperty('--el-fill-color-blank', p.input)
  root.style.setProperty('--el-fill-color-darker', p.borderSoft)

  /* ---- Element Plus 文字 ---- */
  root.style.setProperty('--el-text-color-primary', p.textMain)
  root.style.setProperty('--el-text-color-regular', p.textRegular)
  root.style.setProperty('--el-text-color-secondary', p.textSecondary)
  root.style.setProperty('--el-text-color-placeholder', p.textPlaceholder)
  root.style.setProperty('--el-text-color-disabled', mix(p.textSecondary, p.cardSolid, 0.45))

  /* ---- Element Plus 边框 ---- */
  root.style.setProperty('--el-border-color', borderColor)
  root.style.setProperty('--el-border-color-light', p.borderSoft)
  root.style.setProperty('--el-border-color-lighter', p.fill)
  root.style.setProperty('--el-border-color-extra-light', p.cardSoftSolid)
  root.style.setProperty('--el-border-color-dark', cardBorderColor)
  root.style.setProperty('--el-border-color-darker', theme.dark)

  /* ---- 表格：深色底 + 主色调表头 + 白色文字 + 行悬停高亮 ---- */
  root.style.setProperty('--el-table-bg-color', p.cardSoftSolid)
  root.style.setProperty('--el-table-tr-bg-color', p.cardSoftSolid)
  root.style.setProperty('--el-table-header-bg-color', p.tableHeader)
  root.style.setProperty('--el-table-header-text-color', p.textMain)
  root.style.setProperty('--el-table-text-color', p.textRegular)
  root.style.setProperty('--el-table-border-color', p.border)
  root.style.setProperty('--el-table-row-hover-bg-color', `rgba(${rgb}, 0.14)`)
  root.style.setProperty('--el-table-current-row-bg-color', `rgba(${rgb}, 0.18)`)

  /* ---- 分页 / 弹窗 / 遮罩 ---- */
  root.style.setProperty('--el-pagination-button-bg-color', p.cardSoftSolid)
  root.style.setProperty('--el-pagination-text-color', p.textRegular)
  /* 弹窗背景比卡片亮一点，但不是白色 */
  root.style.setProperty('--el-dialog-bg-color', p.dialog)
  root.style.setProperty('--el-mask-color', lightSurface ? 'rgba(15, 23, 42, 0.45)' : 'rgba(0, 0, 0, 0.6)')
  root.style.setProperty('--el-box-shadow-light', 'var(--shadow)')

  /* ==========================================================
     统一变量层：页面里所有颜色都取自下面这些变量，CSS 里不写死任何颜色。
     切主题时这里全部重算，所以整站（背景 / 卡片 / 边框 / 按钮 / Tab / 文字 /
     输入框 / 表格 / 分页 / 标签 / 滚动条）都跟着变。
     ========================================================== */
  const set = (name, value) => root.style.setProperty(name, value)
  const cardBorderHover = palette ? lighten(cardBorderColor, 0.5) : p.cardBorderHover
  const softAlpha = lightSurface ? 0.08 : 0.14

  /* 背景：底色五件套 + 各面上的文字色，全部由主题深色 D 现场算出 */
  set('--bg', p.bg)
  set('--bg-soft', p.bgSoft)
  set('--bg-page', p.pageBg)
  set('--topbar-bg', p.topbar)
  set('--cls-bg', p.bg)
  set('--cls-bg-soft', p.bgSoft)
  set('--cls-page-bg', p.pageBg)
  set('--cls-topbar-bg', p.topbar)
  set('--cls-topbar-text', p.topbarText)
  set('--cls-bg-soft-text', p.bgSoftText)

  /* 卡片 */
  set('--card-bg', p.card)
  set('--card-bg-soft', p.cardSoft)
  set('--card-bg-solid', p.cardSolid)
  set('--card-inner', p.cardInner)
  set('--card-border', cardBorderColor)
  set('--card-border-hover', cardBorderHover)
  set('--card-glow', glowValue)

  /* 边框 / 分隔线 / 填充 */
  set('--border', borderColor)
  set('--border-soft', p.borderSoft)
  set('--border-strong', cardBorderColor)
  set('--fill', p.fill)

  /* 按钮 */
  set('--btn-bg', btnColor)
  set('--btn-border', btnColor)
  /* 按钮文字：和按钮底色的对比度不够就自动改黑 / 改白 */
  set('--btn-text', onBtnColor)
  set('--btn-hover-bg', lighten(btnColor, 0.14))
  /* 次要（朴素）按钮 */
  set('--btn-plain-bg', p.cardSoft)
  set('--btn-plain-text', p.textMain)
  set('--btn-plain-border', cardBorderColor)
  set('--btn-plain-hover-bg', p.fill)

  /* Tab：选中态「高亮底 + 高对比字」，未选中字要在 Tab 栏底色（--bg-soft）上看清；
   * 两边都过一遍对比度保护，自定义主题取到再奇怪的色也不会出现「同色底同色字」 */
  set('--tab-active-bg', tabColor)
  set('--tab-active-text', ensureTextContrast(onPrimaryColor(tabColor), tabColor, 4.5))
  set('--tab-inactive-bg', 'transparent')
  set(
    '--tab-inactive-text',
    ensureTextContrast(mix(p.bgSoft, p.bgSoftText, 0.7), p.bgSoft, 4.5)
  )

  /* 文字 */
  set('--text-main', p.textMain)
  set('--text-regular', p.textRegular)
  set('--text-sub', p.textSecondary)
  set('--text-placeholder', p.textPlaceholder)
  set('--text-highlight', highlightColor)
  set('--title-color', highlightColor)

  /* 输入框 / 弹窗：边框和聚焦色都过一遍色差保护，输入框永远看得出框 */
  set('--input-bg', p.input)
  set('--input-border', borderColor)
  set('--input-focus-border', ensureBorderContrast(theme.primary, p.input))
  set('--input-text', ensureTextContrast(p.textMain, p.input, 4.5))
  set('--input-placeholder', ensureTextContrast(p.textPlaceholder, p.input, 3))
  set('--dialog-bg', p.dialog)

  /* 表格：表头 / 正文 / 分页文字都按各自的底色再保一次对比度 */
  set('--table-bg', p.cardSoftSolid)
  set('--table-header-bg', p.tableHeader)
  set('--table-header-text', ensureTextContrast(p.textMain, p.tableHeader, 4.5))
  set('--table-text', ensureTextContrast(p.textRegular, p.cardSoftSolid, 4.5))
  set('--table-border', p.border)
  set('--table-row-hover', `rgba(${rgb}, ${softAlpha})`)
  set('--table-row-current', `rgba(${rgb}, ${lightSurface ? 0.12 : 0.18})`)

  /* 分页 / 标签 */
  set('--pagination-bg', p.cardSoftSolid)
  set('--pagination-text', ensureTextContrast(p.textRegular, p.cardSoftSolid, 4.5))
  set('--tag-bg', p.fill)
  set('--tag-text', ensureTextContrast(p.textRegular, contrastBase, gm ? 2.4 : 4.5))
  set('--tag-border', borderColor)

  /* ---- 词条 / 标签（热门搜索词、智能问答提示词这类可点击小标签）----
   * 常态：浅底 + 主题边框 + 正文色；
   * 悬停：只把底色压深一点点，文字用「最强对比色」（浅色主题深灰 / 深色主题纯白），
   * 保证任何主题下都不会出现「同色底 + 同色字」看不清的情况。 */
  set('--chip-bg', lightSurface ? p.cardSolid : p.cardSoft)
  set('--chip-text', ensureTextContrast(p.textRegular, lightSurface ? p.cardSolid : p.cardSoftSolid, 4.5))
  set('--chip-border', cardBorderColor)
  set('--chip-hover-bg', lightSurface ? mix('#ffffff', p.textMain, 0.08) : `rgba(${rgb}, 0.24)`)
  set('--chip-hover-text', p.textMain)
  set('--chip-hover-border', cardBorderHover)

  /* 滚动条 / 阴影 */
  set('--scrollbar-color', lightSurface ? 'rgba(15, 23, 42, 0.24)' : `rgba(${rgb}, 0.5)`)
  set('--scrollbar-color-hover', `rgba(${rgb}, 0.8)`)
  set('--scrollbar-track', 'transparent')
  set('--shadow', lightSurface ? '0 4px 16px rgba(15, 23, 42, 0.1)' : `0 4px 18px rgba(0, 0, 0, 0.35), 0 0 14px rgba(${rgb}, 0.1)`)
  /* 悬停阴影：深色带一点主色光晕（自定义主题保持原样），赛博主题在下面换成朝内的内发光 */
  set('--shadow-hover', lightSurface ? '0 10px 28px rgba(15, 23, 42, 0.16)' : `0 8px 26px rgba(${rgb}, 0.28), 0 0 18px rgba(${rgb}, 0.2)`)
  set('--shadow-dialog', lightSurface ? '0 12px 40px rgba(15, 23, 42, 0.16)' : '0 12px 40px rgba(0, 0, 0, 0.5)')

  /* ==========================================================
     内置 11 套主题专属：赛博朋克 / 电子科技质感
     自定义主题（图片取色）不加，保持用户自己的配色。
     ========================================================== */
  if (cyberOn) {
    const glowRgb = hexToRgbChannels(builtInLightest)
    const onLight = lightSurfaceTheme
    /* 悬停阴影：赛博主题的光一律朝内（inset），不往外发光 */
    set('--shadow-hover', `inset 0 0 28px rgba(${glowRgb}, 0.24)`)
    /* 卡片：完全透明（所有赛博主题，含浅色）—— 背景的网格 / 波纹直接透上来，
       可读性靠 global.css 的 text-shadow（深色主题）兜底 */
    set('--cyber-card-bg', 'transparent')
    set('--cyber-card-backdrop', 'none')
    /* 1px 细边框：卡片全透明后边框是唯一的「框」，必须够亮才看得见 ——
       绿色这类亮主色原来用 cardBorder（lighten 主色 0.4），和页面同色系会隐身，
       所以统一往白里提一档，保证任何主题下框都清晰 */
    set('--cyber-border', mix(builtInLightest, '#ffffff', 0.3))
    set('--cyber-border-hover', mix(builtInLightest, '#ffffff', 0.55))
    set('--cyber-glow', builtInLightest)
    set('--cyber-glow-rgb', glowRgb)
    set('--cyber-inner-glow', `rgba(${glowRgb}, ${onLight ? 0.1 : 0.14})`)
    set('--cyber-edge-glow', `rgba(${glowRgb}, ${onLight ? 0.4 : 0.55})`)
    /* 顶部高光：玻璃反光的一条 1px 渐变亮线（浅色主题上白线看不见，改用主题色） */
    set(
      '--cyber-top-line',
      onLight
        ? `linear-gradient(90deg, transparent 0%, rgba(${rgb}, 0.55) 50%, transparent 100%)`
        : 'linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.6) 50%, transparent 100%)'
    )

    /* 背景装饰：极淡的点阵网格 + 两小团径向光晕（之前 900px 太胖，瘦一半、更聚焦） */
    const gridLine = onLight ? `rgba(${rgb}, 0.1)` : `rgba(${glowRgb}, 0.08)`
    const grid =
      `repeating-linear-gradient(0deg, ${gridLine} 0px, ${gridLine} 1px, transparent 1px, transparent 44px), ` +
      `repeating-linear-gradient(90deg, ${gridLine} 0px, ${gridLine} 1px, transparent 1px, transparent 44px)`
    const halo =
      `radial-gradient(520px 300px at 14% 6%, rgba(${glowRgb}, ${onLight ? 0.22 : 0.12}) 0%, transparent 62%), ` +
      `radial-gradient(440px 260px at 88% 92%, rgba(${rgb}, ${onLight ? 0.16 : 0.1}) 0%, transparent 65%)`
    const pageWithDecor = `${halo}, ${grid}, ${p.pageBg}`
    set('--bg-page', pageWithDecor)
    set('--cls-page-bg', pageWithDecor)

    /* Tab 栏：主题深色版底；选中项 = 淡主题色底 + 主题亮字 + 底部 2px 发光亮线
       （不要大色块，选中感主要靠亮字和那条发光线）
       注意：选中字的对比基准必须是「Tab 栏底色 + 淡主题底」的实际叠加色 */
    const tabbarBg = onLight ? mix(cssPrimary, '#ffffff', 0.3) : mix(p.bg, '#000000', 0.32)
    set('--cyber-tabbar-bg', tabbarBg)
    set('--cyber-tabbar-border', cardBorderColor)
    const tabActiveBg = mix(tabbarBg, cssPrimary, onLight ? 0.1 : 0.14)
    set('--cyber-tab-active-bg', `rgba(${rgb}, ${onLight ? 0.1 : 0.14})`)
    /* 阈值 3.5：再高的话个别主题（深空回响青）亮字够不着，会被反向压成深字，
       就不是「亮字 + 发光线」的选中感了 */
    set('--cyber-tab-active-text', ensureTextContrast(builtInLightest, tabActiveBg, 3.5))
    set('--cyber-tab-line', onLight ? cssPrimary : builtInLightest)
    set(
      '--cyber-tab-inactive-text',
      onLight ? 'rgba(51, 51, 51, 0.72)' : 'rgba(255, 255, 255, 0.72)'
    )
    set('--cyber-tab-hover-text', p.textMain)
    set('--cyber-tab-hover-bg', `rgba(${rgb}, ${onLight ? 0.12 : 0.2})`)
  }

  /* 旧变量（--cls-*）同步成同一套值，保证老代码也跟着主题走 */
  set('--cls-primary', cssPrimary)
  set('--cls-primary-dark', theme.dark)
  set('--cls-primary-light', theme.light)
  set('--cls-primary-rgb', rgb)
  set('--cls-primary-ink', highlightColor)
  set('--cls-on-primary', onBtnColor)
  set('--cls-btn-bg', btnColor)
  set('--cls-btn-border', btnColor)
  set('--cls-tab-active-bg', tabColor)
  set('--cls-card-border', cardBorderColor)
  set('--cls-card-border-hover', cardBorderHover)
  set('--cls-border', borderColor)
  set('--cls-input-bg', p.input)
  set('--cls-dialog-bg', p.dialog)
  set('--cls-text-main', p.textMain)
  set('--cls-text-regular', p.textRegular)
  set('--cls-text-secondary', p.textSecondary)
  set('--cls-text-placeholder', p.textPlaceholder)
  set('--cls-shadow', shadowValue)
  set('--cls-shadow-hover', shadowHoverValue)
  set('--cls-card-glow', glowValue)

  return theme.key
}

/**
 * 应用「初始色」：登录 / 注册 / 忘记密码页专用。
 * 这三个页面用户还没登录，不知道他的主题，固定用浅色初始色（AUTH_THEME）：
 * 浅灰白底 #F5F7FA + 白色卡片 + 浅灰边框 + 深色文字 + 量子脉冲蓝 #00D4FF 点缀。
 * 只改 CSS 变量，不动 store 里的主题状态，登录后用户端会重新按数据库应用他自己的主题。
 */
export function applyGuestTheme() {
  return applyTheme(AUTH_THEME.key, { ...AUTH_THEME })
}
