<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand-panel">
        <div class="brand-icon">
          <el-icon :size="34"><Key /></el-icon>
        </div>
        <h1>找回密码</h1>
        <p class="sub">通过注册邮箱验证码重置登录密码</p>
        <ol class="step-list">
          <li><span class="num">1</span> 输入注册邮箱并获取验证码</li>
          <li><span class="num">2</span> 查收邮件中的 6 位验证码</li>
          <li><span class="num">3</span> 设置新的登录密码</li>
        </ol>
        <p class="hint">
          <el-icon><InfoFilled /></el-icon>
          验证码 5 分钟内有效，请勿泄露给他人
        </p>
      </div>

      <div class="form-panel">
        <div class="form-header">
          <h2>重置登录密码</h2>
          <p>请按提示完成身份验证</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="请输入注册邮箱" clearable>
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-row">
              <el-input v-model="form.code" placeholder="请输入 6 位验证码" maxlength="6">
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
              <el-button :disabled="counting > 0" :loading="sending" class="code-btn" @click="handleSendCode">
                {{ counting > 0 ? `${counting}s 后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="newPassword">
            <el-input v-model="form.newPassword" type="password" placeholder="请设置新密码（6-20 位）" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次确认新密码"
              show-password
              @keyup.enter="handleSubmit"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
              确认重置
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <el-link type="primary" :underline="false" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode, resetPassword } from '@/api'
import { applyGuestTheme } from '@/utils/themes'

// 忘记密码页永远用初始色（量子脉冲蓝）
onMounted(applyGuestTheme)

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const counting = ref(0)
let timer = null

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次确认新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function handleSendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入注册邮箱')
    return
  }
  sending.value = true
  try {
    await sendCode({ email: form.email, type: 1 })
    ElMessage.success('验证码已发送，请查收邮件')
    counting.value = 60
    timer = setInterval(() => {
      counting.value -= 1
      if (counting.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    sending.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await resetPassword(form)
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.replace('/login')
  } catch (e) {
    /* 错误消息已由拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* 忘记密码页：固定浅色（浅灰白底 + 白色卡片 + 深色文字 + 主色点缀），简洁干净 */
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
  min-height: 560px;
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
  margin: 0 0 26px;
  font-size: 13px;
  color: #6b7280;
}

.step-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: #4b5563;
}

.step-list .num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(0, 212, 255, 0.14);
  color: #00d4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.hint {
  margin-top: auto;
  font-size: 12.5px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
}

.hint :deep(.el-icon) {
  color: #00d4ff;
}

.form-panel {
  flex: 1;
  padding: 48px 46px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 26px;
}

.form-header h2 {
  margin: 0 0 6px;
  font-size: 23px;
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

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

/* 次要按钮：白底 + 浅灰边框 + 深色文字 */
.code-btn {
  width: 130px;
  flex-shrink: 0;
  background: #ffffff;
  border-color: #dcdfe6;
  color: #333333;
}

.code-btn:hover,
.code-btn:focus {
  background: #ffffff;
  border-color: #00d4ff;
  color: #00d4ff;
}

/* 主按钮：主色填充 + 白色文字 */
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
}

.form-footer :deep(.el-link) {
  color: #00d4ff;
}

@media (max-width: 820px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 36px 26px;
  }

  .code-btn {
    width: 110px;
  }
}
</style>
