<script setup lang="ts">
import type { BotInfo } from '@aiflowy/types';

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { $t } from '#/locales';

import { ArrowLeft, View } from '@element-plus/icons-vue';
import {
  ElButton,
  ElContainer,
  ElDialog,
  ElEmpty,
  ElHeader,
  ElMain,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import * as Diff2Html from 'diff2html';
import 'diff2html/bundles/css/diff2html.min.css';
import { diffJson, FILE_HEADERS_ONLY, formatPatch } from 'diff';
import DOMPurify from 'dompurify';

import { api } from '#/api/request';
import { router } from '#/router';

const route = useRoute();

// 从路由 query 中获取 botId（从 /ai/bots 列表跳转过来时携带）
const botId = route.query.botId as string;

const botInfo = ref<BotInfo>();
const historyList = ref<BotInfo[]>([]);
const loading = ref(false);

const selectedRows = ref<BotInfo[]>([]);
const mountedModulesList = ref<BotMountedModules[]>([]);
const compareLoading = ref(false);

// 对比弹窗
const compareVisible = ref(false);
const leftVersionId = ref('');
const rightVersionId = ref('');

const selectedVersionOptions = computed(() =>
  selectedRows.value.map((row) => ({
    id: row.id,
    label: `v${row.version ?? '-'} (${row.created ?? ''})`,
  })),
);

const leftVersion = computed(() =>
  selectedRows.value.find((row) => row.id === leftVersionId.value),
);
const rightVersion = computed(() =>
  selectedRows.value.find((row) => row.id === rightVersionId.value),
);

// 对比时忽略的元字段
const META_FIELDS = new Set(['id', 'created', 'createdBy', 'deptId']);

interface MountedModule {
  id: number | string;
  name: string;
}

interface BotMountedModules {
  botId: number | string;
  botTitle: string;
  workflows: MountedModule[];
  knowledges: MountedModule[];
  plugins: MountedModule[];
  mcps: MountedModule[];
  skills: MountedModule[];
  wikis: MountedModule[];
  subagents: MountedModule[];
  models: MountedModule[];
}

function stripMetaFields(source: BotInfo): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(source).filter(([key]) => !META_FIELDS.has(key)),
  );
}

// 挂载模块对比时过滤掉定位用的 botId（两个版本必然不同）
function stripBotId(source: BotMountedModules): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(source).filter(([key]) => key !== 'botId'),
  );
}

// 将两个对象 diffJson 后渲染为 side-by-side 的 diff HTML
function buildDiffHtml(
  oldObj: Record<string, unknown>,
  newObj: Record<string, unknown>,
  oldName: string,
  newName: string,
): string {
  // diffJson 直接接收对象，内部会按字段名排序后再序列化，避免字段顺序差异造成的误报
  const changes = diffJson(oldObj, newObj);

  const patchLines: string[] = [];
  let oldLineCount = 0;
  let newLineCount = 0;

  for (const change of changes) {
    const prefix = change.added ? '+' : change.removed ? '-' : ' ';
    const textLines = change.value.split('\n');
    if (textLines.length > 0 && textLines[textLines.length - 1] === '') {
      textLines.pop();
    }
    for (const text of textLines) {
      patchLines.push(prefix + text);
      if (change.removed) {
        oldLineCount += 1;
      } else if (change.added) {
        newLineCount += 1;
      } else {
        oldLineCount += 1;
        newLineCount += 1;
      }
    }
  }

  const patchText = formatPatch(
    {
      oldFileName: oldName,
      newFileName: newName,
      oldHeader: '',
      newHeader: '',
      hunks: [
        {
          oldStart: 1,
          oldLines: oldLineCount,
          newStart: 1,
          newLines: newLineCount,
          lines: patchLines,
        },
      ],
    },
    FILE_HEADERS_ONLY,
  );

  const rendered = Diff2Html.html(patchText, {
    outputFormat: 'side-by-side',
    drawFileList: false,
    matching: 'lines',
  });

  return DOMPurify.sanitize(rendered);
}

const diffHtml = computed(() => {
  const left = leftVersion.value;
  const right = rightVersion.value;
  if (!left || !right) {
    return '';
  }

  // 拷贝副本后再过滤元字段，不修改原始对象
  return buildDiffHtml(
    stripMetaFields(left),
    stripMetaFields(right),
    `v${left.version ?? '-'}`,
    `v${right.version ?? '-'}`,
  );
});

const leftMounted = computed(() =>
  mountedModulesList.value.find(
    (item) => String(item.botId) === String(leftVersionId.value),
  ),
);
const rightMounted = computed(() =>
  mountedModulesList.value.find(
    (item) => String(item.botId) === String(rightVersionId.value),
  ),
);

const mountedDiffHtml = computed(() => {
  const left = leftMounted.value;
  const right = rightMounted.value;
  if (!left || !right) {
    return '';
  }

  return buildDiffHtml(
    stripBotId(left),
    stripBotId(right),
    left.botTitle || 'A',
    right.botTitle || 'B',
  );
});

function handleSelectionChange(rows: BotInfo[]) {
  selectedRows.value = rows;
}

function openCompare() {
  if (selectedRows.value.length < 2) {
    ElMessage.warning($t('bot.compareSelectTip'));
    return;
  }

  leftVersionId.value = selectedRows.value[0]?.id ?? '';
  rightVersionId.value = selectedRows.value[1]?.id ?? '';
  compareVisible.value = true;
  getMountedModules();
}

function viewVersion(row: BotInfo) {
  router.push({ path: `/ai/bots/setting/${row.id}` });
}

// 查询勾选智能体的挂载模块数据，用于对比
function getMountedModules() {
  const ids = selectedRows.value.map((row) => row.id).join(',');
  if (!ids) {
    return;
  }

  compareLoading.value = true;
  api
    .get('/api/v1/bot/mountedModules', {
      params: { ids },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        mountedModulesList.value = res.data || [];
      }
    })
    .finally(() => {
      compareLoading.value = false;
    });
}

// 获取当前 Bot 基本信息，用于顶部标题展示
function getBotDetail() {
  if (!botId) return;
  api
    .get('/api/v1/bot/getDetail', {
      params: {
        id: botId,
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        botInfo.value = res.data;
      }
    });
}

// 获取历史发布记录列表
function getHistoryList() {
  loading.value = true;
  api
    .get('/api/v1/bot/history', {
      params: {
        id: botId
      },
    })
    .then((res) => {
      if (res.errorCode === 0) {
        historyList.value = res.data || [];
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

onMounted(() => {
  getBotDetail();
  getHistoryList();
});
</script>

<template>
  <ElContainer class="bg-background h-full">
    <ElHeader height="100px" class="border-border border-b !pr-10">
      <div class="flex h-full w-full items-center justify-between">
        <!-- Left -->
        <div class="flex items-center gap-3">
          <ElButton link style="font-size: 20px" :icon="ArrowLeft" @click="router.back()" />
          <div class="flex flex-col gap-2">
            <div class="flex items-center gap-2">
              <span class="text-lg font-medium">{{ botInfo?.title }}</span>
              <div class="text-foreground/70 rounded bg-[var(--el-fill-color-light)] p-1 text-xs">
                {{ $t('button.historicalReleaseRecords') }}
              </div>
            </div>
            <span class="text-foreground/50 text-sm">{{
              botInfo?.created
              }}</span>
          </div>
        </div>

        <!-- Right -->
        <ElButton type="primary" :disabled="selectedRows.length < 2" @click="openCompare">
          {{ $t('bot.compare') }}
        </ElButton>
      </div>
    </ElHeader>

    <ElMain v-loading="loading" class="p-6">
      <div class="overflow-hidden rounded-lg">
        <ElTable :data="historyList" class="w-full" @selection-change="handleSelectionChange">
          <ElTableColumn type="selection" width="50" />
          <ElTableColumn :label="$t('bot.version')" width="160">
            <template #default="{ row }">
              <ElTag v-if="row.version" type="info">
                v{{ row.version }}
              </ElTag>
              <span v-else>-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="title" :label="$t('bot.title')" min-width="180" show-overflow-tooltip />
          <ElTableColumn :label="$t('bot.releaseTime')" width="200">
            <template #default="{ row }">
              {{ row.created }}
            </template>
          </ElTableColumn>
          <!-- <ElTableColumn :label="$t('bot.published')" width="120" align="center">
            <template #default="{ row }">
              <ElTag v-if="row.publishStatus == 1" type="success">
                {{ $t('bot.published') }}
              </ElTag>
              <ElTag v-else type="info">{{ $t('bot.unPublished') }}</ElTag>
            </template>
          </ElTableColumn> -->
          <ElTableColumn :label="$t('common.handle')" width="100" align="center">
            <template #default="{ row }">
              <ElButton link type="primary" :icon="View" @click="viewVersion(row)">
                {{ $t('button.view') }}
              </ElButton>
            </template>
          </ElTableColumn>
          <template #empty>
            <ElEmpty :description="$t('common.noDataAvailable')" />
          </template>
        </ElTable>
      </div>
    </ElMain>

    <!-- 版本对比弹窗 -->
    <ElDialog v-model="compareVisible" :title="$t('bot.compare')" width="min(960px, 90vw)" align-center>
      <div class="mb-4 flex flex-wrap items-center gap-4">
        <div class="flex items-center gap-2">
          <span class="text-sm text-foreground/70">{{ $t('bot.version') }} A</span>
          <ElSelect v-model="leftVersionId" class="!w-[220px]">
            <ElOption v-for="option in selectedVersionOptions" :key="option.id" :label="option.label"
              :value="option.id" />
          </ElSelect>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-foreground/70">{{ $t('bot.version') }} B</span>
          <ElSelect v-model="rightVersionId" class="!w-[220px]">
            <ElOption v-for="option in selectedVersionOptions" :key="option.id" :label="option.label"
              :value="option.id" />
          </ElSelect>
        </div>
      </div>

      <div v-if="leftVersion && rightVersion" class="mt-4 border-t pt-4">
        <div class="diff-container" v-html="diffHtml"></div>
      </div>

      <div v-if="leftVersion && rightVersion" v-loading="compareLoading" class="mt-6 border-t pt-4">
        <div class="mb-2 text-sm font-medium">挂载模块对比</div>
        <div v-if="leftMounted && rightMounted" class="diff-container" v-html="mountedDiffHtml"></div>
        <ElEmpty v-else-if="!compareLoading" :description="$t('common.noDataAvailable')" />
      </div>
    </ElDialog>
  </ElContainer>
</template>

<style lang="css" scoped>
.el-table {
  --el-table-text-color: hsl(var(--foreground) / 0.9);
  --el-font-size-base: 14px;
  --el-table-header-text-color: hsl(var(--accent-foreground));
  --el-table-header-bg-color: #f7f9fd;
  --el-table-border: none;
}

.el-table:where(.dark, .dark *) {
  --el-table-header-bg-color: hsl(var(--accent));
}
</style>

<style lang="css">
.diff-container {
  max-height: 60vh;
  overflow: auto;
  position: relative;
}
</style>
