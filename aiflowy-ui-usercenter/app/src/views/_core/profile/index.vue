<script setup lang="ts">
import { ref } from 'vue';

import { $t } from '#/locales';
import { Profile } from '@aiflowy/common-ui';
import { useUserStore } from '@aiflowy/stores';

import ProfileBase from './base-setting.vue';
import ProfileNotificationSetting from './notification-setting.vue';
import ProfilePasswordSetting from './password-setting.vue';
import ProfileSecuritySetting from './security-setting.vue';

const userStore = useUserStore();

const tabsValue = ref<string>('basic');

const tabs = ref([
  {
    label: $t('profile.tab.basic'),
    value: 'basic',
  },
  {
    label: $t('profile.tab.security'),
    value: 'security',
  },
  {
    label: $t('profile.tab.password'),
    value: 'password',
  },
  {
    label: $t('profile.tab.notice'),
    value: 'notice',
  },
]);
</script>
<template>
  <Profile
    v-model:model-value="tabsValue"
    :title="$t('page.auth.profile')"
    :user-info="userStore.userInfo"
    :tabs="tabs"
  >
    <template #content>
      <ProfileBase v-if="tabsValue === 'basic'" />
      <ProfileSecuritySetting v-if="tabsValue === 'security'" />
      <ProfilePasswordSetting v-if="tabsValue === 'password'" />
      <ProfileNotificationSetting v-if="tabsValue === 'notice'" />
    </template>
  </Profile>
</template>
