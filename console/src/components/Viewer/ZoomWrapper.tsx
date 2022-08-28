import React from 'react'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'
import { Button } from '@/components/Button'
import normalLogoImg from '@/assets/logo_normal_en_white.svg'

export default function ZoomWrapper({ children }: any) {
    return (
        <TransformWrapper
            wheel={{ disabled: false }}
            centerOnInit
            centerZoomedOut
            limitToBounds={false}
            maxScale={10}
            minScale={0.5}
            // initialPositionX={200}
            // initialPositionY={100}
        >
            {({ zoomIn, zoomOut, resetTransform, centerView }) => (
                <>
                    <div
                        className='flow-tools'
                        style={{
                            right: '20px',
                            bottom: '20px',
                            position: 'absolute',
                            display: 'flex',
                            gap: '20px',
                            cursor: 'pointer',
                            zIndex: 10,
                        }}
                    >
                        <Button type='button' onClick={() => zoomIn()}>
                            Zoom in
                        </Button>
                        <Button type='button' onClick={() => zoomOut()}>
                            Zoom out
                        </Button>
                        <Button
                            type='button'
                            onClick={() => {
                                resetTransform()
                                centerView()
                            }}
                        >
                            Reset
                        </Button>
                    </div>
                    <TransformComponent
                        wrapperStyle={{
                            width: '100%',
                            height: '100%',
                        }}
                        contentStyle={{
                            display: 'flex',
                            flexWrap: 'nowrap',
                        }}
                    >
                        {children}
                    </TransformComponent>
                </>
            )}
        </TransformWrapper>
    )
}
