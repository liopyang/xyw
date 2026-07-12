import { createRouter,createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'

const router=createRouter({history:createWebHistory(import.meta.env.BASE_URL),routes:[
  {path:'/login',component:()=>import('../views/LoginView.vue'),meta:{public:true,title:'登录'}},
  {path:'/',component:AdminLayout,redirect:'/dashboard',children:[
    {path:'dashboard',component:()=>import('../views/DashboardView.vue'),meta:{title:'数据看板'}},
    {path:'orders',component:()=>import('../views/orders/OrderListView.vue'),meta:{title:'订单管理'}},
    {path:'orders/create',component:()=>import('../views/orders/OrderFormView.vue'),meta:{title:'新增订单'}},
    {path:'orders/:id',component:()=>import('../views/orders/OrderFormView.vue'),meta:{title:'订单详情'}},
    {path:'agents',component:()=>import('../views/agents/AgentListView.vue'),meta:{title:'代理管理'}},
    {path:'agents/create',component:()=>import('../views/agents/AgentFormView.vue'),meta:{title:'新增代理',ownerOnly:true}},
    {path:'agents/:id',component:()=>import('../views/agents/AgentFormView.vue'),meta:{title:'代理详情'}},
    {path:'issues',component:()=>import('../views/issues/IssueListView.vue'),meta:{title:'问题管理'}},
    {path:'issues/:id',component:()=>import('../views/issues/IssueDetailView.vue'),meta:{title:'问题详情'}},
    {path:'config/driving-school',component:()=>import('../views/config/ConfigView.vue'),meta:{title:'驾校配置',ownerOnly:true,configGroup:'driving'}},
    {path:'config/system',component:()=>import('../views/config/ConfigView.vue'),meta:{title:'系统配置',ownerOnly:true,configGroup:'system'}},
    {path:'system/users',component:()=>import('../views/system/UserListView.vue'),meta:{title:'管理员账号',ownerOnly:true}},
    {path:'system/logs',component:()=>import('../views/system/LogListView.vue'),meta:{title:'操作日志',ownerOnly:true}},
  ]},
  {path:'/:pathMatch(.*)*',redirect:'/dashboard'}
]})
router.beforeEach(to=>{const token=localStorage.getItem('campus_token');if(!to.meta.public&&!token)return{path:'/login',query:{redirect:to.fullPath}};if(to.path==='/login'&&token)return'/dashboard';if(to.meta.ownerOnly){const user=JSON.parse(localStorage.getItem('campus_user')||'null');if(user?.role!=='OWNER')return'/dashboard'}return true})
router.afterEach(to=>{document.title=`${String(to.meta.title||'管理端')} - 校园业务管理系统`})
export default router
