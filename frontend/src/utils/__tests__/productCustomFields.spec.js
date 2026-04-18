import { describe, expect, it } from 'vitest'
import {
  getCustomFieldEntries,
  parseCustomFieldRows,
  serializeCustomFieldRows,
  summarizeCustomFields,
  validateCustomFieldRows
} from '../productCustomFields'

describe('productCustomFields', () => {
  it('应将 JSON 对象解析为键值行', () => {
    expect(parseCustomFieldRows('{"颜色":"红色","产地":"广州"}')).toEqual([
      { key: '颜色', value: '红色' },
      { key: '产地', value: '广州' }
    ])
  })

  it('应将键值行序列化为 JSON 字符串并忽略空行', () => {
    expect(serializeCustomFieldRows([
      { key: '品牌', value: 'A牌' },
      { key: '', value: '' },
      { key: '产地', value: '温州' }
    ])).toBe('{"品牌":"A牌","产地":"温州"}')
  })

  it('应校验空字段名和重复字段名', () => {
    expect(validateCustomFieldRows([
      { key: '', value: '有值' }
    ])).toBe('第 1 行字段名不能为空')

    expect(validateCustomFieldRows([
      { key: '品牌', value: 'A牌' },
      { key: '品牌', value: 'B牌' }
    ])).toBe('字段名不能重复：品牌')
  })

  it('应生成可读的字段摘要和明细列表', () => {
    const value = '{"颜色":"红色","产地":"广州","品牌":"A牌"}'
    expect(getCustomFieldEntries(value)).toHaveLength(3)
    expect(summarizeCustomFields(value)).toBe('颜色：红色；产地：广州 等3项')
  })
})
