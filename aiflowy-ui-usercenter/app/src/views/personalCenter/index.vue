<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, onMounted, reactive, ref } from 'vue';

import { useAppConfig } from '@aiflowy/hooks';
import { useAccessStore, useUserStore } from '@aiflowy/stores';

import {
  ElAvatar,
  ElButton,
  ElDivider,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElSpace,
  ElTable,
  ElTableColumn,
  ElTag,
  ElUpload,
} from 'element-plus';
import { tryit } from 'radash';

import { $t } from '#/locales';
import { api } from '#/api/request';
import { useAuthStore } from '#/store';

interface BoundAccount {
  id: string;
  openidType: string;
  openidValue: string;
  nickname: string;
  avatar: string;
  created: string;
}

interface OAuthConfig {
  platform: string;
  displayName: string;
  icon: string;
  authUrl: string;
}

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const userStore = useUserStore();
const useAuth = useAuthStore();
const formRef = ref<FormInstance>();
const formData = reactive({
  nickname: userStore?.userInfo?.nickname,
  avatar: userStore?.userInfo?.avatar,
  mobile: userStore?.userInfo?.mobile,
});

const editing = reactive({
  nickname: false,
  avatar: false,
  mobile: false,
});
const rules = {
  nickname: [{ required: true, message: $t('personalCenter.usernameRequired') }],
  mobile: [
    { required: true, message: $t('personalCenter.phoneRequired') },
    { pattern: /^1[3-9]\d{9}$/, message: $t('personalCenter.phoneInvalid') },
  ],
};

const boundAccounts = ref<BoundAccount[]>([]);
const oauthList = ref<OAuthConfig[]>([]);
const platformNameMap = ref<Record<string, string>>({});
const loading = ref(false);

onMounted(() => {
  getBoundAccounts();
  getOAuthList();
  getPlatformList();
  checkBindStatus();
});

const handleFieldChange = async (field: keyof typeof formData) => {
  if (!formRef.value) return;
  const [err] = await tryit(formRef.value.validateField)([field]);

  if (!err) {
    submit(field);
  }
};
const handleCancelEdit = async (field: keyof typeof formData) => {
  editing[field] = false;
  formRef.value?.resetFields([field]);
};

const handleUploadChange = (response: any) => {
  formData.avatar = response?.data.path;
  submit('avatar');
};
const accessStore = useAccessStore();
const btnLoading = ref(false);
function submit(field: keyof typeof formData) {
  btnLoading.value = true;
  api.post('/userCenter/sysAccount/updateProfile', formData).then((res) => {
    btnLoading.value = false;
    if (res.errorCode === 0) {
      ElMessage.success($t('personalCenter.updateSuccess'));
      editing[field] = false;
      useAuth.fetchUserInfo();
    }
  });
}
function logout() {
  useAuth.logout();
}

function getBoundAccounts() {
  loading.value = true;
  api.get('/userCenter/thirdAuth/getBoundAccounts').then((res) => {
    loading.value = false;
    if (res.errorCode === 0) {
      boundAccounts.value = res.data || [];
    }
  });
}

function getOAuthList() {
  api.get('/userCenter/thirdAuth/getOAuthList').then((res) => {
    if (res.errorCode === 0) {
      oauthList.value = res.data || [];
    }
  });
}

function getPlatformList() {
  api.get('/userCenter/thirdAuth/getPlatformList').then((res) => {
    if (res.errorCode === 0) {
      const list = res.data || [];
      const map: Record<string, string> = {};
      for (const item of list) {
        map[item.platform] = item.displayName;
      }
      platformNameMap.value = map;
    }
  });
}

function checkBindStatus() {
  const urlParams = new URLSearchParams(window.location.search);
  const bindStatus = urlParams.get('bindStatus');
  switch (bindStatus) {
    case 'alreadyBoundToSelf': {
      ElMessage.warning($t('personalCenter.alreadyBound'));
      window.history.replaceState({}, '', window.location.pathname);

      break;
    }
    case 'boundToOther': {
      ElMessage.error($t('personalCenter.boundToOther'));
      window.history.replaceState({}, '', window.location.pathname);

      break;
    }
    case 'success': {
      ElMessage.success($t('personalCenter.bindSuccess'));
      // 清除URL参数
      window.history.replaceState({}, '', window.location.pathname);
      // 刷新绑定列表
      getBoundAccounts();

      break;
    }
    // No default
  }
}

function toBindAuth(url: string) {
  window.location.href = url;
}

function unbindAccount(platform: string) {
  ElMessageBox.confirm(
    $t('personalCenter.unbindConfirm'),
    $t('personalCenter.notice'),
    {
    confirmButtonText: $t('button.confirm'),
    cancelButtonText: $t('button.cancel'),
    type: 'warning',
  }).then(() => {
    api.post(`/userCenter/thirdAuth/unbind/${platform}`).then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('personalCenter.unbindSuccess'));
        getBoundAccounts();
      }
    });
  });
}

// 根据设备类型过滤 oauthList
const filteredOauthList = computed(() => {
  const isPC = !isMobile() && !isWechatBrowser();
  return oauthList.value.filter((oauth) =>
    isPC
      ? oauth.platform !== 'wx_service_account'
      : oauth.platform !== 'wx_open',
  );
});

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString($t('personalCenter._locale') || 'zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

// 判断是否为移动设备
const isMobile = () => {
  const userAgent = navigator.userAgent;
  return /Mobile|Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    userAgent,
  );
};

// 判断是否为微信浏览器
const isWechatBrowser = () => {
  const userAgent = navigator.userAgent;
  return /MicroMessenger/i.test(userAgent);
};
</script>

<template>
  <div class="bg-background-deep h-full w-full pt-[156px]">
    <ElForm
      ref="formRef"
      class="mx-auto"
      style="max-width: 619px; width: 100%"
      label-position="top"
      :model="formData"
      :rules="rules"
      hide-required-asterisk
    >
      <ElFormItem :label="$t('personalCenter.username')" prop="nickname">
        <div class="flex w-full justify-between gap-5">
          <template v-if="editing.nickname">
            <ElInput v-model="formData.nickname" />
            <ElSpace>
              <ElButton
                :loading="btnLoading"
                type="primary"
                @click="handleFieldChange('nickname')"
              >
                {{ $t('personalCenter.confirm') }}
              </ElButton>
              <ElButton @click="handleCancelEdit('nickname')">{{ $t('personalCenter.cancel') }}</ElButton>
            </ElSpace>
          </template>
          <template v-else>
            <span>{{ formData.nickname }}</span>
            <ElButton @click="editing.nickname = true">{{ $t('personalCenter.editUsername') }}</ElButton>
          </template>
        </div>
      </ElFormItem>
      <ElFormItem :label="$t('personalCenter.avatar')">
        <ElSpace>
          <ElUpload
            :action="`${apiURL}/userCenter/commons/upload`"
            :headers="{
              'aiflowy-token': accessStore.accessToken,
            }"
            :show-file-list="false"
            :on-success="handleUploadChange"
          >
            <ElAvatar :src="formData.avatar" :size="46" />
          </ElUpload>
          <span>{{ $t('personalCenter.avatarTip') }}</span>
        </ElSpace>
      </ElFormItem>
      <ElFormItem :label="$t('personalCenter.phone')" prop="mobile">
        <div class="flex w-full justify-between gap-5">
          <template v-if="editing.mobile">
            <ElInput v-model="formData.mobile" />
            <ElSpace>
              <ElButton
                :loading="btnLoading"
                type="primary"
                @click="handleFieldChange('mobile')"
              >
                {{ $t('personalCenter.confirm') }}
              </ElButton>
              <ElButton @click="handleCancelEdit('mobile')">{{ $t('personalCenter.cancel') }}</ElButton>
            </ElSpace>
          </template>
          <template v-else>
            <span>{{ formData.mobile }}</span>
            <ElButton @click="editing.mobile = true">{{ $t('personalCenter.modifyPhone') }}</ElButton>
          </template>
        </div>
      </ElFormItem>

      <ElDivider v-if="boundAccounts.length > 0 || oauthList.length > 0">
        {{ $t('personalCenter.thirdPartyBinding') }}
      </ElDivider>

      <!-- 可绑定的账号图标展示区域 -->
      <ElFormItem v-if="filteredOauthList.length > 0" :label="$t('personalCenter.bindableAccounts') + '：'">
        <div class="flex gap-5">
          <div v-for="oauth in filteredOauthList" :key="oauth.platform"
            class="flex cursor-pointer flex-col items-center gap-2" @click="toBindAuth(oauth.authUrl)">
            <img :src="oauth.icon" :alt="oauth.displayName" class="h-10 w-10" />
            <span class="text-sm">
              {{ oauth.displayName }}
            </span>
          </div>
        </div>
      </ElFormItem>

      <!-- 已绑定账号表格展示区域 -->
      <ElFormItem v-if="boundAccounts.length > 0" :label="$t('personalCenter.boundAccounts') + '：'">
        <ElTable :data="boundAccounts" border stripe style="width: 100%">
          <ElTableColumn type="index" :label="$t('personalCenter.sequence')" width="70"
            :index="(index: number) => index + 1" />
          <ElTableColumn :label="$t('personalCenter.status')" width="100" align="center">
            <template #default>
              <ElTag type="success">{{ $t('personalCenter.inUse') }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="openidType" :label="$t('personalCenter.loginMethod')" width="150">
            <template #default="{ row }">
              {{ platformNameMap[row.openidType] || row.openidType }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="openidValue" :label="$t('personalCenter.detail')" min-width="200">
            <template #default="{ row }">
              <div class="text-xs" style="word-break: break-all">
                {{ row.openidValue }}
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="created" :label="$t('personalCenter.bindTime')" width="180">
            <template #default="{ row }">
              {{ formatDate(row.created) }}
            </template>
          </ElTableColumn>
          <ElTableColumn :label="$t('personalCenter.operation')" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <ElButton type="danger" size="small" text @click="unbindAccount(row.openidType)">
                {{ $t('personalCenter.unbind') }}
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </ElFormItem>

      <ElFormItem>
        <div class="mt-20 flex w-full justify-center">
          <ElButton @click="logout" type="primary" class="!h-11 w-[333px]">
            {{ $t('common.logout') }}
          </ElButton>
        </div>
      </ElFormItem>
    </ElForm>
  </div>
</template>
