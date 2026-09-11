<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { ElContainer, ElHeader, ElMain } from 'element-plus';

import { api } from '#/api/request';
import { ChatBubbleList } from '#/components/chat';

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
</script>

<template>
  <div class="h-full w-full px-12 py-8 max-sm:p-3">
    <ElContainer class="bg-background h-full">
      <ElHeader
        height="80px"
        class="rounded-xl bg-[#F8F8F9] !pr-9 max-sm:!h-16 max-sm:!pr-3"
      >
        <div class="flex h-full w-full items-center justify-between">
          <!-- Left -->
          <div class="flex flex-col gap-2">
            <div class="flex items-center gap-2">
              <span class="text-lg font-medium max-sm:text-base">{{
                conversationInfo?.title
              }}</span>
              <div
                v-if="conversationInfo?.bot.title"
                class="text-foreground/70 rounded bg-[#ECECEE] p-1 text-xs"
              >
                {{ conversationInfo.bot.title }}
              </div>
            </div>
            <span class="text-foreground/50 text-sm max-sm:text-xs">{{
              conversationInfo?.created
            }}</span>
          </div>

          <!-- Right -->
          <img src="/logo.svg" class="w-40 max-sm:w-28" />
        </div>
      </ElHeader>
      <ElMain class="relative max-sm:mt-2 max-sm:!p-0" v-loading="loading">
        <div
          class="absolute bottom-5 left-1/2 top-5 w-full max-w-[1000px] -translate-x-1/2 max-sm:bottom-0 max-sm:top-0"
        >
          <ChatBubbleList
            class="relative mx-auto h-full max-w-[1000px]"
            :bot="conversationInfo?.bot"
            :messages="messageList"
            :editable="false"
            :open-editor="() => {}"
          />
        </div>
      </ElMain>
    </ElContainer>
  </div>
</template>

<style lang="css" scoped>
:deep(.elx-bubble-list) {
  max-height: 100%;
}

:deep(.elx-bubble__content-wrapper .elx-bubble__content) {
  --elx-bubble-content-max-width: calc(100% - 52px);
}

@media not all and (min-width: 640px) {
  :deep(.elx-bubble) {
    gap: 8px;
  }

  :deep(.el-avatar) {
    width: 30px;
    height: 30px;
  }

  :deep(.elx-bubble__content-wrapper .elx-bubble__content) {
    --elx-bubble-content-max-width: calc(100% - 38px);
  }
}
</style>
