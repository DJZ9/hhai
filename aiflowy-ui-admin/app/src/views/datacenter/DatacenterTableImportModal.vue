<script setup lang="ts">
import type { FormInstance, UploadFile } from 'element-plus';

import { ref } from 'vue';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElUpload,
} from 'element-plus';

import { api } from '#/api/request';
import uploadIcon from '#/assets/datacenter/upload.png';
import { $t } from '#/locales';

interface Props {
  categories: any[];
}
const props = defineProps<Props>();

const emit = defineEmits(['reload']);
defineExpose({
  openDialog,
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const formData = ref({
  tableName: '',
  categoryId: '',
});
const rules = {
  tableName: [
    { required: true, message: $t('message.required'), trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: $t('datacenterTable.nameRegx'),
    },
  ],
};
const fileList = ref<any[]>([]);
const currentFile = ref<File | null>();
const btnLoading = ref(false);

function openDialog() {
  dialogVisible.value = true;
}
function closeDialog() {
  formRef.value?.resetFields();
  formData.value.categoryId = '';
  fileList.value = [];
  currentFile.value = null;
  dialogVisible.value = false;
}
function onFileChange(uploadFile: UploadFile) {
  currentFile.value = uploadFile.raw;
  return false;
}
function handleUpload() {
  formRef.value?.validate((valid) => {
    if (!valid) {
      return;
    }
    if (!currentFile.value) {
      ElMessage.warning($t('datacenterTable.uploadDesc'));
      return;
    }
    const data = new FormData();
    data.append('file', currentFile.value);
    data.append('tableName', formData.value.tableName);
    data.append('categoryId', formData.value.categoryId || '');
    btnLoading.value = true;
    api
      .postFile('/api/v1/datacenterTable/importTable', data)
      .then((res) => {
        btnLoading.value = false;
        if (res.errorCode === 0) {
          const arr: any[] = res.data.errorRows || [];
          let html = '';
          for (const element of arr) {
            html += `<p>${JSON.stringify(element)}</p><br>`;
          }
          closeDialog();
          ElMessageBox.alert(
            `<strong>${$t('datacenterTable.totalNum')}：</strong>${res.data.totalCount}
            <strong>${$t('datacenterTable.successNum')}：</strong>${res.data.successCount}
            <strong>${$t('datacenterTable.failNum')}：</strong>${res.data.errorCount}<br>
            <strong>${$t('datacenterTable.failList')}：</strong>${html}`,
            $t('datacenterTable.importComplete'),
            {
              confirmButtonText: $t('message.ok'),
              dangerouslyUseHTMLString: true,
              callback: () => {
                emit('reload');
              },
            },
          );
        }
      })
      .catch(() => {
        btnLoading.value = false;
      });
  });
}
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="`${$t('button.import')} Excel`"
    :before-close="closeDialog"
    :close-on-click-modal="false"
  >
    <ElForm ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <ElFormItem prop="tableName" :label="$t('datacenterTable.tableName')">
        <ElInput v-model.trim="formData.tableName" />
      </ElFormItem>
      <ElFormItem :label="$t('datacenterTable.selectCategory')">
        <ElSelect
          v-model="formData.categoryId"
          clearable
          :placeholder="$t('datacenterTable.selectCategory')"
          class="w-full"
        >
          <ElOption
            v-for="item in props.categories"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label-width="0">
        <ElUpload
          class="w-full"
          :file-list="fileList"
          drag
          action="#"
          accept=".xlsx,.xls,.csv"
          :auto-upload="false"
          :on-change="onFileChange"
          :limit="1"
        >
          <div class="flex flex-col items-center">
            <img alt="" :src="uploadIcon" class="h-12 w-12" />
            <div class="text-base font-medium">
              {{ $t('datacenterTable.uploadTitle') }}
            </div>
            <div class="desc text-[13px]">
              {{ $t('datacenterTable.uploadDesc') }}
            </div>
          </div>
        </ElUpload>
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="closeDialog">
          {{ $t('button.cancel') }}
        </ElButton>
        <ElButton
          :disabled="btnLoading"
          :loading="btnLoading"
          type="primary"
          @click="handleUpload"
        >
          {{ $t('button.confirm') }}
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<style scoped>
.desc {
  color: var(--el-text-color-secondary);
}
</style>
