import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'AssistantMarket',
    path: '/assistantMarket',
    component: () => import('#/views/assistantMarket/index.vue'),
    meta: {
      icon: 'svg:app-store',
      order: 11,
      title: $t('page.assistantMarket'),
    },
  },
  {
    name: 'Assistant',
    path: '/assistantMarket/:id',
    component: () => import('#/views/assistantMarket/assistant/index.vue'),
    meta: {
      title: $t('page.assistantMarket'),
      hideInMenu: true,
      hideInTab: true,
      hideInBreadcrumb: true,
      // noBasicLayout: true,
      activePath: '/assistantMarket',
    },
  },
];

export default routes;
