<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';

import { ArrowLeft, View } from '@element-plus/icons-vue';
import { ElButton, ElDialog, ElTable, ElTableColumn } from 'element-plus';

import PageData from '#/components/page/PageData.vue';
import { $t } from '#/locales';
import { router } from '#/router';

const route = useRoute();

const botId = route.params.botId as string;
const pageDataRef = ref();
const detailDialog = ref(false);
const detailContent = ref('');

function formatJson(val: string) {
  if (!val) return '-';
  try {
    const obj = JSON.parse(val);
    return JSON.stringify(obj, null, 2);
  } catch {
    return val;
  }
}

function showDetail(content: string) {
  detailContent.value = formatJson(content);
  detailDialog.value = true;
}
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-6">
    <div class="flex items-center gap-4">
      <ElButton :icon="ArrowLeft" @click="router.back()">
        {{ $t('toolCallRecord.return') }}
      </ElButton>
      <h2 class="text-lg font-bold">{{ $t('toolCallRecord.title') }}</h2>
    </div>

    <div class="bg-background border-border flex-1 rounded-lg border p-5">
      <PageData
        ref="pageDataRef"
        page-url="/api/v1/bot/toolCallRecord/page"
        :page-size="10"
        :page-sizes="[10, 20, 30, 50]"
        :extra-query-params="{ botId }"
      >
        <template #default="{ pageList }">
          <ElTable :data="pageList" border>
            <ElTableColumn
              prop="conversationId"
              :label="$t('toolCallRecord.conversationId')"
              width="180"
            />
            <ElTableColumn
              :label="$t('toolCallRecord.creator')"
              width="120"
            >
              <template #default="{ row }">
                {{ row.creator?.loginName || '-' }}
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="botTitle"
              :label="$t('toolCallRecord.botName')"
              min-width="120"
            />
            <ElTableColumn
              prop="toolName"
              :label="$t('toolCallRecord.toolName')"
              min-width="120"
            />
            <ElTableColumn
              prop="inputParams"
              :label="$t('toolCallRecord.inputParams')"
              min-width="150"
            >
              <template #default="{ row }">
                <ElButton
                  v-if="row.inputParams"
                  link
                  type="primary"
                  :icon="View"
                  @click="showDetail(row.inputParams)"
                >
                  {{ $t('toolCallRecord.view') }}
                </ElButton>
                <span v-else>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="outputParams"
              :label="$t('toolCallRecord.outputParams')"
              min-width="150"
            >
              <template #default="{ row }">
                <ElButton
                  v-if="row.outputParams"
                  link
                  type="primary"
                  :icon="View"
                  @click="showDetail(row.outputParams)"
                >
                  {{ $t('toolCallRecord.view') }}
                </ElButton>
                <span v-else>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="created"
              :label="$t('toolCallRecord.callTime')"
              width="180"
            />
          </ElTable>
        </template>
      </PageData>
    </div>

    <ElDialog
      v-model="detailDialog"
      :title="$t('toolCallRecord.detail')"
      width="min(700px,80vw)"
      destroy-on-close
    >
      <pre
        class="max-h-[60vh] overflow-auto whitespace-pre-wrap rounded bg-gray-50 p-4 text-sm"
        >{{ detailContent }}</pre
      >
    </ElDialog>
  </div>
</template>

<style scoped>
:deep(.el-dialog__body) {
  padding: 16px;
  overflow: hidden;
}
</style>
