<template>
  <div class="ai-chat">
    <div class="cls-card chat-card">
      <div class="cls-section-title">
        <div class="title-left">
          <span class="bar"></span>
          <span>智能图书助手</span>
          <el-tag v-if="!lastAiGenerated && hasChat" size="small" type="info" effect="plain">
            本地知识库模式
          </el-tag>
          <el-tag v-else-if="hasChat" size="small" type="success" effect="plain">AI 大模型模式</el-tag>
        </div>
        <el-button text @click="clearChat">
          <el-icon><Delete /></el-icon>
          <span>清空对话</span>
        </el-button>
      </div>

      <!-- 消息列表 -->
      <div ref="listRef" class="message-list">
        <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
          <div class="avatar">
            <el-icon v-if="msg.role === 'ai'"><MagicStick /></el-icon>
            <el-icon v-else><UserFilled /></el-icon>
          </div>
          <div class="bubble-wrap">
            <div class="bubble">
              <pre class="content">{{ msg.content }}</pre>
              <div v-if="msg.references && msg.references.length" class="refs">
                <div class="refs-title">参考来源：</div>
                <ul>
                  <li v-for="(ref, i) in msg.references" :key="i">{{ ref }}</li>
                </ul>
              </div>
            </div>
            <div class="time">{{ msg.time }}</div>
          </div>
        </div>

        <div v-if="loading" class="message ai">
          <div class="avatar"><el-icon><MagicStick /></el-icon></div>
          <div class="bubble-wrap">
            <div class="bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷提问 -->
      <div class="quick-row">
        <span class="label">你可以这样问：</span>
        <el-tag
          v-for="q in quickQuestions"
          :key="q"
          class="quick-item"
          effect="plain"
          round
          @click="send(q)"
        >
          {{ q }}
        </el-tag>
      </div>

      <!-- 输入区 -->
      <div class="input-row">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="200"
          show-word-limit
          placeholder="请输入你的问题（Enter 发送，Ctrl + Enter 换行），例如：借阅规则是什么？"
          @keydown.enter.exact.prevent="send()"
          @keydown.ctrl.enter.prevent="insertNewline"
        />
        <el-button type="primary" :loading="loading" class="send-btn" @click="send()">
          <el-icon><Promotion /></el-icon>
          <span>发送</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { aiChat, aiClearHistory } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

/**
 * 对话历史保存在 localStorage，但按「用户ID」隔离：
 * 每个用户一个 key（cloud-library-ai-chat-{userId}），换账号登录只会看到自己的记录；
 * 退出登录时 store.clear() 会把本浏览器里所有该前缀的缓存清掉。
 */
const STORAGE_PREFIX = 'cloud-library-ai-chat'
const GREETING =
  '你好！我是云图书管理系统的智能助手 📚\n可以帮你查找图书、推荐读物、解答借阅与预约规则。请问有什么可以帮到你？'

const storageKey = computed(() => `${STORAGE_PREFIX}-${userStore.userInfo?.id ?? 'guest'}`)

const messages = ref([])
const input = ref('')
const loading = ref(false)
const listRef = ref()
const hasChat = ref(false)
const lastAiGenerated = ref(true)

const quickQuestions = [
  '借阅规则是什么？',
  '有哪些好看的科幻小说？',
  '怎么预约已经被借出的书？',
  '刘慈欣的作品推荐'
]

onMounted(() => {
  const saved = loadHistory()
  if (saved.length) {
    // 有历史记录就恢复，不再重复播报开场白
    messages.value = saved
    hasChat.value = true
    scrollToBottom()
  } else {
    pushMessage('ai', GREETING)
  }
})

/**
 * 换账号（用户ID 变化）时重新加载该用户的历史：
 * A 退出、B 登录后看到的是 B 自己的对话，不会残留 A 的内容。
 */
watch(storageKey, () => {
  const saved = loadHistory()
  messages.value = saved
  hasChat.value = saved.length > 0
  if (!saved.length) {
    pushMessage('ai', GREETING)
  }
  scrollToBottom()
})

/** 读取「当前用户」的本地对话历史，数据损坏时按新会话处理 */
function loadHistory() {
  try {
    const raw = localStorage.getItem(storageKey.value)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

/** 对话内容一变化就写回当前用户的本地缓存 */
watch(messages, () => {
  try {
    localStorage.setItem(storageKey.value, JSON.stringify(messages.value))
  } catch (e) {
    /* 忽略本地存储写入失败（如隐私模式） */
  }
}, { deep: true })

/** Ctrl + Enter 换行（Enter 单独按是发送） */
function insertNewline() {
  input.value += '\n'
}

function pushMessage(role, content, references = []) {
  messages.value.push({
    role,
    content,
    references,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  scrollToBottom()
}

async function send(text) {
  const question = (text || input.value).trim()
  if (loading.value) return

  // 空消息不再拦截：交给服务端用轻松的语气回应，不弹客服式提示
  if (question) {
    pushMessage('user', question)
  }
  input.value = ''
  loading.value = true
  hasChat.value = true

  try {
    const res = await aiChat(question)
    // 后端 Result.success(chatVO) → ChatVO 字段名：answer / references / aiGenerated
    const data = res.data || {}
    const answer = (data.answer || '').trim()
    lastAiGenerated.value = data.aiGenerated !== false
    // 后端返回了文字就原样渲染；只有真的没有文字时才提示，不再显示写死的兜底话术
    if (answer) {
      pushMessage('ai', answer, data.references || [])
    } else {
      pushMessage('ai', '抱歉，智能助手这次没有返回内容，请再试一次。')
    }
  } catch (e) {
    // 请求出错时优先展示后端返回的 message，便于定位（如「请先登录」）
    const msg = e?.response?.data?.message || '抱歉，服务暂时不可用，请稍后再试。'
    pushMessage('ai', msg)
  } finally {
    loading.value = false
  }
}

async function clearChat() {
  messages.value = []
  hasChat.value = false
  try {
    localStorage.removeItem(storageKey.value)
  } catch (e) {
    /* 忽略本地存储清理失败 */
  }
  // 同时清掉后端保存的多轮上下文，避免清屏后 AI 还记着之前聊过什么
  try {
    await aiClearHistory()
  } catch (e) {
    /* 后端上下文清理失败不影响前端清屏 */
  }
  pushMessage('ai', '对话已清空，我们可以重新开始。请问你想了解哪方面的内容？')
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 200px);
  min-height: 520px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--btn-text);
}

.message.ai .avatar {
  background: linear-gradient(135deg, var(--cls-primary-dark), var(--cls-primary));
}

.message.user .avatar {
  background: linear-gradient(135deg, var(--cls-success), var(--cls-primary-light));
}

.bubble-wrap {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}

.message.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 12px 15px;
  border-radius: 0;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.message.ai .bubble {
  background: var(--cls-card-soft);
  color: var(--cls-text-main);
  border: 1px solid var(--cls-border);
  border-top-left-radius: 3px;
}

.message.user .bubble {
  background: var(--cls-primary);
  /* 浅色主题（如极光圣殿白）下自动用深色文字，保证可读 */
  color: var(--cls-on-primary, #fff);
  border-top-right-radius: 3px;
}

.content {
  margin: 0;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
}

.refs {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  font-size: 12.5px;
  color: var(--cls-text-regular);
}

.refs-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.refs ul {
  margin: 0;
  padding-left: 18px;
}

.time {
  font-size: 11.5px;
  color: var(--cls-text-secondary);
  margin-top: 4px;
  padding: 0 4px;
}

/* 打字动画 */
.bubble.typing {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 15px;
}

.bubble.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--cls-primary-light);
  animation: blink 1.2s infinite ease-in-out;
}

.bubble.typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.bubble.typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.quick-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-top: 1px solid var(--cls-border);
}

.quick-row .label {
  font-size: 12.5px;
  color: var(--cls-text-secondary);
}

/*
 * 提示词条：常态 = 浅底 + 主题边框 + 正文色；
 * 悬停 = 底色只压深一点点 + 文字用最强对比色（--chip-hover-text），
 * 任何主题下都不会出现「同色底 + 同色字」看不清的情况。
 */
.quick-item {
  cursor: pointer;
  background: var(--chip-bg);
  border-color: var(--chip-border);
  color: var(--chip-text);
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.quick-item:hover {
  background: var(--chip-hover-bg);
  border-color: var(--chip-hover-border);
  color: var(--chip-hover-text);
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.send-btn {
  width: 100px;
  height: 40px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .bubble-wrap {
    max-width: 85%;
  }
}
</style>
