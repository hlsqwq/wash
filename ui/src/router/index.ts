import { createRouter, createWebHashHistory } from 'vue-router'

import ModelDetailView from '../views/ModelDetailView.vue'
import ModelFormView from '../views/ModelFormView.vue'
import ModelListView from '../views/ModelListView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/models' },
    { path: '/models', name: 'model-list', component: ModelListView },
    { path: '/models/new', name: 'model-create', component: ModelFormView },
    { path: '/models/:id', name: 'model-detail', component: ModelDetailView, props: true },
    { path: '/models/:id/edit', name: 'model-edit', component: ModelFormView, props: true },
    { path: '/:pathMatch(.*)*', redirect: '/models' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

export default router
