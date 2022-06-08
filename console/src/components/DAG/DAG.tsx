import React, { useRef } from 'react'
import { Canvas, Node, Edge, Port, MarkerArrow, Label, CanvasPosition, CanvasRef } from 'reaflow'
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch'
import IconFont from '../IconFont/index'
import { Spinner, SIZE } from 'baseui/spinner'
import './index.scss'
import classNames from 'classnames'
import { Link } from 'react-router-dom'

enum Status {
    CREATED = 'CREATED',
    RUNNING = 'RUNNING',
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
    CANCELED = 'CANCELED',
    ASSIGNING = 'ASSIGNING',
}

enum Group {
    JOB = 'Job',
    STEP = 'Step',
    TASK = 'Task',
}

export default function DAG({ nodes = [], edges = [] }: any) {
    const ref = useRef<CanvasRef | null>(null)

    const getStatusMap = (status: string) => {
        let icon,
            edge = { className: '' }
        switch (status) {
            case Status.SUCCESS:
            case Status.CREATED:
                icon = <IconFont type='success' style={{ color: 'green' }} />
                break
            case Status.FAILED:
                icon = <IconFont type='clear' style={{ color: 'red' }} />
                break
            case Status.CANCELED:
                icon = <IconFont type='clear' />
                break

            case Status.RUNNING:
            case Status.ASSIGNING:
                icon = <Spinner $size={20} />
                edge = {
                    className: 'edge--dash',
                }
                break
            default:
                icon = <IconFont type='eye' />
                break
        }
        return {
            icon,
            edge,
        }
    }

    const $nodes = nodes.map((node: any, index: number) => {
        return {
            ...node,
            // use foreignObject , default text should be empty string
            text: ' ',
            width: 200,
            height: 44,
            // config : only one port
            ports: [
                {
                    id: `${node.id}-from`,
                    width: 10,
                    height: 10,
                    side: 'EAST',
                },
                {
                    id: `${node.id}-to`,
                    width: 10,
                    height: 10,
                    side: 'WEST',
                    hidden: true,
                },
            ],
        }
    })

    const $edges = edges.map((edge: any, index: number) => {
        return {
            ...edge,
            fromPort: `${edge.from}-from`,
            toPort: `${edge.to}-to`,
        }
    })

    return (
        <div
            className='flowContainer'
            style={{
                width: '100%',
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                gridGap: '16px',
            }}
        >
            <TransformWrapper wheel={{ disabled: true }} limitToBounds={false} maxScale={1.2} minScale={0.8}>
                {({ zoomIn, zoomOut, resetTransform, ...rest }) => (
                    <React.Fragment>
                        <div className='flow-tools'>
                            <button onClick={() => zoomIn()}>+</button>
                            <button onClick={() => zoomOut()}>-</button>
                            <button onClick={() => resetTransform()}>x</button>
                        </div>
                        <TransformComponent>
                            <Canvas
                                // fit={true}
                                ref={ref}
                                maxHeight={300}
                                nodes={$nodes}
                                edges={$edges}
                                direction='RIGHT'
                                defaultPosition={CanvasPosition.LEFT}
                                node={
                                    <Node
                                        className='node'
                                        width={200}
                                        height={80}
                                        style={{
                                            stroke: '#1a192b',
                                            fill: 'transparent',
                                            strokeWidth: 0,
                                            border: '0px',
                                        }}
                                        label={<Label style={{ fill: 'transparent' }} />}
                                        port={
                                            <Port
                                                className='port'
                                                style={{ fill: '#b1b1b7', stroke: 'white' }}
                                                rx={10}
                                                ry={10}
                                            />
                                        }
                                    >
                                        {(event) => {
                                            const conf = getStatusMap(event.node?.data?.content)
                                            return (
                                                <foreignObject height={event.height} width={event.width} x={0} y={0}>
                                                    <Link to='tasks'>
                                                        <div className='flow-card'>
                                                            <div className='flow-title'>{event.node?.data?.type}</div>
                                                            {conf.icon ?? ''}
                                                            <div className='flow-content'>
                                                                {event.node?.data?.content}
                                                            </div>
                                                            <div className='flow-time'>3s</div>
                                                        </div>
                                                    </Link>
                                                </foreignObject>
                                            )
                                        }}
                                    </Node>
                                }
                                arrow={<MarkerArrow className='marker' />}
                                edge={(edge) => {
                                    const node = nodes.find((v: any) => v.id === edge.source)
                                    const conf = getStatusMap(node?.data?.content)
                                    return <Edge className={`edge ${conf?.edge?.className}`} />
                                }}
                            />
                        </TransformComponent>
                    </React.Fragment>
                )}
            </TransformWrapper>
        </div>
    )
}
