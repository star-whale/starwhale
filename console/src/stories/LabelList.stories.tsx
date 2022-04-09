import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import { Modal, ModalBody } from 'baseui/modal'

import LabelList from '@/components/LabelList'

export default {
    title: 'Example/LabelList',
    component: LabelList,
    argTypes: {
        backgroundColor: { control: 'color' },
    },
} as ComponentMeta<typeof LabelList>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof LabelList> = (args) => <LabelList {...args} />

export const Primary = Template.bind({})
Primary.args = {
    value: [{ key: 'a', value: 1 }],
}
