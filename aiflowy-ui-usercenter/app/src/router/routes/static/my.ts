import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'My',
    path: '/my',
    meta: {
      title: $t('page.my.title'),
      hideInBreadcrumb: true,
    },
    children: [
      {
        name: 'AssetLibrary',
        path: '/assetLibrary',
        component: () => import('#/views/assetLibrary/index.vue'),
        meta: {
          icon: 'svg:asset-library',
          order: 77,
          title: $t('page.my.assetLibrary'),
        },
      },
      {
        name: 'ChatHistory',
        path: '/chatHistory',
        component: () => import('#/views/chatHistory/index.vue'),
        meta: {
          icon: 'svg:chat-history',
          order: 80,
          title: $t('page.my.chatHistory'),
        },
      },
      {
        name: 'ChatHistoryDetails',
        path: '/chatHistory/:id',
        component: () => import('#/views/chatHistory/details/index.vue'),
        meta: {
          title: $t('page.my.chatHistory'),
          hideInMenu: true,
          hideInTab: true,
          hideInBreadcrumb: true,
          activePath: '/chatHistory',
        },
      },
      {
        name: 'ExecHistory',
        path: '/execHistory',
        component: () => import('#/views/execHistory/index.vue'),
        meta: {
          icon: 'svg:exec-history',
          order: 88,
          title: $t('page.my.execHistory'),
        },
      },
      {
        name: 'ExecHistoryDetails',
        path: '/execHistory/:id',
        component: () => import('#/views/execHistory/details/index.vue'),
        meta: {
          title: $t('page.my.execHistory'),
          hideInMenu: true,
          hideInTab: true,
          hideInBreadcrumb: true,
          activePath: '/execHistory',
        },
      },
      {
        name: 'PersonalCenter',
        path: '/personalCenter',
        component: () => import('#/views/personalCenter/index.vue'),
        meta: {
          icon: 'svg:people',
          order: 99,
          title: $t('page.auth.profile'),
        },
      },
    ],
  },
];

export default routes;
