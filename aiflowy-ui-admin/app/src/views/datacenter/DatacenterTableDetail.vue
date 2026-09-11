<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { $t } from '@aiflowy/locales';
import { downloadFileFromBlob } from '@aiflowy/utils';

import {
  ArrowLeft,
  Delete,
  Download,
  MoreFilled,
  Plus,
  Upload,
} from '@element-plus/icons-vue';
import {
  ElButton,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElIcon,
  ElImage,
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { api } from '#/api/request';
import tableIcon from '#/assets/datacenter/table2x.png';
import PageData from '#/components/page/PageData.vue';
import PageSide from '#/components/page/PageSide.vue';
import { router } from '#/router';
import { useDictStore } from '#/store';
import BatchImportModal from '#/views/datacenter/BatchImportModal.vue';
import RecordModal from '#/views/datacenter/RecordModal.vue';

const pageDataRef = ref();
const route = useRoute();
const tableId = ref(route.query.tableId);
onMounted(() => {
  initDict();
  getDetailInfo(tableId.value);
  getHeaders(tableId.value);
});
const detailInfo = ref<any>({});
const fieldList = ref<any[]>([]);
const headers = ref<any[]>([]);
const activeMenu = ref('2');
const recordModal = ref();
const batchImportModal = ref();
const dictStore = useDictStore();
const categoryData = [
  { key: '2', name: $t('datacenterTable.data') },
  { key: '1', name: $t('datacenterTable.structure') },
];
const exportLoading = ref(false);
function initDict() {
  dictStore.fetchDictionary('fieldType');
  dictStore.fetchDictionary('yesOrNo');
}
function getDetailInfo(id: any) {
  api.get(`/api/v1/datacenterTable/detailInfo?tableId=${id}`).then((res) => {
    detailInfo.value = res.data;
    fieldList.value = res.data.fields;
  });
}
function getHeaders(id: any) {
  api.get(`/api/v1/datacenterTable/getHeaders?tableId=${id}`).then((res) => {
    headers.value = res.data;
  });
}
function showDialog(row: any) {
  recordModal.value.openDialog({ ...row });
}
function remove(row: any) {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true;
        api
          .post(
            `/api/v1/datacenterTable/removeValue`,
            {},
            {
              params: {
                tableId: tableId.value,
                id: row.id,
              },
            },
          )
          .then((res) => {
            instance.confirmButtonLoading = false;
            if (res.errorCode === 0) {
              ElMessage.success(res.message);
              refresh();
              done();
            }
          })
          .catch(() => {
            instance.confirmButtonLoading = false;
          });
      } else {
        done();
      }
    },
  }).catch(() => {});
}
function refresh() {
  pageDataRef.value.setQuery({});
}
function openImportModal() {
  batchImportModal.value.openDialog();
}
function changeTab(category: any) {
  activeMenu.value = category.key;
  if (category.key === '2' && pageDataRef.value) {
    refresh();
  }
}
function exportData() {
  exportLoading.value = true;
  api
    .download(`/api/v1/datacenterTable/exportData?tableId=${tableId.value}`)
    .then((res) => {
      exportLoading.value = false;
      downloadFileFromBlob({
        fileName: `${detailInfo.value.tableName}数据.xlsx`,
        source: res,
      });
    });
}
</script>

<template>
  <div class="bg-background-deep flex h-full flex-col gap-6 p-6">
    <BatchImportModal
      :table-id="tableId"
      :title="$t('button.batchImport')"
      ref="batchImportModal"
      @reload="refresh"
    />
    <RecordModal
      ref="recordModal"
      :form-items="headers"
      :table-id="tableId"
      @reload="refresh"
    />

    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <ElIcon class="cursor-pointer" @click="router.back()">
          <ArrowLeft />
        </ElIcon>
        <div class="flex items-center gap-3">
          <ElImage :src="tableIcon" class="h-9 w-9 rounded-full" />
          <div class="flex flex-col gap-1.5">
            <div class="text-base font-medium">{{ detailInfo.tableName }}</div>
            <div class="desc text-sm">{{ detailInfo.tableDesc }}</div>
          </div>
        </div>
      </div>

      <div class="flex items-center" v-if="activeMenu === '2'">
        <ElButton type="primary" @click="showDialog({})">
          <ElIcon class="mr-1">
            <Plus />
          </ElIcon>
          {{ $t('button.addLine') }}
        </ElButton>
        <ElButton type="primary" @click="openImportModal">
          <ElIcon class="mr-1">
            <Upload />
          </ElIcon>
          {{ $t('button.batchImport') }}
        </ElButton>
        <ElButton type="primary" @click="exportData" :loading="exportLoading">
          <ElIcon class="mr-1">
            <Download />
          </ElIcon>
          {{ $t('button.export') }}
        </ElButton>
      </div>
    </div>

    <div class="flex h-full max-h-[calc(100vh-191px)] gap-3">
      <PageSide
        label-key="name"
        value-key="key"
        :menus="categoryData"
        default-selected="2"
        @change="changeTab"
      />

      <div
        class="bg-background border-border flex-1 overflow-auto rounded-lg border p-5"
      >
        <ElTable v-show="activeMenu === '1'" :data="fieldList">
          <ElTableColumn
            prop="fieldName"
            :label="$t('datacenterTableFields.fieldName')"
          />
          <ElTableColumn
            prop="fieldDesc"
            :label="$t('datacenterTableFields.fieldDesc')"
          />
          <ElTableColumn
            prop="fieldType"
            :label="$t('datacenterTableFields.fieldType')"
          >
            <template #default="{ row }">
              {{ dictStore.getDictLabel('fieldType', row.fieldType) }}
            </template>
          </ElTableColumn>
          <ElTableColumn
            prop="required"
            :label="$t('datacenterTableFields.required')"
          >
            <template #default="{ row }">
              {{ dictStore.getDictLabel('yesOrNo', row.required) }}
            </template>
          </ElTableColumn>
        </ElTable>
        <PageData
          v-show="activeMenu === '2'"
          ref="pageDataRef"
          page-url="/api/v1/datacenterTable/getPageData"
          :extra-query-params="{ tableId }"
          :page-size="10"
        >
          <template #default="{ pageList }">
            <ElTable :data="pageList">
              <ElTableColumn
                v-for="item in headers"
                :key="item.key"
                :prop="item.key"
                :label="item.title || item.key"
              >
                <template #default="{ row }">
                  <div v-if="item.fieldType === 5">
                    {{
                      1 === row[item.key] ? $t('common.yes') : $t('common.no')
                    }}
                  </div>
                  <div v-else-if="item.fieldType === 4">
                    {{ parseFloat(row[item.key]) }}
                  </div>
                  <div v-else>{{ row[item.key] }}</div>
                </template>
              </ElTableColumn>
              <ElTableColumn
                :label="$t('common.handle')"
                width="90"
                align="right"
              >
                <template #default="{ row }">
                  <div class="flex items-center gap-3">
                    <ElButton link type="primary" @click="showDialog(row)">
                      {{ $t('button.edit') }}
                    </ElButton>

                    <ElDropdown>
                      <ElButton link :icon="MoreFilled" />

                      <template #dropdown>
                        <ElDropdownMenu>
                          <ElDropdownItem @click="remove(row)">
                            <ElButton link :icon="Delete" type="danger">
                              {{ $t('button.delete') }}
                            </ElButton>
                          </ElDropdownItem>
                        </ElDropdownMenu>
                      </template>
                    </ElDropdown>
                  </div>
                </template>
              </ElTableColumn>
            </ElTable>
          </template>
        </PageData>
      </div>
    </div>
  </div>
</template>

<style scoped>
.desc {
  color: #75808d;
}
</style>
