import request from './request'

/* ========================= 用户认证 ========================= */
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const sendCode = (data) => request.post('/auth/send-code', data)
export const resetPassword = (data) => request.post('/auth/reset-password', data)
export const logout = () => request.post('/auth/logout')

/* ========================= 图书 ========================= */
export const getBooks = (params) => request.get('/books', { params })
export const getBookDetail = (id) => request.get(`/books/${id}`)
export const semanticSearch = (params) => request.get('/books/semantic', { params })
export const guessYouLike = (params) => request.get('/books/guess-you-like', { params })
export const getRanks = (params) => request.get('/books/ranks', { params })
export const hotKeywords = () => request.get('/books/hot-keywords')
export const recordSearch = (keyword) => request.post('/books/search-log', null, { params: { keyword } })

/* ========================= 借阅 ========================= */
export const borrowBook = (bookId) => request.post('/borrows', { bookId })
export const getMyBorrows = (params) => request.get('/borrows/my', { params })
export const getMyBookCounts = () => request.get('/borrows/counts')
export const returnBook = (id) => request.put(`/borrows/${id}/return`)
export const renewBook = (id) => request.put(`/borrows/${id}/renew`)

/* ========================= 预约 ========================= */
export const reserveBook = (bookId) => request.post('/reservations', { bookId })
export const getMyReservations = () => request.get('/reservations/my')
export const cancelReservation = (id) => request.put(`/reservations/${id}/cancel`)
export const borrowFromReservation = (id) => request.put(`/reservations/${id}/borrow`)

/* ========================= 收藏 ========================= */
export const toggleFavorite = (bookId) => request.post('/favorites', { bookId })
export const getMyFavorites = () => request.get('/favorites/my')

/* ========================= 评论 ========================= */
export const getComments = (params) => request.get('/comments', { params })
export const addComment = (data) => request.post('/comments', data)
export const likeComment = (id) => request.put(`/comments/${id}/like`)
export const dislikeComment = (id) => request.put(`/comments/${id}/dislike`)
export const reportComment = (id) => request.put(`/comments/${id}/report`)

/* ========================= 通知 / 公告 ========================= */
export const getAnnouncements = () => request.get('/notifications/announcements')
export const getNotifications = (params) => request.get('/notifications', { params })
/** 我的通知各类型数量（筛选下拉显示各类型条数用） */
export const getNotificationTypeCounts = () => request.get('/notifications/type-counts')
export const getUnreadCount = () => request.get('/notifications/unread-count')
export const markNotificationRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllNotificationRead = () => request.put('/notifications/read-all')
export const markAllAnnouncementRead = () => request.put('/notifications/announcements/read-all')
export const markAnnouncementRead = (id) => request.put(`/notifications/announcements/${id}/read`)

/* ========================= 反馈 ========================= */
export const submitFeedback = (data) => request.post('/feedback', data)
export const getMyFeedbacks = () => request.get('/feedback/my')

/* ========================= 个人信息 ========================= */
export const getProfile = () => request.get('/user/profile')
/** 用户个人主页（评论区点头像/昵称进入）：头像、昵称、注册时间、身份、正在阅读、已收藏 */
export const getUserHomepage = (id) => request.get(`/user/homepage/${id}`)
export const updateNickname = (nickname) => request.put('/user/nickname', { nickname })
export const updateEmail = (data) => request.put('/user/email', data)
export const verifyEmailPassword = (password) => request.post('/user/email/verify-password', { password })
export const sendEmailChangeCode = () => request.post('/user/email/send-code')
export const updatePassword = (data) => request.put('/user/password', data)
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ========================= 主题（皮肤） ========================= */
/** 主题存数据库、跟账号绑定，不用 localStorage */
export const getTheme = () => request.get('/user/theme')
export const updateTheme = (theme) => request.put('/user/theme', { theme })

/* ========================= 「光影幻境」主题（magic-bg）的背景图 ========================= */
/** 上传背景图（服务器存图 + 自动取色）：返回 { url, primaryColor, darkColor, lightColor, colors } */
export const uploadUserBg = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/bg', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
/** 保存用户确认后的配色：{ primaryColor, darkColor, lightColor } */
export const updateBgPalette = (data) => request.put('/user/bg/palette', data)
/** 删除背景图（回落到默认的白底黑星星） */
export const clearUserBg = () => request.delete('/user/bg')

/* ========================= 自定义主题（图片取色，存 user_theme 表） ========================= */
/** 当前用户的全部自定义主题 */
export const getMyThemes = () => request.get('/user/themes')
/** 保存一套自定义主题：{ name, primaryColor, darkColor, lightColor, palette?: string[] } */
export const saveMyTheme = (data) => request.post('/user/themes', data)
/** 删除自定义主题（只能删自己的） */
export const deleteMyTheme = (id) => request.delete(`/user/themes/${id}`)
/** 上传图片分析配色：返回 { colors: string[](8~12 个主要颜色), primaryColor, darkColor, lightColor, suggestName } */
export const analyzeThemeImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/themes/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/* ========================= AI ========================= */
export const aiChat = (question) => request.post('/ai/chat', { question })
/** 清空多轮对话上下文（后端按当前登录用户清理内存中的历史） */
export const aiClearHistory = () => request.delete('/ai/history')

/* ========================= 通用上传 ========================= */
export const uploadCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/common/upload/cover', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ========================= 管理端 - 图书 ========================= */
export const adminGetBooks = (params) => request.get('/admin/books', { params })
export const adminAddBook = (data) => request.post('/admin/books', data)
export const adminUpdateBook = (id, data) => request.put(`/admin/books/${id}`, data)
/** 保存封面裁剪参数：coverCardCrop 首页卡片（横向）、coverDetailCrop 详情页（竖向），空字符串表示清除 */
export const adminUpdateCoverCrop = (id, data) => request.put(`/admin/books/${id}/cover-crop`, data)
export const adminDeleteBook = (id) => request.delete(`/admin/books/${id}`)
export const adminBatchDeleteBooks = (ids) => request.delete('/admin/books/batch', { data: { ids } })
export const adminImportBooks = (list) => request.post('/admin/books/import', list)
export const adminImportExcel = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/books/import-excel', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const adminDownloadTemplate = () => request.get('/admin/books/template', { responseType: 'blob' })
/** 批量上传封面：多张图片，按文件名匹配书名 */
export const adminBatchUploadCovers = (files) => {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post('/admin/books/covers/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    // 一次可能传很多张图，放宽超时
    timeout: 300000
  })
}
export const adminAiSummary = (data) => request.post('/admin/books/ai-summary', data)

/* ========================= 管理端 - 用户 ========================= */
export const adminGetUsers = (params) => request.get('/admin/users', { params })
export const adminBanUser = (id, data) => request.put(`/admin/users/${id}/ban`, data)
export const adminEnableUser = (id) => request.put(`/admin/users/${id}/enable`)
export const adminCancelUser = (id) => request.put(`/admin/users/${id}/cancel`)
/** 设为管理员：返回 true 表示因有未归还图书而设为「待激活管理员」 */
export const adminSetUserAdmin = (id) => request.put(`/admin/users/${id}/set-admin`)
/** 取消管理员权限（仅最高管理员） */
export const adminCancelUserAdmin = (id) => request.put(`/admin/users/${id}/cancel-admin`)
/** 该用户未归还（借阅中+逾期）图书数量 */
export const adminGetUnreturnedCount = (id) => request.get(`/admin/users/${id}/unreturned-count`)
/** 彻底删除用户及其关联数据（超管直接执行，普通管理员需先提交操作申请） */
export const adminDeleteUser = (id) => request.delete(`/admin/users/${id}`)
/** 设置/清除管理员封号（仅最高管理员；title 传空表示清除） */
export const adminSetAdminTitle = (id, title) => request.put(`/admin/users/${id}/admin-title`, { title })

/* ========================= 管理端 - 管理员操作记录 ========================= */
/** 某管理员的操作统计：处理举报 / 删除评论 / 编辑图书 + 最近操作明细（仅最高管理员） */
export const adminGetAdminOperations = (adminId) => request.get(`/admin/operations/${adminId}`)

/* ========================= 管理端 - 操作申请 ========================= */
/** 提交操作申请（普通管理员） */
export const submitApplication = (data) => request.post('/admin/applications', data)
/** 操作申请列表（超管看全部，mine=true 看自己提交的） */
export const adminGetApplications = (params) => request.get('/admin/applications', { params })
/** 待审批申请数量 */
export const adminGetPendingApplicationCount = () => request.get('/admin/applications/pending-count')
/** 同意申请：系统代为执行操作 */
export const adminApproveApplication = (id, reply) =>
  request.put(`/admin/applications/${id}/approve`, { reply })
/** 拒绝申请：不执行操作 */
export const adminRejectApplication = (id, reply) =>
  request.put(`/admin/applications/${id}/reject`, { reply })

/* ========================= 管理端 - 借阅 ========================= */
export const adminGetBorrows = (params) => request.get('/admin/borrows', { params })
export const adminRemind = (id) => request.put(`/admin/borrows/${id}/remind`)
/** 一键催还：给所有逾期 / 3 天内到期的记录批量发催还通知，返回发送条数 */
export const adminRemindAll = () => request.put('/admin/borrows/remind-all')

/* ========================= 管理端 - 评论 ========================= */
export const adminGetComments = (params) => request.get('/admin/comments', { params })
export const adminDeleteComment = (id) => request.delete(`/admin/comments/${id}`)
export const adminRestoreComment = (id) => request.put(`/admin/comments/${id}/restore`)
/** 按图书聚合的评论统计列表（置顶优先 → 被举报多优先 → 评论数多优先） */
export const adminGetCommentBooks = (params) => request.get('/admin/comments/books', { params })
/** 某本图书的全部评论（含被删除的、被举报的） */
export const adminGetBookComments = (bookId, params) =>
  request.get(`/admin/comments/books/${bookId}/comments`, { params })
/** 置顶 / 取消置顶某本书 */
export const adminToggleCommentBookPin = (bookId) => request.put(`/admin/comments/books/${bookId}/pin`)

/* ========================= 管理端 - 公告 ========================= */
export const adminGetAnnouncements = () => request.get('/admin/announcements')
export const adminPublishAnnouncement = (data) => request.post('/admin/announcements', data)
export const adminUpdateAnnouncement = (id, data) => request.put(`/admin/announcements/${id}`, data)
export const adminDeleteAnnouncement = (id) => request.delete(`/admin/announcements/${id}`)

/* ========================= 管理端 - 反馈 ========================= */
export const adminGetFeedbacks = (params) => request.get('/admin/feedback', { params })
export const adminReplyFeedback = (id, reply) => request.put(`/admin/feedback/${id}/reply`, { reply })
/** 待回复反馈数量（管理端「用户管理」Tab 红点） */
export const adminGetPendingFeedbackCount = () => request.get('/admin/feedback/pending-count')

/* ========================= 管理端 - 统计 ========================= */
export const adminGetOverview = () => request.get('/admin/statistics/overview')
export const adminGetHotBooks = (params) => request.get('/admin/statistics/hot-books', { params })
export const adminGetTrend = (params) => request.get('/admin/statistics/trend', { params })
export const adminGetActiveUsers = (params) => request.get('/admin/statistics/active-users', { params })
export const adminGetAiReport = (params) => request.get('/admin/statistics/ai-report', { params })
