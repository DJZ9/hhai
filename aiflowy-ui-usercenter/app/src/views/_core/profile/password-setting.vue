<script setup lang="ts">
import type { AIFlowyFormSchema } from '#/adapter/form';

import { computed, ref } from 'vue';

import { $t } from '#/locales';
import { ProfilePasswordSetting, z } from '@aiflowy/common-ui';

import { ElMessage } from 'element-plus';

const profilePasswordSettingRef = ref();

const formSchema = computed((): AIFlowyFormSchema[] => {
  return [
    {
      fieldName: 'oldPassword',
      label: $t('profile.password.oldPwd'),
      component: 'AIFlowyInputPassword',
      componentProps: {
        placeholder: $t('profile.password.oldPwdPlaceholder'),
      },
    },
    {
      fieldName: 'newPassword',
      label: $t('profile.password.newPwd'),
      component: 'AIFlowyInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: $t('profile.password.newPwdPlaceholder'),
      },
    },
    {
      fieldName: 'confirmPassword',
      label: $t('profile.password.confirmPwd'),
      component: 'AIFlowyInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: $t('profile.password.confirmPwdPlaceholder'),
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({ required_error: $t('profile.password.confirmPwdPlaceholder') })
            .min(1, { message: $t('profile.password.confirmPwdPlaceholder') })
            .refine((value) => value === newPassword, {
              message: $t('profile.password.mismatch'),
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

function handleSubmit() {
  ElMessage.success($t('profile.password.success'));
}
</script>
<template>
  <ProfilePasswordSetting
    ref="profilePasswordSettingRef"
    class="w-1/3"
    :form-schema="formSchema"
    @submit="handleSubmit"
  />
</template>
