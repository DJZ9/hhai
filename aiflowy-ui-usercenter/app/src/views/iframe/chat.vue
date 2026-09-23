<script setup lang="ts">
import { computed, onMounted, ref, useTemplateRef } from 'vue';
import { useRoute } from 'vue-router';

import { updatePreferences } from '@aiflowy/preferences';
import { useAccessStore } from '@aiflowy/stores';
import { cloneDeep } from '@aiflowy/utils';

import { ElAvatar, ElButton, ElContainer, ElMain } from 'element-plus';

import { api } from '#/api/request';
import defaultAssistantAvatar from '#/assets/defaultAssistantAvatar.svg';
import { ChatBubbleList, ChatContainer, ChatSender } from '#/components/chat';

const route = useRoute();
const queryBotId = ref<string>((route.query.botId as string) || '');
const recentUsedAssistant = ref<any[]>([]);
const currentBot = ref<any>({});
const messageList = ref<any>([]);
const bubbleListRef = useTemplateRef<any>('bubbleListRef');
const presetMessage = ref('');
const presetSendTrigger = ref(0);
const showQuestions = computed(() => {
  const list = messageList.value.filter(
    (message: any) => message?.content && message.content.length > 0,
  );
  return list.length === 0;
});

onMounted(() => {
  injectUrlToken();
  updateTheme();
  getAssistantList();
  window.addEventListener('pageshow', updateTheme);
});

function injectUrlToken() {
  const params = getUrlParams();
  if (params.token) {
    const accessStore = useAccessStore();
    if (!accessStore.accessToken) {
      accessStore.setAccessToken(params.token as string);
    }
    // 清理 URL 中的 token，避免泄露并防止刷新后残留
    updateQueryWithoutReload({ token: null });
  }
}

function updateTheme(event?: PageTransitionEvent) {
  const params = getUrlParams();
  const theme: any = {};

  if (params.primaryColor) {
    theme.colorPrimary = params.primaryColor;
  }
  if (params.dark) {
    theme.mode = params.dark === 'true' ? 'dark' : 'light';
  }

  if (Object.keys(theme).length > 0) {
    if (!event) {
      updatePreferences({ theme });
    } else if (event.persisted) {
      updatePreferences({ theme });
    }
  }
}
function getUrlParams() {
  const params: any = {};
  const queryString = window.location.href.split('?')[1];

  if (!queryString) {
    return params;
  }

  if (queryString) {
    const pairs = queryString.split('&');

    pairs.forEach((pair) => {
      const [key, value] = pair.split('=');
      params[decodeURIComponent(key!)] = decodeURIComponent(value || '');
    });
  }

  return params;
}
function updateQueryWithoutReload(params: any) {
  const hash = window.location.hash;

  if (hash && hash.startsWith('#/')) {
    // 解析当前的 hash 路由和参数
    const [hashPath, queryString] = hash.slice(1).split('?');
    const query = new URLSearchParams(queryString || '');

    // 更新参数
    Object.keys(params).forEach((key) => {
      if (params[key] === null || params[key] === undefined) {
        query.delete(key);
      } else {
        query.set(key, params[key]);
      }
    });

    // 构建新的 hash
    const newQuery = query.toString();
    const newHash = `#${hashPath}${newQuery ? `?${newQuery}` : ''}`;

    // 更新 URL
    window.history.pushState({}, '', newHash);
  } else {
    // 非 hash 路由模式的处理
    const url = new URL(window.location.href);

    Object.keys(params).forEach((key) => {
      if (params[key] === null || params[key] === undefined) {
        url.searchParams.delete(key);
      } else {
        url.searchParams.set(key, params[key]);
      }
    });

    window.history.pushState({}, '', url.toString());
  }
}
function handleSessionChange(session: any) {
  if (!session?.id) {
    return;
  }
  const query = getUrlParams();
  updateQueryWithoutReload({
    ...query,
    sessionId: session.id,
  });
}
function getAssistantList() {
  api.get('/userCenter/botRecentlyUsed/getRecentlyBot').then((res) => {
    recentUsedAssistant.value = res.data;
    if (recentUsedAssistant.value.length > 0) {
      if (queryBotId.value) {
        const targetItem = recentUsedAssistant.value.find(
          (item: any) => item.id === queryBotId.value,
        );
        currentBot.value = targetItem || recentUsedAssistant.value[0];
      } else {
        currentBot.value = recentUsedAssistant.value[0];
      }
    }
  });
}
const getPerQuestions = (presetQuestions: any[]) => {
  if (!presetQuestions) {
    return [];
  }
  return presetQuestions
    .filter((item: any) => {
      return (
        typeof item.description === 'string' && item.description.trim() !== ''
      );
    })
    .map((item: any) => ({
      key: item.key,
      description: item.description,
    }));
};
function handlePresetSubmit(content: string) {
  if (!content?.trim()) {
    return;
  }
  presetMessage.value = content;
  presetSendTrigger.value += 1;
}
function addMessage(message: any) {
  messageList.value.push(message);
}
function updateLastMessage(item: any) {
  const lastIndex = messageList.value.length - 1;
  let message = item;

  if (typeof item === 'function') {
    message = item(messageList.value[lastIndex]);
  }

  if (lastIndex >= 0) {
    messageList.value[lastIndex] = {
      ...messageList.value[lastIndex],
      ...message,
    };
  }
}
const stopThinking = () => {
  const lastIndex = messageList.value.length - 1;

  if (lastIndex >= 0 && messageList.value[lastIndex]?.chains) {
    const chains = cloneDeep(messageList.value[lastIndex].chains);

    for (const chain of chains) {
      if (!('id' in chain) && chain.thinkingStatus === 'thinking') {
        chain.thinkingStatus = 'end';
        chain.thinkCollapse = false;
      }
    }

    messageList.value[lastIndex].chains = chains;
  }
  bubbleListRef.value?.scrollBottom();
};
function setMessageList(messages: any) {
  messageList.value = messages;
}
</script>

<template>
  <ElContainer class="bg-background h-full overflow-hidden">
    <ElMain class="!p-0">
      <ChatContainer
        class="border-none"
        :bot="currentBot"
        :on-message-list="setMessageList"
        :on-session-change="handleSessionChange"
        :get-url-params="getUrlParams"
      >
        <template #default="{ conversationId }">
          <div
            class="mx-auto flex h-full max-w-[1000px] flex-col justify-between"
          >
            <template v-if="messageList.length > 0">
              <ChatBubbleList
                ref="bubbleListRef"
                :bot="currentBot"
                :messages="messageList"
                max-height="calc(100vh - 220px)"
              />
            </template>
            <template v-else>
              <div class="my-auto flex flex-col items-center">
                <ElAvatar
                  :src="currentBot.icon || defaultAssistantAvatar"
                  :size="72"
                />
                <span class="mt-5 text-xl font-medium">{{
                  currentBot.title
                }}</span>
                <span class="text-foreground/70 mt-1">{{
                  currentBot.description
                }}</span>
              </div>
            </template>
            <div class="w-full">
              <div
                class="questions-preset-container"
                v-if="
                  getPerQuestions(currentBot?.options?.presetQuestions).length >
                    0 && showQuestions
                "
              >
                <ElButton
                  v-for="item in getPerQuestions(
                    currentBot?.options?.presetQuestions,
                  )"
                  :key="item.key"
                  @click="handlePresetSubmit(item.description)"
                >
                  {{ item.description }}
                </ElButton>
              </div>
              <ChatSender
                :add-message="addMessage"
                :update-last-message="updateLastMessage"
                :stop-thinking="stopThinking"
                :bot="currentBot"
                :conversation-id="conversationId"
                :external-send-message="presetMessage"
                :external-send-trigger="presetSendTrigger"
              />
            </div>
          </div>
        </template>
      </ChatContainer>
    </ElMain>
  </ElContainer>
</template>

<style lang="css" scoped>
.questions-preset-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  width: 100%;
  margin-bottom: 10px;
  overflow: hidden;
}

.questions-preset-container :deep(.el-button) {
  max-width: 100%;
  height: auto;
}

.questions-preset-container :deep(.el-button span) {
  display: -webkit-box;
  max-height: 33.59px;
  overflow: hidden;
  -webkit-line-clamp: 2;
  line-height: 1.2;
  text-align: left;
  text-wrap: wrap;
  -webkit-box-orient: vertical;
}

:deep(.el-button + .el-button) {
  margin-left: 0;
}

.el-aside::-webkit-scrollbar {
  display: none;
}
</style>
