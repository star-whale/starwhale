import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'

import { Button } from '@starwhale/ui'
import { IconFont } from '@starwhale/ui'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Example/Button',
    component: Button,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    argTypes: {
        backgroundColor: { control: 'color' },
    },
} as ComponentMeta<typeof Button>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
const Template: ComponentStory<typeof Button> = (args) => <Button {...args} />

export const Primary = Template.bind({})
// More on args: https://storybook.js.org/docs/react/writing-stories/args
Primary.args = {
    kind: 'primary',
    children: 'Button',
}

export const Secondary = Template.bind({})
Secondary.args = {
    kind: 'secondary',
    children: 'Button',
}

export const ButtonWithIcon = Template.bind({})
ButtonWithIcon.args = {
    size: 'compact',
    as: 'withIcon',
    startEnhancer: () => <IconFont type='runtime' />,
    children: 'Button',
}

export const ButtonIcon = Template.bind({})
ButtonIcon.args = {
    kind: 'primary',
    children: <IconFont type='runtime' />,
}

export const ButtonAsLink = Template.bind({})
ButtonAsLink.args = {
    as: 'link',
    children: 'Button',
}
