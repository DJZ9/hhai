<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { $t } from '@aiflowy/locales';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElTag,
} from 'element-plus';

import { api } from '#/api/request';

interface Props {
  visible: boolean;
  botId: string;
  toolType: 'mcp' | 'plugin' | 'workflow';
  toolId: number | string;
  recordId?: number | string;
  toolName?: string;
  outputTemplate?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'success'): void;
}>();

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

const loading = ref(false);
const customTemplate = ref('');

// 常用变量提示
const commonVariables = [
  { name: 'code', desc: $t('bot.statusCode') },
  { name: 'data', desc: $t('bot.data') },
  { name: 'message', desc: $t('bot.msgContent') },
  { name: 'result', desc: $t('bot.Result') },
];

watch(
  () => [props.visible, props.recordId],
  ([visible, recordId]) => {
    if (visible && recordId) {
      // 使用传入的 outputTemplate 作为初始值
      customTemplate.value = props.outputTemplate || '';
    } else if (!visible) {
      // 关闭时清空数据
      customTemplate.value = '';
    }
  },
);

// 插入变量到模板
const insertVariable = (variable: string) => {
  const textarea = document.querySelector(
    '.template-textarea textarea',
  ) as HTMLTextAreaElement;
  if (!textarea) {
    customTemplate.value += `{{${variable}}}`;
    return;
  }

  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = customTemplate.value;
  const before = text.slice(0, Math.max(0, start));
  const after = text.slice(Math.max(0, end));

  customTemplate.value = `${before}{{${variable}}}${after}`;

  // 设置光标位置
  setTimeout(() => {
    textarea.focus();
    const newPos = start + `{{${variable}}}`.length;
    textarea.setSelectionRange(newPos, newPos);
  }, 0);
};

const handleSubmit = async () => {
  if (!props.recordId) {
    ElMessage.error($t('bot.recordIdRequired'));
    return;
  }

  loading.value = true;

  try {
    const apiUrl =
      props.toolType === 'workflow'
        ? '/api/v1/botWorkflow/update'
        : props.toolType === 'mcp'
          ? '/api/v1/botMcp/update'
          : '/api/v1/botPlugins/update';

    const res = await api.post(apiUrl, {
      id: props.recordId,
      outputTemplate: customTemplate.value,
    });

    if (res.errorCode === 0) {
      ElMessage.success($t('message.updateOkMessage'));
      emit('success');
      dialogVisible.value = false;
    } else {
      ElMessage.error(res.message || $t('message.fail'));
    }
  } catch (error) {
    ElMessage.error($t('message.fail'));
    console.error($t('message.fail'), error);
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  dialogVisible.value = false;
};

const handleClear = () => {
  customTemplate.value = '';
};
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    :title="`${$t('bot.outputTemplateConfig')} - ${toolName || ''}`"
    width="700px"
    destroy-on-close
  >
    <div class="template-config-container">
      <!-- 自定义模板输入 -->
      <ElForm class="config-form" label-position="top">
        <ElFormItem :label="$t('bot.customOutputTemplate')">
          <ElInput
            v-model="customTemplate"
            type="textarea"
            :rows="8"
            :placeholder="$t('bot.placeholder.outputTemplate')"
            class="template-textarea"
          />
        </ElFormItem>

        <!-- 变量提示 -->
        <ElFormItem :label="$t('bot.commonVariables')">
          <div class="variable-tags">
            <ElTag
              v-for="item in commonVariables"
              :key="item.name"
              class="variable-tag"
              type="info"
              effect="plain"
              size="small"
              @click="insertVariable(item.name)"
            >
              {{ item.name }}
              <span class="variable-desc">({{ item.desc }})</span>
            </ElTag>
          </div>
          <div class="variable-hint">
            {{ $t('bot.jsonpathSupport') }}<span v-text="'{{location[0].name}}'"></span
            >、<span v-text="'{{data.field}}'"></span>
          </div>
        </ElFormItem>
      </ElForm>

      <!-- 操作按钮 -->
      <div class="template-actions">
        <ElButton type="danger" link size="small" @click="handleClear">
          {{ $t('button.clear') }}
        </ElButton>
      </div>
    </div>

    <template #footer>
      <ElButton @click="handleCancel">{{ $t('button.cancel') }}</ElButton>
      <ElButton type="primary" @click="handleSubmit" :loading="loading">
        {{ $t('button.save') }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.template-config-container {
  padding: 0 10px;
}

.config-form {
  margin-top: 20px;
}

.variable-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.variable-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.variable-tag:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.variable-desc {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.variable-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.template-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>
