<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, markRaw, onMounted, ref } from 'vue';

import {
  Clock,
  Delete,
  Edit,
  MoreFilled,
  Plus,
  Upload,
} from '@element-plus/icons-vue';
import {
  ElButton,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
} from 'element-plus';
import { tryit } from 'radash';

import { api } from '#/api/request';
import HeaderSearch from '#/components/headerSearch/HeaderSearch.vue';
import PageData from '#/components/page/PageData.vue';
import PageSide from '#/components/page/PageSide.vue';
import { $t } from '#/locales';
import { router } from '#/router';
import { useDictStore } from '#/store';

import AiBatchImportModal from './AiBatchImportModal.vue';
import DatacenterTableImportModal from './DatacenterTableImportModal.vue';
import DatacenterTableModal from './DatacenterTableModal.vue';
import ImportRecordModal from './ImportRecordModal.vue';

onMounted(() => {
  initDict();
  getSideList();
});

const pageDataRef = ref();
const saveDialog = ref();
const importDialog = ref();
const aiImportDialog = ref();
const importRecordDialog = ref();
const currentCategoryId = ref<string>('');
const dictStore = useDictStore();
const headerButtons = [
  {
    key: 'aiImport',
    text: $t('button.aiImport'),
    icon: markRaw(Upload),
    type: 'default',
    data: { action: 'aiImport' },
    permission: '/api/v1/datacenterTable/save',
  },
  {
    key: 'import',
    text: `${$t('button.import')} Excel`,
    icon: markRaw(Upload),
    type: 'default',
    data: { action: 'import' },
    permission: '/api/v1/datacenterTable/save',
  },
  {
    key: 'create',
    text: $t('button.add'),
    icon: markRaw(Plus),
    type: 'primary',
    data: { action: 'create' },
    permission: '/api/v1/datacenterTable/save',
  },
  {
    key: 'importRecord',
    text: $t('datacenterTable.importRecord.button') || '查看导入记录',
    icon: markRaw(Clock),
    type: 'default',
    data: { action: 'importRecord' },
    permission: '/api/v1/datacenterTable/query',
  },
];

function initDict() {
  dictStore.fetchDictionary('dataStatus');
}
const handleSearch = (params: string) => {
  pageDataRef.value.setQuery({
    tableName: params,
    isQueryOr: true,
    categoryId: currentCategoryId.value,
  });
};
function reset(formEl?: FormInstance) {
  formEl?.resetFields();
  pageDataRef.value.setQuery({ categoryId: currentCategoryId.value });
}
function showDialog(row: any) {
  saveDialog.value.openDialog({ ...row });
}
function openImportDialog() {
  importDialog.value.openDialog();
}
function openImportRecordDialog() {
  importRecordDialog.value.openDialog();
}
function handleHeaderButton(payload: any) {
  if (payload?.key === 'aiImport') {
    aiImportDialog.value.openDialog();
    return;
  }
  if (payload?.key === 'import') {
    openImportDialog();
    return;
  }
  if (payload?.key === 'importRecord') {
    importRecordDialog.value.openDialog();
    return;
  }
  showDialog({});
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
          .get(`/api/v1/datacenterTable/removeTable?tableId=${row.id}`)
          .then((res) => {
            instance.confirmButtonLoading = false;
            if (res.errorCode === 0) {
              ElMessage.success(res.message);
              pageDataRef.value.setQuery({
                categoryId: currentCategoryId.value,
              });
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
function toDetailPage(row: any) {
  router.push({
    name: 'TableDetail',
    query: {
      tableId: row.id,
    },
  });
}

// 分类侧边栏相关
const fieldDefinitions = ref<any[]>([
  {
    prop: 'categoryName',
    label: $t('aiWorkflowCategory.categoryName'),
    type: 'input',
    required: true,
    placeholder: $t('aiWorkflowCategory.categoryName'),
  },
  {
    prop: 'sortNo',
    label: $t('aiWorkflowCategory.sortNo'),
    type: 'number',
    required: false,
    placeholder: $t('aiWorkflowCategory.sortNo'),
  },
]);
const sideList = ref<any>([]);
const sideLoading = ref(true);
const controlBtns = [
  {
    icon: Edit,
    label: $t('button.edit'),
    onClick(row: any) {
      showControlDialog(row);
    },
  },
  {
    type: 'danger',
    icon: Delete,
    label: $t('button.delete'),
    onClick(row: any) {
      removeCategory(row);
    },
  },
];
const footerButton = {
  icon: Plus,
  label: $t('button.add'),
  onClick() {
    showControlDialog({});
  },
};
const sideDialogVisible = ref(false);
const sideFormData = ref<any>({});
const sideFormRef = ref<FormInstance>();
const sideFormRules = computed(() => {
  const rules: Record<string, any[]> = {};
  fieldDefinitions.value.forEach((field) => {
    const fieldRules = [];
    if (field.required) {
      fieldRules.push({
        required: true,
        message: `${$t('message.required')}`,
        trigger: 'blur',
      });
    }
    if (fieldRules.length > 0) {
      rules[field.prop] = fieldRules;
    }
  });
  return rules;
});
const sideSaveLoading = ref(false);

function changeCategory(category: any) {
  currentCategoryId.value = category.id;
  pageDataRef.value.setQuery({ categoryId: category.id });
}
function showControlDialog(item: any) {
  sideFormRef.value?.resetFields();
  sideFormData.value = { ...item };
  sideDialogVisible.value = true;
}
function removeCategory(row: any) {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true;
        api
          .post('/api/v1/datacenterCategory/remove', { id: row.id })
          .then((res) => {
            instance.confirmButtonLoading = false;
            if (res.errorCode === 0) {
              ElMessage.success(res.message);
              done();
              getSideList();
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
function handleSideSubmit() {
  sideFormRef.value?.validate((valid) => {
    if (valid) {
      sideSaveLoading.value = true;
      const url = sideFormData.value.id
        ? '/api/v1/datacenterCategory/update'
        : '/api/v1/datacenterCategory/save';
      api.post(url, sideFormData.value).then((res) => {
        sideSaveLoading.value = false;
        if (res.errorCode === 0) {
          ElMessage.success(res.message);
          sideDialogVisible.value = false;
          getSideList();
        }
      });
    }
  });
}
const getSideList = async () => {
  sideLoading.value = true;
  const [, res] = await tryit(api.get)('/api/v1/datacenterCategory/list', {
    params: { sortKey: 'sortNo', sortType: 'asc' },
  });

  if (res && res.errorCode === 0) {
    sideList.value = [
      {
        id: '',
        categoryName: $t('common.allCategories'),
      },
      ...res.data,
    ];
  }
  sideLoading.value = false;
};
</script>

<template>
  <div class="flex h-full flex-col gap-6 p-6">
    <DatacenterTableModal
      ref="saveDialog"
      @reload="reset"
      :categories="sideList"
    />
    <DatacenterTableImportModal
      ref="importDialog"
      @reload="reset"
      :categories="sideList"
    />
    <AiBatchImportModal
      ref="aiImportDialog"
      @reload="reset"
      @open-import-record="openImportRecordDialog"
      :categories="sideList"
    />
    <ImportRecordModal ref="importRecordDialog" />
    <HeaderSearch
      :buttons="headerButtons"
      @search="handleSearch"
      @button-click="handleHeaderButton"
    />

    <div class="flex max-h-[calc(100vh-191px)] flex-1 gap-6">
      <PageSide
        label-key="categoryName"
        value-key="id"
        :menus="sideList"
        :control-btns="controlBtns"
        :footer-button="footerButton"
        :loading="sideLoading"
        @change="changeCategory"
      />
      <div class="bg-background h-full flex-1 overflow-auto rounded-lg p-5">
        <PageData
          ref="pageDataRef"
          page-url="/api/v1/datacenterTable/page"
          :page-size="10"
        >
          <template #default="{ pageList }">
            <ElTable :data="pageList" border>
              <ElTableColumn
                prop="tableName"
                :label="$t('datacenterTable.tableName')"
              >
                <template #default="{ row }">
                  {{ row.tableName }}
                </template>
              </ElTableColumn>
              <ElTableColumn
                prop="tableDesc"
                :label="$t('datacenterTable.tableDesc')"
              >
                <template #default="{ row }">
                  {{ row.tableDesc }}
                </template>
              </ElTableColumn>
              <ElTableColumn
                prop="created"
                :label="$t('datacenterTable.created')"
              >
                <template #default="{ row }">
                  {{ row.created }}
                </template>
              </ElTableColumn>
              <ElTableColumn
                :label="$t('common.handle')"
                width="140"
                align="right"
              >
                <template #default="{ row }">
                  <div class="flex items-center gap-3">
                    <div class="flex items-center">
                      <ElButton link type="primary" @click="toDetailPage(row)">
                        {{ $t('button.view') }}
                      </ElButton>
                      <ElButton link type="primary" @click="showDialog(row)">
                        {{ $t('button.edit') }}
                      </ElButton>
                    </div>

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

    <!-- 分类编辑对话框 -->
    <ElDialog
      v-model="sideDialogVisible"
      :title="sideFormData.id ? `${$t('button.edit')}` : `${$t('button.add')}`"
      :close-on-click-modal="false"
    >
      <ElForm
        ref="sideFormRef"
        :model="sideFormData"
        :rules="sideFormRules"
        label-width="120px"
      >
        <!-- 动态生成表单项 -->
        <ElFormItem
          v-for="field in fieldDefinitions"
          :key="field.prop"
          :label="field.label"
          :prop="field.prop"
        >
          <ElInput
            v-if="!field.type || field.type === 'input'"
            v-model="sideFormData[field.prop]"
            :placeholder="field.placeholder"
          />
          <ElInputNumber
            v-else-if="field.type === 'number'"
            v-model="sideFormData[field.prop]"
            :placeholder="field.placeholder"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="sideDialogVisible = false">
          {{ $t('button.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="handleSideSubmit"
          :loading="sideSaveLoading"
        >
          {{ $t('button.confirm') }}
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped></style>
