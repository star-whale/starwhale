import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import { Modal, ModalBody } from 'baseui/modal'

import Grid from '@/components/Grid'

export default {
    title: 'Example/Grid',
    component: Grid,
    argTypes: {
        backgroundColor: { control: 'color' },
    },
} as ComponentMeta<typeof Grid>

const handleRenderItem = (item) => {
    return <p>{item}</p>
}
/* eslint-disable react/jsx-props-no-spreading */
const Template: ComponentStory<typeof Grid> = (args) => (
    <Grid
        isLoading={false}
        items={[1, 2, 3]}
        onRenderItem={handleRenderItem}
        paginationProps={{
            start: 1,
            count: 5,
            total: 5,
            afterPageChange: () => {
                // Info.refetch()
            },
        }}
    />
)

export const Primary = Template.bind({})
Primary.args = {}
