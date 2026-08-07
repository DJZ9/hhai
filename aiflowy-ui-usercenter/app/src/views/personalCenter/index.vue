<script setup lang="ts">
import type { FormInstance } from 'element-plus';

import { reactive, ref } from 'vue';

import { useAppConfig } from '@aiflowy/hooks';
import { useAccessStore, useUserStore } from '@aiflowy/stores';

import {
  ElAvatar,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElSpace,
  ElUpload,
} from 'element-plus';
import { tryit } from 'radash';

import { $t } from '#/locales';
import { api } from '#/api/request';
import { useAuthStore } from '#/store';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const userStore = useUserStore();
const useAuth = useAuthStore();
const formRef = ref<FormInstance>();
const formData = reactive({
  nickname: userStore?.userInfo?.nickname,
  avatar: userStore?.userInfo?.avatar,
  mobile: userStore?.userInfo?.mobile,
});

const editing = reactive({
  nickname: false,
  avatar: false,
  mobile: false,
});
const rules = {
  nickname: [{ required: true, message: $t('personalCenter.usernameRequired') }],
  mobile: [
    { required: true, message: $t('personalCenter.phoneRequired') },
    { pattern: /^1[3-9]\d{9}$/, message: $t('personalCenter.phoneInvalid') },
  ],
};

const handleFieldChange = async (field: keyof typeof formData) => {
  if (!formRef.value) return;
  const [err] = await tryit(formRef.value.validateField)([field]);

  if (!err) {
    submit(field);
  }
};
const handleCancelEdit = async (field: keyof typeof formData) => {
  editing[field] = false;
  formRef.value?.resetFields([field]);
};

const handleUploadChange = (response: any) => {
  formData.avatar = response?.data.path;
  submit('avatar');
};
const accessStore = useAccessStore();
const btnLoading = ref(false);
function submit(field: keyof typeof formData) {
  btnLoading.value = true;
  api.post('/userCenter/sysAccount/updateProfile', formData).then((res) => {
    btnLoading.value = false;
    if (res.errorCode === 0) {
      ElMessage.success($t('personalCenter.updateSuccess'));
      editing[field] = false;
      useAuth.fetchUserInfo();
    }
  });
}
function logout() {
  useAuth.logout();
}
</script>

<template>
  <div class="bg-background-deep h-full w-full pt-[156px]">
    <ElForm
      ref="formRef"
      class="mx-auto"
      style="max-width: 619px; width: 100%"
      label-position="top"
      :model="formData"
      :rules="rules"
      hide-required-asterisk
    >
      <ElFormItem :label="$t('personalCenter.username')" prop="nickname">
        <div class="flex w-full justify-between gap-5">
          <template v-if="editing.nickname">
            <ElInput v-model="formData.nickname" />
            <ElSpace>
              <ElButton
                :loading="btnLoading"
                type="primary"
                @click="handleFieldChange('nickname')"
              >
                {{ $t('personalCenter.confirm') }}
              </ElButton>
              <ElButton @click="handleCancelEdit('nickname')">{{ $t('personalCenter.cancel') }}</ElButton>
            </ElSpace>
          </template>
          <template v-else>
            <span>{{ formData.nickname }}</span>
            <ElButton @click="editing.nickname = true">{{ $t('personalCenter.editUsername') }}</ElButton>
          </template>
        </div>
      </ElFormItem>
      <ElFormItem :label="$t('personalCenter.avatar')">
        <ElSpace>
          <ElUpload
            :action="`${apiURL}/userCenter/commons/upload`"
            :headers="{
              'aiflowy-token': accessStore.accessToken,
            }"
            :show-file-list="false"
            :on-success="handleUploadChange"
          >
            <ElAvatar :src="formData.avatar" :size="46" />
          </ElUpload>
          <span>{{ $t('personalCenter.avatarTip') }}</span>
        </ElSpace>
      </ElFormItem>
      <ElFormItem :label="$t('personalCenter.phone')" prop="mobile">
        <div class="flex w-full justify-between gap-5">
          <template v-if="editing.mobile">
            <ElInput v-model="formData.mobile" />
            <ElSpace>
              <ElButton
                :loading="btnLoading"
                type="primary"
                @click="handleFieldChange('mobile')"
              >
                {{ $t('personalCenter.confirm') }}
              </ElButton>
              <ElButton @click="handleCancelEdit('mobile')">{{ $t('personalCenter.cancel') }}</ElButton>
            </ElSpace>
          </template>
          <template v-else>
            <span>{{ formData.mobile }}</span>
            <ElButton @click="editing.mobile = true">{{ $t('personalCenter.modifyPhone') }}</ElButton>
          </template>
        </div>
      </ElFormItem>
      <ElFormItem>
        <div class="mt-20 flex w-full justify-center">
          <ElButton @click="logout" type="primary" class="!h-11 w-[333px]">
            {{ $t('personalCenter.logout') }}
          </ElButton>
        </div>
      </ElFormItem>
    </ElForm>
  </div>
</template>
