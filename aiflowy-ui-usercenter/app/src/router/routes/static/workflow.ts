import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'Workflow',
    path: '/workflow',
    component: () => import('#/views/bots/index.vue'),
    meta: {
      icon: 'svg:bot',
      order: 33,
      title: $t('bot.title'),
    },
  },
  {
    name: 'WorkflowDetail',
    path: '/workflow/:id',
    component: () => import('#/views/bots/bot/index.vue'),
    meta: {
      title: $t('bot.title'),
      hideInMenu: true,
      hideInTab: true,
      hideInBreadcrumb: true,
      activePath: '/workflow',
    },
  },
];

export default routes;
