<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { uuid } from '@aiflowy/utils';

import { ElButton, ElEmpty, ElMessage, ElTabPane, ElTabs } from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';
import OAuthConfig from '#/views/system/sysOption/oauth/OAuthConfig.vue';

const oauthConfig = ref<any>([]);
const saveLoading = ref(false);
const active = ref('');
onMounted(() => {
  getOptionByKey('oauthConfig');
});
// 初始配置数据
const initConfigData = () => ({
  id: uuid(),
  platform: '',
  displayName: 'OAuth',
  icon: '',
  enabled: 1,
  roleId: '',
  clientId: '',
  clientSecret: '',
  redirectUrl: '',
  authUrl: '',
  authHttpConfig: {
    params: {},
  },
  tokenUrl: '',
  tokenHttpConfig: {
    method: 'get',
    headers: {},
    params: {},
  },
  userInfoUrl: '',
  userInfoHttpConfig: {
    method: 'get',
    headers: {},
    params: {},
    fieldMapping: {
      userKey: '',
      username: '',
      nickname: '',
      avatar: '',
    },
  },
});
function handleSave(key: string) {
  saveLoading.value = true;
  api
    .post('/api/v1/sysOption/saveOption', {
      key,
      value: JSON.stringify(oauthConfig.value),
    })
    .then((res) => {
      saveLoading.value = false;
      if (res.errorCode === 0) {
        ElMessage.success($t('message.saveOkMessage'));
        getOptionByKey(key);
      }
    });
}
function getOptionByKey(key: string) {
  api
    .get('/api/v1/sysOption/getByKey', {
      params: {
        key,
      },
    })
    .then((res) => {
      if (res.errorCode === 0 && res.data) {
        oauthConfig.value = JSON.parse(res.data.value);
        active.value = oauthConfig.value[0].id;
      } else {
        handleTabsEdit('', 'add');
      }
    });
}
function handleTabsEdit(targetName: any, action: any) {
  if (action === 'add') {
    const data = initConfigData();
    oauthConfig.value.push(data);
    active.value = data.id;
  }
  if (action === 'remove') {
    oauthConfig.value = oauthConfig.value.filter(
      (item: any) => item.id !== targetName,
    );
    if (active.value === targetName) {
      active.value =
        oauthConfig.value.length > 0 ? oauthConfig.value[0].id : '';
    }
  }
}
</script>

<template>
  <div class="page-container">
    <ElButton
      class="mb-2"
      type="primary"
      @click="handleSave('oauthConfig')"
      :disabled="saveLoading"
    >
      {{ $t('button.save') }}
    </ElButton>
    <ElTabs v-model="active" type="card" editable @edit="handleTabsEdit">
      <ElTabPane
        v-for="(item, i) in oauthConfig"
        :key="i"
        :label="item.displayName"
        :name="item.id"
      >
        <OAuthConfig :config-json="item" />
      </ElTabPane>
    </ElTabs>
    <ElEmpty v-if="oauthConfig.length === 0" />
  </div>
</template>

<style scoped></style>
