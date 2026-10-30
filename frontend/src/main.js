import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from '@/store/user'
import './styles/global.css'

const app = createApp(App)

// 注册全部 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())

// 启动即应用主题（先用本地缓存的用户信息，进页面后由布局再拉一次数据库）
useUserStore().initTheme()

app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
