import React from 'react'
import { ComponentStory, ComponentMeta, Story } from '@storybook/react'
import normalLogoImg from '@/assets/logo_normal_en_gray.svg'
import ZoomWrapper from '../components/Viewer/ZoomWrapper'

import ImageSegmentationViewer from '../components/Viewer/ImageSegmentationViewer'

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    title: 'Viewer/ZoomWrapper',
    component: ZoomWrapper,
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    // argTypes: {},
} as ComponentMeta<typeof ZoomWrapper>

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable */
// @ts-ignore
const Template: ComponentStory<typeof ZoomWrapper> = (args) => (
    <div
        className='flowContainer'
        style={{
            width: '100%',
            height: '100%',
            position: 'absolute',
        }}
    >
        <ZoomWrapper {...args}>{args.children}</ZoomWrapper>
    </div>
)

export const Primary = Template.bind({})

Primary.args = {
    children: (
        <img
            style={{
                margin: '0 auto',
                width: '100%',
            }}
            src={normalLogoImg}
            alt='logo'
        />
    ),
}
