import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import { Modal, ModalBody } from 'baseui/modal'

import Toggle from '@/components/selector/Toggle'

export default {
    title: 'Example/Toggle',
    component: Toggle,
    argTypes: {
        backgroundColor: { control: 'color' },
    },
} as ComponentMeta<typeof Toggle>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Modal> = (args) => <Toggle {...args} />

export const Primary = Template.bind({})
Primary.args = {}
