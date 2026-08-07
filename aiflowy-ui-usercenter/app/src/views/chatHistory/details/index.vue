<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { IconifyIcon } from '@aiflowy/icons';
import { copyToClipboard } from '@aiflowy/utils';

import { ArrowLeft, Delete, MoreFilled } from '@element-plus/icons-vue';
import {
  ElButton,
  ElContainer,
  ElDropdown,
  ElDropdownItem,
  ElHeader,
  ElMain,
  ElMessage,
} from 'element-plus';
import { tryit } from 'radash';

import { api } from '#/api/request';
import { ChatBubbleList } from '#/components/chat';
import { router } from '#/router';
import { $t } from '#/locales';

const route = useRoute();

const ids = reactive({
  botId: '',
  conversationId: '',
});
const conversationInfo = ref<any>();
const messageList = ref<any[]>([]);
const loading = ref(true);

onMounted(() => {
  if (route.params.id) {
    ids.conversationId = route.params.id as string;
    getConversationDetails();
  }
});

function getConversationDetails() {
  api
    .get('/userCenter/botConversation/detail', {
      params: {
        id: ids.conversationId,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        conversationInfo.value = res.data;
        ids.botId = res.data.botId;
        getMessageList();
      }
    });
}
function getMessageList() {
  api
    .get('/userCenter/botMessage/getMessages', {
      params: ids,
    })
    .then((res) => {
      if (res.errorCode === 0) {
        messageList.value = res.data;
        loading.value = false;
      }
    });
}
async function handleShare() {
  const shareLink = import.meta.env.DEV
    ? `${location.origin}/share/${ids.conversationId}`
    : `${location.origin}/#/share/${ids.conversationId}`;
  const { success, error } = await copyToClipboard(shareLink);

  if (success) {
    ElMessage.success($t('chatHistory.shareSuccess'));
  } else {
    ElMessage.error(error);
  }
}
async function handleDelete() {
  const [, res] = await tryit(api.post)('/userCenter/botConversation/remove', {
    id: ids.conversationId,
  });

  if (res && res.errorCode === 0) {
    ElMessage.success($t('chatHistory.deleteSuccess'));
    router.back();
  }
}
</script>

<template>
  <ElContainer class="bg-background h-full">
    <ElHeader height="100px" class="border-border border-b !pr-10">
      <div class="flex h-full w-full items-center justify-between">
        <!-- Left -->
        <div class="flex items-center gap-3">
          <ElButton
            link
            style="font-size: 20px"
            :icon="ArrowLeft"
            @click="router.back()"
          />
          <div class="flex flex-col gap-2">
            <div class="flex items-center gap-2">
              <span class="text-lg font-medium">{{
                conversationInfo?.title
              }}</span>
              <div
                v-if="conversationInfo?.bot.title"
                class="text-foreground/70 rounded bg-[var(--el-fill-color-light)] p-1 text-xs"
              >
                {{ conversationInfo.bot.title }}
              </div>
            </div>
            <span class="text-foreground/50 text-sm">{{
              conversationInfo?.created
            }}</span>
          </div>
        </div>

        <!-- Right -->
        <div class="flex items-center gap-5">
          <ElButton link style="font-size: 20px" @click="handleShare">
            <template #icon>
              <IconifyIcon icon="svg:share" />
            </template>
          </ElButton>
          <ElDropdown>
            <ElButton link style="font-size: 20px" :icon="MoreFilled" />

            <template #dropdown>
              <ElDropdownItem
                style="color: var(--el-color-danger)"
                :icon="Delete"
                @click="handleDelete"
              >
                {{ $t('chatHistory.delete') }}
              </ElDropdownItem>
            </template>
          </ElDropdown>
        </div>
      </div>
    </ElHeader>
    <ElMain class="relative" v-loading="loading">
      <div
        class="absolute bottom-5 left-1/2 top-5 w-full max-w-[1000px] -translate-x-1/2"
      >
        <ChatBubbleList
          :bot="conversationInfo?.bot"
          :messages="messageList"
          :editable="false"
          :open-editor="() => {}"
        />
      </div>
    </ElMain>
  </ElContainer>
</template>

<style lang="css" scoped>
:deep(.el-bubble-list) {
  max-height: 100%;
}

:deep(.el-bubble-content-wrapper .el-bubble-content) {
  --bubble-content-max-width: calc(100% - 52px);
}
</style>
