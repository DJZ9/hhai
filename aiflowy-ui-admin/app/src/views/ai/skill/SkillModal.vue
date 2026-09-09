<script setup lang="ts">
import type { FormInstance, UploadUserFile } from 'element-plus';

import { ref } from 'vue';

import { useAppConfig } from '@aiflowy/hooks';
import { $t } from '@aiflowy/locales';
import { useAccessStore } from '@aiflowy/stores';

import { UploadFilled } from '@element-plus/icons-vue';
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElUpload,
} from 'element-plus';

import { api } from '#/api/request';
import UploadAvatar from '#/components/upload/UploadAvatar.vue';

const emit = defineEmits(['reload']);

const accessStore = useAccessStore();
const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

const headers = {
  'aiflowy-token': accessStore.accessToken,
};

const saveForm = ref<FormInstance>();
const dialogVisible = ref(false);
const isAdd = ref(true);
const btnLoading = ref(false);
const uploadLoading = ref(false);

const categoryList = ref<any[]>([]);

const defaultEntity = {
  id: '',
  title: '',
  name: '',
  englishName: '',
  categoryId: '',
  description: '',
  icon: '',

  author: '',
  version: '',
  skillPath: '',
  fileCount: 0,
  skillSize: 0,
};

const entity = ref<any>({ ...defaultEntity });
const fileList = ref<UploadUserFile[]>([]);

const rules = ref({
  title: [{ required: true, message: $t('message.required'), trigger: 'blur' }],
  englishName: [
    { required: true, message: $t('message.required'), trigger: 'blur' },
    {
      pattern: /^[\w-]{1,64}$/,
      message: $t('common.englishNameRule'),
      trigger: 'blur',
    },
  ],
});

async function openDialog(row: any = {}) {
  // 获取分类列表
  await getCategoryList();

  if (row.id) {
    isAdd.value = false;
    entity.value = { ...defaultEntity, ...row };
    fileList.value = [];
  } else {
    isAdd.value = true;
    entity.value = { ...defaultEntity };
    fileList.value = [];
  }
  dialogVisible.value = true;
}

async function getCategoryList() {
  try {
    const res = await api.get('/api/v1/skillCategory/list');
    if (res.errorCode === 0) {
      categoryList.value = res.data;
    }
  } catch (error) {
    console.error('获取分类列表失败：', error);
  }
}

const handleUploadChange = (
  uploadFile: UploadUserFile,
  uploadFiles: UploadUserFile[],
) => {
  fileList.value = uploadFiles.slice(-1);
};

const handleUploadSuccess = (response: any) => {
  uploadLoading.value = false;

  // 如果响应是字符串，尝试解析为 JSON
  let result = response;
  if (typeof response === 'string') {
    try {
      result = JSON.parse(response);
    } catch {
      ElMessage.error('响应格式错误');
      return;
    }
  }

  // 使用宽松比较，兼容数字和字符串类型的 errorCode
  if (result.errorCode === 0 || result.errorCode === '0') {
    // 用解析出的元数据填充表单（包含 skillPath 等临时路径信息）
    const parsedData = result.data || {};

    // 直接赋值，确保所有字段都被正确设置
    entity.value.name = parsedData.name || '';
    entity.value.englishName = parsedData.englishName || '';
    entity.value.author = parsedData.author || '';
    entity.value.version = parsedData.version || '';
    entity.value.description = parsedData.description || '';
    // title 如果解析出来就用解析的，否则保持空让用户填写
    if (parsedData.title) {
      entity.value.title = parsedData.title;
    }
    // 保存临时路径信息（保存时必须）
    entity.value.skillPath = parsedData.skillPath || '';
    entity.value.fileCount = parsedData.fileCount || 0;
    entity.value.skillSize = parsedData.skillSize || 0;

    ElMessage.success(
      $t('skill.upload.parseSuccess') || '解析成功，请确认信息后保存',
    );
  } else {
    ElMessage.error(result.message || $t('message.upload.fail') || '解析失败');
  }
};

const handleUploadError = () => {
  uploadLoading.value = false;
  ElMessage.error($t('message.upload.fail') || '上传失败');
};

const handleBeforeUpload = (file: File) => {
  const isZip = file.name.endsWith('.zip');
  if (!isZip) {
    ElMessage.error($t('skill.upload.zipOnly') || '只能上传 ZIP 文件');
    return false;
  }
  uploadLoading.value = true;
  return true;
};

async function save() {
  try {
    const valid = await saveForm.value?.validate();
    if (!valid) return;

    btnLoading.value = true;

    // 构建 multipart/form-data（使用 fetch 直接发送，避免 api 包装器干扰 FormData）
    const formData = new FormData();
    formData.append(
      'skill',
      new Blob([JSON.stringify(entity.value)], { type: 'application/json' }),
    );

    if (fileList.value.length > 0 && fileList?.value[0].raw) {
      formData.append('file', fileList?.value[0].raw);
    }

    const response = await fetch(`${apiURL}/api/v1/skill/save`, {
      method: 'POST',
      headers: {
        'aiflowy-token': accessStore.accessToken,
      },
      body: formData,
    });

    const result = await response.json();
    if (result.errorCode === 0) {
      ElMessage.success(result.message || $t('message.saveOkMessage'));
      emit('reload');
      closeDialog();
    } else {
      ElMessage.error(result.message || $t('message.saveFailMessage'));
    }
  } catch (error) {
    ElMessage.error($t('message.saveFailMessage'));
    console.error('保存失败：', error);
  } finally {
    btnLoading.value = false;
  }
}

function closeDialog() {
  // 注意：不要调用 resetFields()，因为它会将表单重置到初始值
  // 导致上传时设置的 skillPath 等字段被清空
  isAdd.value = true;
  entity.value = { ...defaultEntity };
  fileList.value = [];
  dialogVisible.value = false;
}

defineExpose({
  openDialog,
});
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="isAdd ? $t('skill.actions.addSkill') : $t('button.edit')"
    :before-close="closeDialog"
    :close-on-click-modal="false"
    align-center
    width="600px"
  >
    <ElForm
      label-width="100px"
      ref="saveForm"
      :model="entity"
      status-icon
      :rules="rules"
    >
      <ElFormItem
        prop="icon"
        :label="$t('skill.icon')"
        style="display: flex; align-items: center"
      >
        <UploadAvatar v-model="entity.icon" />
      </ElFormItem>

      <!-- ZIP 文件上传（新增必传，编辑可选重新上传） -->
      <ElFormItem :label="$t('skill.upload.file')" :required="isAdd">
        <ElUpload
          class="upload-demo w-full"
          drag
          v-model:file-list="fileList"
          :headers="headers"
          :action="`${apiURL}/api/v1/skill/parse`"
          :on-change="handleUploadChange"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :before-upload="handleBeforeUpload"
          accept=".zip"
          :limit="1"
        >
          <ElIcon size="48" color="hsl(var(--primary))">
            <UploadFilled />
          </ElIcon>
          <div class="flex flex-col gap-1">
            <span class="text-base">{{
                $t('skill.upload.title') || '上传 Skill ZIP 文件'
              }}</span>
            <span class="text-sm text-[#75808d]">{{
                isAdd
                  ? $t('skill.upload.description') ||
                  '将 ZIP 文件拖拽到此处，或点击上传'
                  : '可选：上传新的 ZIP 文件替换旧的 Skill 包'
              }}</span>
          </div>
        </ElUpload>
      </ElFormItem>

      <ElFormItem prop="title" :label="$t('skill.title')">
        <ElInput
          v-model.trim="entity.title"
          :placeholder="$t('skill.placeholder.title') || '请输入 Skill 标题'"
        />
      </ElFormItem>

      <ElFormItem prop="englishName" :label="$t('skill.englishName')">
        <ElInput
          v-model.trim="entity.englishName"
          :placeholder="$t('skill.placeholder.englishName') || '请输入英文名称'"
        />
      </ElFormItem>

      <ElFormItem prop="categoryId" :label="$t('skill.categoryId')">
        <ElSelect
          v-model="entity.categoryId"
          :placeholder="$t('skill.placeholder.category') || '请选择分类'"
          class="w-full"
        >
          <ElOption
            v-for="item in categoryList"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </ElSelect>
      </ElFormItem>

      <ElFormItem prop="version" :label="$t('skill.version')">
        <ElInput
          v-model.trim="entity.version"
          :placeholder="$t('skill.placeholder.version') || '请输入版本号'"
        />
      </ElFormItem>

      <ElFormItem prop="author" :label="$t('skill.author')">
        <ElInput
          v-model.trim="entity.author"
          :placeholder="$t('skill.placeholder.author') || '请输入作者'"
        />
      </ElFormItem>

      <ElFormItem prop="description" :label="$t('skill.description')">
        <ElInput
          v-model.trim="entity.description"
          :rows="4"
          type="textarea"
          :placeholder="$t('skill.placeholder.description') || '请输入描述'"
        />
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
        :disabled="btnLoading || uploadLoading"
      >
        {{ $t('button.save') }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.upload-demo {
  width: 100%;
}

.upload-demo :deep(.el-upload) {
  width: 100%;
}

.upload-demo :deep(.el-upload-dragger) {
  width: 100%;
}
</style>
