<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, markRaw, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { InfoFilled, Refresh, Tickets, View } from '@element-plus/icons-vue';
import {
  ElAlert,
  ElButton,
  ElCard,
  ElCol,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElMessage,
  ElRow,
  ElSpace,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { api } from '#/api/request';
import HeaderSearch from '#/components/headerSearch/HeaderSearch.vue';
import PageData from '#/components/page/PageData.vue';
import { $t } from '#/locales';

const route = useRoute();

const botIdQuery = computed(() => {
  const botId = route.query.botId;
  return botId ? { botId } : {};
});

onMounted(() => {
  getConversationList();
  loadSystemModelConfig();
});

const pageDataRef = ref();
const detailDialog = ref(false);
const currentConversation = ref<any>({});
const analysisResult = ref<any>({});
const loading = ref(false);
const systemModelConfig = ref<any>({
  model_of_chat: '',
  chatgpt_model_name: '',
});

const headerButtons = [
  {
    key: 'analyzeToday',
    text: $t('conversationAnalysis.analyzeToday'),
    icon: markRaw(Refresh),
    type: 'success',
    data: { action: 'analyzeToday' },
  },
];

const conversationList = ref<any[]>([]);

function getConversationList() {
  api.get('/api/v1/bot/userProfile/list').then((res) => {
    if (res.errorCode === 0) {
      conversationList.value = res.data || [];
    }
  });
}

function loadSystemModelConfig() {
  api
    .get('/api/v1/sysOption/list', {
      params: { keys: ['model_of_chat', 'chatgpt_model_name'] },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        systemModelConfig.value = {
          model_of_chat: res.data?.model_of_chat || '',
          chatgpt_model_name: res.data?.chatgpt_model_name || '',
        };
      }
    });
}

const handleSearch = (params: string) => {
  pageDataRef.value.setQuery({ title: params });
};

function reset(formEl?: FormInstance) {
  formEl?.resetFields();
  pageDataRef.value.setQuery({});
}

function analyzeTodayConversations() {
  ElMessage.info($t('conversationAnalysis.startTodayAnalyzing'));
  api.post('/api/v1/bot/userProfile/analyzeToday').then((res) => {
    if (res.errorCode === 0) {
      ElMessage.success($t('conversationAnalysis.todayAnalyzeSuccess'));
      setTimeout(() => {
        getConversationList();
        reset();
      }, 2000);
    }
  });
}

function handleButtonClick(event: any) {
  const data = event.data;
  if (data.action === 'analyzeToday') {
    analyzeTodayConversations();
  }
}

function analyzeConversation(conversationId: string) {
  ElMessage.info($t('conversationAnalysis.startAnalyzing'));
  api.post(`/api/v1/bot/userProfile/${conversationId}/analyze`).then((res) => {
    if (res.errorCode === 0) {
      ElMessage.success($t('conversationAnalysis.analyzeSuccess'));
      setTimeout(() => {
        getAnalysisResult(conversationId);
      }, 3000);
    }
  });
}

function getAnalysisResult(conversationId: string) {
  loading.value = true;
  api
    .get(`/api/v1/bot/userProfile/${conversationId}/analysis`)
    .then((res) => {
      if (res.errorCode === 0) {
        analysisResult.value = res.data || {};
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

function viewDetail(row: any) {
  currentConversation.value = row;
  detailDialog.value = true;
  if (row.options?.analysis) {
    analysisResult.value = row.options.analysis;
  } else {
    getAnalysisResult(row.id);
  }
}

function formatDate(timestamp: number) {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  return date.toLocaleString();
}
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-6">
    <!-- 系统模型提示区域 -->
    <div class="bg-background border-border rounded-lg border p-5">
      <ElAlert type="info" :closable="false" show-icon>
        <template #icon>
          <InfoFilled />
        </template>
        <div class="flex items-center gap-2">
          <span>{{ $t('conversationAnalysis.usingSystemModel') }}</span>
          <span
            v-if="systemModelConfig.model_of_chat"
            class="text-sm text-gray-600"
          >
            ({{ $t('conversationAnalysis.provider') }}:
            {{ systemModelConfig.model_of_chat }},
            {{ $t('conversationAnalysis.model') }}:
            {{
              systemModelConfig.chatgpt_model_name ||
              $t('conversationAnalysis.notConfigured')
            }})
          </span>
        </div>
      </ElAlert>
    </div>

    <!-- 会话列表区域 -->
    <div class="bg-background border-border flex-1 rounded-lg border p-5">
      <HeaderSearch
        class="mb-4"
        :buttons="headerButtons"
        @search="handleSearch"
        @button-click="handleButtonClick"
      />
      <PageData
        ref="pageDataRef"
        page-url="/api/v1/bot/userProfile/page"
        :page-size="10"
        :page-sizes="[10, 20, 30, 50]"
        :extra-query-params="botIdQuery"
      >
        <template #default="{ pageList }">
          <ElTable :data="pageList" border>
            <ElTableColumn
              prop="title"
              :label="$t('conversationAnalysis.conversationTitle')"
              min-width="150"
            >
              <template #default="{ row }">
                {{ row.title || $t('conversationAnalysis.newConversation') }}
              </template>
            </ElTableColumn>
            <ElTableColumn
              :label="$t('conversationAnalysis.chatAssistant')"
              min-width="120"
            >
              <template #default="{ row }">
                {{ row.bot?.title || '-' }}
              </template>
            </ElTableColumn>
            <ElTableColumn
              :label="$t('conversationAnalysis.coreDemand')"
              min-width="120"
            >
              <template #default="{ row }">
                {{ row.options?.analysis?.coreDemand || '-' }}
              </template>
            </ElTableColumn>
            <ElTableColumn
              :label="$t('conversationAnalysis.userProfile')"
              min-width="100"
            >
              <template #default="{ row }">
                <ElSpace direction="vertical" :size="2" alignment="start">
                  <span>
                    {{ $t('conversationAnalysis.occupation') }}:
                    {{ row.options?.analysis?.userProfile?.occupation || '-' }}
                  </span>
                  <span>
                    {{ $t('conversationAnalysis.interest') }}:
                    {{ row.options?.analysis?.userProfile?.interest || '-' }}
                  </span>
                  <span>
                    {{ $t('conversationAnalysis.personality') }}:
                    {{ row.options?.analysis?.userProfile?.personality || '-' }}
                  </span>
                  <span>
                    {{ $t('conversationAnalysis.communicationStyle') }}:
                    {{
                      row.options?.analysis?.userProfile?.communicationStyle ||
                      '-'
                    }}
                  </span>
                </ElSpace>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="created"
              :label="$t('conversationAnalysis.createdTime')"
              width="180"
            />
            <ElTableColumn
              :label="$t('common.handle')"
              width="245"
              align="right"
            >
              <template #default="{ row }">
                <ElSpace :size="0">
                  <ElButton
                    link
                    type="primary"
                    :icon="View"
                    @click="viewDetail(row)"
                  >
                    {{ $t('conversationAnalysis.viewDetail') }}
                  </ElButton>
                  <ElButton
                    link
                    type="primary"
                    :icon="Tickets"
                    @click="$router.push(`/ai/conversationAnalysis/${row.id}`)"
                  >
                    {{ $t('conversationAnalysis.conversationRecord') }}
                  </ElButton>
                  <ElButton
                    link
                    type="warning"
                    :icon="Refresh"
                    @click="analyzeConversation(row.id)"
                  >
                    {{ $t('conversationAnalysis.analyze') }}
                  </ElButton>
                </ElSpace>
                <!-- <div class="flex items-center gap-3">
                  <ElButton
                    link
                    type="primary"
                    :icon="View"
                    @click="viewDetail(row)"
                  >
                    {{ $t('conversationAnalysis.viewDetail') }}
                  </ElButton>

                  <ElDropdown>
                    <ElButton link :icon="MoreFilled" />

                    <template #dropdown>
                      <ElDropdownMenu>
                        <ElDropdownItem
                          :icon="Tickets"
                          @click="
                            $router.push(`/ai/conversationAnalysis/${row.id}`)
                          "
                        >
                          {{ $t('conversationAnalysis.conversationRecord') }}
                        </ElDropdownItem>
                        <ElDropdownItem
                          v-if="!row.options?.analysis"
                          :icon="Refresh"
                          @click="analyzeConversation(row.id)"
                        >
                          {{ $t('conversationAnalysis.analyze') }}
                        </ElDropdownItem>
                      </ElDropdownMenu>
                    </template>
                  </ElDropdown>
                </div> -->
              </template>
            </ElTableColumn>
          </ElTable>
        </template>
      </PageData>
    </div>

    <ElDialog
      v-model="detailDialog"
      :title="$t('conversationAnalysis.viewDetail')"
      width="min(800px,80vw)"
      destroy-on-close
    >
      <div v-loading="loading">
        <ElRow :gutter="20">
          <ElCol :span="24">
            <ElCard shadow="never" class="mb-4">
              <template #header>
                <span class="font-bold">{{
                  $t('conversationAnalysis.basicInfo')
                }}</span>
              </template>
              <ElDescriptions :column="2" border>
                <ElDescriptionsItem
                  :label="`${$t('conversationAnalysis.conversationTitle')}ID`"
                >
                  {{ currentConversation.id }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.conversationTitle')"
                >
                  {{
                    currentConversation.title ||
                    $t('conversationAnalysis.newConversation')
                  }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.chatAssistant')"
                >
                  {{ currentConversation.bot?.title || '-' }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.createdTime')"
                >
                  {{ currentConversation.created }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.analysisTime')"
                >
                  {{ formatDate(analysisResult.analysisTime) }}
                </ElDescriptionsItem>
              </ElDescriptions>
            </ElCard>
          </ElCol>
        </ElRow>

        <ElRow v-if="analysisResult.conversationTheme" :gutter="20">
          <ElCol :span="24">
            <ElCard shadow="never" class="mb-4">
              <template #header>
                <span class="font-bold">{{
                  $t('conversationAnalysis.conversationAnalysis')
                }}</span>
              </template>
              <ElDescriptions :column="1" border>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.conversationTheme')"
                >
                  <ElTag type="primary">
                    {{ analysisResult.conversationTheme }}
                  </ElTag>
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.coreDemand')"
                >
                  {{ analysisResult.coreDemand }}
                </ElDescriptionsItem>
                <ElDescriptionsItem :label="$t('conversationAnalysis.summary')">
                  {{ analysisResult.summary }}
                </ElDescriptionsItem>
              </ElDescriptions>
            </ElCard>
          </ElCol>
        </ElRow>

        <ElRow v-if="analysisResult.userProfile" :gutter="20">
          <ElCol :span="24">
            <ElCard shadow="never">
              <template #header>
                <span class="font-bold">{{
                  $t('conversationAnalysis.userProfile')
                }}</span>
              </template>
              <ElDescriptions :column="2" border>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.occupation')"
                >
                  {{
                    analysisResult.userProfile?.occupation ||
                    $t('conversationAnalysis.unknown')
                  }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.interest')"
                >
                  {{
                    analysisResult.userProfile?.interest ||
                    $t('conversationAnalysis.unknown')
                  }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.personality')"
                >
                  {{
                    analysisResult.userProfile?.personality ||
                    $t('conversationAnalysis.unknown')
                  }}
                </ElDescriptionsItem>
                <ElDescriptionsItem
                  :label="$t('conversationAnalysis.communicationStyle')"
                >
                  {{
                    analysisResult.userProfile?.communicationStyle ||
                    $t('conversationAnalysis.unknown')
                  }}
                </ElDescriptionsItem>
              </ElDescriptions>
            </ElCard>
          </ElCol>
        </ElRow>

        <ElRow v-if="!analysisResult.conversationTheme" :gutter="20">
          <ElCol :span="24">
            <div
              class="flex flex-col items-center justify-center py-10 text-gray-400"
            >
              <span class="mb-4">{{
                $t('conversationAnalysis.noAnalysisData')
              }}</span>
              <ElButton
                type="primary"
                :icon="Refresh"
                @click="analyzeConversation(currentConversation.id)"
              >
                {{ $t('conversationAnalysis.startAnalysis') }}
              </ElButton>
            </div>
          </ElCol>
        </ElRow>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
:deep(.el-card__header) {
  padding: 12px 16px;
}

:deep(.el-dialog__body) {
  overflow: hidden auto;
}
</style>
