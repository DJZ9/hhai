<script setup lang="ts">
import type { BubbleListInstance } from 'vue-element-plus-x/types/BubbleList';

import { useTemplateRef } from 'vue';

import { IconifyIcon } from '@aiflowy/icons';
import { useUserStore } from '@aiflowy/stores';

import { CircleCheck } from '@element-plus/icons-vue';
import { ElAvatar, ElCollapse, ElCollapseItem, ElIcon } from 'element-plus';

import defaultAssistantAvatar from '#/assets/defaultAssistantAvatar.svg';
import defaultUserAvatar from '#/assets/defaultUserAvatar.png';
import ShowJson from '#/components/json/ShowJson.vue';
import { $t } from '#/locales';
import {
  getFileExt,
  getFileIcon,
  getFileIconColor,
  getFileName,
  isImageFile,
  previewImage,
} from '#/utils/file';

interface Props {
  bot: any;
  messages: any[];
  maxHeight?: string;
}
const props = defineProps<Props>();
const store = useUserStore();
const bubbleListRef = useTemplateRef<BubbleListInstance>('bubbleListRef');

function getAssistantAvatar() {
  return props.bot.icon || defaultAssistantAvatar;
}
function getUserAvatar() {
  return store.userInfo?.avatar || defaultUserAvatar;
}
function scrollBottom() {
  bubbleListRef.value?.scrollToBottom();
}

defineExpose({ scrollBottom });
</script>

<template>
  <ElBubbleList
    ref="bubbleListRef"
    :list="messages"
    :auto-scroll="true"
    :max-height="maxHeight || 'calc(100% - 180px)'"
  >
    <!-- 自定义头像 -->
    <template #avatar="{ item }">
      <ElAvatar
        :src="
          item.role === 'assistant' ? getAssistantAvatar() : getUserAvatar()
        "
        :size="40"
      />
    </template>

    <!-- 自定义头部 -->
    <template #header="{ item }">
      <div class="flex flex-col">
        <span class="text-foreground/50 text-xs">
          {{ item.created }}
        </span>

        <template v-if="item.chains">
          <template
            v-for="(chain, index) in item.chains"
            :key="chain.id || index"
          >
            <ElThinking
              v-if="!('id' in chain)"
              v-model="chain.thinkCollapse"
              :content="chain.reasoning_content"
              :status="chain.thinkingStatus"
            />
            <ElCollapse v-else class="mb-2">
              <ElCollapseItem :title="chain.name" :name="chain.id">
                <template #title>
                  <div class="flex items-center gap-2 pl-5">
                    <ElIcon size="16">
                      <IconifyIcon icon="svg:wrench" />
                    </ElIcon>
                    <span>{{ chain.name }}</span>
                    <template v-if="chain.status === 'TOOL_CALL'">
                      <div
                        class="bg-secondary flex items-center gap-1 rounded-full px-2 py-0.5 leading-none"
                      >
                        <ElIcon size="16">
                          <IconifyIcon icon="mdi:clock-time-five-outline" />
                        </ElIcon>
                        <span>{{ $t('bot.toolCalling') }}</span>
                      </div>
                    </template>
                    <template v-else>
                      <div
                        class="bg-secondary flex items-center gap-1 rounded-full px-2 py-0.5 leading-none"
                      >
                        <ElIcon size="16" color="var(--el-color-success)">
                          <CircleCheck />
                        </ElIcon>
                        <span>{{ $t('bot.callSuccess') }}</span>
                      </div>
                    </template>
                  </div>
                </template>
                <div class="border-border flex flex-col gap-1 border-t pt-2">
                  <span class="ml-2">{{ $t('bot.paramsLabel') }}</span>
                  <ShowJson :value="chain.arguments" />
                </div>
                <div class="mt-2 flex flex-col gap-1">
                  <span class="ml-2">{{ $t('bot.resultLabel') }}</span>
                  <ShowJson :value="chain.result" />
                </div>
              </ElCollapseItem>
            </ElCollapse>
          </template>
        </template>

        <!-- <ElThinking
          v-if="item.reasoning_content"
          v-model="item.thinkCollapse"
          :content="item.reasoning_content"
          :status="item.thinkingStatus"
        /> -->
        <!-- <ElCollapse v-if="item.tools" class="mb-2">
          <ElCollapseItem
            class="mb-2"
            v-for="tool in item.tools"
            :key="tool.id"
            :title="tool.name"
            :name="tool.id"
          >
            <template #title>
              <div class="flex items-center gap-2 pl-5">
                <ElIcon size="16">
                  <IconifyIcon icon="svg:wrench" />
                </ElIcon>
                <span>{{ tool.name }}</span>
                <template v-if="tool.status === 'TOOL_CALL'">
                  <ElIcon size="16">
                    <IconifyIcon icon="svg:spinner" />
                  </ElIcon>
                </template>
                <template v-else>
                  <ElIcon size="16" color="var(--el-color-success)">
                    <CircleCheck />
                  </ElIcon>
                </template>
              </div>
            </template>
            <ShowJson :value="tool.result" />
          </ElCollapseItem>
        </ElCollapse> -->
      </div>
    </template>

    <!-- 自定义气泡内容 -->
    <template #content="{ item }">
      <!-- 附件独立气泡 -->
      <div v-if="item.isAttachment" class="flex flex-wrap gap-2">
        <template v-for="(url, idx) in item.fileList" :key="idx">
          <img
            v-if="isImageFile(url)"
            :src="url"
            class="h-20 w-20 cursor-pointer rounded-lg border border-gray-200 object-cover"
            @click="previewImage(url)"
          />
          <a
            v-else
            :href="url"
            target="_blank"
            class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm text-gray-700 shadow-sm transition-all hover:border-blue-300 hover:bg-blue-50 hover:text-blue-600 hover:shadow"
          >
            <ElIcon :size="32" :color="getFileIconColor(url)">
              <IconifyIcon :icon="getFileIcon(url)" />
            </ElIcon>
            <div class="flex flex-col">
              <span class="max-w-[200px] truncate font-medium">{{ getFileName(url) }}</span>
              <span v-if="getFileExt(url)" class="text-xs text-gray-400">{{ getFileExt(url) }}</span>
            </div>
          </a>
        </template>
      </div>
      <!-- 普通消息内容 -->
      <ElXMarkdown v-else :markdown="item.content" />
    </template>

    <!-- 自定义底部 -->
    <!--<template #footer="{ item }">
      <div class="flex items-center">
        <template v-if="item.role === 'assistant'">
          <ElButton :icon="RefreshRight" link />
          <ElButton :icon="CopyDocument" link />
        </template>
        <template v-else>
          <ElButton :icon="CopyDocument" link />
          <ElButton :icon="EditPen" link />
        </template>
      </div>
    </template>-->
  </ElBubbleList>
</template>

<style lang="css" scoped>
:deep(.elx-bubble__content-wrapper) {
  flex: none;
  max-width: calc(100% - 104px);
}

:deep(.elx-bubble__header),
:deep(.elx-bubble__footer) {
  width: 100%;
}

:deep(.elx-bubble--end .elx-bubble__header) {
  width: fit-content;
}

:deep(.elx-bubble__content-wrapper .elx-bubble__content) {
  --elx-bubble-content-max-width: 100%;
}

:deep(.elx-bubble__content) {
  --elx-bubble-bg: #f7f7f7;
  --elx-bubble-padding-y: 5px;
  --elx-bubble-padding-x: 10px;
}

:deep(.x-md-renderer) {
  background-color: #f7f7f7 !important;
  padding: 12px !important;
}

:deep(.mermaid-content) {
  min-height: 300px;
}

:deep(.elx-thinking) {
  margin: 0 !important;
  width: 100%;
  max-width: 100%;
}

:deep(.elx-thinking .elx-thinking__content-wrapper) {
  --elx-thinking-content-wrapper-width: 100%;

  margin-bottom: 8px;
}

:deep(.el-collapse) {
  overflow: hidden;
  border: 1px solid var(--el-collapse-border-color);
  border-radius: 8px;
}

:deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

/* ========== 手机端适配 ========== */
@media (width <= 768px) {
  :deep(.el-avatar) {
    display: none !important;
  }

  :deep(.elx-bubble) {
    column-gap: 0 !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
  }

  :deep(.elx-bubble__content-wrapper) {
    max-width: 100% !important;
    min-width: 0 !important;
    flex-shrink: 1 !important;
  }

  :deep(.elx-bubble--end .elx-bubble__content-wrapper) {
    max-width: 80% !important;
    min-width: 0 !important;
    flex-shrink: 1 !important;
    margin-left: auto !important;
  }

  :deep(.elx-bubble__content) {
    padding: 12px !important;
  }

  :deep(.elx-bubble__content img) {
    max-width: 100%;
    height: auto;
  }
}
</style>
