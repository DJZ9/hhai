<script setup lang="ts">
import type { BasicOption } from '@aiflowy/types';

import type { AIFlowyFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { $t } from '#/locales';
import { ProfileBaseSetting } from '@aiflowy/common-ui';

import { getUserInfoApi } from '#/api';

const profileBaseSettingRef = ref();

const MOCK_ROLES_OPTIONS: BasicOption[] = [
  {
    label: $t('profile.role.admin'),
    value: 'super',
  },
  {
    label: $t('profile.role.user'),
    value: 'user',
  },
  {
    label: $t('profile.role.test'),
    value: 'test',
  },
];

const formSchema = computed((): AIFlowyFormSchema[] => {
  return [
    {
      fieldName: 'realName',
      component: 'Input',
      label: $t('profile.label.realName'),
    },
    {
      fieldName: 'username',
      component: 'Input',
      label: $t('profile.label.username'),
    },
    {
      fieldName: 'roles',
      component: 'Select',
      componentProps: {
        mode: 'tags',
        options: MOCK_ROLES_OPTIONS,
      },
      label: $t('profile.label.role'),
    },
    {
      fieldName: 'introduction',
      component: 'Textarea',
      label: $t('profile.label.introduction'),
    },
  ];
});

onMounted(async () => {
  const data = await getUserInfoApi();
  profileBaseSettingRef.value.getFormApi().setValues(data);
});
</script>
<template>
  <ProfileBaseSetting ref="profileBaseSettingRef" :form-schema="formSchema" />
</template>
