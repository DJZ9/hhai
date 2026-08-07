<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { sortNodes } from '@aiflowy/utils';

import { ArrowLeft } from '@element-plus/icons-vue';
import {
  ElAside,
  ElButton,
  ElContainer,
  ElHeader,
  ElIcon,
  ElMain,
  ElText,
} from 'element-plus';

import { api } from '#/api/request';
import defaultBotAvatar from '#/assets/defaultBotAvatar.png';
import {
  Card,
  CardAvatar,
  CardContent,
  CardDescription,
  CardTitle,
} from '#/components/card';
import ExecResult from '#/views/ai/workflow/components/ExecResult.vue';
import WorkflowForm from '#/views/ai/workflow/components/WorkflowForm.vue';
import WorkflowSteps from '#/views/ai/workflow/components/WorkflowSteps.vue';

onMounted(async () => {
  pageLoading.value = true;
  await Promise.all([getWorkflowInfo(workflowId.value), getRunningParams()]);
  pageLoading.value = false;
});
const pageLoading = ref(false);
const route = useRoute();
const router = useRouter();
const workflowId = ref(route.params.id);
const workflowInfo = ref<any>({});
const runParams = ref<any>(null);
const initState = ref(false);
const tinyFlowData = ref<any>(null);
const workflowForm = ref();
async function getWorkflowInfo(workflowId: any) {
  api.get(`/userCenter/workflow/detail?id=${workflowId}`).then((res) => {
    workflowInfo.value = res.data;
    tinyFlowData.value = workflowInfo.value.content
      ? JSON.parse(workflowInfo.value.content)
      : {};
  });
}
async function getRunningParams() {
  api
    .get(`/userCenter/workflow/getRunningParameters?id=${workflowId.value}`)
    .then((res) => {
      runParams.value = res.data;
    });
}
function onSubmit() {
  initState.value = !initState.value;
}
function resumeChain(data: any) {
  workflowForm.value?.resume(data);
}
const chainInfo = ref<any>(null);
function onAsyncExecute(info: any) {
  chainInfo.value = info;
}
</script>

<template>
  <ElContainer class="h-full">
    <ElHeader class="!px-8 !py-4" height="fit-content">
      <div class="flex flex-col gap-6">
        <div
          class="flex cursor-pointer items-center gap-2.5"
          @click="router.back()"
        >
          <ElIcon size="24"><ArrowLeft /></ElIcon>
          <ElText truncated class="!text-lg font-medium">
            {{ workflowInfo?.title }}
          </ElText>
        </div>
        <div
          class="bg-background border-border flex items-center justify-between overflow-hidden rounded-lg border px-5 py-6"
        >
          <Card
            class="w-full max-w-none cursor-auto items-center gap-7 overflow-hidden"
          >
            <CardAvatar
              :size="72"
              :src="workflowInfo?.icon"
              :default-avatar="defaultBotAvatar"
            />
            <CardContent class="gap-3">
              <CardTitle
                class="text-lg font-medium"
                :title="workflowInfo?.title"
              >
                {{ workflowInfo?.title }}
              </CardTitle>
              <CardDescription
                class="text-sm"
                :title="workflowInfo?.description"
              >
                {{ workflowInfo?.description }}
              </CardDescription>
            </CardContent>
          </Card>
          <RouterLink to="/execHistory">
            <ElButton type="primary" size="large" round plain>
              {{ $t('bot.execRecord') }}
            </ElButton>
          </RouterLink>
        </div>
      </div>
    </ElHeader>
    <ElMain class="!px-8 !pb-4 !pt-0">
      <ElContainer class="h-full gap-4">
        <ElAside
          width="366px"
          class="border-border bg-background flex flex-col gap-6 rounded-lg border p-5"
        >
          <h1 class="text-base font-medium">{{ $t('bot.inputParams') }}</h1>
          <WorkflowForm
            v-if="runParams && tinyFlowData"
            ref="workflowForm"
            :workflow-id="workflowId"
            :workflow-params="runParams"
            :on-submit="onSubmit"
            :on-async-execute="onAsyncExecute"
            :tiny-flow-data="tinyFlowData"
          />
        </ElAside>
        <ElAside width="366px">
          <div
            class="border-border bg-background flex h-full flex-col gap-6 rounded-lg border p-5"
          >
            <h1 class="text-base font-medium">{{ $t('bot.execSteps') }}</h1>
            <WorkflowSteps
              v-if="tinyFlowData"
              :workflow-id="workflowId"
              :node-json="sortNodes(tinyFlowData)"
              :init-signal="initState"
              :polling-data="chainInfo"
              @resume="resumeChain"
            />
          </div>
        </ElAside>
        <div
          class="bg-background border-border flex flex-1 flex-col gap-6 rounded-lg border p-5"
        >
          <h1 class="text-base font-medium">{{ $t('bot.runResult') }}</h1>
          <div
            class="bg-background-deep border-border flex-1 rounded-lg border p-4"
          >
            <ExecResult
              v-if="tinyFlowData"
              :workflow-id="workflowId"
              :node-json="sortNodes(tinyFlowData)"
              :init-signal="initState"
              :polling-data="chainInfo"
            />
          </div>
        </div>
      </ElContainer>
    </ElMain>
  </ElContainer>
</template>
