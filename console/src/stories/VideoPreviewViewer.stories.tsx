import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'

import VideoPreviewViewer from '../components/Viewer/VideoPreviewViewer'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Viewer/VideoPreviewViewer',
    component: VideoPreviewViewer,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
} as ComponentMeta<typeof VideoPreviewViewer>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable */
// @ts-ignore
const Template: ComponentStory<typeof VideoPreviewViewer> = (args) => <VideoPreviewViewer {...args} />

export const Primary = Template.bind({})

Primary.args = {}
