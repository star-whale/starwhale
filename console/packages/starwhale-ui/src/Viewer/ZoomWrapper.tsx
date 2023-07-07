import Button from '@starwhale/ui/Button'
import React from 'react'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'

export default function ZoomWrapper({ children, isTools }: any) {
    return (
        <TransformWrapper
            wheel={{ disabled: false }}
            centerOnInit
            centerZoomedOut
            limitToBounds
            maxScale={10}
            minScale={0.5}
        >
            {/* eslint-disable-next-line @typescript-eslint/no-unused-vars */}
            {({ zoomIn, zoomOut, resetTransform, centerView }) => (
                <>
                    {isTools && (
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
                    )}
                    <TransformComponent
                        wrapperStyle={{
                            width: '100%',
                            height: '100%',
                        }}
                        contentStyle={{
                            display: 'flex',
                            flexWrap: 'nowrap',
                            justifyContent: 'center',
                            maxHeight: '100%',
                        }}
                    >
                        {children}
                    </TransformComponent>
                </>
            )}
        </TransformWrapper>
    )
}
