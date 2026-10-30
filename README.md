# 📚 云图书管理系统 (Cloud Library System)

基于 Vue 3 + Spring Boot 3 的前后端分离 AI 智慧云图书服务平台。

## ✨ 项目核心亮点
- **AI 智能问答助手**：采用本地数据库实时检索 + 智谱 GLM 大模型 API（RAG 架构），实现高精度的图书推荐、错别字纠错与多轮上下文对话。
- **多主题动态换肤**：内置 11 套主题，支持自定义图片取色、深浅模式切换及“光影幻境”特效。
- **多级权限与审批流**：最高管理员、普通管理员、待激活管理员的三级体系，支持高危操作审批。
- **自动化定时任务**：自动催还、预约超时顺延等。

## 🛠️ 技术栈
- **前端**: Vue 3, Vite, Element Plus, Pinia, ECharts, Axios
- **后端**: Java 18, Spring Boot 3.2, MyBatis-Plus, JWT, MySQL 8.0, Redis
- **AI 层**: 智谱 GLM-4-Flash (直连 API)

## 📂 项目目录说明
- `frontend/` : 前端 Vue 3 源代码
- `src/` : 后端 Spring Boot 源代码
- `sql/` : 数据库建表及初始化数据脚本 (`cloud_library.sql`)
- `docs/` : 项目详细设计说明文档 (需求分析与系统设计)

## 🚀 快速启动指南
### 1. 后端启动
1. 下载项目到D盘。
2. 在本地 MySQL 中新建数据库 `cloud-library-system`。
3. 导入 `sql/cloud_library.sql` 脚本。
4. 修改 `src/main/resources/application.yml`，填入你的数据库密码，智谱 API Key，邮箱号以及邮箱授权码。
5. 运行 `CloudLibrarySystemApplication`。

### 2. 前端启动
1. 终端进入 `frontend` 目录，运行 `npm install` 安装依赖。
2. 运行 `npm run dev` 启动项目。

## 📸 系统预览
用户端tab 
<img width="1479" height="118" alt="image" src="https://github.com/user-attachments/assets/485dd4bf-a8f5-4982-b27b-5e46346da08a" />
管理端tab
<img width="1439" height="135" alt="image" src="https://github.com/user-attachments/assets/26014a13-035e-4a25-9462-d5ea0b130493" />
用户端最喜欢的页面（光影幻境皮肤超赞的）
<img width="786" height="581" alt="image" src="https://github.com/user-attachments/assets/b8a78003-9b34-49b0-b083-32e1069e8b43" />

4. ## 🎬 最喜欢的皮肤演示视频
**[点击这里直接在线观看演示视频](./demo.mp4)**
<video src="https://github.com/llp51117/cloud-library-system/raw/main/demo.mp4" controls="controls" width="100%"></video>
<img width="850" height="113" alt="image" src="https://github.com/user-attachments/assets/2873d1c4-12e4-4b2a-8ffc-050e61a3bb71" />







