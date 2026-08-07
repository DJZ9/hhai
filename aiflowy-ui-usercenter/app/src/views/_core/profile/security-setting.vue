<script setup lang="ts">
import { computed } from 'vue';

import { $t } from '#/locales';
import { ProfileSecuritySetting } from '@aiflowy/common-ui';

const formSchema = computed(() => {
  return [
    {
      value: true,
      fieldName: 'accountPassword',
      label: $t('profile.security.password'),
      description: $t('profile.security.passwordStrength'),
    },
    {
      value: true,
      fieldName: 'securityPhone',
      label: $t('profile.security.phone'),
      description: $t('profile.security.phoneBound'),
    },
    {
      value: true,
      fieldName: 'securityQuestion',
      label: $t('profile.security.question'),
      description: $t('profile.security.questionDesc'),
    },
    {
      value: true,
      fieldName: 'securityEmail',
      label: $t('profile.security.email'),
      description: $t('profile.security.emailBound'),
    },
    {
      value: false,
      fieldName: 'securityMfa',
      label: $t('profile.security.mfa'),
      description: $t('profile.security.mfaDesc'),
    },
  ];
});
</script>
<template>
  <ProfileSecuritySetting :form-schema="formSchema" />
</template>
