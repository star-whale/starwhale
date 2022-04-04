import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import DoubleCheckForm from '@/components/DoubleCheckForm'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/DoubleCheckForm',
    component: DoubleCheckForm,
    argTypes: {},
} as ComponentMeta<typeof DoubleCheckForm>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof DoubleCheckForm> = (args) => <DoubleCheckForm {...args} />
export const Primary = Template.bind({})
Primary.args = {
    tips: <p>expected == input value</p>,
    buttonLabel: 'submit',
    expected: 'a',
}
