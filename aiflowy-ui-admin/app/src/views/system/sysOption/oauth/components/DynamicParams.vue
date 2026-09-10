<script setup>
import { ref, watch } from 'vue';

import { $t } from '@aiflowy/locales';

import { Delete, Plus } from '@element-plus/icons-vue';
import { ElButton, ElCol, ElInput, ElRow } from 'element-plus';

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

// 使用 ref 管理本地状态
const localParamsList = ref([]);

// 初始化
const initLocalParams = () => {
  localParamsList.value = Object.entries(props.modelValue).map(
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
    initLocalParams();
  },
  { immediate: true },
);

// 添加参数
const addParam = () => {
  localParamsList.value.push({ key: '', value: '' });
};

// 移除参数
const removeParam = (index) => {
  localParamsList.value.splice(index, 1);
  updateParams();
};

// 更新参数
const updateParams = () => {
  const newObj = {};
  localParamsList.value.forEach((item) => {
    if (item.key && item.key.trim()) {
      newObj[item.key.trim()] = item.value || '';
    }
  });
  emit('update:modelValue', newObj);
};
</script>

<template>
  <div class="dynamic-params">
    <div
      v-for="(param, index) in localParamsList"
      :key="index"
      class="param-item"
    >
      <ElRow :gutter="10" align="middle">
        <ElCol :span="8">
          <ElInput
            v-model="param.key"
            placeholder="param key"
            @blur="updateParams"
          />
        </ElCol>
        <ElCol :span="12">
          <ElInput
            v-model="param.value"
            placeholder="param value"
            @blur="updateParams"
          />
        </ElCol>
        <ElCol :span="4">
          <ElButton
            type="danger"
            :icon="Delete"
            circle
            @click="removeParam(index)"
          />
        </ElCol>
      </ElRow>
    </div>

    <ElButton type="primary" :icon="Plus" text @click="addParam">
      {{ $t('oauth.addParam') }}
    </ElButton>
  </div>
</template>

<style scoped>
.dynamic-params {
  margin-bottom: 16px;
}

.param-item {
  margin-bottom: 12px;
  padding: 12px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.param-item:last-child {
  margin-bottom: 8px;
}
</style>
