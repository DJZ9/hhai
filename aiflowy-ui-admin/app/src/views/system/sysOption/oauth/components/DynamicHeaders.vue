<script setup>
import { ref, watch } from 'vue';

import { Delete, Plus } from '@element-plus/icons-vue';
import { ElButton, ElCol, ElInput, ElRow } from 'element-plus';

import { $t } from '#/locales';

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  placeholders: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['update:modelValue']);
// 使用 ref 来管理本地状态
const localHeadersList = ref([]);
// 初始化时转换数据
const initLocalHeaders = () => {
  localHeadersList.value = Object.entries(props.modelValue).map(
    ([key, value]) => ({
      key,
      value,
    }),
  );
};
// 监听props变化
watch(
  () => props.modelValue,
  () => {
    initLocalHeaders();
  },
  { immediate: true },
);

// 添加Header - 修复的关键代码
const addHeader = () => {
  localHeadersList.value.push({ key: '', value: '' });
};

// 移除Header
const removeHeader = (index) => {
  localHeadersList.value.splice(index, 1);
  updateHeaders();
};

// 更新Headers - 发送到父组件
const updateHeaders = () => {
  const newObj = {};
  localHeadersList.value.forEach((item) => {
    if (item.key && item.key.trim()) {
      newObj[item.key.trim()] = item.value || '';
    }
  });
  emit('update:modelValue', newObj);
};
</script>

<template>
  <div class="dynamic-headers">
    <div
      v-for="(header, index) in localHeadersList"
      :key="index"
      class="header-item"
    >
      <ElRow :gutter="10" align="middle">
        <ElCol :span="8">
          <ElInput
            v-model="header.key"
            placeholder="header key"
            @blur="updateHeaders"
          />
        </ElCol>
        <ElCol :span="12">
          <ElInput
            v-model="header.value"
            placeholder="header value"
            @blur="updateHeaders"
          />
        </ElCol>
        <ElCol :span="4">
          <ElButton
            type="danger"
            :icon="Delete"
            circle
            @click="removeHeader(index)"
          />
        </ElCol>
      </ElRow>
    </div>

    <ElButton type="primary" :icon="Plus" text @click="addHeader">
      {{ $t('oauth.addHeader') }}
    </ElButton>
  </div>
</template>

<style scoped>
.dynamic-headers {
  margin-bottom: 16px;
}

.header-item {
  margin-bottom: 12px;
  padding: 12px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.header-item:last-child {
  margin-bottom: 8px;
}
</style>
