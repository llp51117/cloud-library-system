<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand-panel">
        <div class="brand-icon">
          <el-icon :size="34"><Reading /></el-icon>
        </div>
        <h1>加入云图书</h1>
        <p class="sub">注册后即可借阅、收藏与预约图书</p>
        <ul class="feature-list">
          <li><el-icon><Check /></el-icon> 免费注册，随时借阅馆藏图书</li>
          <li><el-icon><Check /></el-icon> 收藏心仪图书，热度排行实时更新</li>
          <li><el-icon><Check /></el-icon> 借阅到期自动提醒，支持在线续借</li>
          <li><el-icon><Check /></el-icon> AI 助手为你推荐更合适的读物</li>
        </ul>
      </div>

      <div class="form-panel">
        <div class="form-header">
          <h2>注册新账号</h2>
          <p>请填写邮箱和密码完成注册</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱地址" clearable>
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请设置密码（6-20 位）" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次确认密码"
              show-password
              @keyup.enter="handleRegister"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
              立即注册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span class="tip">已有账号？</span>
          <el-link type="primary" :underline="false" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api'
import { applyGuestTheme } from '@/utils/themes'

// 注册页永远用初始色（量子脉冲蓝）
onMounted(applyGuestTheme)

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 注册邮箱格式：
 * 1. 整体必须是 xxx@xxx.xxx；
 * 2. @ 前面只能是字母、数字、下划线、点、减号，长度 1~30；
 * 3. @ 后面必须是主流邮箱域名（qq.com / 163.com / gmail.com / outlook.com / 126.com / foxmail.com 等）。
 * 与后端 RegisterDTO.EMAIL_PATTERN 保持一致，格式不对统一提示「邮箱格式不正确」。
 */
const EMAIL_DOMAINS = 'qq|163|126|gmail|outlook|hotmail|foxmail|sina|sohu|yahoo|aliyun|icloud|live'
const EMAIL_REGEX = new RegExp(`^[a-z0-9._-]{1,30}@(${EMAIL_DOMAINS})\\.com$`, 'i')

const validateEmail = (rule, value, callback) => {
  const email = String(value || '').trim()
  if (!email) {
    callback(new Error('请输入邮箱地址'))
  } else if (!EMAIL_REGEX.test(email)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const rules = {
  email: [{ validator: validateEmail, trigger: 'blur' }],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function handleRegister() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请使用新账号登录')
    router.replace('/login')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 注册页：固定浅色（浅灰白底 + 白色卡片 + 深色文字 + 主色点缀），简洁干净 */
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

.form-footer {
  text-align: center;
  font-size: 13px;
  margin-top: 4px;
}

.form-footer :deep(.el-link) {
  color: #00d4ff;
}

.form-footer .tip {
  color: #6b7280;
  margin-right: 4px;
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
