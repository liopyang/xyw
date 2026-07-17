<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  DataAnalysis,
  Document,
  User,
  ChatDotRound,
  Setting,
  Tools,
  ArrowDown,
} from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore();
const active = computed(() => route.path);
async function logout() {
  await auth.logout();
  router.replace('/login');
}
</script>
<template>
  <el-container class="shell">
    <el-aside
      width="236px"
      class="aside"
    >
      <div class="logo">
        <span class="logo-mark">校</span>
        <div>
          <b>校园业务</b>
          <small>管理系统</small>
        </div>
      </div>
      <el-menu
        :default-active="active"
        router
        class="menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/agents">
          <el-icon><User /></el-icon>
          <span>代理管理</span>
        </el-menu-item>
        <el-menu-item index="/issues">
          <el-icon><ChatDotRound /></el-icon>
          <span>问题管理</span>
        </el-menu-item>
        <el-sub-menu index="mini">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>小程序内容</span>
          </template>
          <el-menu-item index="/mini/home">首页配置</el-menu-item>
          <el-menu-item index="/mini/categories">内容栏目</el-menu-item>
          <el-menu-item index="/mini/articles">图文内容</el-menu-item>
          <el-menu-item index="/mini/media">图片素材</el-menu-item>
          <el-menu-item index="/mini/places">校园地图</el-menu-item>
          <el-menu-item index="/mini/publish-records">发布记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu
          v-if="auth.isOwner"
          index="config"
        >
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>业务配置</span>
          </template>
          <el-menu-item index="/config/driving-school">驾校价格</el-menu-item>
          <el-menu-item index="/config/system">系统配置</el-menu-item>
        </el-sub-menu>
        <el-sub-menu
          v-if="auth.isOwner"
          index="system"
        >
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">管理员账号</el-menu-item>
          <el-menu-item index="/mini/users">小程序用户</el-menu-item>
          <el-menu-item index="/system/logs">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">{{ route.meta.title }}</div>
        <el-dropdown trigger="click">
          <div class="user">
            <span class="avatar">{{ auth.user?.realName?.slice(0, 1) || '管' }}</span>
            <div>
              <b>{{ auth.user?.realName || auth.user?.username || '管理员' }}</b>
              <small>{{ auth.user?.role === 'OWNER' ? '老板' : '管理员' }}</small>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>
<style scoped>
.shell {
  min-height: 100vh;
}
.aside {
  background: #101c34;
  color: #fff;
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 3;
}
.shell > .el-container {
  margin-left: 236px;
}
.logo {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #2d76ee;
  font-weight: 800;
}
.logo b,
.logo small {
  display: block;
}
.logo b {
  font-size: 17px;
}
.logo small {
  color: #8d9ab0;
  margin-top: 3px;
}
.menu {
  border: 0;
  background: transparent;
  padding: 12px;
}
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  color: #aab5c8;
  border-radius: 8px;
  margin: 4px 0;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: #182844;
  color: #fff;
}
.menu :deep(.el-menu-item.is-active) {
  background: #2469dc;
  color: #fff;
}
.header {
  height: 68px;
  background: #fff;
  border-bottom: 1px solid #e8edf4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 2;
}
.breadcrumb {
  font-weight: 650;
}
.user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.user b,
.user small {
  display: block;
  font-size: 13px;
}
.user small {
  color: #8a95a5;
  margin-top: 2px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #e8f0ff;
  color: #2469dc;
  font-weight: 700;
}
.main {
  padding: 0;
  background: #f4f7fb;
  overflow: visible;
}
</style>
