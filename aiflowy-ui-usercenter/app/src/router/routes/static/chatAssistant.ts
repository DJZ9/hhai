import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    name: 'ChatAssistant',
    path: '/chatAssistant',
    component: () => import('#/views/chatAssistant/index.vue'),
    meta: {
      icon: 'svg:chat-assistant',
      order: 22,
      title: $t('page.my.agent'),
    },
  },
];

export default routes;
