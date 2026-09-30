<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

import { $t } from '@aiflowy/locales';

import {
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSlider,
  ElSwitch,
  ElTooltip,
} from 'element-plus';

const formRef = ref();
const form = reactive({
  fileType: 'doc',
  splitterName: 'SimpleDocumentSplitter',
  chunkSize: 512,
  overlapSize: 128,
  regex: '',
  rowsPerChunk: 5,
  includeHeader: true,
  mdSplitterLevel: 1,
});

const isExcelFile = computed(() => form.fileType === 'excel');

// 文件类型联动：Excel 表格锁定为 ExcelDocumentSplitter
watch(
  () => form.fileType,
  (val) => {
    if (val === 'excel') {
      form.splitterName = 'ExcelDocumentSplitter';
    } else if (form.splitterName === 'ExcelDocumentSplitter') {
      form.splitterName = 'SimpleDocumentSplitter';
    }
  },
);

const fileTypes = [
  {
    label: $t('documentCollection.splitterDoc.document'),
    value: 'doc',
  },
  {
    label: $t('documentCollection.splitterDoc.excel'),
    value: 'excel',
  },
];
const splitterNames = [
  {
    label: $t('documentCollection.splitterDoc.simpleDocumentSplitter'),
    value: 'SimpleDocumentSplitter',
  },
  {
    label: $t('documentCollection.splitterDoc.simpleTokenizeSplitter'),
    value: 'SimpleTokenizeSplitter',
  },
  {
    label: $t('documentCollection.splitterDoc.regexDocumentSplitter'),
    value: 'RegexDocumentSplitter',
  },
  {
    label: $t('documentCollection.splitterDoc.markdownHeaderSplitter'),
    value: 'MarkdownHeaderSplitter',
  },
];
const mdSplitterLevel = [
  {
    label: '#',
    value: 1,
  },
  {
    label: '##',
    value: 2,
  },
  {
    label: '###',
    value: 3,
  },
  {
    label: '####',
    value: 4,
  },
  {
    label: '#####',
    value: 5,
  },
  {
    label: '######',
    value: 6,
  },
];
const rules = {
  name: [
    { required: true, message: 'Please input Activity name', trigger: 'blur' },
  ],
  region: [
    {
      required: true,
      message: 'Please select Activity zone',
      trigger: 'change',
    },
  ],
};
defineExpose({
  getSplitterFormValues() {
    return form;
  },
});
</script>

<template>
  <div class="splitter-doc-container">
    <ElForm
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="auto"
      class="custom-form"
    >
      <ElFormItem
        :label="$t('documentCollection.splitterDoc.fileType')"
        prop="fileType"
      >
        <ElSelect v-model="form.fileType">
          <ElOption
            v-for="item in fileTypes"
            :key="item.value"
            v-bind="item"
            :label="item.label"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem
        :label="$t('documentCollection.splitterDoc.splitterName')"
        prop="splitterName"
        v-if="!isExcelFile"
      >
        <ElSelect v-model="form.splitterName">
          <ElOption
            v-for="item in splitterNames"
            :key="item.value"
            v-bind="item"
            :label="item.label"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem
        :label="$t('documentCollection.splitterDoc.chunkSize')"
        v-if="
          form.splitterName === 'SimpleDocumentSplitter' ||
          form.splitterName === 'SimpleTokenizeSplitter'
        "
        prop="chunkSize"
      >
        <ElSlider v-model="form.chunkSize" show-input :max="2048" />
      </ElFormItem>
      <ElFormItem
        :label="$t('documentCollection.splitterDoc.overlapSize')"
        v-if="
          form.splitterName === 'SimpleDocumentSplitter' ||
          form.splitterName === 'SimpleTokenizeSplitter'
        "
        prop="overlapSize"
      >
        <ElSlider v-model="form.overlapSize" show-input :max="2048" />
      </ElFormItem>
      <ElFormItem
        :label="$t('documentCollection.splitterDoc.regex')"
        prop="regex"
        v-if="form.splitterName === 'RegexDocumentSplitter'"
      >
        <ElInput v-model="form.regex" />
      </ElFormItem>
      <ElFormItem
        v-if="form.splitterName === 'MarkdownHeaderSplitter'"
        :label="$t('documentCollection.splitterDoc.mdSplitterLevel')"
        prop="splitterName"
      >
        <ElSelect v-model="form.mdSplitterLevel">
            <ElOption
              v-for="item in mdSplitterLevel"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
      <ElFormItem
        v-if="isExcelFile"
        :label="$t('documentCollection.splitterDoc.rowsPerChunk')"
        prop="rowsPerChunk"
      >
        <ElTooltip
          :content="$t('documentCollection.splitterDoc.rowsPerChunkTooltip')"
          placement="top"
        >
          <ElInputNumber
            v-model="form.rowsPerChunk"
            :min="1"
            :max="1000"
            :step="1"
            controls-position="right"
          />
        </ElTooltip>
      </ElFormItem>
      <ElFormItem
        v-if="isExcelFile"
        :label="$t('documentCollection.splitterDoc.includeHeader')"
        prop="includeHeader"
      >
        <ElTooltip
          :content="$t('documentCollection.splitterDoc.includeHeaderTooltip')"
          placement="top"
        >
          <ElSwitch v-model="form.includeHeader" />
        </ElTooltip>
      </ElFormItem>
    </ElForm>
  </div>
</template>

<style scoped>
.splitter-doc-container {
  height: 100%;
  width: 100%;
  align-items: center;
  display: flex;
  justify-content: center;
}
.custom-form {
  width: 500px;
}
.custom-form :deep(.el-input),
.custom-form :deep(.ElSelect) {
  width: 100%;
}
</style>
