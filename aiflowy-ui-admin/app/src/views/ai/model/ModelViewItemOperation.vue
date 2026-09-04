<script setup lang="ts">
import type { PropType } from 'vue';

import type { llmType } from '#/api';
import type { ModelAbilityItem } from '#/views/ai/model/modelUtils/model-ability';

import { ref } from 'vue';

import { Minus, Plus, Setting } from '@element-plus/icons-vue';
import { ElButton, ElIcon, ElImage, ElMessage, ElTag } from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';
import { getIconByValue } from '#/views/ai/model/modelUtils/defaultIcon';
import { getDefaultModelAbility } from '#/views/ai/model/modelUtils/model-ability';
import { mapLlmToModelAbility } from '#/views/ai/model/modelUtils/model-ability-utils';

defineProps({
  llmList: {
    type: Array as PropType<llmType[]>,
    default: () => [],
  },
  icon: {
    type: String,
    default: '',
  },
  needHiddenSettingIcon: {
    type: Boolean,
    default: false,
  },
  isManagement: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  'deleteLlm',
  'editLlm',
  'addLlm',
  'updateWithUsed',
  'publishChange',
]);

// 正在发布/下架中的模型ID集合，用于防止重复提交
const publishingMap = ref<Record<string, boolean>>({});

const handleDeleteLlm = (id: string) => {
  emit('deleteLlm', id);
};

const handleAddLlm = (id: string) => {
  emit('addLlm', id);
};

const handleEditLlm = (id: string) => {
  emit('editLlm', id);
};

// 修改该模型为未使用状态，修改数据库的with_used字段为false
const handleUpdateWithUsedLlm = (id: string) => {
  emit('updateWithUsed', id);
};

// 单个模型发布
const handlePublishLlm = (id: string) => {
  if (publishingMap.value[id]) return;
  publishingMap.value = { ...publishingMap.value, [id]: true };
  api
    .post('/api/v1/model/singlePublish', null, {
      params: { id },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('llm.publishSuccess'));
        emit('publishChange');
      }
    })
    .finally(() => {
      publishingMap.value = { ...publishingMap.value, [id]: false };
    });
};

// 单个模型下架
const handleOfflineLlm = (id: string) => {
  if (publishingMap.value[id]) return;
  publishingMap.value = { ...publishingMap.value, [id]: true };
  api
    .post('/api/v1/model/modelOffline', null, {
      params: { id },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        ElMessage.success($t('llm.offlineSuccess'));
        emit('publishChange');
      }
    })
    .finally(() => {
      publishingMap.value = { ...publishingMap.value, [id]: false };
    });
};

/**
 * 获取LLM支持的选中的能力标签
 * 只返回 selected 为 true 的标签
 */
const getSelectedAbilityTagsForLlm = (llm: llmType): ModelAbilityItem[] => {
  const defaultAbility = getDefaultModelAbility();
  const allTags = mapLlmToModelAbility(llm, defaultAbility);
  return allTags.filter((tag) => tag.selected);
};
</script>

<template>
  <div v-for="llm in llmList" :key="llm.id" class="container">
    <div class="llm-item">
      <div class="start">
        <ElTag
          :type="llm.publishStatus === 1 ? 'success' : 'info'"
          size="small"
          effect="light"
          class="publish-status-tag"
        >
          {{
            llm.publishStatus === 1
              ? $t('llm.published')
              : $t('llm.unPublished')
          }}
        </ElTag>
        <ElImage
          v-if="llm.modelProvider.icon"
          :src="llm.modelProvider.icon"
          style="width: 21px; height: 21px"
        />

        <div
          v-else
          v-html="getIconByValue(llm.modelProvider.providerType)"
          :style="{
            width: '21px',
            height: '21px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }"
          class="svg-container"
        ></div>

        <div>{{ llm?.modelProvider?.providerName }}/{{ llm.title }}</div>

        <!-- 模型能力 -->
        <div
          v-if="getSelectedAbilityTagsForLlm(llm).length > 0"
          class="ability-tags"
        >
          <ElTag
            v-for="tag in getSelectedAbilityTagsForLlm(llm)"
            :key="tag.value"
            class="ability-tag"
            :type="tag.activeType"
            size="small"
          >
            {{ tag.label }}
          </ElTag>
        </div>
      </div>
      <div class="end">
        <ElButton
          link
          size="small"
          :type="llm.publishStatus === 1 ? 'warning' : 'primary'"
          :loading="publishingMap[llm.id] || false"
          :disabled="publishingMap[llm.id] || false"
          @click="
            llm.publishStatus === 1
              ? handleOfflineLlm(llm.id)
              : handlePublishLlm(llm.id)
          "
        >
          {{ llm.publishStatus === 1 ? $t('llm.offline') : $t('llm.publish') }}
        </ElButton>
        <ElIcon
          v-if="!needHiddenSettingIcon"
          size="16"
          @click="handleEditLlm(llm.id)"
          style="cursor: pointer"
        >
          <Setting />
        </ElIcon>
        <template v-if="!isManagement">
          <ElIcon
            size="16"
            @click="handleUpdateWithUsedLlm(llm.id)"
            style="cursor: pointer"
          >
            <Minus />
          </ElIcon>
        </template>

        <template v-if="isManagement">
          <ElIcon
            v-if="llm.withUsed"
            size="16"
            @click="handleDeleteLlm(llm.id)"
            style="cursor: pointer"
          >
            <Minus />
          </ElIcon>
          <ElIcon
            v-else
            size="16"
            @click="handleAddLlm(llm.id)"
            style="cursor: pointer"
          >
            <Plus />
          </ElIcon>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.llm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 40px;
}

.container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 18px;
  border-bottom: 1px solid #e4e7ed;
}

.container:last-child {
  border-bottom: none;
}

.start {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
}

.publish-status-tag {
  flex-shrink: 0;
  margin: 0;
}

.end {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ability-tag {
  cursor: default;
  user-select: none;
}

.svg-container {
  display: flex;
  align-items: center;
  justify-content: center;
}

.svg-container :deep(svg) {
  width: 21px;
  height: 21px;
}
</style>
