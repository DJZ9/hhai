<script setup lang="ts">
import { computed } from 'vue';
import { $t } from '#/locales';

import { ProfileNotificationSetting } from '@aiflowy/common-ui';

const formSchema = computed(() => {
  return [
    {
      value: true,
      fieldName: 'accountPassword',
      label: $t('profile.notification.account'),
      description: $t('profile.notification.accountDesc'),
    },
    {
      value: true,
      fieldName: 'systemMessage',
      label: $t('profile.notification.system'),
      description: $t('profile.notification.systemDesc'),
    },
    {
      value: true,
      fieldName: 'todoTask',
      label: $t('profile.notification.todo'),
      description: $t('profile.notification.todoDesc'),
    },
  ];
});
</script>
<template>
  <ProfileNotificationSetting :form-schema="formSchema" />
</template>
