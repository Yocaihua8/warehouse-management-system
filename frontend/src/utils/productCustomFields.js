export const createEmptyCustomField = () => ({
  key: '',
  value: ''
})

const isPlainObject = (value) => Boolean(value) && !Array.isArray(value) && typeof value === 'object'

const normalizeRow = (row = {}) => ({
  key: String(row.key || ''),
  value: String(row.value ?? '')
})

export const ensureCustomFieldRows = (rows = []) => {
  if (!Array.isArray(rows) || rows.length === 0) {
    return [createEmptyCustomField()]
  }
  return rows.map(normalizeRow)
}

export const getCustomFieldEntries = (jsonText) => {
  const text = String(jsonText || '').trim()
  if (!text) {
    return []
  }

  try {
    const parsed = JSON.parse(text)
    if (!isPlainObject(parsed)) {
      return []
    }

    return Object.entries(parsed).map(([key, value]) => ({
      key: String(key),
      value: value == null ? '' : String(value)
    }))
  } catch (_error) {
    return []
  }
}

export const parseCustomFieldRows = (jsonText) => {
  const entries = getCustomFieldEntries(jsonText)
  if (entries.length === 0) {
    return [createEmptyCustomField()]
  }
  return entries
}

export const validateCustomFieldRows = (rows = []) => {
  const normalizedRows = ensureCustomFieldRows(rows)
  const seenKeys = new Set()

  for (let index = 0; index < normalizedRows.length; index += 1) {
    const row = normalizedRows[index]
    const key = row.key.trim()
    const value = String(row.value ?? '')
    const hasValue = value.trim().length > 0

    if (!key && !hasValue) {
      continue
    }

    if (!key) {
      return `第 ${index + 1} 行字段名不能为空`
    }

    if (seenKeys.has(key)) {
      return `字段名不能重复：${key}`
    }
    seenKeys.add(key)
  }

  const json = serializeCustomFieldRows(normalizedRows)
  if (json.length > 4000) {
    return '自定义字段长度不能超过4000个字符'
  }

  return ''
}

export const serializeCustomFieldRows = (rows = []) => {
  const normalizedRows = ensureCustomFieldRows(rows)
  const result = {}

  normalizedRows.forEach((row) => {
    const key = row.key.trim()
    const value = String(row.value ?? '')
    const hasValue = value.trim().length > 0

    if (!key && !hasValue) {
      return
    }

    if (key) {
      result[key] = value
    }
  })

  return Object.keys(result).length > 0 ? JSON.stringify(result) : ''
}

export const summarizeCustomFields = (jsonText, maxItems = 2) => {
  const entries = getCustomFieldEntries(jsonText)
  if (entries.length === 0) {
    return '-'
  }

  const visibleEntries = entries.slice(0, maxItems).map(item => `${item.key}：${item.value || '-'}`)
  if (entries.length <= maxItems) {
    return visibleEntries.join('；')
  }
  return `${visibleEntries.join('；')} 等${entries.length}项`
}
