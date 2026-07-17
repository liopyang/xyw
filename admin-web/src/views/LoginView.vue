<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../stores/auth';
const form = reactive({ username: '', password: '' }),
  loading = ref(false),
  router = useRouter(),
  route = useRoute(),
  auth = useAuthStore();
async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码');
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    router.replace(String(route.query.redirect || '/dashboard'));
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <div class="login">
    <section class="intro">
      <div class="intro-inner">
        <div class="badge">CAMPUS OPERATIONS</div>
        <h1>
          让每一笔校园业务
          <br />
          都清晰、可追踪
        </h1>
        <p>统一管理校园卡、校园网、驾校与续费订单，轻松完成审核、统计和业务协作。</p>
        <div class="features">
          <span>四类业务统一管理</span>
          <span>数据趋势实时掌握</span>
          <span>角色权限安全隔离</span>
        </div>
      </div>
    </section>
    <section class="form-side">
      <div class="login-card">
        <div class="mobile-logo">校</div>
        <h2>欢迎回来</h2>
        <p>登录校园业务管理系统</p>
        <el-form
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item>
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="submit"
            @click="submit"
          >
            登录
          </el-button>
        </el-form>
        <div class="notice">请使用老板或管理员账号登录</div>
      </div>
    </section>
  </div>
</template>
<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 55% 45%;
  background: #fff;
}
.intro {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(145deg, #102b61, #1660cf);
  color: #fff;
}
.intro:before,
.intro:after {
  content: '';
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.intro:before {
  width: 520px;
  height: 520px;
  right: -100px;
  top: -140px;
}
.intro:after {
  width: 360px;
  height: 360px;
  left: -120px;
  bottom: -140px;
}
.intro-inner {
  width: 580px;
  position: relative;
  z-index: 1;
}
.badge {
  font-size: 12px;
  letter-spacing: 2px;
  color: #9ec4ff;
}
.intro h1 {
  font-size: 46px;
  line-height: 1.25;
  margin: 24px 0;
}
.intro p {
  font-size: 17px;
  line-height: 1.9;
  color: #c7d9f7;
  max-width: 520px;
}
.features {
  display: flex;
  gap: 26px;
  margin-top: 40px;
  font-size: 13px;
}
.features span:before {
  content: '✓';
  margin-right: 7px;
  color: #70e0bb;
}
.form-side {
  display: grid;
  place-items: center;
}
.login-card {
  width: 380px;
}
.mobile-logo {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  background: #2469dc;
  color: #fff;
  border-radius: 13px;
  font-weight: 800;
}
.login-card h2 {
  font-size: 30px;
  margin: 26px 0 8px;
}
.login-card > p {
  color: #8a95a5;
  margin: 0 0 32px;
}
.submit {
  width: 100%;
  height: 46px;
  margin-top: 8px;
}
.notice {
  text-align: center;
  color: #9aa4b2;
  font-size: 12px;
  margin-top: 26px;
}
</style>
