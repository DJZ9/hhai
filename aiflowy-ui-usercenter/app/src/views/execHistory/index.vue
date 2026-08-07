<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Delete, MoreFilled } from '@element-plus/icons-vue';
import {
  ElButton,
  ElContainer,
  ElDatePicker,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElHeader,
  ElMain,
  ElSelect,
  ElSpace,
  ElTag,
  ElText,
} from 'element-plus';

import { api } from '#/api/request';
import PageData from '#/components/page/PageData.vue';
import { $t } from '#/locales';

onMounted(() => {
  getWorkflowList();
});
const options = [
  {
    value: 1,
    label: $t('aiWorkflowExecRecord.status1'),
  },
  {
    value: 5,
    label: $t('aiWorkflowExecRecord.status5'),
  },
  {
    value: 10,
    label: $t('aiWorkflowExecRecord.status10'),
  },
  {
    value: 20,
    label: $t('aiWorkflowExecRecord.status20'),
  },
  {
    value: 21,
    label: $t('aiWorkflowExecRecord.status21'),
  },
  {
    value: 22,
    label: $t('aiWorkflowExecRecord.status22'),
  },
];

const listTitles = [
  $t('execHistory.taskName'),
  $t('execHistory.startTime'),
  $t('execHistory.duration'),
  $t('execHistory.status'),
  $t('execHistory.operation'),
];

const queryParams = ref<any>({});
const pageRef = ref();
const workflowList = ref<any[]>([]);
const router = useRouter();
function search() {
  getDateRange();
  pageRef.value.setQuery(queryParams.value);
}
function getWorkflowList() {
  api
    .get('/userCenter/workflow/list', {
      params: { ...queryParams.value },
    })
    .then((res) => {
      workflowList.value = res.data;
    });
}
function getTagType(row: any) {
  switch (row.status) {
    case 1: {
      return 'primary';
    }
    case 5: {
      return 'warning';
    }
    case 10: {
      return 'danger';
    }
    case 20: {
      return 'success';
    }
    case 21: {
      return 'danger';
    }
    default: {
      return 'info';
    }
  }
}
function toDetail(record: any) {
  router.push({
    path: `/execHistory/${record.id}`,
    query: {
      title: record.title,
    },
  });
  localStorage.setItem(`${record.id}-workflow-json`, record.workflowJson);
}
const dateRange = ref<any>('');
function getDateRange() {
  if (dateRange.value) {
    queryParams.value.queryBegin = dateRange.value[0];
    queryParams.value.queryEnd = dateRange.value[1];
  } else {
    queryParams.value.queryBegin = '';
    queryParams.value.queryEnd = '';
  }
}
</script>

<template>
  <ElContainer class="bg-background-deep h-full">
    <ElHeader class="!h-auto !p-8 !pb-0">
      <ElSpace direction="vertical" :size="24" alignment="flex-start">
        <h1 class="text-2xl font-medium">{{ $t('execHistory.title') }}</h1>
        <div class="flex items-center gap-8">
          <div class="flex items-center gap-4">
            <span class="text-nowrap text-sm">{{ $t('execHistory.execStatus') }}</span>
            <ElSelect
              clearable
              v-model="queryParams.status"
              :options="options"
              :placeholder="$t('execHistory.selectExecStatus')"
              @change="search"
            />
          </div>
          <div class="flex items-center gap-4">
            <span class="text-nowrap text-sm">{{ $t('execHistory.filterAgent') }}</span>
            <ElSelect
              class="bot-select"
              clearable
              :options="workflowList"
              :placeholder="$t('execHistory.agentPlaceholder')"
              v-model="queryParams.workflowId"
              @change="search"
              :props="{
                value: 'id',
                label: 'title',
              }"
            />
          </div>
          <div class="flex items-center gap-4">
            <span class="text-nowrap text-sm">{{ $t('execHistory.filterTime') }}</span>
            <ElDatePicker
              clearable
              type="daterange"
              v-model="dateRange"
              :start-placeholder="$t('execHistory.selectStartDate')"
              :end-placeholder="$t('execHistory.selectEndDate')"
              @change="search"
              value-format="YYYY-MM-DD"
            />
          </div>
        </div>
      </ElSpace>
    </ElHeader>
    <ElMain class="!px-8">
      <ElContainer class="bg-background rounded-lg p-5">
        <ElHeader
          class="dark:bg-accent grid grid-cols-[repeat(4,minmax(0,1fr))_120px] place-items-center rounded-lg bg-[#f7f9fd] !p-0"
          height="54px"
        >
          <span
            class="text-accent-foreground text-sm"
            v-for="title in listTitles"
            :key="title"
          >
            {{ title }}
          </span>
        </ElHeader>
        <ElMain class="!p-0">
          <div class="flex flex-col items-center gap-5">
            <div class="w-full">
              <PageData
                page-url="/userCenter/workflowExecResult/getPage"
                ref="pageRef"
              >
                <template #default="{ pageList }">
                  <div
                    class="text-foreground/90 grid h-[60px] grid-cols-[repeat(4,minmax(0,1fr))_120px] place-items-center text-sm hover:bg-[var(--el-fill-color-light)]"
                    v-for="record in pageList"
                    :key="record.id"
                  >
                    <ElText truncated>{{ record.title }}</ElText>
                    <span>{{ record.startTime }}</span>
                    <span>{{ record.execTime }} ms</span>
                    <span>
                      <ElTag :type="getTagType(record)">
                        {{ $t(`aiWorkflowExecRecord.status${record.status}`) }}
                      </ElTag>
                    </span>

                    <div class="flex items-center gap-3">
                      <ElButton
                        class="[--el-font-weight-primary:400]"
                        link
                        type="primary"
                        @click="toDetail(record)"
                      >
                        {{ $t('execHistory.viewDetail') }}
                      </ElButton>

                      <ElDropdown>
                        <ElButton :icon="MoreFilled" link />

                        <template #dropdown>
                          <ElDropdownMenu>
                            <ElDropdownItem>
                              <ElButton type="danger" :icon="Delete" link>
                                {{ $t('button.delete') }}
                              </ElButton>
                            </ElDropdownItem>
                          </ElDropdownMenu>
                        </template>
                      </ElDropdown>
                    </div>
                  </div>
                </template>
              </PageData>
            </div>
          </div>
        </ElMain>
      </ElContainer>
    </ElMain>
  </ElContainer>
</template>

<style lang="css" scoped>
.el-select {
  --el-select-width: 165px;
}

.el-select.bot-select {
  --el-select-width: 343px;
}

.el-select :deep(.el-select__wrapper) {
  --el-border-radius-base: 8px;
}
</style>
