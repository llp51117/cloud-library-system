<template>
  <el-dialog
    :model-value="modelValue"
    title="提交操作申请"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="删除用户 / 注销用户 / 批量删除图书属于高风险操作，需要提交申请由最高管理员审批，审批通过后由系统自动执行。"
      class="apply-tip"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="apply-form">
      <el-form-item label="申请类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择申请类型" style="width: 100%">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="操作目标" prop="targetId">
        <el-input
          :model-value="form.targetId"
          :placeholder="targetPlaceholder"
          clearable
          @update:model-value="onTargetIdInput"
        />
        <div class="field-tip">{{ targetTip }}</div>
      </el-form-item>

      <el-form-item label="申请理由" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请说明申请理由，便于最高管理员审批"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { submitApplication } from '@/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 预设申请类型（如从图书管理页发起批量删除申请） */
  presetType: { type: String, default: '' },
  /** 预设目标ID */
  presetTargetId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'submitted'])

const formRef = ref(null)
const submitting = ref(false)

const typeOptions = [
  { value: 'delete_user', label: '删除用户' },
  { value: 'cancel_user', label: '注销用户' },
  { value: 'batch_delete_book', label: '批量删除图书' },
  { value: 'other', label: '其他' }
]

const form = reactive({
  type: 'delete_user',
  targetId: '',
  content: ''
})

/**
 * 目标ID校验：只认数字（批量删书时多个用逗号 / 空格分隔）。
 * 后端 target_id 存的是 ID，填了文字会触发 Data truncated for column 'target_id'，
 * 所以这里在提交前就拦下来并提示「目标 ID 必须是数字」。
 */
const validateTargetId = (rule, value, callback) => {
  const raw = (value ?? '').trim()
  if (!raw) {
    // 「其他」类型可以不填目标，提交时留空；其余类型必须填
    if (form.type === 'other') {
      callback()
    } else {
      callback(new Error('请填写操作目标ID'))
    }
    return
  }
  const parts = raw.split(/[,，\s]+/).filter(Boolean)
  if (parts.some((p) => !/^\d+$/.test(p))) {
    callback(new Error('目标 ID 必须是数字'))
    return
  }
  if (form.type !== 'batch_delete_book' && parts.length > 1) {
    callback(new Error('该申请类型只能填写一个目标 ID'))
    return
  }
  callback()
}

const rules = {
  type: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  targetId: [{ validator: validateTargetId, trigger: ['blur', 'change'] }],
  content: [{ required: true, message: '请填写申请理由', trigger: 'blur' }]
}

/** 输入时就过滤掉非数字字符（多个 ID 允许逗号 / 中文逗号 / 空格分隔） */
function onTargetIdInput(value) {
  form.targetId = String(value ?? '').replace(/[^\d,，\s]/g, '')
}

const targetPlaceholder = computed(() => {
  if (form.type === 'batch_delete_book') return '图书ID，多个用英文逗号分隔，如 1,2,3'
  if (form.type === 'other') return '可填写相关对象ID（可留空）'
  return '用户ID，如 12'
})

const targetTip = computed(() => {
  if (form.type === 'batch_delete_book') return '多个图书ID用英文逗号分隔；同意后系统会删除图书及其评论、借阅、预约、收藏等关联数据。'
  if (form.type === 'delete_user') return '删除后用户及其评论、借阅、收藏等关联数据都会被清除（不可恢复）。'
  if (form.type === 'cancel_user') return '注销后该用户将无法登录系统。'
  return '如有需要可填写目标对象ID。'
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      form.type = props.presetType || 'delete_user'
      form.targetId = props.presetTargetId || ''
      form.content = ''
    }
  }
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await submitApplication({
        type: form.type,
        targetId: form.targetId ? form.targetId.trim() : '',
        content: form.content
      })
      ElMessage.success('申请已提交，等待最高管理员审批')
      emit('submitted')
      close()
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.apply-tip {
  margin-bottom: 16px;
}

.apply-form {
  margin-top: 4px;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.5;
}
</style>
