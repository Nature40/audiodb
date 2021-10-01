
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      //{ path: '', component: () => import('pages/index.vue') },
      { path: '', redirect: '/projects' },      
      { path: 'projects', component: () => import('pages/projects.vue') },      
    ]
  },
  {path: '/projects/:project', component: () => import('layouts/ProjectLayout.vue'),
    children: [
      {path: ':catchAll(.*)*', component: () => import('pages/error404.vue')},
    ],  
  },    

  {path: '/:catchAll(.*)*', component: () => import('pages/error404.vue')}
]

export default routes
