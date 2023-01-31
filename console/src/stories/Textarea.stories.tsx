import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import Input from '@starwhale/ui/Input'

export default {
    title: 'Example/Input/Input',
    component: Input,
    argTypes: {},
} as ComponentMeta<typeof Input>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Input> = (args) => <Input {...args} clearable />

export const Primary = Template.bind({})
Primary.args = {
    clearable: true,
    disabled: false,
    size: 'compact',
}

export const Clearable = Template.bind({})
Clearable.args = {
    clearable: true,
    disabled: false,
    size: 'compact',
    value: '123',
}
