/**
 * 公告「发布人」展示名（用户端公告列表 / 管理端公告管理共用）
 *
 * 规则：
 * 1. 最高管理员（is_super_admin = 1）→ 显示「最高管理员」；
 * 2. 普通管理员（role = 1）→ 显示昵称；
 * 3. 都没有 → 兜底「系统管理员」。
 *
 * 注意：任何情况下都不展示邮箱，避免把管理员邮箱暴露出去。
 */
export function publisherName(item) {
  const data = item || {}
  if (Number(data.adminSuperAdmin) === 1) return '最高管理员'
  if (data.adminNickname) return String(data.adminNickname)
  if (Number(data.adminRole) === 1) return '管理员'
  return '系统管理员'
}

/** 是否最高管理员（用于在列表里加红色标签） */
export function isSuperPublisher(item) {
  return Number(item?.adminSuperAdmin) === 1
}
