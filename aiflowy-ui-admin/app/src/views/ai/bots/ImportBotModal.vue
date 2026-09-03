<script setup lang="ts">
import type { FormInstance, UploadInstance, UploadProps } from 'element-plus';

import { computed, ref } from 'vue';

import { $t } from '@aiflowy/locales';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElUpload,
} from 'element-plus';
import { tryit } from 'radash';

import { api } from '#/api/request';
import DictSelect from '#/components/dict/DictSelect.vue';
import UploadAvatar from '#/components/upload/UploadAvatar.vue';

const emit = defineEmits(['success']);

const initialFormData = {
  icon: '',
  title: '',
  alias: '',
  description: '',
  categoryId: '',
  status: 1,
  scopeOfAuthorityDeptIds: [],
};

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const formData = ref(initialFormData);
const loading = ref(false);

// 导入相关变量
const jsonFile = ref<any>(null);
const uploadFileList = ref<any[]>([]);
const uploadRef = ref<UploadInstance>();

const jsonFileModel = computed({
  get: () => (uploadFileList.value.length > 0 ? uploadFileList.value[0] : null),
  set: (value: any) => {
    if (!value) {
      uploadFileList.value = [];
    }
  },
});

const rules = computed(() => ({
  jsonFile: [
    { required: true, message: $t('message.required'), trigger: 'change' },
  ],
  title: [{ required: true, message: $t('message.required'), trigger: 'blur' }],
  alias: [{ required: true, message: $t('message.required'), trigger: 'blur' }]
}));

defineExpose({
  open() {
    // 重置表单数据和上传文件
    formData.value = { ...initialFormData };
    uploadFileList.value = [];
    jsonFile.value = null;
    formRef.value?.resetFields();
    dialogVisible.value = true;
  },
});

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  jsonFile.value = file;
  uploadFileList.value = [file];
  formRef.value?.clearValidate('jsonFile');
  return false;
};

const handleChange: UploadProps['onChange'] = (file, fileList) => {
  jsonFile.value = file.raw;
  uploadFileList.value = fileList.slice(-1);
  formRef.value?.clearValidate('jsonFile');
};

const handleRemove: UploadProps['onRemove'] = () => {
  jsonFile.value = null;
  uploadFileList.value = [];
  formRef.value?.clearValidate('jsonFile');
};

const handleSubmit = async () => {
  // 触发表单校验
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (!jsonFile.value) {
    ElMessage.warning($t('bot.message.selectFile'));
    return;
  }

  loading.value = true;

  const formDataMultipart = new FormData();
  formDataMultipart.append('jsonFile', jsonFile.value);
  Object.keys(formData.value).forEach((key) => {
    const value = formData.value[key as keyof typeof formData.value];
    if (
      value !== null &&
      value !== undefined &&
      key !== 'scopeOfAuthorityDeptIds'
    ) {
      formDataMultipart.append(key, value as any);
    }
  });
  // 处理部门ID数组
  if (
    formData.value.scopeOfAuthorityDeptIds &&
    formData.value.scopeOfAuthorityDeptIds.length > 0
  ) {
    formData.value.scopeOfAuthorityDeptIds.forEach((deptId: any) => {
      formDataMultipart.append('scopeOfAuthorityDeptIds', deptId);
    });
  }

  const [err, res] = await tryit(api.post)(
    '/api/v1/bot/importBot',
    formDataMultipart,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  if (!err && res.errorCode === 0) {
    emit('success');
    ElMessage.success($t('message.saveOkMessage'));
    dialogVisible.value = false;
  }
  loading.value = false;
};

const closeDialog = () => {
  formRef.value?.resetFields();
  uploadRef.value?.clearFiles();
  uploadFileList.value = [];
  jsonFile.value = null;
  formData.value = { ...initialFormData };
  dialogVisible.value = false;
};
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    :title="$t('button.import')"
    draggable
    align-center
    :before-close="closeDialog"
    :close-on-click-modal="false"
  >
    <ElForm
      ref="formRef"
      :model="{ ...formData, jsonFile: jsonFileModel }"
      :rules="rules"
      label-width="150px"
    >
      <ElFormItem prop="jsonFile" :label="$t('bot.jsonFile')" required>
        <ElUpload
          class="w-full"
          ref="uploadRef"
          v-model:file-list="uploadFileList"
          :limit="1"
          :auto-upload="false"
          :on-change="handleChange"
          :before-upload="beforeUpload"
          :on-remove="handleRemove"
          accept=".json"
          drag
        >
          <div class="el-upload__text w-full">
            {{ $t('bot.dragJsonFile') }}<em>{{ $t('bot.clickUpload') }}</em>
          </div>
        </ElUpload>
      </ElFormItem>
      <ElFormItem :label="$t('common.avatar')" prop="icon">
        <UploadAvatar v-model="formData.icon" />
      </ElFormItem>
      <ElFormItem prop="categoryId" :label="$t('aiWorkflow.categoryId')">
        <DictSelect v-model="formData.categoryId" dict-code="aiBotCategory" />
      </ElFormItem>
      <ElFormItem :label="$t('aiWorkflow.title')" prop="title">
        <ElInput v-model="formData.title" />
      </ElFormItem>
      <ElFormItem :label="$t('plugin.alias')" prop="alias">
        <ElInput v-model="formData.alias" />
      </ElFormItem>
      <ElFormItem :label="$t('plugin.description')" prop="description">
        <ElInput type="textarea" :rows="3" v-model="formData.description" />
      </ElFormItem>
      <ElFormItem prop="status" :label="$t('aiWorkflow.status')">
        <DictSelect v-model="formData.status" dict-code="showOrNot" />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <ElButton @click="closeDialog">
        {{ $t('button.cancel') }}
      </ElButton>
      <ElButton
        type="primary"
        :loading="loading"
        :disabled="loading"
        @click="handleSubmit"
      >
        {{ $t('button.save') }}
      </ElButton>
    </template>
  </ElDialog>
</template>
