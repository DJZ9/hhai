<script lang="ts" setup>
import { ref } from 'vue';

import { Page } from '@aiflowy/common-ui';

import {
  ElButton,
  ElCard,
  ElMessage,
  ElNotification,
  ElSegmented,
  ElSpace,
  ElTable,
} from 'element-plus';

import { $t } from '#/locales';

type NotificationType = 'error' | 'info' | 'success' | 'warning';

function info() {
  ElMessage.info('How many roads must a man walk down');
}

function error() {
  ElMessage.error({
    duration: 2500,
    message: 'Once upon a time you dressed so fine',
  });
}

function warning() {
  ElMessage.warning('How many roads must a man walk down');
}
function success() {
  ElMessage.success(
    'Cause you walked hand in hand With another man in my place',
  );
}

function notify(type: NotificationType) {
  ElNotification({
    duration: 2500,
    message: $t('demos.notifyMessage'),
    type,
  });
}
const tableData = [
  { prop1: '1', prop2: 'A' },
  { prop1: '2', prop2: 'B' },
  { prop1: '3', prop2: 'C' },
  { prop1: '4', prop2: 'D' },
  { prop1: '5', prop2: 'E' },
  { prop1: '6', prop2: 'F' },
];

const segmentedValue = ref('Mon');

const segmentedOptions = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
</script>

<template>
  <Page
    :description="$t('demos.elementDesc')"
    :title="$t('demos.elementTitle')"
  >
    <div class="flex flex-wrap gap-5">
      <ElCard class="mb-5 w-auto">
        <template #header> {{ $t('demos.cardButton') }} </template>
        <ElSpace>
          <ElButton text>Text</ElButton>
          <ElButton>Default</ElButton>
          <ElButton type="primary"> Primary </ElButton>
          <ElButton type="info"> Info </ElButton>
          <ElButton type="success"> Success </ElButton>
          <ElButton type="warning"> Warning </ElButton>
          <ElButton type="danger"> Error </ElButton>
        </ElSpace>
      </ElCard>
      <ElCard class="mb-5 w-80">
        <template #header> Message </template>
        <ElSpace>
          <ElButton type="info" @click="info"> {{ $t('demos.info') }} </ElButton>
          <ElButton type="danger" @click="error"> {{ $t('demos.error') }} </ElButton>
          <ElButton type="warning" @click="warning"> {{ $t('demos.warning') }} </ElButton>
          <ElButton type="success" @click="success"> {{ $t('demos.success') }} </ElButton>
        </ElSpace>
      </ElCard>
      <ElCard class="mb-5 w-80">
        <template #header> Notification </template>
        <ElSpace>
          <ElButton type="info" @click="notify('info')"> {{ $t('demos.info') }} </ElButton>
          <ElButton type="danger" @click="notify('error')"> {{ $t('demos.error') }} </ElButton>
          <ElButton type="warning" @click="notify('warning')"> {{ $t('demos.warning') }} </ElButton>
          <ElButton type="success" @click="notify('success')"> {{ $t('demos.success') }} </ElButton>
        </ElSpace>
      </ElCard>
      <ElCard class="mb-5 w-auto">
        <template #header> Segmented </template>
        <ElSegmented
          v-model="segmentedValue"
          :options="segmentedOptions"
          size="large"
        />
      </ElCard>
      <ElCard class="mb-5 w-80">
        <template #header> V-Loading </template>
        <div class="flex size-72 items-center justify-center" v-loading="true">
          {{ $t('demos.sampleContent') }}
        </div>
      </ElCard>
      <ElCard class="mb-5 w-80">
        <ElTable :data="tableData" stripe>
          <ElTable.TableColumn :label="$t('demos.testCol1')" prop="prop1" />
          <ElTable.TableColumn :label="$t('demos.testCol2')" prop="prop2" />
        </ElTable>
      </ElCard>
    </div>
  </Page>
</template>
