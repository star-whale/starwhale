import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'
// import { Story, Meta } from '@storybook/react/types-6-0'

import { Button, ButtonProps } from 'baseui/button'

// import { Button } from './Button'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/Button',
    component: Button,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
} as ComponentMeta<typeof Button>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Button> = (args) => <Button {...args} />

export const Primary = Template.bind({})

Primary.args = {
    isLoading: false,
    displayName: 'primary',
}

// More on args: https://storybook.js.org/docs/react/writing-stories/args
// Primary.args = {
//     // primary: true,
//     // label: 'Button',
//     // displayName: '123',
// }

// export const Secondary = Template.bind({})
// Secondary.args = {
//     label: 'Button',
// }

// export const Large = Template.bind({})
// Large.args = {
//     size: 'large',
//     label: 'Button',
// }

// export const Small = Template.bind({})
// Small.args = {
//     size: 'small',
//     label: 'Button',
// }
