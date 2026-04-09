export const parsePageData = (payload) => {
    if (Array.isArray(payload)) {
        return {
            list: payload,
            total: payload.length
        }
    }

    return {
        list: Array.isArray(payload?.list) ? payload.list : [],
        total: typeof payload?.total === 'number' ? payload.total : 0
    }
}

export const parseDraftId = (draftIdValue) => {
    const normalized = Number(draftIdValue)
    if (!Number.isFinite(normalized) || normalized <= 0) {
        return null
    }
    return normalized
}

export const displayText = (value) => {
    if (value === null || value === undefined) {
        return '-'
    }
    const normalized = String(value).trim()
    return normalized || '-'
}

