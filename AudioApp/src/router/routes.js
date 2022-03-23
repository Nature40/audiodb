
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/projects' },      
      { path: 'projects', component: () => import('pages/projects.vue') },      
      { path: 'account', component: () => import('pages/account.vue') }, 
      { path: 'accounts', component: () => import('pages/accounts.vue') }, 
    ]
  },
  {path: '/projects/:project', component: () => import('layouts/ProjectLayout.vue'),
    children: [
      { path: '/projects/:project/', component: () => import('pages/project/index.vue') },
      { path: '/projects/:project/main', component: () => import('pages/project/main.vue') },
      { path: '/projects/:project/task', component: () => import('pages/project/task.vue') },
      { path: '/projects/:project/tasks', component: () => import('pages/project/tasks.vue') },
      {path: ':catchAll(.*)*', component: () => import('pages/error404.vue')},
    ],  
  },    

  {path: '/:catchAll(.*)*', component: () => import('pages/error404.vue')}
]

export default routes
