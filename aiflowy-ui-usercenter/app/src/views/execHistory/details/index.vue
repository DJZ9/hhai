<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';

import { sortNodes } from '@aiflowy/utils';

import { ArrowLeft } from '@element-plus/icons-vue';
import {
  ElContainer,
  ElDivider,
  ElHeader,
  ElIcon,
  ElMain,
  ElSpace,
} from 'element-plus';

import { api } from '#/api/request';
import ExecResult from '#/views/ai/workflow/components/ExecResult.vue';
import WorkflowSteps from '#/views/ai/workflow/components/WorkflowSteps.vue';

const router = useRouter();
const route = useRoute();
const workflowJson = localStorage.getItem(`${route.params.id}-workflow-json`);
const nodeJson = JSON.parse(workflowJson || '{}');

onMounted(() => {
  getStepList();
  if (!workflowJson) {
    router.push({
      path: '/execHistory',
    });
  }
});
onBeforeRouteLeave(() => {
  localStorage.removeItem(`${route.params.id}-workflow-json`);
});

const stepList = ref<any>([]);

function getStepList() {
  api
    .get('/userCenter/workflowExecStep/getListByRecordId', {
      params: {
        recordId: route.params.id,
      },
    })
    .then((res) => {
      stepList.value = res.data;
    });
}
const result = computed(() => {
  if (stepList.value.length > 0) {
    const finalNode = stepList.value[stepList.value.length - 1];
    return {
      status: finalNode.status,
      result: JSON.parse(finalNode.output),
      message: finalNode.errorInfo,
    };
  } else {
    return {};
  }
});
const steps = computed(() => {
  return stepList.value.length > 0
    ? stepList.value.map((item: any) => {
        return {
          key: item.id,
          label: item.nodeName,
          status: item.status,
          message: item.errorInfo,
          result: JSON.parse(item.output || '{}'),
          original: {
            type: '',
          },
        };
      })
    : [];
});
</script>

<template>
  <ElContainer class="bg-background-deep h-full">
    <ElHeader class="!p-8 !pb-0" height="auto">
      <ElSpace class="cursor-pointer" :size="10" @click="router.back()">
        <ElIcon size="24"><ArrowLeft /></ElIcon>
        <h1 class="text-2xl font-medium">
          {{ route.query.title }}
        </h1>
      </ElSpace>
    </ElHeader>
    <ElMain class="h-full items-center !px-8">
      <div class="border-border flex h-full overflow-hidden rounded-xl border">
        <div
          class="bg-background flex flex-1 flex-col gap-6 overflow-hidden rounded-lg p-5"
        >
          <h1 class="text-base font-medium">{{ $t('execHistory.runResult') }}</h1>
          <div
            class="border-border bg-background-deep flex-1 rounded-lg border p-4"
          >
            <ExecResult
              v-if="nodeJson"
              workflow-id="workflowId"
              :node-json="sortNodes(nodeJson)"
              :polling-data="result"
              :show-message="false"
            />
          </div>
        </div>
        <ElDivider class="!border-border !m-0 !h-full" direction="vertical" />
        <div
          class="bg-background flex h-full flex-1 flex-col gap-6 rounded-lg p-5"
        >
          <h1 class="text-base font-medium">{{ $t('aiWorkflow.steps') }}</h1>
          <WorkflowSteps
            v-if="nodeJson"
            workflow-id="workflowId"
            :node-json="steps"
            :polling-data="result"
          />
        </div>
      </div>
    </ElMain>
  </ElContainer>
</template>
