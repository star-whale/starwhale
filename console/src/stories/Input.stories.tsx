import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'

import NumberInput from '@/components/Input/NumberInput'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/NumberInput',
    component: NumberInput,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
} as ComponentMeta<typeof NumberInput>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof NumberInput> = (args) => <NumberInput {...args}>{args.children}</NumberInput>

export const Primary = Template.bind({})

Primary.args = {
    isLoading: false,
    disabled: false,
    size: 'compact',
    value: 123123123123,
}
