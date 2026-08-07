<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { cn } from '@aiflowy/utils';

import { Search } from '@element-plus/icons-vue';
import { ElContainer, ElHeader, ElInput, ElMain, ElSpace } from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';
import defaultBotAvatar from '#/assets/ai/workflow/workflowIcon.png';
import {
  Card,
  CardAvatar,
  CardContent,
  CardDescription,
  CardTitle,
} from '#/components/card';

const categories = ref<any[]>([]);
const workflowList = ref<any[]>([]);
const queryParams = ref<any>({});
const pageLoading = ref(false);
const activeTag = ref('');
onMounted(async () => {
  getWorkflowList();
  getCategoryList();
});
function getCategoryList() {
  api.get('/userCenter/workflowCategory/list').then((res) => {
    categories.value = [
      {
        id: '',
        categoryName: '全部',
      },
      ...res.data,
    ];
  });
}
function getWorkflowList() {
  pageLoading.value = true;
  api
    .get('/userCenter/workflow/list', {
      params: { ...queryParams.value, status: 1 },
    })
    .then((res) => {
      pageLoading.value = false;
      workflowList.value = res.data;
    });
}
function handleTagClick(tag: any) {
  activeTag.value = tag;
  queryParams.value.categoryId = tag;
  getWorkflowList();
}
</script>

<template>
  <ElContainer class="bg-background-deep h-full">
    <ElHeader class="!h-auto !p-8 !pb-0">
      <ElSpace direction="vertical" :size="24" alignment="flex-start">
        <h1 class="text-2xl font-medium">{{ $t('bot.title') }}</h1>
        <ElSpace :size="20">
          <ElInput
            :placeholder="$t('bot.searchPlaceholder')"
            v-model="queryParams.title"
            @keyup.enter="getWorkflowList"
            :prefix-icon="Search"
          />
          <ElSpace :size="12">
            <button
              type="button"
              :class="
                cn(
                  'border-border text-foreground bg-background h-[35px] w-[94px] rounded-3xl border text-sm',
                  activeTag === category.id
                    ? 'border-primary text-primary bg-primary/10'
                    : 'hover:bg-accent',
                )
              "
              v-for="category in categories"
              :key="category.id"
              @click="handleTagClick(category.id)"
            >
              {{ category.categoryName }}
            </button>
          </ElSpace>
        </ElSpace>
      </ElSpace>
    </ElHeader>
    <ElMain class="!px-8">
      <div
        class="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-5"
        v-loading="pageLoading"
      >
        <RouterLink
          v-for="workflow in workflowList"
          :key="workflow.id"
          :to="`/workflow/${workflow.id}`"
        >
          <Card
            class="border-border bg-background h-[168px] max-w-none flex-col gap-3 rounded-xl border p-6 pb-0 pr-4 transition hover:-translate-y-2 hover:shadow-[0px_2px_16px_0px_rgba(6,27,57,0.07)]"
          >
            <CardContent class="flex-row items-center gap-3">
              <CardAvatar
                :src="workflow.icon"
                :default-avatar="defaultBotAvatar"
              />
              <CardTitle :title="workflow.title">
                {{ workflow.title }}
              </CardTitle>
            </CardContent>
            <CardDescription
              class="line-clamp-2 text-sm text-[#566882]"
              :title="workflow.description"
            >
              {{ workflow.description }}
            </CardDescription>
          </Card>
        </RouterLink>
      </div>
    </ElMain>
  </ElContainer>
</template>

<style lang="css" scoped>
.el-input :deep(.el-input__wrapper) {
  --el-input-border-radius: 18px;
  --el-input-border-color: #e6e9ee;
}
</style>
