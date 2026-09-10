<script setup>
import { computed } from 'vue';

import { ElCol, ElFormItem, ElInput, ElRow } from 'element-plus';

import { $t } from '#/locales';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['update:modelValue']);

const fieldMapping = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value);
  },
});

const updateFieldMapping = () => {
  // 自动更新
};
</script>

<template>
  <div class="field-mapping">
    <ElRow :gutter="20">
      <ElCol :span="12">
        <ElFormItem :label="$t('oauth.userKey')" prop="userKey">
          <ElInput
            v-model="fieldMapping.userKey"
            placeholder="unionid"
            @input="updateFieldMapping"
          />
        </ElFormItem>
      </ElCol>
      <ElCol :span="12">
        <ElFormItem :label="$t('oauth.username')" prop="username">
          <ElInput
            v-model="fieldMapping.username"
            placeholder="unionid"
            @input="updateFieldMapping"
          />
        </ElFormItem>
      </ElCol>
    </ElRow>

    <ElRow :gutter="20">
      <ElCol :span="12">
        <ElFormItem :label="$t('oauth.nickname')" prop="nickname">
          <ElInput
            v-model="fieldMapping.nickname"
            placeholder="nickname"
            @input="updateFieldMapping"
          />
        </ElFormItem>
      </ElCol>
      <ElCol :span="12">
        <ElFormItem :label="$t('oauth.avatar')" prop="avatar">
          <ElInput
            v-model="fieldMapping.avatar"
            placeholder="headimgurl"
            @input="updateFieldMapping"
          />
        </ElFormItem>
      </ElCol>
    </ElRow>
  </div>
</template>

<style scoped>
.field-mapping {
  margin-bottom: 20px;
}

.mapping-hint {
  margin-top: 20px;
}

.mapping-hint p {
  margin: 8px 0;
  font-size: 13px;
  color: #606266;
}

.mapping-hint code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #e6a23c;
}
</style>
