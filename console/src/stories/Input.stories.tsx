import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'

import Input, { NumberInput, SIZE } from '@/components/Input'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/Input',
    component: Input,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
} as ComponentMeta<typeof Input>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Input> = (args) => <Input {...args}>{args.children}</Input>

export const Primary = Template.bind({})

Primary.args = {
    isLoading: false,
    disabled: false,
    size: 'compact',
    value: 'someting input',
}
