import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import Filter from '@/components/Filter'

export default {
    title: 'Example/Filter',
    component: Filter,
    argTypes: {},
} as ComponentMeta<typeof Filter>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Filter> = (args) => <Filter {...args} />

export const Primary = Template.bind({})
Primary.args = {}
