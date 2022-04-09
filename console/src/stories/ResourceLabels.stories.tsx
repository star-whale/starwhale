import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import ResourceLabels from '@/components/ResourceLabels'
import { IResourceSchema } from '../schemas/resource'

export default {
    title: 'Example/ResourceLabels',
    component: ResourceLabels,
    argTypes: {},
} as ComponentMeta<typeof ResourceLabels>

/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof ResourceLabels> = (args) => <ResourceLabels {...args} />

export const Primary = Template.bind({})
Primary.args = {
    resource: {
        labels: [
            {
                key: 'key',
                value: 2,
            },
            {
                key: 'key',
                value: 3,
            },
        ],
    },
}
