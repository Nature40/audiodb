
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      /*{ path: '', component: () => import('pages/Index.vue') },*/
      { path: '', redirect: 'query' },
      { path: 'query', component: () => import('pages/query.vue') },
      { path: 'browser', component: () => import('pages/browser.vue') },
      { path: 'viewer', component: () => import('pages/viewer.vue') },
    ]
  },

  { path: '*', component: () => import('pages/Error404.vue') }
]

export default routes
