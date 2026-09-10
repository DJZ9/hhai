<script lang="ts" setup>
import type { AIFlowyFormSchema } from '@aiflowy/common-ui';

import { computed, onMounted, ref } from 'vue';

import { AuthenticationLogin, z } from '@aiflowy/common-ui';
import { useAppConfig } from '@aiflowy/hooks';
import { $t } from '@aiflowy/locales';
import { preferences } from '@aiflowy/preferences';
import { ElDivider, ElMessage } from 'element-plus';
import { api } from '#/api/request';
import { useAuthStore } from '#/store';
import { useRoute } from 'vue-router';
const route = useRoute();
defineOptions({ name: 'Login' });
onMounted(() => {});

const authStore = useAuthStore();

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

type PlatformType = 'ding_talk' | 'wx_web';

const title = computed(() => preferences.auth.welcomeBack);
const subTitle = computed(() => preferences.auth.loginSubtitle);
const formSchema = computed((): AIFlowyFormSchema[] => {
  return [
    {
      component: 'AIFlowyInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'account',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'AIFlowyInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
  ];
});

const oauthList = ref<any>([])

// 判断是否为移动设备
function isMobileDevice(): boolean {
  const userAgent = navigator.userAgent.toLowerCase();
  return /mobile|android|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
}

// 判断是否为微信浏览器
function isWechatBrowser(): boolean {
  const userAgent = navigator.userAgent.toLowerCase();
  return /micromessenger/i.test(userAgent);
}

// 根据设备类型过滤oauthList
const filteredOauthList = computed(() => {
  const isMobile = isMobileDevice();
  const isWechat = isWechatBrowser();

  // 如果是PC浏览器（非移动设备且非微信浏览器）：过滤掉wx_service_account
  // 如果是移动设备或微信浏览器：过滤掉wx_open
  return oauthList.value.filter((item: any) => {
    // 钉钉等其他平台不受过滤影响，在所有设备类型下都正常显示
    if (item.platform !== 'wx_service_account' && item.platform !== 'wx_open') {
      return true;
    }

    // PC浏览器：保留wx_open，过滤wx_service_account
    if (!isMobile && !isWechat) {
      return item.platform === 'wx_open';
    }

    // 移动设备或微信浏览器：保留wx_service_account，过滤wx_open
    return item.platform === 'wx_service_account';
  });
});

function onSubmit(values: any) {
  // config 对象为TAC验证码的一些配置和验证的回调
  const config = {
    // 生成接口 (必选项,必须配置, 要符合tianai-captcha默认验证码生成接口规范)
    requestCaptchaDataUrl: `${apiURL}/api/v1/public/getCaptcha`,
    // 验证接口 (必选项,必须配置, 要符合tianai-captcha默认验证码校验接口规范)
    validCaptchaUrl: `${apiURL}/api/v1/public/check`,
    // 验证码绑定的div块 (必选项,必须配置)
    bindEl: '#captcha-box',
    // 验证成功回调函数(必选项,必须配置)
    validSuccess: (res: any, _: any, tac: any) => {
      // 销毁验证码服务
      tac.destroyWindow();
      // 调用具体的login方法
      values.validToken = res.data;
      authStore.authLogin(values);
    },
    // 验证失败的回调函数(可忽略，如果不自定义 validFail 方法时，会使用默认的)
    validFail: (_: any, __: any, tac: any) => {
      // 验证失败后重新拉取验证码
      tac.reloadCaptcha();
    },
    // 刷新按钮回调事件
    btnRefreshFun: (_: any, tac: any) => {
      tac.reloadCaptcha();
    },
    // 关闭按钮回调事件
    btnCloseFun: (_: any, tac: any) => {
      tac.destroyWindow();
    },
  };
  const style = {
    logoUrl: null, // 去除logo
    // logoUrl: "/xx/xx/xxx.png" // 替换成自定义的logo
    btnUrl: '/tac-btn.png',
  };
  window
    // @ts-ignore
    .initTAC('/tac', config, style)
    .then((tac: any) => {
      tac.init(); // 调用init则显示验证码
    })
    .catch((error: any) => {
      console.error('初始化tac失败', error);
    });
}

function getAuthUrl(platform: PlatformType) {
  return api.get('/thirdAuth/getAuthUrl', {
    params: {
      platform,
    },
  });
}
async function platformLogin(platform: PlatformType) {
  const { errorCode, data } = await getAuthUrl(platform);
  if (errorCode === 0) {
    window.location.href = data;
  }
}
function getOAuthList() {
  api.get('/thirdAuth/getOAuthList').then(res => {
    oauthList.value = res.data;
  });
}
function toAuthPage(url: string) {
  window.location.href = url;
}

function checkOAuthError() {
  const oauthError = route.query.oauthError as string;
  if (oauthError === 'unbound') {
    ElMessage.error('该三方账号未绑定系统用户，请先在个人中心绑定账号');
  }
}

onMounted(() => {
  getOAuthList();
  // 检查OAuth登录错误
  checkOAuthError();
});

</script>

<template>
  <div>
    <AuthenticationLogin
      :form-schema="formSchema"
      :loading="authStore.loginLoading"
      :title="title"
      :sub-title="subTitle"
      @submit="onSubmit"
    />
    <div id="captcha-box" class="captcha-div"></div>
    <ElDivider v-if="filteredOauthList.length > 0" content-position="center">
      {{ $t('common.otherLoginType') }}
    </ElDivider>
    <div class="flex-center flex gap-[20px]">
      <img
        v-for="item in filteredOauthList"
        :key="item.platform"
        class="platform-icon"
        :src="item.icon"
        :alt="item.displayName"
        @click="toAuthPage(item.authUrl)"
      />
    </div>
  </div>
</template>

<style scoped>
.captcha-div {
  position: absolute;
  top: 30vh;
  left: 21vh;
}

.platform-icon {
  width: 30px;
  height: 30px;
  cursor: pointer;
}
</style>
