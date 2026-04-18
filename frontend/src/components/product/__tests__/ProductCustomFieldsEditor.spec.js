// @vitest-environment jsdom

import { mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import ProductCustomFieldsEditor from '../ProductCustomFieldsEditor.vue'

const ElInputStub = defineComponent({
  name: 'ElInputStub',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
  template: `
    <input
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
})

const ElButtonStub = defineComponent({
  name: 'ElButtonStub',
  emits: ['click'],
  template: '<button @click="$emit(\'click\')"><slot /></button>'
})

const mountEditor = () => {
  return mount(defineComponent({
    components: { ProductCustomFieldsEditor },
    setup() {
      const rows = ref([{ key: '', value: '' }])
      return { rows }
    },
    template: `
      <div>
        <ProductCustomFieldsEditor v-model="rows" />
        <pre class="rows-json">{{ JSON.stringify(rows) }}</pre>
      </div>
    `
  }), {
    global: {
      stubs: {
        'el-input': ElInputStub,
        'el-button': ElButtonStub
      }
    }
  })
}

describe('ProductCustomFieldsEditor', () => {
  it('应支持新增字段行并同步到外层 v-model', async () => {
    const wrapper = mountEditor()

    await wrapper.findAll('button')[0].trigger('click')

    expect(JSON.parse(wrapper.find('.rows-json').text())).toHaveLength(2)
  })

  it('应支持修改字段内容并删除字段行', async () => {
    const wrapper = mountEditor()

    await wrapper.findAll('input')[0].setValue('品牌')
    await wrapper.findAll('input')[1].setValue('A牌')

    expect(JSON.parse(wrapper.find('.rows-json').text())).toEqual([
      { key: '品牌', value: 'A牌' }
    ])

    await wrapper.findAll('button')[1].trigger('click')

    expect(JSON.parse(wrapper.find('.rows-json').text())).toEqual([
      { key: '', value: '' }
    ])
  })
})
