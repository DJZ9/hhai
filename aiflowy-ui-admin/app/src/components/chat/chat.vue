<script setup lang="ts">
import type {
  BubbleListInstance,
  BubbleListProps,
} from 'vue-element-plus-x/types/BubbleList';
import type { ThinkingStatus } from 'vue-element-plus-x/types/Thinking';

import type { BotInfo, ChatMessage } from '@aiflowy/types';

import {
  computed,
  nextTick,
  onMounted,
  ref,
  useTemplateRef,
  watchEffect,
} from 'vue';
import { XSender } from 'vue-element-plus-x';
import { useRoute, useRouter } from 'vue-router';

import { IconifyIcon } from '@aiflowy/icons';
import { $t } from '@aiflowy/locales';
import { useBotStore } from '@aiflowy/stores';
import { cloneDeep, cn, uuid } from '@aiflowy/utils';

import {
  CircleCheck,
  CopyDocument,
  Loading,
  Paperclip,
  RefreshRight,
} from '@element-plus/icons-vue';
import { useThrottleFn } from '@vueuse/core';
import {
  ElButton,
  ElCollapse,
  ElCollapseItem,
  ElIcon,
  ElMessage,
  ElSpace,
} from 'element-plus';
import { tryit } from 'radash';

import { getMessageList, getPerQuestions } from '#/api';
import { api, sseClient } from '#/api/request';
import SendEnableIcon from '#/components/icons/SendEnableIcon.vue';
import SendIcon from '#/components/icons/SendIcon.vue';
import ShowJson from '#/components/json/ShowJson.vue';
import ChatFileUploader from '#/components/upload/ChatFileUploader.vue';
import { useVoiceRecorder } from '#/composables/useVoiceRecorder';
import {
  getFileExt,
  getFileIcon,
  getFileIconColor,
  getFileName,
  isImageFile,
  previewImage,
} from '#/utils/file';

import BotAvatar from '../botAvatar/botAvatar.vue';
import SendingIcon from '../icons/SendingIcon.vue';

type Think = {
  reasoning_content?: string;
  thinkCollapse?: boolean;
  thinkingStatus?: ThinkingStatus;
};

type Tool = {
  arguments: string;
  id: string;
  name: string;
  result?: string;
  status: 'TOOL_CALL' | 'TOOL_RESULT';
};

type MessageItem = ChatMessage & {
  chains?: (Think | Tool)[];
};

const props = defineProps<{
  bot?: BotInfo;
  conversationId?: string;
  // 是否显示对话列表
  showChatConversations?: boolean;
}>();
const botStore = useBotStore();
interface historyMessageType {
  role: string;
  content: string;
}
interface presetQuestionsType {
  key: string;
  description: string;
}
const route = useRoute();
const botId = ref<string>((route.params.id as string) || '');
const router = useRouter();

const bubbleItems = ref<BubbleListProps<MessageItem>['list']>([]);
const bubbleListRef = useTemplateRef<BubbleListInstance>('bubbleListRef');
const senderRef = ref<InstanceType<typeof XSender>>();
// v2 的 XSender 不再支持 v-model，输入内容通过实例 API 读取，空态用 change 事件同步
const senderEmpty = ref(true);
const sending = ref(false);

// v2 的 BubbleList 不再可靠地自动跟到底部（长会话与流式增量都会漏滚），这里显式补偿
const scrollToLatest = () => {
  nextTick(() => {
    bubbleListRef.value?.scrollToBottom(false);
  });
};

// 智能跟随：仅当用户处于底部时才滚动，上滑查看历史期间不打断，滚回底部后自动恢复
const followBottom = useThrottleFn(() => {
  if (bubbleListRef.value?.currentScrollState === 'AT_BOTTOM') {
    bubbleListRef.value.scrollToBottom(false);
  }
}, 16);

const { isRecording, isProcessing, startRecording, stopRecording } =
  useVoiceRecorder();
const getConversationId = async () => {
  const res = await api.get('/api/v1/bot/generateConversationId');
  return res.data;
};
const localeConversationId = ref<any>('');

const presetQuestions = ref<presetQuestionsType[]>([]);
const showQuestions = computed(() => {
  const list = bubbleItems.value.filter(
    (message) => message?.content && message.content.length > 0,
  );
  return list.length === 0;
});

// 附件功能是否启用
const attachmentEnabled = computed(() => {
  const storeOptions = botStore.getBotOptions(botId.value);
  if (storeOptions && storeOptions.attachmentEnabled !== undefined) {
    return storeOptions.attachmentEnabled;
  }
  return props.bot?.options?.attachmentEnabled !== false;
});

// 语音功能是否启用
const voiceEnabled = computed(() => {
  // 默认关闭，只有明确设置为true时才启用
  const storeOptions = botStore.getBotOptions(botId.value);
  if (storeOptions && storeOptions.voiceEnabled !== undefined) {
    return storeOptions.voiceEnabled === true;
  }
  return props.bot?.options?.voiceEnabled === true;
});
defineExpose({
  clear() {
    bubbleItems.value = [];
    messages.value = [];
  },
});
const getPresetQuestions = () => {
  api
    .get('/api/v1/bot/detail', {
      params: {
        id: botId.value,
      },
    })
    .then((res) => {
      // 同步 options 到 botStore，供附件开关等配置实时读取
      if (res.data.options) {
        botStore.setBotOptions(botId.value, res.data.options);
      }
      if (res.data.options?.presetQuestions) {
        presetQuestions.value = res.data.options?.presetQuestions
          .filter(
            (item: presetQuestionsType) =>
              item.description && item.description.trim() !== '',
          )
          .map((item: presetQuestionsType) => ({
            key: item.key,
            description: item.description,
          }));
      }
    });
};
onMounted(async () => {
  // 初始化 conversationId
  localeConversationId.value =
    props.conversationId && props.conversationId.length > 0
      ? props.conversationId
      : await getConversationId();
  getPresetQuestions();
});
watchEffect(async () => {
  if (props.bot && props.conversationId) {
    const [, res] = await tryit(getMessageList)({
      conversationId: props.conversationId,
      botId: props.bot.id,
      tempUserId: uuid() + props.bot.id,
    });

    if (res?.errorCode === 0) {
      bubbleItems.value = res.data.map((item) => ({
        ...item,
        content:
          item.role === 'assistant'
            ? item.content.replace(/^Final Answer:\s*/i, '')
            : item.content,
        placement: item.role === 'assistant' ? 'start' : 'end',
      }));
      // 进入或切换会话时直接定位到最新消息
      scrollToLatest();
    }
  } else {
    bubbleItems.value = [];
  }
});
const lastUserMessage = ref('');
const messages = ref<historyMessageType[]>([]);
const stopSse = () => {
  sseClient.abort();
  sending.value = false;
  const lastBubbleItem = bubbleItems.value[bubbleItems.value.length - 1];
  if (lastBubbleItem) {
    bubbleItems.value[bubbleItems.value.length - 1] = {
      ...lastBubbleItem,
      content: lastBubbleItem.content,
      loading: false,
    };
  }
  followBottom();
};
const clearSenderFiles = () => {
  files.value = [];
  attachmentsRef.value?.clearFiles();
  showHeaderFlog.value = false;
};

// v2 XSender 的输入内容通过实例 API 读取
const getSenderText = () => senderRef.value?.getModelValue()?.text ?? '';

const handleSenderChange = () => {
  senderEmpty.value = senderRef.value?.senderState?.isEmpty ?? true;
};

async function handleVoiceToggle() {
  if (isRecording.value) {
    const text = await stopRecording();
    if (text) {
      senderRef.value?.setText(getSenderText() + text);
      senderEmpty.value = false;
    }
  } else {
    await startRecording();
  }
}
const handleSubmit = async (refreshContent?: string) => {
  const attachments = attachmentsRef.value?.getFileList();
  const currentPrompt = refreshContent || getSenderText().trim();
  if (!currentPrompt) {
    return;
  }
  sending.value = true;
  lastUserMessage.value = currentPrompt;
  messages.value.push({
    role: 'user',
    content: currentPrompt,
  });
  const copyMessages = [...messages.value];
  const data = {
    botId: botId.value,
    prompt: currentPrompt,
    conversationId: localeConversationId.value,
    messages: copyMessages,
    attachments,
  };
  clearSenderFiles();
  messages.value.pop();
  const fileList = attachments || [];
  const mockMessages = generateMockMessages(refreshContent ?? '', fileList);
  bubbleItems.value.push(...mockMessages);
  scrollToLatest();
  senderRef.value?.clear();
  senderEmpty.value = true;
  sseClient.post('/api/v1/bot/chat', data, {
    onMessage(message) {
      const event = message.event;
      const lastIndex = bubbleItems.value.length - 1;
      const lastBubbleItem = bubbleItems.value[lastIndex];

      //  finish
      if (event === 'done') {
        sending.value = false;
        scrollToLatest();
        // v1 由 BubbleList 的打字机 complete 事件触发，v2 移除了该事件，改为流结束时替换路由
        if (props.conversationId && props.conversationId.length <= 0) {
          setTimeout(() => {
            router.replace({
              params: { conversationId: localeConversationId.value },
            });
          }, 100);
        }
        return;
      }
      if (!message.data) {
        return;
      }
      // 处理系统错误
      const sseData = JSON.parse(message.data);
      if (
        sseData?.domain === 'SYSTEM' &&
        sseData.payload?.code === 'SYSTEM_ERROR'
      ) {
        const errorMessage = sseData.payload.message;
        if (!lastBubbleItem) return;
        bubbleItems.value[lastIndex] = {
          ...lastBubbleItem,
          content: errorMessage,
          loading: false,
        };
        followBottom();
        return;
      }

      if (lastIndex >= 0 && sseData?.domain === 'TOOL') {
        const chains = cloneDeep(lastBubbleItem?.chains ?? []);
        const index = chains.findIndex(
          (chain) =>
            isTool(chain) && chain.id === sseData?.payload?.tool_call_id,
        );

        if (index === -1) {
          chains.push({
            id: sseData?.payload?.tool_call_id,
            name: sseData?.payload?.name,
            status: sseData?.type,
            arguments: sseData?.payload?.arguments,
          });
        } else {
          chains[index] = {
            ...chains[index]!,
            status: sseData?.type,
            result: sseData?.payload?.result,
          };
        }
        bubbleItems.value[lastIndex]!.chains = chains;
        stopThinking();
        followBottom();
        return;
      }

      // 处理流式消息
      const delta = sseData.payload?.delta;
      const role = sseData.payload?.role;

      if (lastBubbleItem && delta) {
        if (sseData.type === 'THINKING') {
          const chains = cloneDeep(lastBubbleItem?.chains ?? []);
          const index = chains.findIndex(
            (chain) => isThink(chain) && chain.thinkingStatus === 'thinking',
          );

          if (index === -1) {
            chains.push({
              thinkingStatus: 'thinking',
              thinkCollapse: true,
              reasoning_content: delta,
            });
          } else {
            const think = chains[index]! as Think;
            chains[index] = {
              ...think,
              reasoning_content: think.reasoning_content + delta,
            };
          }
          bubbleItems.value[lastIndex]!.chains = chains;
        } else if (sseData.type === 'MESSAGE') {
          bubbleItems.value[lastIndex] = {
            ...lastBubbleItem,
            content: (lastBubbleItem.content + delta).replaceAll(
              '```echartsoption',
              '```echarts\noption',
            ),
            loading: false,
          };
          stopThinking();
        }
        followBottom();
      }

      // 是否需要保存聊天记录
      if (event === 'needSaveMessage') {
        messages.value.push({
          role,
          content: sseData.payload?.content,
        });
      }
    },
    onFinished() {
      sending.value = false;

      const lastIndex = bubbleItems.value.length - 1;
      if (lastIndex) {
        bubbleItems.value[lastIndex] = {
          ...bubbleItems.value[lastIndex]!,
          loading: false,
        };
      }
      stopThinking();
      followBottom();
    },
    onError(err) {
      console.error(err);
      sending.value = false;
    },
  });
};

const isTool = (item: Think | Tool) => {
  return 'id' in item;
};
const isThink = (item: Think | Tool): item is Think => {
  return !('id' in item);
};
const stopThinking = () => {
  const lastIndex = bubbleItems.value.length - 1;

  if (lastIndex >= 0 && bubbleItems.value[lastIndex]?.chains) {
    const chains = cloneDeep(bubbleItems.value[lastIndex].chains);

    for (const chain of chains) {
      if (isThink(chain) && chain.thinkingStatus === 'thinking') {
        chain.thinkingStatus = 'end';
      }
    }

    bubbleItems.value[lastIndex].chains = chains;
  }
};

const generateMockMessages = (refreshContent: string, fileList: string[] = []) => {
  const messages: MessageItem[] = [];
  // 附件单独一个气泡显示在文字上方
  if (fileList.length > 0) {
    messages.push({
      role: 'user',
      id: `${Date.now()}-att`,
      isAttachment: true,
      fileList: fileList as any,
      content: '',
      created: Date.now(),
      updateAt: Date.now(),
      placement: 'end',
    });
  }
  const userMessage: MessageItem = {
    role: 'user',
    id: Date.now().toString(),
    fileList: [],
    content: refreshContent || getSenderText(),
    created: Date.now(),
    updateAt: Date.now(),
    placement: 'end',
  };

  const assistantMessage: MessageItem = {
    role: 'assistant',
    id: Date.now().toString(),
    content: '',
    loading: true,
    created: Date.now(),
    updateAt: Date.now(),
    placement: 'start',
  };

  messages.push(userMessage);
  messages.push(assistantMessage);
  return messages;
};

const handleCopy = (content: string) => {
  navigator.clipboard
    .writeText(content)
    .then(() => ElMessage.success($t('message.copySuccess')))
    .catch(() => ElMessage.error($t('message.copyFail')));
};

const handleRefresh = () => {
  handleSubmit(lastUserMessage.value);
};
// v2 的 XSender 头部由使用方控制显隐（插槽 v-if），不再有 openHeader/closeHeader API
const showHeaderFlog = ref(false);
function openCloseHeader() {
  if (showHeaderFlog.value) {
    files.value = [];
  }
  showHeaderFlog.value = !showHeaderFlog.value;
}
const attachmentsRef = ref();
const files = ref<any[]>([]);
const isDragOver = ref(false);

function handleDragOver(e: DragEvent) {
  e.preventDefault();
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'copy';
  }
}

function handleDragEnter(e: DragEvent) {
  e.preventDefault();
  if (attachmentEnabled.value) {
    isDragOver.value = true;
  }
}

function handleDragLeave(e: DragEvent) {
  e.preventDefault();
  isDragOver.value = false;
}

function handleDrop(e: DragEvent) {
  e.preventDefault();
  isDragOver.value = false;

  if (!attachmentEnabled.value) return;

  const fileList = e.dataTransfer?.files;
  if (!fileList || fileList.length === 0) return;

  showHeaderFlog.value = true;
  files.value = [...fileList];
}

function handlePasteFile(_: any, fileList: FileList) {
  // 附件功能未启用时不处理
  if (!attachmentEnabled.value) return;
  showHeaderFlog.value = true;
  files.value = [...fileList];
}
</script>

<template>
  <div class="mx-auto h-full max-w-[780px]">
    <div
      :class="
        cn(
          'flex h-full w-full flex-col gap-3',
          !localeConversationId && 'items-center justify-center gap-8',
        )
      "
    >
      <!-- 对话列表 -->
      <div
        v-if="localeConversationId || bubbleItems.length > 0"
        class="message-container w-full flex-1 overflow-hidden"
      >
        <ElBubbleList
          ref="bubbleListRef"
          class="!h-full"
          :auto-scroll="true"
          max-height="100%"
          :list="bubbleItems"
        >
          <template #header="{ item }">
            <div class="flex flex-col">
              <span class="chat-bubble-item-time-style">
                {{ new Date(item.created).toLocaleString() }}
              </span>

              <template v-if="item.chains">
                <template
                  v-for="(chain, index) in item.chains"
                  :key="chain.id || index"
                >
                  <ElThinking
                    v-if="isThink(chain)"
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
                                <IconifyIcon
                                  icon="mdi:clock-time-five-outline"
                                />
                              </ElIcon>
                              <span>{{ $t('bot.Running') }}...</span>
                            </div>
                          </template>
                          <template v-else>
                            <div
                              class="bg-secondary flex items-center gap-1 rounded-full px-2 py-0.5 leading-none"
                            >
                              <ElIcon size="16" color="var(--el-color-success)">
                                <CircleCheck />
                              </ElIcon>
                              <span>{{ $t('bot.Completed') }}</span>
                            </div>
                          </template>
                        </div>
                      </template>
                      <div
                        class="border-border flex flex-col gap-1 border-t pt-2"
                      >
                        <span class="ml-2">{{ $t('bot.Parameters') }}：</span>
                        <ShowJson :value="chain.arguments" />
                      </div>
                      <div class="mt-2 flex flex-col gap-1">
                        <span class="ml-2">{{ $t('bot.Result') }}：</span>
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
                class="mb-3"
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
          <!-- 自定义头像 -->
          <template #avatar="{ item }">
            <BotAvatar
              v-if="item.role === 'assistant'"
              :src="bot?.icon"
              :size="40"
            />
          </template>
          <template #content="{ item }">
            <!-- 附件独立气泡：图片缩略图 / 文件卡片 -->
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
          <template #footer="{ item }">
            <ElSpace :size="10">
              <ElSpace>
                <span @click="handleRefresh()" style="cursor: pointer">
                  <ElIcon>
                    <RefreshRight />
                  </ElIcon>
                </span>
                <span @click="handleCopy(item.content)" style="cursor: pointer">
                  <ElIcon>
                    <CopyDocument />
                  </ElIcon>
                </span>
              </ElSpace>
            </ElSpace>
          </template>
        </ElBubbleList>
      </div>

      <!-- 新对话显示bot信息 -->
      <div v-else class="flex flex-col items-center gap-3.5">
        <BotAvatar :src="bot?.icon" :size="88" />
        <h1 class="text-base font-medium text-black/85">
          {{ bot?.title }}
        </h1>
        <span class="text-sm text-[#757575]">{{ bot?.description }}</span>
      </div>

      <!--问题预设-->
      <div
        class="questions-preset-container"
        v-if="botStore.getPresetQuestions(botId).length > 0 && showQuestions"
      >
        <ElButton
          v-for="item in getPerQuestions(botStore.getPresetQuestions(botId))"
          :key="item.key"
          @click="handleSubmit(item.description)"
        >
          {{ item.description }}
        </ElButton>
      </div>
      <!-- Sender -->
      <ElSender
        ref="senderRef"
        class="w-full"
        :placeholder="$t('message.pleaseInputContent')"
        variant="updown"
        :custom-style="{ maxHeight: '120px' }"
        :class="{ 'drag-over': isDragOver }"
        @change="handleSenderChange"
        @submit="handleSubmit"
        @paste-file="handlePasteFile"
        @dragover.prevent="handleDragOver"
        @dragenter="handleDragEnter"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
      >
        <!-- 自定义头部内容 -->
        <template v-if="showHeaderFlog" #header>
          <ChatFileUploader
            ref="attachmentsRef"
            :external-files="files"
            :max-size="10"
            @delete-all="openCloseHeader"
          />
        </template>

        <template #action-list>
          <ElSpace>
            <template v-if="voiceEnabled">
              <ElButton v-if="isProcessing" circle disabled>
                <ElIcon size="16">
                  <Loading />
                </ElIcon>
              </ElButton>
              <ElButton
                v-else
                circle
                :class="{ 'voice-recording': isRecording }"
                @click="handleVoiceToggle"
              >
                <ElIcon :size="16" :color="isRecording ? '#ef4444' : undefined">
                  <IconifyIcon icon="mdi:microphone" />
                </ElIcon>
              </ElButton>
            </template>
            <ElButton v-if="attachmentEnabled" circle @click="openCloseHeader">
              <ElIcon><Paperclip /></ElIcon>
            </ElButton>
            <ElButton v-if="sending" circle @click="stopSse">
              <ElIcon size="30" color="#409eff"><SendingIcon /></ElIcon>
            </ElButton>
            <template v-else>
              <ElButton v-if="senderEmpty" circle disabled>
                <SendIcon />
              </ElButton>
              <ElButton v-else circle @click="handleSubmit('')">
                <SendEnableIcon />
              </ElButton>
            </template>
          </ElSpace>
        </template>
      </ElSender>
    </div>
  </div>
</template>

<style scoped>
.questions-preset-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  width: 100%;
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

.message-container {
  /* 气泡底色：与商业版一致（纯白），亮/暗主题同色 */
  --bot-chat-bubble-bg: var(--bot-chat-message-item-back);

  padding: 8px;
  background-color: var(--bot-chat-message-container);
  border-radius: 8px;
}

.dark .message-container {
  border: 1px solid hsl(var(--border));
}

.chat-bubble-item-time-style {
  font-size: 12px;
  color: var(--common-font-placeholder-color);
}

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
  --elx-bubble-bg: var(--bot-chat-bubble-bg);
  --elx-bubble-padding-y: 4px;
  --elx-bubble-padding-x: 8px;
}

/* x-md-renderer 内联 backgroundColor: #ffffff / padding: 16px 需 !important 覆盖 */
:deep(.x-md-renderer) {
  background-color: var(--bot-chat-bubble-bg) !important;
  padding: 4px !important;
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

/* 拖拽上传时的高亮样式 */
.drag-over {
  outline: 2px dashed var(--el-color-primary);
  outline-offset: -2px;
  border-radius: 8px;
}

/* ========== 语音录制脉冲动画 ========== */
.voice-recording {
  animation: voice-pulse 1.5s ease-in-out infinite;
}

@keyframes voice-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
</style>
