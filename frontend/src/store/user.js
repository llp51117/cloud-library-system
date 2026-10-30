import { defineStore } from 'pinia'
import {
  login as loginApi,
  getProfile,
  getTheme as getThemeApi,
  updateTheme as updateThemeApi,
  getMyThemes,
  saveMyTheme as saveMyThemeApi,
  deleteMyTheme as deleteMyThemeApi
} from '@/api'
import {
  applyTheme,
  DEFAULT_THEME,
  normalizeTheme,
  setCustomThemes,
  getCustomThemes,
  customKey,
  MAGIC_BG_KEY,
  parseBgPalette
} from '@/utils/themes'
import {
  uploadUserBg as uploadUserBgApi,
  updateBgPalette as updateBgPaletteApi,
  clearUserBg as clearUserBgApi
} from '@/api'

/**
 * 用户状态管理：令牌、用户信息、登录/退出
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('cls_token') || '',
    userInfo: safeParse(localStorage.getItem('cls_user')),
    /** 当前主题标识：默认「极光圣殿白」，登录后从数据库读取 */
    theme: DEFAULT_THEME,
    /** 当前用户的自定义主题（图片取色保存的），只属于本账号 */
    customThemes: [],
    /**
     * 「光影幻境」主题的背景图 URL（为空 = 默认的白底星空）。
     * 初始值直接取本地缓存的个人信息：刷新页面时接口还没回来，
     * 背景图和配色也要在，不然会先闪一下「没上传图」的状态。
     */
    bgUrl: safeParse(localStorage.getItem('cls_user'))?.bgUrl || '',
    /** 「光影幻境」的配色（背景图取色得到）：{ primary, dark, light } */
    bgPalette: parseBgPalette(safeParse(localStorage.getItem('cls_user'))?.bgPalette),
    /**
     * 背景图是不是「深色图」：真正下载图片采样算出来的平均亮度（< 0.42 算深色）。
     * 深色图 → 白色文字；浅色 / 白色图 → 深色文字（相反色，保证突出）。
     * 没上传图时固定 false（默认白底星空 → 深色文字）。
     */
    bgDark: false
  }),

  getters: {
    isLogin: (state) => !!state.token,
    /** 正式管理员才能进管理端：role=1 且不是「待激活」状态 */
    isAdmin: (state) => state.userInfo?.role === 1 && !state.userInfo?.pendingAdmin,
    /** 最高管理员（超管）：齿轮 + 魂环头像，能设置/取消管理员、审批操作申请 */
    isSuperAdmin: (state) => state.userInfo?.isSuperAdmin === 1,
    /** 待激活管理员：role=1 且 pendingAdmin=1，停留在用户端，只能还书不能借书 */
    isPendingAdmin: (state) => state.userInfo?.role === 1 && state.userInfo?.pendingAdmin === 1,
    /** 能否借阅图书：管理员（含待激活）都不能借书 */
    canBorrow: (state) => state.userInfo?.role !== 1,
    email: (state) => state.userInfo?.email || '',
    /** 展示名：优先昵称，没昵称才用邮箱 @ 前缀（绝不直接把邮箱当昵称展示） */
    nickname: (state) => {
      const info = state.userInfo || {}
      if (info.nickname) return info.nickname
      const email = info.email || ''
      return email ? String(email).split('@')[0] : ''
    },
    avatar: (state) => state.userInfo?.avatar || '',
    /** 当前主题（已规范化，非法值自动回退默认主题） */
    themeKey: (state) => normalizeTheme(state.theme)
  },

  actions: {
    /** 登录 */
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.userInfo = res.data.userInfo
      localStorage.setItem('cls_token', this.token)
      localStorage.setItem('cls_user', JSON.stringify(this.userInfo))
      return this.userInfo
    },

    /** 拉取最新用户信息 */
    async fetchProfile() {
      const res = await getProfile()
      this.setUserInfo(res.data)
      return this.userInfo
    },

    setUserInfo(info) {
      this.userInfo = info
      localStorage.setItem('cls_user', JSON.stringify(info))
      // 「光影幻境」的背景图 / 配色跟着账号走（登录、刷新个人信息都会带上）
      if (info?.bgUrl !== undefined) {
        this.bgUrl = info.bgUrl || ''
        // 登录 / 刷新个人信息拿到背景图后，采样一次亮度决定文字用深色还是白色
        this.measureBgBrightness()
      }
      if (info?.bgPalette !== undefined) {
        this.bgPalette = parseBgPalette(info.bgPalette)
      }
      // 个人信息里带着主题，顺手同步，保证刷新后就地应用
      if (info?.theme) {
        this.setThemeLocal(info.theme)
      }
    },

    /* ==================== 主题（皮肤） ==================== */

    /**
     * 本地应用主题：只改 CSS 变量，页面不刷新；
     * 主题本身存在数据库里，这里只是把值刷到样式上。
     */
    setThemeLocal(key) {
      const themeKey = normalizeTheme(key)
      /*
       * 「光影幻境」没有固定的主 / 深 / 浅：
       * 颜色来自用户上传的背景图（取色结果存在 user.bg_palette），
       * 这里以 override 形式传给主题引擎，卡片 / 文字 / 按钮按它配色；
       * 还没上传图（默认白底星空）时传 magicLight，告诉引擎「背景是亮的 → 文字用深色」。
       */
      let override = null
      if (themeKey === MAGIC_BG_KEY) {
        if (!this.bgUrl) {
          // 没上传背景图：默认白底星空 → 强制深色文字
          override = { magicLight: true }
        } else if (this.bgDark) {
          override = { ...(this.bgPalette || {}), magicDark: true }
        } else {
          override = { ...(this.bgPalette || {}), magicLight: true }
        }
      }
      this.theme = applyTheme(themeKey, override)
      return this.theme
    },

    /** 应用默认主题（未登录 / 接口还没返回时用） */
    initTheme() {
      this.setThemeLocal(this.userInfo?.theme || DEFAULT_THEME)
    },

    /**
     * 从数据库读取当前用户主题并应用（GET /api/user/theme）
     * 先拉自定义主题再应用：当前主题可能就是某套自定义主题（custom-{id}）
     */
    async loadTheme() {
      try {
        await this.loadCustomThemes()
        const res = await getThemeApi()
        this.setThemeLocal(res.data)
        return this.theme
      } catch (e) {
        this.setThemeLocal(DEFAULT_THEME)
        return this.theme
      }
    },

    /** 切换主题：写入数据库（PUT /api/user/theme）+ 立即应用到页面 */
    async saveTheme(key) {
      const res = await updateThemeApi(normalizeTheme(key))
      this.setThemeLocal(res.data)
      if (this.userInfo) {
        this.userInfo.theme = this.theme
        localStorage.setItem('cls_user', JSON.stringify(this.userInfo))
      }
      return this.theme
    },

    /* ==================== 自定义主题（图片取色） ==================== */

    /** 拉取当前用户的自定义主题并注册到主题引擎（无限制条数，按用户隔离） */
    async loadCustomThemes() {
      try {
        const res = await getMyThemes()
        this.customThemes = setCustomThemes(res.data || [])
        // 注册后重新应用一次：当前正在用的可能是刚注册进来的自定义主题
        this.setThemeLocal(this.theme)
        return this.customThemes
      } catch (e) {
        this.customThemes = setCustomThemes([])
        return this.customThemes
      }
    },

    /** 保存一套自定义颜色（{ name, primaryColor, darkColor, lightColor }） */
    async addCustomTheme(data) {
      const res = await saveMyThemeApi(data)
      await this.loadCustomThemes()
      return res.data
    },

    /** 删除一套自定义颜色；删的正好是正在用的主题时，主题回到默认 */
    async removeCustomTheme(id) {
      await deleteMyThemeApi(id)
      await this.loadCustomThemes()
      if (this.theme === customKey(id)) {
        this.setThemeLocal(DEFAULT_THEME)
        if (this.userInfo) {
          this.userInfo.theme = DEFAULT_THEME
          localStorage.setItem('cls_user', JSON.stringify(this.userInfo))
        }
      }
      return getCustomThemes()
    },

    /* ==================== 「光影幻境」（magic-bg）背景图 ==================== */

    /**
     * 上传背景图：服务器存图 + 自动取色，返回 { url, primaryColor, darkColor, lightColor, colors }
     * 只上传不应用，等用户在弹窗里确认（或重新分析）配色后再 applyMagicBg()。
     */
    async uploadMagicBg(file) {
      const res = await uploadUserBgApi(file)
      const data = res.data || {}
      this.bgUrl = data.url || ''
      this.bgPalette = {
        primary: data.primaryColor,
        dark: data.darkColor,
        light: data.lightColor
      }
      // 真正下载这张图采样亮度：深色图 → 白字，浅色 / 白色图 → 深字
      await this.measureBgBrightness()
      return data
    },

    /**
     * 采样背景图的真实平均亮度，判断它是「深色图」还是「浅色图」。
     * 文字取相反色：深色图 → 白色文字；白色 / 浅色图 → 深色文字，保证突出。
     * 采样失败（跨域等）时回退到「取色结果的平均亮度」兜底。
     */
    async measureBgBrightness() {
      if (!this.bgUrl) {
        this.bgDark = false
        return false
      }
      try {
        const img = new Image()
        // 跨域的图（后端返回完整 http 地址时）必须带 crossOrigin，否则画到 canvas 上会被污染、取不到像素
        if (/^https?:\/\//i.test(this.bgUrl) && !this.bgUrl.startsWith(window.location.origin)) {
          img.crossOrigin = 'anonymous'
        }
        img.src = this.bgUrl
        await new Promise((resolve, reject) => {
          img.onload = resolve
          img.onerror = () => reject(new Error('背景图加载失败'))
          // 卡住的话最多等 6 秒，不能把主题永远晾在错误的文字色上
          setTimeout(() => reject(new Error('背景图加载超时')), 6000)
        })
        const size = 40
        const canvas = document.createElement('canvas')
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, size, size)
        const data = ctx.getImageData(0, 0, size, size).data
        const linear = (c) => {
          const s = c / 255
          return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
        }
        let sum = 0
        for (let i = 0; i < data.length; i += 4) {
          sum += 0.2126 * linear(data[i]) + 0.7152 * linear(data[i + 1]) + 0.0722 * linear(data[i + 2])
        }
        const avg = sum / (size * size)
        // 略偏向「判成深色」：白字加黑描边在任何图上都能看清，深色字压深色图会糊
        this.bgDark = avg < 0.45
      } catch (e) {
        /* 采样不了（跨域 / 超时）就按取色结果兜底，绝不默认浅色 */
        console.warn('[光影幻境] 背景图亮度采样失败，改用取色结果判断：', e && e.message)
        this.bgDark = this.paletteLooksDark()
      }
      // 已经在用「光影幻境」的话，立刻按新的明暗重新上色
      if (this.themeKey === MAGIC_BG_KEY) {
        this.setThemeLocal(MAGIC_BG_KEY)
      }
      return this.bgDark
    },

    /**
     * 采样失败时的兜底：用后端取出来的那三个颜色判断图是深是浅。
     * 同样偏向「判成深色」——白字 + 黑描边永远比深色字压在深色图上安全。
     */
    paletteLooksDark() {
      const p = this.bgPalette
      if (!p) return false
      const list = [p.primary, p.dark, p.light].filter((c) => typeof c === 'string' && /^#[0-9a-fA-F]{6}$/.test(c))
      if (!list.length) return false
      const linear = (c) => {
        const s = c / 255
        return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
      }
      const lums = list.map((hexColor) => {
        const n = parseInt(hexColor.slice(1), 16)
        return (
          0.2126 * linear((n >> 16) & 255) +
          0.7152 * linear((n >> 8) & 255) +
          0.0722 * linear(n & 255)
        )
      })
      const avg = lums.reduce((a, b) => a + b, 0) / lums.length
      return avg < 0.5 || Math.min(...lums) < 0.12
    },

    /** 保存用户确认的配色（{ primary, dark, light }）并立即应用到页面 */
    async saveBgPalette(colors) {
      this.bgPalette = {
        primary: colors.primary,
        dark: colors.dark || colors.primary,
        light: colors.light || colors.primary
      }
      await updateBgPaletteApi({
        primaryColor: this.bgPalette.primary,
        darkColor: this.bgPalette.dark,
        lightColor: this.bgPalette.light
      })
      this.setThemeLocal(MAGIC_BG_KEY)
      if (this.userInfo) {
        this.userInfo.bgPalette = `${this.bgPalette.primary},${this.bgPalette.dark},${this.bgPalette.light}`
        localStorage.setItem('cls_user', JSON.stringify(this.userInfo))
      }
      return this.bgPalette
    },

    /** 应用「光影幻境」：主题写库（user.theme = magic-bg）+ 本地按背景图取到的配色渲染 */
    async applyMagicBg() {
      // 先确认这张图是深是浅，再上色，避免先闪一下「深色字压深色图」
      if (this.bgUrl) {
        await this.measureBgBrightness()
      }
      await this.saveTheme(MAGIC_BG_KEY)
      return this.theme
    },

    /** 删除背景图：回落到默认的「白底 + 黑色星星」，主题仍是光影幻境 */
    async clearMagicBg() {
      await clearUserBgApi()
      this.bgUrl = ''
      this.bgPalette = null
      this.bgDark = false
      if (this.userInfo) {
        this.userInfo.bgUrl = ''
        this.userInfo.bgPalette = ''
        localStorage.setItem('cls_user', JSON.stringify(this.userInfo))
      }
      this.setThemeLocal(MAGIC_BG_KEY)
    },

    /** 清除登录状态 */
    clear() {
      this.token = ''
      this.userInfo = null
      // 退出登录：清掉上一个用户的自定义主题和背景图，避免下一个人看到
      this.customThemes = setCustomThemes([])
      this.bgUrl = ''
      this.bgPalette = null
      this.bgDark = false
      localStorage.removeItem('cls_token')
      localStorage.removeItem('cls_user')
      // 退出登录时清空本浏览器的智能问答对话缓存，避免下一个用户看到上一个人的聊天记录
      clearAiChatCache()
    }
  }
})

/**
 * 清空智能问答的本地对话缓存（AiChat.vue 按用户ID 分 key 存储，这里按前缀全清）。
 * 退出登录时调用，保证下一个人登录看到的是全新的对话。
 */
function clearAiChatCache() {
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith('cloud-library-ai-chat'))
      .forEach((key) => localStorage.removeItem(key))
  } catch (e) {
    /* 忽略隐私模式等场景下的清理失败 */
  }
}

function safeParse(text) {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch (e) {
    return null
  }
}
