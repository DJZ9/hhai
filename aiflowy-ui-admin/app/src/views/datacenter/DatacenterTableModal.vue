<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { onMounted, ref, watch } from 'vue';

import { Plus } from '@element-plus/icons-vue';
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
} from 'element-plus';

import { api } from '#/api/request';
import { $t } from '#/locales';

const props = defineProps<{
  categories?: any[];
}>();

const emit = defineEmits(['reload']);
// vue
onMounted(() => {
  getFieldType();
  getYesOrNo();
});
defineExpose({
  openDialog,
});
const saveForm = ref<FormInstance>();
// variables
const dialogVisible = ref(false);
const isAdd = ref(true);
const entity = ref<any>({
  deptId: '',
  tableName: '',
  tableDesc: '',
  actualTable: '',
  categoryId: '',
  status: '',
  options: '',
});
const btnLoading = ref(false);
const rules = ref({
  tableName: [
    { required: true, message: $t('message.required'), trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: $t('datacenterTable.nameRegx'),
    },
  ],
  fields: [
    {
      required: true,
      validator: (_: any, value: any, callback: any) => {
        if (!value || value.length === 0) {
          callback(new Error($t('datacenterTable.noFieldError')));
        } else {
        // 检查字段数组中的fieldName和fieldDesc字段
        value.forEach((field: any) => {
            if (!field.fieldName || !field.fieldDesc) {
              callback(new Error($t('datacenterTable.fieldInfoError')));
            }
            if (!/^[a-z][a-z0-9_]*$/.test(field.fieldName)) {
            callback(new Error($t('datacenterTable.nameRegx')));
          }
        });
        callback();
        }
      },
      trigger: 'blur',
    },
  ],
});
const fieldsData = ref();
const removeFields = ref<any[]>([]);
const loadFields = ref(false);
const fieldTypes = ref<any>([]);
const yesOrNoDict = ref<any>([]);
watch(
  () => fieldsData.value,
  (newVal) => {
    entity.value.fields = newVal;
  },
  { deep: true },
);

// functions
function getDetailInfo(tableId: any) {
  loadFields.value = true;
  api
    .get(`/api/v1/datacenterTable/detailInfo?tableId=${tableId}`)
    .then((res) => {
      loadFields.value = false;
      fieldsData.value = res.data.fields;
    });
}
function openDialog(row: any) {
  fieldsData.value = [];
  removeFields.value = [];
  if (row.id) {
    getDetailInfo(row.id);
    isAdd.value = false;
  }
  entity.value = row;
  dialogVisible.value = true;
}
function save() {
  saveForm.value?.validate((valid) => {
    if (valid) {
      if (fieldsData.value.length === 0) {
        ElMessage.error($t('message.required'));
        return;
      }
      const obj = {
        ...entity.value,
        fields: [...fieldsData.value, ...removeFields.value],
      };
      btnLoading.value = true;
      api
        .post('/api/v1/datacenterTable/saveTable', obj)
        .then((res) => {
          btnLoading.value = false;
          if (res.errorCode === 0) {
            ElMessage.success(res.message);
            emit('reload');
            closeDialog();
          }
        })
        .catch(() => {
          btnLoading.value = false;
        });
    }
  });
}
function closeDialog() {
  saveForm.value?.resetFields();
  isAdd.value = true;
  entity.value = {};
  dialogVisible.value = false;
}
function addField() {
  fieldsData.value.push({
    fieldName: '',
    fieldDesc: '',
    fieldType: 1,
    required: 0,
    handleDelete: false,
    rowKey: Date.now().toString(),
  });
}
function deleteField(row: any, $index: number) {
  ElMessageBox.confirm($t('message.deleteAlert'), $t('message.noticeTitle'), {
    confirmButtonText: $t('message.ok'),
    cancelButtonText: $t('message.cancel'),
    type: 'warning',
    beforeClose: (action, _, done) => {
      if (action === 'confirm') {
        if (row.id) {
          row.handleDelete = true;
          removeFields.value.push(row);
        }
        fieldsData.value.splice($index, 1);
        done();
      } else {
        done();
      }
    },
  }).catch(() => {});
}
function getFieldType() {
  api.get('/api/v1/dict/items/fieldType').then((res) => {
    fieldTypes.value = res.data;
  });
}
function getYesOrNo() {
  api.get('/api/v1/dict/items/yesOrNo').then((res) => {
    yesOrNoDict.value = res.data;
  });
}
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="isAdd ? $t('button.add') : $t('button.edit')"
    :before-close="closeDialog"
    :close-on-click-modal="false"
    width="800px"
  >
    <ElForm
      label-width="100px"
      ref="saveForm"
      :model="entity"
      status-icon
      :rules="rules"
    >
      <ElFormItem
        prop="categoryId"
        :label="$t('aiWorkflowCategory.categoryName')"
      >
        <ElSelect v-model="entity.categoryId" class="w-full">
          <ElOption
            v-for="item in props.categories?.filter((c) => c.id)"
            :key="item.id"
            :label="item.categoryName"
            :value="item.id"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem prop="tableName" :label="$t('datacenterTable.tableName')">
        <ElInput :disabled="!isAdd" v-model.trim="entity.tableName" />
      </ElFormItem>
      <ElFormItem prop="tableDesc" :label="$t('datacenterTable.tableDesc')">
        <ElInput v-model.trim="entity.tableDesc" />
      </ElFormItem>
      <ElFormItem prop="fields" label-width="0">
        <div v-loading="loadFields" class="w-full">
          <ElTable :data="fieldsData">
            <ElTableColumn :label="$t('datacenterTable.fieldName')">
              <template #default="{ row }">
                <ElInput v-model.trim="row.fieldName" />
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('datacenterTable.fieldDesc')">
              <template #default="{ row }">
                <ElInput v-model.trim="row.fieldDesc" />
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('datacenterTable.fieldType')">
              <template #default="{ row }">
                <ElSelect v-model.trim="row.fieldType" :disabled="!!row.id">
                  <ElOption
                    v-for="item in fieldTypes"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('datacenterTable.required')">
              <template #default="{ row }">
                <ElSelect v-model.trim="row.required">
                  <ElOption
                    v-for="item in yesOrNoDict"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </template>
            </ElTableColumn>
            <ElTableColumn :label="$t('common.handle')" width="80">
              <template #default="{ row, $index }">
                <ElButton link type="danger" @click="deleteField(row, $index)">
                  {{ $t('button.delete') }}
                </ElButton>
              </template>
            </ElTableColumn>
          </ElTable>
          <div class="mt-3">
            <ElButton plain type="primary" @click="addField">
              <ElIcon class="mr-1">
                <Plus />
              </ElIcon>
              {{ $t('button.add') }}
            </ElButton>
          </div>
        </div>
      </ElFormItem>
    </ElForm>
    <template #footer>
      <ElButton @click="closeDialog">
        {{ $t('button.cancel') }}
      </ElButton>
      <ElButton
        type="primary"
        @click="save"
        :loading="btnLoading"
        :disabled="btnLoading"
      >
        {{ $t('button.save') }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped></style>
