import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 点击头像 / 昵称的统一跳转规则：
 *
 * 1. 最高管理员不公开个人信息 → 只提示，不跳转；
 * 2. 点击自己的头像 → 直接进入「个人信息」页；
 * 3. 点击其他人的头像 → 进入「用户主页」（普通管理员同样跳主页，主页上会展示管理员身份）。
 *
 * 管理端与用户端路由前缀不同，按当前路由自动选择。
 */
export function useUserHomepageNav() {
  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()

  /**
   * @param {object} user 目标用户，至少包含 id；可选 isSuperAdmin
   * @returns {boolean} 是否发生了跳转
   */
  function openUserHome(user) {
    const userId = Number(user && user.id)
    if (!userId) return false

    // 1. 超管不公开个人信息
    if (Number(user.isSuperAdmin) === 1) {
      ElMessage.info('该用户不公开个人信息')
      return false
    }

    const adminSide = route.path.startsWith('/admin')

    // 2. 自己 → 个人信息页
    if (userId === Number(userStore.userInfo?.id)) {
      router.push(adminSide ? '/admin/profile' : '/user/profile')
      return true
    }

    // 3. 其他人 → 用户主页
    router.push(adminSide ? `/admin/u/${userId}` : `/user/u/${userId}`)
    return true
  }

  return { openUserHome }
}
