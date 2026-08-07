<script setup lang="ts">
import { IconifyIcon } from '@aiflowy/icons';

import { Upload } from '@element-plus/icons-vue';
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElUpload,
} from 'element-plus';

const options = [
  {
    value: 'Option1',
    label: 'Option1',
  },
  {
    value: 'Option2',
    label: 'Option2',
  },
  {
    value: 'Option3',
    label: 'Option3',
  },
  {
    value: 'Option4',
    label: 'Option4',
  },
  {
    value: 'Option5',
    label: 'Option5',
  },
];
</script>

<template>
  <div class="flex flex-1 flex-col justify-between">
    <ElForm>
      <ElFormItem required :label="$t('bot.labelInput')">
        <div class="flex w-full flex-col gap-2">
          <ElInput />
          <span class="text-xs text-[#969799]">{{ $t('bot.tipsPrompt') }}</span>
        </div>
      </ElFormItem>
      <ElFormItem required :label="$t('bot.labelIdash')">
        <ElUpload :auto-upload="false" list-type="picture" :limit="1">
          <ElButton :icon="Upload">{{ $t('bot.upload') }}</ElButton>
        </ElUpload>
      </ElFormItem>
      <ElFormItem required :label="$t('bot.labelIdash')">
        <ElSelect :options="options" />
      </ElFormItem>
    </ElForm>
    <ElButton color="#0066FF" class="!h-11">
      <template #icon>
        <IconifyIcon icon="mdi:play-circle" />
      </template>
      {{ $t('bot.startRun') }}
    </ElButton>
  </div>
</template>
