<script setup lang="ts">
import type { BubbleProps } from 'vue-element-plus-x/types/Bubble';
import type { ThinkingStatus } from 'vue-element-plus-x/types/Thinking';

import { computed, inject, nextTick, ref, watch } from 'vue';
import { XSender } from 'vue-element-plus-x';

import { IconifyIcon } from '@aiflowy/icons';
import { cloneDeep, uuid } from '@aiflowy/utils';

import { Loading, Paperclip, Promotion } from '@element-plus/icons-vue';
import { ElButton, ElIcon } from 'element-plus';

import { sseClient } from '#/api/request';
import SendingIcon from '#/components/icons/SendingIcon.vue';
import ChatFileUploader from '#/components/upload/ChatFileUploader.vue';
import { useVoiceRecorder } from '#/composables/useVoiceRecorder';
import { $t } from '#/locales';
const { isRecording, isProcessing, startRecording, stopRecording } =
  useVoiceRecorder();
// import PaperclipIcon from '#/components/icons/PaperclipIcon.vue';

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

type MessageItem = BubbleProps & {
  chains?: (Think | Tool)[];
  fileList?: string[];
  isAttachment?: boolean;
  key: string;
  role: 'assistant' | 'user';
};

interface Props {
  conversationId: string | undefined;
  bot: any;
  addMessage: (message: MessageItem) => void;
  updateLastMessage: (item: any) => void;
  stopThinking: () => void;
  externalSendMessage?: string;
  externalSendTrigger?: number;
}

const props = defineProps<Props>();
// v2 的 XSender 不再支持 v-model，输入内容通过实例 API 读取，空态用 change 事件同步
const senderEmpty = ref(true);
const btnLoading = ref(false);
const getSessionList = inject<any>('getSessionList');
const addSession = inject<any>('addSession');
const getSenderText = () => senderRef.value?.getModelValue()?.text ?? '';
const handleSenderChange = () => {
  senderEmpty.value = senderRef.value?.senderState?.isEmpty ?? true;
};
const clearSenderFiles = () => {
  files.value = [];
  attachmentsRef.value?.clearFiles();
  showHeaderFlog.value = false;
};
async function sendMessage() {
  let conversationId = props.conversationId;
  if (!conversationId) {
    conversationId = await addSession();
  }

  const data = {
    conversationId,
    prompt: getSenderText(),
    botId: props.bot.id,
    attachments: attachmentsRef.value?.getFileList(),
  };
  // 获取附件列表
  const fileList = attachmentsRef.value?.getFileList() || [];
  clearSenderFiles();
  btnLoading.value = true;
  // 附件单独一个气泡显示在文字上方
  if (fileList.length > 0) {
    props.addMessage({
      key: uuid(),
      role: 'user',
      placement: 'end',
      isAttachment: true,
      fileList,
      content: '',
    });
  }
  props.addMessage({
    key: uuid(),
    role: 'user',
    placement: 'end',
    content: getSenderText(),
  });
  props.addMessage({
    key: uuid(),
    role: 'assistant',
    placement: 'start',
    content: '',
    loading: true,
  });
  senderRef.value?.clear();
  senderEmpty.value = true;

  let content = '';

  sseClient.post('/userCenter/bot/chat', data, {
    onMessage(res) {
      if (!res.data) {
        return;
      }
      const sseData = JSON.parse(res.data);
      const delta = sseData.payload?.delta;

      if (res.event === 'done') {
        btnLoading.value = false;
        getSessionList();
      }

      // 处理系统错误
      if (
        sseData?.domain === 'SYSTEM' &&
        sseData.payload?.code === 'SYSTEM_ERROR'
      ) {
        const errorMessage = sseData.payload.message;
        props.updateLastMessage({
          content: errorMessage,
          loading: false,
        });
        return;
      }

      if (sseData?.domain === 'TOOL') {
        props.updateLastMessage((message: MessageItem) => {
          const chains = cloneDeep(message.chains ?? []);
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
          return { chains };
        });
        props.stopThinking();
        return;
      }

      if (sseData.type === 'THINKING') {
        props.updateLastMessage((message: MessageItem) => {
          const chains = cloneDeep(message.chains ?? []);
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
          return { chains };
        });
      } else if (sseData.type === 'MESSAGE') {
        props.updateLastMessage({
          thinkingStatus: 'end',
          loading: false,
          content: (content += delta),
        });
        props.stopThinking();
      }
    },
    onError(err) {
      console.error(err);
      btnLoading.value = false;
    },
    onFinished() {
      senderRef.value?.clear();
      senderEmpty.value = true;
      btnLoading.value = false;
      props.updateLastMessage({ loading: false });
      props.stopThinking();
    },
  });
}
const isTool = (item: Think | Tool) => {
  return 'id' in item;
};
const isThink = (item: Think | Tool): item is Think => {
  return !('id' in item);
};
function getDisabled() {
  return senderEmpty.value;
}
const stopSse = () => {
  sseClient.abort();
  btnLoading.value = false;
};
const showHeaderFlog = ref(false);
const attachmentsRef = ref();
const senderRef = ref<InstanceType<typeof XSender>>();
const files = ref<any[]>([]);
const isDragOver = ref(false);

function handlePasteFile(_: any, fileList: FileList) {
  // 检查附件功能是否启用
  if (!attachmentEnabled.value) {
    return;
  }
  showHeaderFlog.value = true;
  files.value = [...fileList];
}
function openCloseHeader() {
  if (showHeaderFlog.value) {
    files.value = [];
  }
  showHeaderFlog.value = !showHeaderFlog.value;
}

// 语音功能是否启用
const voiceEnabled = computed(() => {
  // 默认关闭，只有明确设置为true时才启用
  return props.bot?.options?.voiceEnabled === true;
});

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

// 附件功能是否启用
const attachmentEnabled = computed(() => {
  // 默认启用，只有明确设置为false时才禁用
  return props.bot?.options?.attachmentEnabled !== false;
});

// 拖拽上传支持
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

watch(
  () => props.externalSendTrigger,
  async () => {
    if (!props.externalSendMessage?.trim()) {
      return;
    }
    await senderRef.value?.setText(props.externalSendMessage);
    senderEmpty.value = false;
    await nextTick();
    sendMessage();
  },
);
</script>

<template>
  <ElSender
    ref="senderRef"
    variant="updown"
    :custom-style="{ maxHeight: '120px' }"
    clearable
    :placeholder="$t('bot.sendMessage')"
    :class="{ 'drag-over': isDragOver }"
    @change="handleSenderChange"
    @submit="sendMessage"
    @paste-file="handlePasteFile"
    @dragover.prevent="handleDragOver"
    @dragenter="handleDragEnter"
    @dragleave="handleDragLeave"
    @drop="handleDrop"
  >
    <!-- 自定义 prefix 前缀 -->
    <!-- <template #prefix>
    </template> -->
    <!-- 自定义头部内容 -->
    <template v-if="showHeaderFlog" #header>
      <ChatFileUploader
        ref="attachmentsRef"
        :external-files="files"
        @delete-all="openCloseHeader"
        :max-size="10"
      />
    </template>
    <template #action-list>
      <div class="flex items-center gap-2">
          <template v-if="voiceEnabled">
          <ElButton v-if="isProcessing" link disabled>
            <ElIcon :size="18">
              <Loading />
            </ElIcon>
          </ElButton>
          <ElButton v-else link :class="{ 'voice-recording': isRecording }" @click="handleVoiceToggle">
            <ElIcon :size="18" :color="isRecording ? '#ef4444' : undefined">
              <IconifyIcon icon="mdi:microphone" />
            </ElIcon>
          </ElButton>
        </template>
        <ElButton v-if="attachmentEnabled" link @click="openCloseHeader">
          <ElIcon size="18">
            <Paperclip />
          </ElIcon>
        </ElButton>
        <ElButton v-if="btnLoading" circle @click="stopSse">
          <ElIcon size="30" color="#409eff"><SendingIcon /></ElIcon>
        </ElButton>
        <ElButton
          v-else
          type="primary"
          :icon="Promotion"
          :disabled="getDisabled()"
          @click="sendMessage"
          round
        />
      </div>
    </template>
  </ElSender>
</template>

<style lang="css" scoped>
/* ========== 拖拽上传视觉反馈 ========== */
:deep(.elx-x-sender).drag-over {
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

/* ========== 手机端适配 ========== */
@media (width <= 768px) {
  :deep(.elx-x-sender) {
    padding: 0 !important;
  }

  :deep(.elx-x-sender__content) {
    padding: 10px 14px !important;
    font-size: 15px;
    min-height: 22px !important;
    line-height: 1.5 !important;
  }

  :deep(.elx-x-sender__action-list) {
    gap: 2px !important;
    padding: 0 4px 4px !important;
  }
}
</style>
