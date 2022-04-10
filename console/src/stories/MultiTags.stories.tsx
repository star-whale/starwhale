import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import MultiTags from '@/components/Tag/MultiTags'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/MultiTags',
    component: MultiTags,
    argTypes: {},
} as ComponentMeta<typeof MultiTags>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof MultiTags> = (args) => <MultiTags {...args} />
export const Primary = Template.bind({})
Primary.args = {
    value: ['a', 'b', 'c'],
    getValueLabel(v) {
        return v.option.id + ':label'
    },
}
