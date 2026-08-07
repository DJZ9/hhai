<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { IconifyIcon } from '@aiflowy/icons';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSelect,
} from 'element-plus';
import { tryit } from 'radash';

import Upload from '#/components/upload/Upload.vue';

import { api } from './api/request';
import { $t } from '#/locales';

const options = [
  {
    value: 1,
    label: $t('issueFeedback.malfunction'),
  },
  {
    value: 2,
    label: $t('issueFeedback.suggestion'),
  },
  {
    value: 3,
    label: $t('issueFeedback.accountIssue'),
  },
  {
    value: 4,
    label: $t('issueFeedback.other'),
  },
];

const route = useRoute();
const showDialog = ref(false);
const formRef = ref<FormInstance>();
const formData = reactive({
  feedbackType: '',
  feedbackContent: '',
  contactInfo: '',
  attachmentUrl: '',
});
const loading = ref(false);
const uploadRef = ref();

function uploadSuccess(res: any) {
  formData.attachmentUrl = res;
}
function onRemove() {
  formData.attachmentUrl = '';
}
const handleSubmit = async () => {
  const isValid = await formRef.value?.validate();

  if (isValid) {
    loading.value = true;

    const [, res] = await tryit(api.post)(
      '/userCenter/sysUserFeedback/save',
      formData,
    );

    if (res && res.errorCode === 0) {
      ElMessage.success($t('issueFeedback.success'));
      showDialog.value = false;
      formRef.value?.resetFields();
      uploadRef.value?.clear();
    }
    loading.value = false;
  }
};
</script>

<template>
  <Teleport
    v-if="!route.path.includes('auth') && !route.path.includes('share')"
    to="#app"
  >
    <div
      class="issue-feedback-btn fixed bottom-1 right-2 cursor-pointer text-6xl active:opacity-70"
      @click="showDialog = !showDialog"
    >
      <IconifyIcon icon="svg:issue" />
    </div>
  </Teleport>
  <ElDialog
    draggable
    v-model="showDialog"
    :title="$t('issueFeedback.title')"
    style="max-width: 560px"
  >
    <ElForm
      ref="formRef"
      :model="formData"
      label-width="90px"
      label-position="left"
      require-asterisk-position="right"
    >
      <ElFormItem
        prop="feedbackType"
        :label="$t('issueFeedback.feedbackType')"
        :rules="[{ required: true, message: $t('issueFeedback.required') }]"
      >
        <ElSelect
          :options="options"
          v-model="formData.feedbackType"
          :placeholder="$t('issueFeedback.selectType')"
        />
      </ElFormItem>
      <ElFormItem
        prop="feedbackContent"
        :label="$t('issueFeedback.feedbackContent')"
        :rules="[{ required: true, message: $t('issueFeedback.required') }]"
      >
        <ElInput
          type="textarea"
          :rows="5"
          v-model="formData.feedbackContent"
          :placeholder="$t('issueFeedback.describeIssue')"
        />
      </ElFormItem>
      <ElFormItem
        prop="contactInfo"
        :label="$t('issueFeedback.contactInfo')">
        <ElInput
          v-model="formData.contactInfo"
          :placeholder="$t('issueFeedback.contactPlaceholder')"
        />
      </ElFormItem>
      <ElFormItem
        prop="attachmentUrl"
        :label="$t('issueFeedback.uploadAttachment')">
        <Upload
          ref="uploadRef"
          @success="uploadSuccess"
          @handle-delete="onRemove"
        />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="showDialog = false">{{ $t('button.cancel') }}</ElButton>
        <ElButton type="primary" :loading="loading" @click="handleSubmit">
          {{ $t('button.submitFeedback') }}
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<style scoped>
@media (width <= 768px) {
  .issue-feedback-btn {
    display: none !important;
  }
}
</style>
