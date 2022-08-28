import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'

import DatasetViewer from '../components/Viewer/DatasetViewer'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Viewer/DatasetViewer',
    component: DatasetViewer,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
    parameters: {
        // More on Story layout: https://storybook.js.org/docs/react/configure/story-layout
        layout: 'fullscreen',
    },
} as ComponentMeta<typeof DatasetViewer>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable */
// @ts-ignore
const Template: ComponentStory<typeof DatasetViewer> = (args) => (
    <div
        style={{
            minHeight: '800px',
            padding: '20px',
        }}
    >
        <DatasetViewer {...args} />
    </div>
)

export const Primary = Template.bind({})

Primary.args = {}
