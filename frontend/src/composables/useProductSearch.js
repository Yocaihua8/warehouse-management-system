import { ref } from 'vue'
import { parsePageData } from '../utils/orderHelper'

export function useProductSearch(options) {
    const {
        fetchProductList,
        onError,
        defaultPageNum = 1,
        defaultPageSize = 200
    } = options || {}

    const productOptions = ref([])
    const productLoading = ref(false)

    const notifyError = (message, error) => {
        if (typeof onError === 'function') {
            onError(message, error)
        }
    }

    const loadProductsByName = async (productName = '') => {
        if (typeof fetchProductList !== 'function') {
            productOptions.value = []
            return
        }

        try {
            productLoading.value = true
            const res = await fetchProductList({
                productName: productName || undefined,
                pageNum: defaultPageNum,
                pageSize: defaultPageSize
            })

            if (res.data?.code === 1) {
                const pageData = parsePageData(res.data?.data)
                productOptions.value = pageData.list
            } else {
                productOptions.value = []
                notifyError(res.data?.message || '加载商品列表失败')
            }
        } catch (error) {
            productOptions.value = []
            notifyError(error?.response?.data?.message || error?.message || '加载商品列表失败', error)
        } finally {
            productLoading.value = false
        }
    }

    const loadProducts = async () => {
        await loadProductsByName('')
    }

    const loadProductsByQuery = async (query = {}) => {
        if (typeof fetchProductList !== 'function') {
            productOptions.value = []
            return
        }

        try {
            productLoading.value = true
            const res = await fetchProductList({
                pageNum: query.pageNum ?? defaultPageNum,
                pageSize: query.pageSize ?? defaultPageSize,
                productName: (query.productName || '').trim() || undefined
            })

            if (res.data?.code === 1) {
                const pageData = parsePageData(res.data?.data)
                productOptions.value = pageData.list
            } else {
                productOptions.value = []
                notifyError(res.data?.message || '加载商品列表失败')
            }
        } catch (error) {
            productOptions.value = []
            notifyError(error?.response?.data?.message || error?.message || '加载商品列表失败', error)
        } finally {
            productLoading.value = false
        }
    }

    const handleProductSearch = (keyword) => {
        loadProductsByName((keyword || '').trim())
    }

    const upsertProductOption = (product) => {
        if (!product || product.id == null) {
            return
        }
        const exists = productOptions.value.some(item => Number(item.id) === Number(product.id))
        if (!exists) {
            productOptions.value = [product, ...productOptions.value]
        }
    }

    const handleProductChange = (row, productId) => {
        const selectedProduct = productOptions.value.find(
            item => Number(item.id) === Number(productId)
        )

        if (selectedProduct) {
            row.productCode = selectedProduct.productCode || ''
            row.productName = selectedProduct.productName || ''
            row.specification = selectedProduct.specification || ''
            row.unit = selectedProduct.unit || ''
        } else {
            row.productCode = ''
            row.productName = ''
            row.specification = ''
            row.unit = ''
        }
    }

    return {
        productOptions,
        productLoading,
        loadProducts,
        loadProductsByName,
        loadProductsByQuery,
        handleProductSearch,
        upsertProductOption,
        handleProductChange
    }
}
