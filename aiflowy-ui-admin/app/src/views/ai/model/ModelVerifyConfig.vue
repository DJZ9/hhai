<script setup lang="ts">
import { reactive, ref } from 'vue';

import { useAppConfig } from '@aiflowy/hooks';
import { useAccessStore } from '@aiflowy/stores';

import { Upload } from '@element-plus/icons-vue';
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElMessage,
  ElOption,
  ElSelect,
  ElUpload,
} from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const accessStore = useAccessStore();
const headers = ref({
  'aiflowy-token': accessStore.accessToken,
});

const options = ref<any[]>([]);
const getLlmList = (providerId: string) => {
  api.get(`/api/v1/model/list?providerId=${providerId}`, {}).then((res) => {
    if (res.errorCode === 0) {
      options.value = res.data;
    }
  });
};
const modelType = ref('');
const vectorDimension = ref('');
const formDataRef = ref();
const dialogVisible = ref(false);
defineExpose({
  openDialog(providerId: string) {
    formDataRef.value?.resetFields();
    modelType.value = '';
    vectorDimension.value = '';
    verifyStatus.value = 'idle';
    testFileUrl.value = '';
    getLlmList(providerId);
    dialogVisible.value = true;
  },
});
const formData = reactive({
  llmId: '',
});

const rules = {
  llmId: [
    {
      required: true,
      message: $t('message.required'),
      trigger: 'change',
    },
  ],
};

const testFileUrl = ref('');
const uploadLoading = ref(false);

// 文件上传成功处理
const handleUploadSuccess = (response: any, file: any) => {
  if (response.errorCode === 0) {
    testFileUrl.value = response.data.path || response.data;
    ElMessage.success($t('message.uploadSuccess'));
  } else {
    ElMessage.error(response.message || $t('message.uploadFailed'));
  }
  uploadLoading.value = false;
};

// 文件上传前处理
const beforeUpload = (file: File) => {
  const allowedTypes = [
    // PDF
    'application/pdf',
    // Word
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    // PowerPoint
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    // Excel
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    // 图片
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/bmp',
    'image/jp2',
    // HTML
    'text/html',
  ];
  const isAllowed = allowedTypes.includes(file.type);
  const isLt10M = file.size / 1024 / 1024 < 10;

  if (!isAllowed) {
    ElMessage.error($t('llm.uploadFileTypeError'));
    return false;
  }
  if (!isLt10M) {
    ElMessage.error($t('llm.uploadFileSizeError'));
    return false;
  }
  uploadLoading.value = true;
  return true;
};

const save = async () => {
  btnLoading.value = true;
  verifyStatus.value = 'idle';
  await formDataRef.value.validate();

  // OCR 模型需要先上传测试文件
  if (modelType.value === 'ocrModel') {
    if (!testFileUrl.value) {
      ElMessage.error($t('llm.testFileRequired'));
      btnLoading.value = false;
      return;
    }
    // 调用 OCR 验证 API
    api
      .get(
        `/api/v1/model/verifyOcrModelConfig?id=${formData.llmId}&fileUrl=${testFileUrl.value}`,
        {},
      )
      .then((res) => {
        if (res.errorCode === 0) {
          ElMessage.success($t('llm.testSuccess'));
          verifyStatus.value = 'success';
        } else {
          verifyStatus.value = 'failed';
        }
        btnLoading.value = false;
      })
      .catch(() => {
        verifyStatus.value = 'failed';
        btnLoading.value = false;
      });
  } else {
    // 其他模型验证
    api
      .get(`/api/v1/model/verifyLlmConfig?id=${formData.llmId}`, {})
      .then((res) => {
        if (res.errorCode === 0) {
          ElMessage.success($t('llm.testSuccess'));
          verifyStatus.value = 'success';
          if (modelType.value === 'embeddingModel' && res?.data?.dimension) {
            vectorDimension.value = res?.data?.dimension;
          }
        } else {
          verifyStatus.value = 'failed';
        }
        btnLoading.value = false;
      })
      .catch(() => {
        verifyStatus.value = 'failed';
        btnLoading.value = false;
      });
  }
};

const btnLoading = ref(false);
const verifyStatus = ref<'idle' | 'success' | 'failed'>('idle');
const closeDialog = () => {
  dialogVisible.value = false;
};
const getModelInfo = (id: string) => {
  options.value.forEach((item: any) => {
    if (item.id === id) {
      modelType.value = item.modelType;
    }
  });
};
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="$t('llm.verifyLlmTitle')"
    :close-on-click-modal="false"
    align-center
    width="482"
  >
    <ElForm ref="formDataRef" :model="formData" status-icon :rules="rules">
      <ElFormItem prop="llmId" :label="$t('llm.modelToBeTested')">
        <ElSelect v-model="formData.llmId" @change="getModelInfo">
          <ElOption
            v-for="item in options"
            :key="item.id"
            :label="item.title"
            :value="item.id || ''"
          />
        </ElSelect>
      </ElFormItem>

      <!-- OCR 模型文件上传 -->
      <ElFormItem v-if="modelType === 'ocrModel'" :label="$t('llm.testFile')" required>
        <ElAlert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        >
          {{ $t('llm.testFileTip') }}
        </ElAlert>
        <ElUpload
          :action="`${apiURL}/api/v1/commons/uploadTemp`"
          :headers="headers"
          :on-success="handleUploadSuccess"
          :before-upload="beforeUpload"
          :show-file-list="true"
          :limit="1"
        >
          <ElButton type="primary" :icon="Upload" :loading="uploadLoading">
            {{ $t('llm.uploadButton') }}
          </ElButton>
        </ElUpload>
      </ElFormItem>

      <ElFormItem
        v-if="modelType === 'embeddingModel' && vectorDimension"
        :label="$t('documentCollection.dimensionOfVectorModel')"
        label-width="100px"
      >
        {{ vectorDimension }}
      </ElFormItem>

      <!-- 验证结果显示 -->
      <ElFormItem v-if="verifyStatus === 'success'">
        <ElAlert type="success" :closable="false" show-icon>
          {{ $t('llm.verifySuccess') }}
        </ElAlert>
      </ElFormItem>
      <ElFormItem v-if="verifyStatus === 'failed'">
        <ElAlert type="error" :closable="false" show-icon>
          {{ $t('llm.verifyFailed') }}
        </ElAlert>
      </ElFormItem>
    </ElForm>
    <template #footer>
      <ElButton @click="closeDialog">
        {{ $t('button.cancel') }}
      </ElButton>
      <ElButton
        type="primary"
        @click="save"
        :loading="btnLoading"
        :disabled="btnLoading"
      >
        {{ $t('button.confirm') }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.headers-container-reduce {
  align-items: center;
}
.addHeadersBtn {
  width: 100%;
  border-style: dashed;
  border-color: var(--el-color-primary);
  border-radius: 8px;
  margin-top: 8px;
}
.head-con-content {
  margin-bottom: 8px;
  align-items: center;
}
</style>
