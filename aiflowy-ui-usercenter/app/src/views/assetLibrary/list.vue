<script setup lang="ts">
import { onMounted } from 'vue';

import { formatBytes } from '@aiflowy/utils';

import { Delete, Download, MoreFilled } from '@element-plus/icons-vue';
import {
  ElAvatar,
  ElButton,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import Tag from '#/components/tag/Tag.vue';
import { useDictStore } from '#/store';
import {
  getResourceOriginColor,
  getResourceTypeColor,
  getSrc,
} from '#/utils/resource';

interface ListProps {
  data: any[];
  onCheckedChange?: (ids: any[]) => void;
  onPreview?: (row: any) => void;
  onEdit?: (row: any) => void;
  onRemove?: (row: any) => void;
  onDownload?: (row: any) => void;
}
const props = defineProps<ListProps>();
onMounted(() => {
  initDict();
});
const dictStore = useDictStore();
function initDict() {
  dictStore.fetchDictionary('resourceType');
  dictStore.fetchDictionary('resourceOriginType');
}
function handleSelectionChange(items: any[]) {
  props.onCheckedChange?.(items);
}
</script>

<template>
  <div class="bg-background w-full rounded-lg p-5">
    <ElTable :data="props.data" @selection-change="handleSelectionChange">
      <ElTableColumn type="selection" width="30" />
      <ElTableColumn :label="$t('assetLibrary.fileName')" show-overflow-tooltip :width="300">
        <template #default="{ row }">
          <div class="flex items-center gap-2.5">
            <ElAvatar
              class="shrink-0"
              :src="getSrc(row)"
              shape="square"
              :size="32"
            />
            <span class="w-full overflow-hidden text-ellipsis">{{
              row.resourceName
            }}</span>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn :label="$t('assetLibrary.fileSource')" align="center">
        <template #default="{ row }">
          <Tag
            size="small"
            :background-color="`${getResourceOriginColor(row)}15`"
            :text-color="getResourceOriginColor(row)"
            :text="dictStore.getDictLabel('resourceOriginType', row.origin)"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn :label="$t('assetLibrary.fileType')" align="center">
        <template #default="{ row }">
          <Tag
            size="small"
            :background-color="`${getResourceTypeColor(row)}15`"
            :text-color="getResourceTypeColor(row)"
            :text="dictStore.getDictLabel('resourceType', row.resourceType)"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn prop="fileSize" :label="$t('assetLibrary.fileSize')" align="center">
        <template #default="{ row }">
          {{ formatBytes(row.fileSize) }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="modified" :label="$t('assetLibrary.modified')" align="center" />
      <ElTableColumn :label="$t('assetLibrary.operation')" width="150" align="center">
        <template #default="{ row }">
          <div class="flex items-center gap-3">
            <div class="flex items-center">
              <ElButton
                class="[--el-font-weight-primary:400]"
                link
                type="primary"
                @click="onPreview?.(row)"
              >
                {{ $t('assetLibrary.preview') }}
              </ElButton>
              <ElButton
                class="[--el-font-weight-primary:400]"
                link
                type="primary"
                @click="onEdit?.(row)"
              >
                {{ $t('assetLibrary.edit') }}
              </ElButton>
            </div>

            <ElDropdown>
              <ElButton link :icon="MoreFilled" />

              <template #dropdown>
                <ElDropdownMenu>
                  <ElDropdownItem @click="onDownload?.(row)">
                    <ElButton :icon="Download" link>{{ $t('assetLibrary.download') }}</ElButton>
                  </ElDropdownItem>
                  <ElDropdownItem @click="onRemove?.(row)">
                    <ElButton type="danger" :icon="Delete" link>
                      {{ $t('assetLibrary.delete') }}
                    </ElButton>
                  </ElDropdownItem>
                </ElDropdownMenu>
              </template>
            </ElDropdown>
          </div>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
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

:deep(.el-table__header) {
  border-radius: 8px;
  overflow: hidden;
}

.el-table :deep(.el-table__inner-wrapper:before) {
  display: none;
}

.el-table :deep(thead),
.el-table :deep(tr) {
  height: 60px;
}

.el-table :deep(thead th) {
  font-weight: 400;
}
</style>
