<script setup lang="ts">
import type { BubbleListInstance } from 'vue-element-plus-x/types/BubbleList';

import { h, useTemplateRef } from 'vue';

import { IconifyIcon } from '@aiflowy/icons';
import { useUserStore } from '@aiflowy/stores';

import { CircleCheck } from '@element-plus/icons-vue';
import { ElAvatar, ElCollapse, ElCollapseItem, ElIcon } from 'element-plus';

import defaultUserAvatar from '#/assets/defaultUserAvatar.png';
import ShowJson from '#/components/json/ShowJson.vue';

import Echarts from './echarts.vue';
import Infographic from './infographic.vue';

interface Props {
  bot: any;
  messages: any[];
  maxHeight?: string;
  conversationId?: string;
}
const props = defineProps<Props>();
const store = useUserStore();
const bubbleListRef = useTemplateRef<BubbleListInstance>('bubbleListRef');

function scrollBottom() {
  bubbleListRef.value?.scrollToBottom();
}
function getAssistantAvatar() {
  return props.bot?.icon || '/defaultAssistantAvatar.svg';
}
function getUserAvatar() {
  return store.userInfo?.avatar || defaultUserAvatar;
}

const selfCodeXRender = {
  echarts: (_props: { raw: any }) =>
    h(Echarts, {
      code: _props.raw.content,
      editable: false,
      openEditor: () => {},
    }),
  infographic: (_props: { raw: any }) =>
    h(Infographic, {
      code: _props.raw.content,
      editable: false,
      openEditor: () => {},
    }),
};

defineExpose({ scrollBottom });
</script>

<template>
  <ElBubbleList
    ref="bubbleListRef"
    :list="messages"
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
                        <span>工具调用中...</span>
                      </div>
                    </template>
                    <template v-else>
                      <div
                        class="bg-secondary flex items-center gap-1 rounded-full px-2 py-0.5 leading-none"
                      >
                        <ElIcon size="16" color="var(--el-color-success)">
                          <CircleCheck />
                        </ElIcon>
                        <span>调用成功</span>
                      </div>
                    </template>
                  </div>
                </template>
                <div class="border-border flex flex-col gap-1 border-t pt-2">
                  <span class="ml-2">参数：</span>
                  <ShowJson :value="chain.arguments" />
                </div>
                <div class="mt-2 flex flex-col gap-1">
                  <span class="ml-2">结果：</span>
                  <ShowJson :value="chain.result" />
                </div>
              </ElCollapseItem>
            </ElCollapse>
          </template>
        </template>
      </div>
    </template>

    <!-- 自定义气泡内容 -->
    <template #content="{ item }">
      <ElXMarkdown :markdown="item.content" :code-x-render="selfCodeXRender" />
    </template>
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

:deep(.elx-thinking) {
  margin: 0;
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
</style>
