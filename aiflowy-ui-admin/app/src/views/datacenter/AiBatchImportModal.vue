<script setup lang="ts">
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus';

import { computed, ref } from 'vue';

import { useAppConfig } from '@aiflowy/hooks';
import { useAccessStore } from '@aiflowy/stores';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElProgress,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag,
  ElUpload,
} from 'element-plus';

import { api } from '#/api/request';
import uploadIcon from '#/assets/datacenter/upload.png';
import { $t } from '#/locales';

const props = defineProps<Props>();
const emit = defineEmits(['reload', 'openImportRecord']);
const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const accessStore = useAccessStore();

interface Props {
  categories: any[];
}

defineExpose({
  openDialog,
});

const dialogVisible = ref(false);
const categoryId = ref<string>('');
const fileList = ref<any>([]);
const uploadedFilesMap = ref<Map<string | number, string>>(new Map()); // 存储 uid -> path 的映射
const previewData = ref<any[]>([]);
const activeTab = ref('upload');
const uploadLoading = ref(false);
const confirmLoading = ref(false);
const importProgress = ref(0);
const taskId = ref<string>('');
const pollTimer = ref<any>(null);
const progressMessage = ref<string>('');

// 上传请求头
const headers = computed(() => ({
  'aiflowy-token': accessStore.accessToken || '',
}));

function openDialog() {
  dialogVisible.value = true;
  resetData();
}

function closeDialog() {
  dialogVisible.value = false;
  resetData();
}

function resetData() {
  categoryId.value = '';
  fileList.value = [];
  uploadedFilesMap.value.clear();
  previewData.value = [];
  activeTab.value = 'upload';
  uploadLoading.value = false;
  confirmLoading.value = false;
  importProgress.value = 0;
  taskId.value = '';
  progressMessage.value = '';
  stopPolling();
}

// 文件变化回调
function handleFileChange(uploadFile: UploadFile, uploadFiles: UploadFiles) {
  fileList.value = uploadFiles;
}

// 上传成功回调
function handleUploadSuccess(
  response: any,
  uploadFile: UploadFile,
  uploadFiles: UploadFiles,
) {
  if (response.errorCode === 0 && response.data?.path) {
    uploadedFilesMap.value.set(uploadFile.uid, response.data.path);
  } else {
    ElMessage.error(
      `${uploadFile.name} 上传失败: ${response.message || '未知错误'}`,
    );
  }
}

// 上传失败回调
function handleUploadError(
  error: Error,
  uploadFile: UploadFile,
  uploadFiles: UploadFiles,
) {
  ElMessage.error(`${uploadFile.name} 上传失败: ${error.message}`);
}

// 移除文件回调
function handleRemove(uploadFile: UploadFile, uploadFiles: UploadFiles) {
  fileList.value = uploadFiles;
  uploadedFilesMap.value.delete(uploadFile.uid);
}

async function handlePreview() {
  if (fileList.value.length === 0) {
    ElMessage.warning($t('datacenterTable.uploadDesc'));
    return;
  }

  // 检查是否有未上传完成的文件
  const pendingFiles = fileList.value.filter((f: UploadFile) => f.status !== 'success');
  if (pendingFiles.length > 0) {
    ElMessage.warning('有文件正在上传中，请等待上传完成');
    return;
  }

  // 检查是否有上传失败的文件
  const failedFiles = fileList.value.filter(
    (f: UploadFile) => !uploadedFilesMap.value.has(f.uid),
  );
  if (failedFiles.length > 0) {
    ElMessage.error('有文件上传失败，请重新上传');
    return;
  }

  uploadLoading.value = true;

  try {
    // 构建已上传的文件列表
    const uploadedFiles: { originalFileName: string; path: string }[] = [];
    for (const file of fileList.value) {
      const path = uploadedFilesMap.value.get(file.uid);
      if (path) {
        uploadedFiles.push({
          path,
          originalFileName: file.name,
        });
      }
    }

    // 创建解析任务
    const res = await api.post('/api/v1/datacenterTable/createParseTask', {
      files: uploadedFiles,
      categoryId: categoryId.value || null,
    });

    if (res.errorCode === 0) {
      taskId.value = res.data;
      importProgress.value = 0;
      progressMessage.value = '解析任务已创建，正在后台处理...';
      // 开始轮询任务状态
      startPolling();
      ElMessage.success(progressMessage.value);
    }
  } catch (error: any) {
    uploadLoading.value = false;
    ElMessage.error(error.message || $t('message.error'));
  }
}

// 轮询任务状态
function startPolling() {
  stopPolling();
  pollTimer.value = setInterval(async () => {
    try {
      const res = await api.get('/api/v1/datacenterTable/getParseTaskStatus', {
        params: { taskId: taskId.value },
      });

      if (res.errorCode === 0) {
        const { status, progress, message, error } = res.data;
        importProgress.value = progress || 0;
        progressMessage.value = message || '';

        if (status === 1) {
          // 成功
          stopPolling();
          await fetchTaskResult();
        } else if (status === 2) {
          // 失败
          stopPolling();
          uploadLoading.value = false;
          ElMessage.error(error || '解析失败');
        }
      }
    } catch {
      stopPolling();
      uploadLoading.value = false;
      ElMessage.error('查询任务状态失败');
    }
  }, 5000);
}

// 停止轮询
function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

// 获取任务结果
async function fetchTaskResult() {
  try {
    const res = await api.get('/api/v1/datacenterTable/getParseTaskResult', {
      params: { taskId: taskId.value },
    });

    if (res.errorCode === 0) {
      previewData.value = res.data || [];
      activeTab.value = 'preview';
      uploadLoading.value = false;
      ElMessage.success($t('datacenterTable.aiGenerating'));
    } else {
      uploadLoading.value = false;
      ElMessage.error(res.message || '获取解析结果失败');
    }
  } catch (error: any) {
    uploadLoading.value = false;
    ElMessage.error(error.message || '获取解析结果失败');
  }
}

function handleTableNameChange(index: number, value: string) {
  previewData.value[index].tableName = value;
}

function handleTableDescChange(index: number, value: string) {
  previewData.value[index].tableDesc = value;
}

function handleFieldNameChange(
  tableIndex: number,
  fieldIndex: number,
  value: string,
) {
  previewData.value[tableIndex].fields[fieldIndex].fieldName = value;
}

function handleFieldDescChange(
  tableIndex: number,
  fieldIndex: number,
  value: string,
) {
  previewData.value[tableIndex].fields[fieldIndex].fieldDesc = value;
}

async function handleConfirmImport() {
  if (previewData.value.length === 0) {
    ElMessage.warning('没有可导入的数据');
    return;
  }

  // 校验表名长度不超过43个字符
  for (const table of previewData.value) {
    if (table.tableName && table.tableName.length > 43) {
      ElMessage.error(
        `表"${table.tableName}"名称长度超过43个字符，请修改后重新提交`,
      );
      return;
    }
  }

  confirmLoading.value = true;

  // 构建确认数据
  const confirmList = previewData.value.map((table) => ({
    taskId: taskId.value,
    originalFileName: table.originalFileName,
    tableName: table.tableName,
    tableDesc: table.tableDesc,
    categoryId:
      table.categoryId || (categoryId.value ? BigInt(categoryId.value) : null),
    fields: table.fields.map((field: any) => ({
      originalName: field.originalName,
      fieldName: field.fieldName,
      fieldDesc: field.fieldDesc,
      fieldType: field.fieldType,
    })),
    tempFilePath: table.tempFilePath,
  }));

  try {
    const res = await api.post(
      '/api/v1/datacenterTable/confirmImportTables',
      confirmList,
    );
    if (res.errorCode === 0) {
      closeDialog();
      emit('reload');
      emit('openImportRecord');
    } else {
      ElMessage.error(res.message || $t('message.error'));
    }
  } catch (error: any) {
    ElMessage.error(error.message || $t('message.error'));
  } finally {
    confirmLoading.value = false;
  }
}
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="$t('button.aiImport')"
    :before-close="closeDialog"
    :close-on-click-modal="false"
    width="900px"
  >
    <ElTabs v-model="activeTab">
      <ElTabPane
        :label="$t('datacenterTable.uploadMultipleFiles')"
        name="upload"
      >
        <ElForm label-width="100px">
          <ElFormItem :label="$t('datacenterTable.selectCategory')">
            <ElSelect
              v-model="categoryId"
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
              v-model:file-list="fileList"
              drag
              :action="`${apiURL}/api/v1/commons/uploadTemp`"
              :headers="headers"
              accept=".xlsx,.xls,.csv"
              :auto-upload="true"
              :on-change="handleFileChange"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :on-remove="handleRemove"
              multiple
            >
              <div class="flex flex-col items-center">
                <img alt="" :src="uploadIcon" class="h-12 w-12" />
                <div class="text-base font-medium">
                  {{ $t('datacenterTable.uploadTitle') }}
                </div>
                <div class="desc text-[13px]">
                  {{ $t('datacenterTable.uploadMultipleFiles') }}
                </div>
              </div>
            </ElUpload>
          </ElFormItem>
          <!-- 解析进度显示 -->
          <ElFormItem
            v-if="uploadLoading && importProgress > 0"
            label-width="0"
          >
            <div class="w-full">
              <ElProgress :percentage="importProgress" :stroke-width="8" />
              <div class="mt-2 text-sm text-gray-500">
                {{ progressMessage }}
              </div>
            </div>
          </ElFormItem>
        </ElForm>
      </ElTabPane>

      <ElTabPane
        :label="$t('datacenterTable.previewTitle')"
        name="preview"
        :disabled="previewData.length === 0"
      >
        <div
          v-for="(table, tableIndex) in previewData"
          :key="tableIndex"
          class="mb-6"
        >
          <div class="mb-3 flex items-center gap-4">
            <ElTag type="info">{{ table.originalFileName }}</ElTag>
            <ElTag type="success">
              {{ $t('datacenterTable.totalNum') }}:
              {{ table.totalRows }}
            </ElTag>
          </div>

          <ElForm label-width="100px" class="mb-3">
            <ElFormItem :label="$t('datacenterTable.tableName')">
              <ElInput
                v-model="table.tableName"
                :placeholder="$t('datacenterTable.tableName')"
                maxlength="43"
                show-word-limit
                @change="(value) => handleTableNameChange(tableIndex, value)"
              />
            </ElFormItem>
            <ElFormItem :label="$t('datacenterTable.tableDesc')">
              <ElInput
                type="textarea"
                :rows="2"
                v-model="table.tableDesc"
                :placeholder="$t('datacenterTable.tableDesc')"
                @change="(value) => handleTableDescChange(tableIndex, value)"
              />
            </ElFormItem>
          </ElForm>

          <ElTable :data="table.fields" border size="small">
            <ElTableColumn
              prop="originalName"
              :label="$t('datacenterTable.originalName')"
              width="150"
            />
            <ElTableColumn :label="$t('datacenterTable.fieldName')" width="150">
              <template #default="{ row, $index }">
                <ElInput
                  v-model="row.fieldName"
                  size="small"
                  @change="
                    (value) => handleFieldNameChange(tableIndex, $index, value)
                  "
                />
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('datacenterTable.fieldDesc')" width="150">
              <template #default="{ row, $index }">
                <ElInput
                  v-model="row.fieldDesc"
                  size="small"
                  @change="
                    (value) => handleFieldDescChange(tableIndex, $index, value)
                  "
                />
              </template>
            </ElTableColumn>
          </ElTable>

          <!-- 示例数据 -->
          <div
            v-if="table.sampleData && table.sampleData.length > 0"
            class="mt-3"
          >
            <div class="mb-2 text-sm text-gray-500">
              {{ $t('datacenterTable.sampleData') }}:
            </div>
            <ElTable
              :data="table.sampleData"
              border
              size="small"
              max-height="200"
            >
              <ElTableColumn
                v-for="field in table.fields"
                :key="field.fieldName"
                :prop="field.originalName"
                :label="field.fieldName"
              />
            </ElTable>
          </div>
        </div>
      </ElTabPane>
    </ElTabs>

    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="closeDialog">
          {{ $t('button.cancel') }}
        </ElButton>
        <ElButton
          v-if="activeTab === 'upload'"
          :disabled="uploadLoading || fileList.length === 0"
          :loading="uploadLoading"
          type="primary"
          @click="handlePreview"
        >
          {{ $t('datacenterTable.previewTitle') }}
        </ElButton>
        <ElButton
          v-if="activeTab === 'preview'"
          :disabled="confirmLoading"
          :loading="confirmLoading"
          type="primary"
          @click="handleConfirmImport"
        >
          {{ $t('datacenterTable.confirmImport') }}
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
