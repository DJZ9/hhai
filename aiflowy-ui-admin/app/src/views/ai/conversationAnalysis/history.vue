<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { extractJsonAndRest } from '@aiflowy/utils';

import { ArrowLeft } from '@element-plus/icons-vue';
import { ElButton, ElContainer, ElHeader, ElMain } from 'element-plus';

import { api } from '#/api/request';
import BubbleList from '#/components/chat/bubbleList.vue';
import { router } from '#/router';

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
        messageList.value = res.data.map((item: any) => {
          const result: any = {
            ...item,
            role: item.role === 'user' ? 'user' : 'assistant',
          };

          if (item.role === 'form') {
            const formData = JSON.parse(item.content);
            if (item.formValues) {
              for (let i = 0; i < formData.formData.length; i++) {
                formData.formData[i].defaultValue =
                  item.formValues[formData.formData[i].attrName];
              }
            }

            result.formData = formData;
            result.formDisabled = true;
            delete result.content;
          } else if (item.role === 'assistant') {
            const [json, rest] = extractJsonAndRest(item.content);
            result.formData = json;
            result.content = rest;
            result.formDisabled = true;
          }

          return result;
        });
        loading.value = false;
      }
    });
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
      </div>
    </ElHeader>
    <ElMain class="relative" v-loading="loading">
      <div
        class="absolute bottom-5 left-1/2 top-5 w-full max-w-[1000px] -translate-x-1/2"
      >
        <BubbleList :bot="conversationInfo?.bot" :messages="messageList" />
      </div>
    </ElMain>
  </ElContainer>
</template>

<style lang="css" scoped>
:deep(.elx-bubble-list) {
  max-height: 100%;
}

/* :deep(.elx-bubble__content-wrapper .elx-bubble__content) {
  --elx-bubble-content-max-width: calc(100% - 52px);
} */
</style>
