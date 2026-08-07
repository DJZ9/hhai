<script setup lang="ts">
import type { FilesCardProps } from 'vue-element-plus-x/types/FilesCard';

import { defineExpose, defineProps, nextTick, ref, watch } from 'vue';
import { Attachments } from 'vue-element-plus-x';

import { ElMessage } from 'element-plus';

import { $t } from '#/locales';
import { api } from '#/api/request';

const props = defineProps({
  maxSize: {
    type: Number,
    default: 2,
  },
  action: {
    type: String,
    default: '/api/v1/commons/upload',
  },
  externalFiles: {
    type: Array as () => File[],
    default: () => [],
  },
});

const emit = defineEmits(['deleteAll']);
type SelfFilesCardProps = {
  fileSize: number;
  id?: number;
  name: string;
  uid: number | string;
  url: string; // 上传后的文件地址
} & FilesCardProps;

const files = ref<SelfFilesCardProps[]>([]);
/**
 * 上传前校验
 */
function handleBeforeUpload(file: File) {
  const maxSizeBytes = props.maxSize * 1024 * 1024;
  if (file.size > maxSizeBytes) {
    ElMessage.error($t('upload.fileSizeLimit', { size: props.maxSize }));
    return false;
  }
  return true;
}

/**
 * 拖拽上传处理
 */
async function handleUploadDrop(dropFiles: File[]) {
  if (dropFiles?.length) {
    if (dropFiles[0]?.type === '') {
      ElMessage.error($t('upload.noFolder'));
      return false;
    }
    for (const file of dropFiles) {
      if (handleBeforeUpload(file)) {
        await handleHttpRequest({ file });
      }
    }
  }
}

/**
 * 自定义上传请求
 */
async function handleHttpRequest(options: { file: File }) {
  const { file } = options;
  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await api.upload(props.action, { file }, {});

    const fileUrl = res.data.path;
    const fileItem: SelfFilesCardProps = {
      id: files.value.length,
      uid: Date.now() + Math.random(),
      name: file.name,
      fileSize: file.size,
      url: fileUrl,
      showDelIcon: true,
      imgVariant: 'square',
    };

    files.value.push(fileItem);
  } catch (error) {
    ElMessage.error($t('upload.uploadFailedName', { name: file.name }));
    console.error('上传失败:', error);
  }
}

/**
 * 批量上传外部文件（父组件传递的文件数组）
 */
async function uploadExternalFiles(fileList: File[]) {
  if (fileList.length === 0) return;

  for (const file of fileList) {
    if (handleBeforeUpload(file)) {
      await handleHttpRequest({ file });
    }
  }
}

/**
 * 组件内部完成删除逻辑
 */
function handleDeleteCard(item: FilesCardProps) {
  const targetItem = item as SelfFilesCardProps;
  files.value = files.value.filter((file) => file.id !== targetItem.id);
  if (files.value.length === 0) {
    emit('deleteAll');
  }
}
/**
 * 暴露给父组件的核心方法：仅返回URL字符串数组
 * @returns string[] 纯文件地址列表
 */
function getFileList(): string[] {
  return files.value.map((file) => file.url);
}

watch(
  () => props.externalFiles,
  async (newFiles) => {
    if (newFiles.length > 0) {
      await nextTick();
      await uploadExternalFiles(newFiles);
    }
  },
  { deep: true, immediate: true },
);

defineExpose({
  getFileList,
  clearFiles() {
    files.value = [];
  },
});
</script>

<template>
  <div style="display: flex; flex-direction: column; gap: 12px">
    <Attachments
      :http-request="handleHttpRequest"
      :items="files"
      drag
      :before-upload="handleBeforeUpload"
      :hide-upload="false"
      @upload-drop="handleUploadDrop"
      @delete-card="handleDeleteCard"
    />
  </div>
</template>

<style scoped lang="less"></style>
