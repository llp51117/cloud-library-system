<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <div class="brand-icon">
          <el-icon :size="34"><Reading /></el-icon>
        </div>
        <h1>云图书管理系统</h1>
        <p class="sub">AI 智慧图书服务平台</p>
        <ul class="feature-list">
          <li><el-icon><Check /></el-icon> 语义搜索，用一句话找到想读的书</li>
          <li><el-icon><Check /></el-icon> 智能问答，图书推荐与借阅规则秒答</li>
          <li><el-icon><Check /></el-icon> 预约排队，热门图书归还自动通知</li>
          <li><el-icon><Check /></el-icon> 数据概览，借阅趋势一目了然</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-panel">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请使用注册邮箱登录系统</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱地址" clearable>
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <el-link type="primary" :underline="false" @click="router.push('/forgot')">忘记密码？</el-link>
          <span class="divider">|</span>
          <el-link type="primary" :underline="false" @click="router.push('/register')">立即注册</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { applyGuestTheme } from '@/utils/themes'

// 登录页永远用初始色（量子脉冲蓝）：用户还没登录，不知道他的主题
onMounted(applyGuestTheme)

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  password: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const info = await userStore.login(form)
    ElMessage.success('登录成功，欢迎回来！')
    // 待激活管理员仍在用户端（只能还书），只有正式管理员才进管理端
    const toAdmin = info.role === 1 && !info.pendingAdmin
    router.replace(toAdmin ? '/admin/dashboard' : '/user/home')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录页：固定浅色（浅灰白底 + 白色卡片 + 深色文字 + 主色点缀），简洁干净 */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f5f7fa 60%, #eef1f5 100%);
}

.auth-card {
  display: flex;
  width: 900px;
  max-width: 100%;
  min-height: 520px;
  background: #ffffff;
  border: 1px solid #e4e7eb;
  border-radius: 0;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(31, 45, 61, 0.08);
}

/* 左侧品牌区：浅灰白，不花哨 */
.brand-panel {
  width: 380px;
  padding: 44px 34px;
  color: #333333;
  background: linear-gradient(160deg, #ffffff 0%, #f5f7fa 100%);
  border-right: 1px solid #e4e7eb;
  display: flex;
  flex-direction: column;
}

.brand-icon {
  width: 62px;
  height: 62px;
  border-radius: 0;
  background: rgba(0, 212, 255, 0.12);
  color: #00d4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
}

.brand-panel h1 {
  font-size: 23px;
  margin: 0 0 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #333333;
}

.brand-panel .sub {
  margin: 0 0 30px;
  font-size: 13px;
  color: #6b7280;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13.5px;
  line-height: 1.5;
  color: #4b5563;
}

.feature-list li :deep(.el-icon) {
  margin-top: 2px;
  flex-shrink: 0;
  color: #00d4ff;
}

/* 右侧表单区 */
.form-panel {
  flex: 1;
  padding: 52px 46px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 28px;
}

.form-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 600;
  color: #00d4ff;
}

.form-header p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

/* 输入框：白色底 + 浅灰边框 */
.form-panel :deep(.el-input__wrapper) {
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.form-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.form-panel :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00d4ff inset;
}

.form-panel :deep(.el-input__inner) {
  color: #333333;
}

/* 按钮：主色填充 + 白色文字 */
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  border-radius: 0;
  background: #00d4ff;
  border-color: #00d4ff;
  color: #ffffff;
}

.submit-btn:hover,
.submit-btn:focus {
  background: #22dcff;
  border-color: #22dcff;
  color: #ffffff;
}

/* 底部链接：主色 */
.form-footer {
  text-align: center;
  font-size: 13px;
  margin-top: 4px;
}

.form-footer :deep(.el-link) {
  color: #00d4ff;
}

.form-footer .divider {
  margin: 0 10px;
  color: #dcdfe6;
}

@media (max-width: 820px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 40px 28px;
  }
}
</style>
