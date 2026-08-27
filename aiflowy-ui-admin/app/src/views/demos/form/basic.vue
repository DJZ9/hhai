<script lang="ts" setup>
import { h } from 'vue';

import { Page, useAIFlowyDrawer } from '@aiflowy/common-ui';

import { ElButton, ElCard, ElCheckbox, ElMessage } from 'element-plus';

import { useAIFlowyForm } from '#/adapter/form';
import { getAllMenusApi } from '#/api';
import { $t } from '#/locales';

const optionLabelMap: Record<string, string> = {
  A: $t('demos.optionA'),
  B: $t('demos.optionB'),
  C: $t('demos.optionC'),
};

const [Form, formApi] = useAIFlowyForm({
  commonConfig: {
    // 所有表单项
    componentProps: {
      class: 'w-full',
    },
  },
  layout: 'horizontal',
  // 大屏一行显示3个，中屏一行显示2个，小屏一行显示1个
  // wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
  handleSubmit: (values) => {
    ElMessage.success($t('demos.formDataMessage', { json: JSON.stringify(values) }));
  },
  schema: [
    {
      component: 'IconPicker',
      fieldName: 'icon',
      label: 'IconPicker',
    },
    {
      // 组件需要在 #/adapter.ts内注册，并加上类型
      component: 'ApiSelect',
      // 对应组件的参数
      componentProps: {
        // 菜单接口转options格式
        afterFetch: (data: { name: string; path: string }[]) => {
          return data.map((item: any) => ({
            label: item.name,
            value: item.path,
          }));
        },
        // 菜单接口
        api: getAllMenusApi,
      },
      // 字段名
      fieldName: 'api',
      // 界面显示的label
      label: 'ApiSelect',
    },
    {
      component: 'ApiTreeSelect',
      // 对应组件的参数
      componentProps: {
        // 菜单接口
        api: getAllMenusApi,
        childrenField: 'children',
        // 菜单接口转options格式
        labelField: 'name',
        valueField: 'path',
      },
      // 字段名
      fieldName: 'apiTree',
      // 界面显示的label
      label: 'ApiTreeSelect',
    },
    {
      component: 'Input',
      fieldName: 'string',
      label: 'String',
    },
    {
      component: 'InputNumber',
      fieldName: 'number',
      label: 'Number',
    },
    {
      component: 'RadioGroup',
      fieldName: 'radio',
      label: 'Radio',
      componentProps: {
        options: [
          { value: 'A', label: 'A' },
          { value: 'B', label: 'B' },
          { value: 'C', label: 'C' },
          { value: 'D', label: 'D' },
          { value: 'E', label: 'E' },
        ],
      },
    },
    {
      component: 'RadioGroup',
      fieldName: 'radioButton',
      label: 'RadioButton',
      componentProps: {
        isButton: true,
        options: ['A', 'B', 'C', 'D', 'E', 'F'].map((v) => ({
          value: v,
          label: `选项${v}`,
        })),
      },
    },
    {
      component: 'CheckboxGroup',
      fieldName: 'checkbox',
      label: 'Checkbox',
      componentProps: {
        options: ['A', 'B', 'C'].map((v) => ({ value: v, label: optionLabelMap[v] || v })),
      },
    },
    {
      component: 'CheckboxGroup',
      fieldName: 'checkbox1',
      label: 'Checkbox1',
      renderComponentContent: () => {
        return {
          default: () => {
            return ['A', 'B', 'C', 'D'].map((v) =>
              h(ElCheckbox, { label: v, value: v }),
            );
          },
        };
      },
    },
    {
      component: 'CheckboxGroup',
      fieldName: 'checkbotton',
      label: 'CheckBotton',
      componentProps: {
        isButton: true,
        options: [
          { value: 'A', label: $t('demos.optionA') },
          { value: 'B', label: $t('demos.optionB') },
          { value: 'C', label: $t('demos.optionC') },
        ],
      },
    },
    {
      component: 'DatePicker',
      fieldName: 'date',
      label: 'Date',
    },
    {
      component: 'Select',
      fieldName: 'select',
      label: 'Select',
      componentProps: {
        filterable: true,
        options: [
          { value: 'A', label: $t('demos.optionA') },
          { value: 'B', label: $t('demos.optionB') },
          { value: 'C', label: $t('demos.optionC') },
        ],
      },
    },
  ],
});

const [Drawer, drawerApi] = useAIFlowyDrawer();
function setFormValues() {
  formApi.setValues({
    string: 'string',
    number: 123,
    radio: 'B',
    radioButton: 'C',
    checkbox: ['A', 'C'],
    checkbotton: ['B', 'C'],
    checkbox1: ['A', 'B'],
    date: new Date(),
    select: 'B',
  });
}
</script>
<template>
  <Page
    :description="$t('demos.formDesc')"
    :title="$t('demos.formTitle')"
  >
    <Drawer class="w-[600px]" :title="$t('demos.basicForm')">
      <Form />
    </Drawer>
    <ElCard>
      <template #header>
        <div class="flex items-center">
          <span class="flex-auto">{{ $t('demos.basicForm') }}</span>
          <ElButton type="primary" @click="setFormValues">{{ $t('demos.setFormValues') }}</ElButton>
        </div>
      </template>
      <ElButton type="primary" @click="drawerApi.open"> {{ $t('demos.openDrawer') }} </ElButton>
    </ElCard>
  </Page>
</template>
