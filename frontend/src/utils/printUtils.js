export const openPrintWindow = (path) => {
    if (!path || typeof window === 'undefined') {
        return
    }
    window.open(path, '_blank')
}

