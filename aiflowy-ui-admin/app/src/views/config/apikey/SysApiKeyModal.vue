<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { computed, onMounted, ref } from 'vue';

import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElCollapse,
  ElCollapseItem,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
} from 'element-plus';

import { api } from '#/api/request';
import DictSelect from '#/components/dict/DictSelect.vue';
import { $t } from '#/locales';

// 定义权限接口
interface ResourcePermission {
  id: number;
  title: string;
  requestInterface: string;
  groupName: string;
}

// 定义分组后的权限结构
interface GroupedPermission {
  groupName: string;
  permissions: ResourcePermission[];
}

// 定义表单数据接口
interface Entity {
  apiKey: string;
  name: string;
  status: number | string;
  deptId: number | string;
  expiredAt: Date | null | string;
  permissionIds: (number | string)[]; // 绑定值：权限 ID 数组
  id?: number; // 编辑时的主键
}

const emit = defineEmits(['reload']);

// 表单实例
const saveForm = ref<FormInstance>();
// 对话框状态
const dialogVisible = ref(false);
const isAdd = ref(true);
// 表单数据（初始化默认值）
const entity = ref<Entity>({
  apiKey: '',
  name: '',
  status: '',
  deptId: '',
  expiredAt: null,
  permissionIds: [],
});
// 加载状态
const btnLoading = ref(false);
// 资源权限列表
const resourcePermissionList = ref<ResourcePermission[]>([]);
// 展开的分组名称列表
const expandedGroups = ref<string[]>([]);

// 按 groupName 分组后的权限列表
const groupedPermissionList = computed<GroupedPermission[]>(() => {
  const groupMap = new Map<string, ResourcePermission[]>();

  resourcePermissionList.value.forEach((item) => {
    const groupName = item.groupName || '未分组';
    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, []);
    }
    groupMap.get(groupName)!.push(item);
  });

  return [...groupMap.entries()].map(([groupName, permissions]) => ({
    groupName,
    permissions,
  }));
});

// 检查某个分组是否全选
function isGroupAllSelected(group: GroupedPermission): boolean {
  const groupIds = group.permissions.map((p) => String(p.id));
  const selectedIds = new Set(entity.value.permissionIds.map(String));
  return groupIds.length > 0 && groupIds.every((id) => selectedIds.has(id));
}

// 检查某个分组是否部分选中
function isGroupIndeterminate(group: GroupedPermission): boolean {
  const groupIds = group.permissions.map((p) => String(p.id));
  const selectedIds = new Set(entity.value.permissionIds.map(String));
  const selectedCount = groupIds.filter((id) => selectedIds.has(id)).length;
  return selectedCount > 0 && selectedCount < groupIds.length;
}

// 切换分组全选状态
function toggleGroupSelection(group: GroupedPermission, checked: boolean) {
  const groupIds = group.permissions.map((p) => String(p.id));

  if (checked) {
    // 全选：添加该分组下所有未选中的权限
    const newIds = [...entity.value.permissionIds];
    groupIds.forEach((id) => {
      if (!newIds.includes(id)) {
        newIds.push(id);
      }
    });
    entity.value.permissionIds = newIds;
  } else {
    // 取消全选：移除该分组下的所有权限
    entity.value.permissionIds = entity.value.permissionIds.filter(
      (id) => !groupIds.includes(String(id)),
    );
  }
}

// 表单校验规则（必填项校验）
const rules = ref({
  name: [
    {
      required: true,
      message: $t('message.required'),
      trigger: 'blur',
    },
  ],
  status: [
    {
      required: true,
      message: $t('message.pleaseSelect', { name: $t('sysApiKey.status') }),
      trigger: 'change',
    },
  ],
  expiredAt: [
    {
      required: true,
      message: $t('message.pleaseSelect', { name: $t('sysApiKey.expiredAt') }),
      trigger: 'change',
    },
  ],
});

function openDialog(row: Partial<Entity> = {}) {
  saveForm.value?.resetFields();
  entity.value = {
    apiKey: '',
    name: '',
    status: '',
    deptId: '',
    expiredAt: null,
    permissionIds: [],
    ...row,
  };
  isAdd.value = !row.id;
  dialogVisible.value = true;
}

// 获取资源权限列表
function getResourcePermissionList() {
  api
    .get('/api/v1/sysApiKeyResourcePermission/list')
    .then((res) => {
      if (res.errorCode === 0) {
        resourcePermissionList.value = res.data;
      } else {
        ElMessage.error(res.message || $t('message.getDataError'));
      }
    })
    .catch(() => {
      ElMessage.error($t('message.getDataError'));
    });
}

// 保存表单
function save() {
  saveForm.value?.validate((valid) => {
    if (valid) {
      btnLoading.value = true;
      const url = isAdd.value
        ? 'api/v1/sysApiKey/key/save'
        : 'api/v1/sysApiKey/update';
      api
        .post(url, entity.value)
        .then((res) => {
          btnLoading.value = false;
          if (res.errorCode === 0) {
            ElMessage.success(res.message || $t('message.saveOkMessage'));
            emit('reload');
            closeDialog();
          } else {
            ElMessage.error(res.message || $t('message.saveFailMessage'));
          }
        })
        .catch(() => {
          btnLoading.value = false;
          ElMessage.error($t('message.saveFailMessage'));
        });
    }
  });
}

// 关闭对话框
function closeDialog() {
  saveForm.value?.resetFields();
  // 重置表单数据
  entity.value = {
    apiKey: '',
    name: '',
    status: '',
    deptId: '',
    expiredAt: null,
    permissionIds: [],
  };
  isAdd.value = true;
  dialogVisible.value = false;
}

onMounted(() => {
  getResourcePermissionList();
});

defineExpose({
  openDialog,
});
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    draggable
    :title="isAdd ? $t('button.add') : $t('button.edit')"
    :before-close="closeDialog"
    :close-on-click-modal="false"
    width="50%"
  >
    <ElForm
      label-width="120px"
      ref="saveForm"
      :model="entity"
      status-icon
      :rules="rules"
      class="form-container"
    >
      <!-- 名称 -->
      <ElFormItem prop="name" :label="$t('sysApiKey.name')">
        <ElInput
          v-model="entity.name"
          :placeholder="
            $t('message.pleaseInput', { name: $t('sysApiKey.name') })
          "
          style="width: 100%"
          maxlength="100"
        />
      </ElFormItem>

      <!-- 状态选择 -->
      <ElFormItem prop="status" :label="$t('sysApiKey.status')">
        <DictSelect
          v-model="entity.status"
          dict-code="dataStatus"
          style="width: 100%"
        />
      </ElFormItem>

      <ElFormItem prop="expiredAt" :label="$t('sysApiKey.expiredAt')">
        <ElDatePicker
          v-model="entity.expiredAt"
          type="datetime"
          :placeholder="
            $t('message.pleaseSelect', { name: $t('sysApiKey.expiredAt') })
          "
          style="width: 100%"
        />
      </ElFormItem>

      <ElFormItem
        prop="permissions"
        :label="$t('sysApiKey.permissions')"
        class="permission-form-item"
      >
        <ElAlert type="info" :closable="false" class="permission-alert">
          接口信息请运行：tech.aiflowy.publicapi.SyncApis 的 main
          方法同步到数据库。
        </ElAlert>
        <ElCollapse
          accordion
          v-model="expandedGroups"
          class="permission-collapse"
        >
          <ElCollapseItem
            v-for="group in groupedPermissionList"
            :key="group.groupName"
            :name="group.groupName"
            class="permission-collapse-item"
          >
            <template #title>
              <div class="collapse-title" @click.stop>
                <ElCheckbox
                  :model-value="isGroupAllSelected(group)"
                  :indeterminate="isGroupIndeterminate(group)"
                  @update:model-value="
                    (checked: any) => toggleGroupSelection(group, checked)
                  "
                  @click.stop
                >
                  <span class="group-name">{{ group.groupName }}</span>
                  <span class="group-count">
                    ({{ group.permissions.length }})
                  </span>
                </ElCheckbox>
              </div>
            </template>
            <ElCheckboxGroup
              v-model="entity.permissionIds"
              class="permission-checkbox-group"
            >
              <ElCheckbox
                v-for="item in group.permissions"
                :key="item.id"
                :value="item.id"
                class="permission-checkbox"
              >
                <div class="permission-content">
                  <div class="permission-interface">
                    {{ item.requestInterface }}
                  </div>
                  <div class="permission-title">{{ item.title }}</div>
                </div>
              </ElCheckbox>
            </ElCheckboxGroup>
          </ElCollapseItem>
        </ElCollapse>
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

<style scoped>
.form-container {
  max-height: 60vh;
  padding-right: 10px;
  overflow-y: auto;
}

.permission-form-item :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.permission-alert {
  margin-bottom: 8px;
}

.permission-collapse {
  width: 100%;
}

.permission-collapse :deep(.el-collapse-item__header) {
  padding-left: 12px;
  font-weight: normal;
  background-color: #f9fafb;
  border-radius: 6px;
}

.permission-collapse :deep(.el-collapse-item__wrap) {
  background-color: #fff;
}

.permission-collapse :deep(.el-collapse-item__content) {
  padding: 12px;
}

.collapse-title {
  display: flex;
  flex: 1;
  align-items: center;
}

.group-name {
  font-weight: 600;
  color: #374151;
}

.group-count {
  margin-left: 4px;
  font-size: 12px;
  color: #6b7280;
}

.permission-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.permission-checkbox {
  display: flex;
  align-items: flex-start;
  height: auto;
  padding: 8px;
  margin: 0;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.permission-checkbox:hover {
  background-color: #f3f4f6;
}

.permission-checkbox :deep(.el-checkbox__label) {
  line-height: 1.5;
  word-break: break-all;
  white-space: normal;
}

.permission-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.permission-interface {
  font-family: monospace;
  font-size: 13px;
  font-weight: 500;
  color: #2563eb;
  word-break: break-all;
}

.permission-title {
  font-size: 12px;
  color: #6b7280;
  word-break: break-all;
}

.form-container::-webkit-scrollbar {
  width: 6px;
}

.form-container::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 3px;
}
</style>