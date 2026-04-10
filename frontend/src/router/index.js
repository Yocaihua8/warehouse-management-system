import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import InboundCreateView from '../views/inbound/InboundCreateView.vue'
import InboundDetailView from '../views/inbound/InboundDetailView.vue'
import InboundListView from '../views/inbound/InboundListView.vue'
import ProductCreateView from '../views/product/ProductCreateView.vue'
import ProductUpdateView from '../views/product/ProductUpdateView.vue'
import ProductListView from '../views/product/ProductListView.vue'
import CustomerListView from '../views/customer/CustomerListView.vue'
import CustomerCreateView from '../views/customer/CustomerCreateView.vue'
import CustomerUpdateView from '../views/customer/CustomerUpdateView.vue'
import SupplierListView from '../views/supplier/SupplierListView.vue'
import SupplierCreateView from '../views/supplier/SupplierCreateView.vue'
import SupplierUpdateView from '../views/supplier/SupplierUpdateView.vue'
import UserListView from '../views/user/UserListView.vue'
import StockListView from '../views/stock/StockListView.vue'
import OutboundCreateView from '../views/outbound/OutboundCreateView.vue'
import OutboundListView from '../views/outbound/OutboundListView.vue'
import OutboundDetailView from '../views/outbound/OutboundDetailView.vue'
import InboundPrintView from '../views/inbound/InboundPrintView.vue'
import OutboundPrintView from '../views/outbound/OutboundPrintView.vue'
import AiInboundRecordListView from '../views/ai/AiInboundRecordListView.vue'
import AiOutboundRecordListView from '../views/ai/AiOutboundRecordListView.vue'
import { getCurrentUserApi } from '../api/user'
import {
    clearAuth,
    clearAuthAndBuildLoginRedirect,
    buildLoginRedirectLocation,
    getToken,
    setNickname,
    setRole,
    setUsername
} from '../utils/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginView
    },
    {
        path: '/inbound/print/:id',
        name: 'InboundPrint',
        component: InboundPrintView
    },
    {
        path: '/outbound/print/:id',
        name: 'OutboundPrint',
        component: OutboundPrintView
    },
    {
        path: '/',
        component: MainLayout,
        children: [
            {
                path: '',
                name: 'Home',
                component: HomeView
            },
            {
                path: 'inbound/list',
                name: 'InboundList',
                component: InboundListView
            },
            {
                path: 'inbound/create',
                name: 'InboundCreate',
                component: InboundCreateView
            },
            {
                path: 'inbound/detail/:id',
                name: 'InboundDetail',
                component: InboundDetailView
            },
            {
                path: 'product/list',
                name: 'ProductList',
                component: ProductListView
            },
            {
                path: 'product/create',
                name: 'ProductCreate',
                component: ProductCreateView
            },
            {
                path: 'product/update/:id',
                name: 'ProductUpdate',
                component: ProductUpdateView
            },
            {
                path: 'customer/list',
                name: 'CustomerList',
                component: CustomerListView
            },
            {
                path: 'customer/create',
                name: 'CustomerCreate',
                component: CustomerCreateView
            },
            {
                path: 'customer/update/:id',
                name: 'CustomerUpdate',
                component: CustomerUpdateView
            },
            {
                path: 'supplier/list',
                name: 'SupplierList',
                component: SupplierListView
            },
            {
                path: 'supplier/create',
                name: 'SupplierCreate',
                component: SupplierCreateView
            },
            {
                path: 'supplier/update/:id',
                name: 'SupplierUpdate',
                component: SupplierUpdateView
            },
            {
                path: 'user/list',
                name: 'UserList',
                component: UserListView
            },
            {
                path: 'stock/list',
                name: 'StockList',
                component: StockListView
            },
            {
                path: '/stock/log/list',
                name: 'StockAdjustLog',
                component: () => import('../views/StockAdjustLogView.vue')
            },
            {
                path: '/operation/log/list',
                name: 'OperationLog',
                component: () => import('../views/OperationLogView.vue')
            },
            {
                path: 'outbound/list',
                name: 'OutboundList',
                component: OutboundListView
            },
            {
                path: 'outbound/create',
                name: 'OutboundCreate',
                component: OutboundCreateView
            },
            {
                path: 'outbound/detail/:id',
                name: 'OutboundDetail',
                component: OutboundDetailView
            },
            {
                path: 'inbound/ai/list',
                component: AiInboundRecordListView
            },
            {
                path: 'outbound/ai/list',
                component: AiOutboundRecordListView
            },
            {
                path: '/ai/inbound/list',
                redirect: '/inbound/ai/list'
            },
            {
                path: '/ai/outbound/list',
                redirect: '/outbound/ai/list'
            },

        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

let authChecked = false
let authCheckPromise = null

const ensureAuthenticated = async () => {
    if (!getToken()) {
        authChecked = false
        return false
    }

    if (authChecked) {
        return true
    }

    if (!authCheckPromise) {
        authCheckPromise = getCurrentUserApi()
            .then((res) => {
                if (res.data?.code !== 1 || !res.data?.data?.username) {
                    throw new Error(res.data?.message || '登录状态校验失败')
                }

                setUsername(res.data.data.username)
                setNickname(res.data.data.nickname || '')
                setRole(res.data.data.role || '')
                authChecked = true
                return true
            })
            .catch((error) => {
                authChecked = false
                throw error
            })
            .finally(() => {
                authCheckPromise = null
            })
    }

    return authCheckPromise
}

router.beforeEach(async (to) => {
    const token = getToken()

    if (to.path === '/login') {
        if (token) {
            try {
                await ensureAuthenticated()
                return '/'
            } catch (error) {
                clearAuth()
                return true
            }
        }
        return true
    }

    if (!token) {
        return buildLoginRedirectLocation(to.fullPath)
    }

    try {
        await ensureAuthenticated()
    } catch (error) {
        return clearAuthAndBuildLoginRedirect(to.fullPath)
    }

    return true
})

export default router
