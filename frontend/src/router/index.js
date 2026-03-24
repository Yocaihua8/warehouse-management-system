import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import HomeView from '../views/HomeView.vue'
import InboundCreateView from '../views/inbound/InboundCreateView.vue'
import InboundDetailView from '../views/inbound/InboundDetailView.vue'
import InboundListView from '../views/inbound/InboundListView.vue'
import ProductCreateView from '../views/product/ProductCreateView.vue'
import ProductUpdateView from '../views/product/ProductUpdateView.vue'
import ProductListView from '../views/product/ProductListView.vue'
import CustomerListView from '../views/customer/CustomerListView.vue'
import CustomerCreateView from '../views/customer/CustomerCreateView.vue'
import CustomerUpdateView from '../views/customer/CustomerUpdateView.vue'
import StockListView from '../views/stock/StockListView.vue'
import OutboundCreateView from '../views/outbound/OutboundCreateView.vue'
import OutboundListView from '../views/outbound/OutboundListView.vue'
import OutboundDetailView from '../views/outbound/OutboundDetailView.vue'


const routes = [
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
                path: 'stock/list',
                name: 'StockList',
                component: StockListView
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
            }

        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router