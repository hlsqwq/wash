<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="app-header__inner">
        <router-link to="/" class="brand">
          <span class="brand__mark">蜂</span>
          <span class="brand__text">
            <strong>蜂窝盘模型管理</strong>
            <small>云端模型参数与图片维护</small>
          </span>
        </router-link>
        <el-menu
          mode="horizontal"
          :default-active="route.path.startsWith('/models') ? '/models' : '/'"
          :ellipsis="false"
          class="app-nav"
        >
          <el-menu-item index="/models" @click="$router.push('/models')">模型列表</el-menu-item>
          <el-menu-item index="/models/new" @click="$router.push('/models/new')">新建模型</el-menu-item>
        </el-menu>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  height: auto;
  padding: 0;
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 1px 4px rgba(16, 31, 53, 0.04);
}

.app-header__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0;
}

.brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffb648, #ff8a3c);
  color: #ffffff;
  font-weight: 700;
  font-size: 18px;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand__text strong {
  font-size: 16px;
}

.brand__text small {
  color: var(--text-muted);
  font-size: 12px;
}

.app-nav {
  border-bottom: none;
  flex: 1;
}

.app-main {
  padding: 0;
  background: var(--page-bg);
}

@media (max-width: 720px) {
  .app-header__inner {
    padding: 0 16px;
    gap: 8px;
  }
}
</style>
