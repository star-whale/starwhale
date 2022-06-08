import React, { useRef } from 'react'
import { Canvas, Node, Edge, Port, MarkerArrow, Label, CanvasPosition, CanvasRef } from 'reaflow'
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch'
import IconFont from '../IconFont/index'
import { Spinner } from 'baseui/spinner'
import './index.scss'
import { Link } from 'react-router-dom'
import { durationToStr } from '@/utils/datetime'

enum Status {
    CREATED = 'CREATED',
    RUNNING = 'RUNNING',
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
    FAIL = 'FAIL',
    CANCELED = 'CANCELED',
    ASSIGNING = 'ASSIGNING',
}

enum Type {
    JOB = 'Job',
    STEP = 'Step',
    TASK = 'Task',
}

type JobT = {
    id: string
    finishTime: number
    startTime: number
    jobType: 'EVALUATION'
    status: Status
}

type StepT = {
    id: string
    name: 'PPL' | 'CMP'
    startTime: number
    finishTime: number
    status: Status
}

type TaskT = {
    id: string
    name: string
    startTime: number
    finishTime: number
    status: Status
    agentIp: string
    type: 'PPL' | 'CMP'
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
            case Status.FAIL:
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
                                            const data = event.node?.data
                                            const conf = getStatusMap(data?.content?.status)

                                            let content: JobT | StepT | TaskT | undefined
                                            let sectionContent: string = ''

                                            if (data?.type === Type.JOB) {
                                                content = data.content as JobT
                                                sectionContent = [content.jobType].join(' ')
                                            } else if (data?.type === Type.STEP) {
                                                content = data.content as StepT
                                                sectionContent = [content.name].join(' ')
                                            } else if (data?.type === Type.TASK) {
                                                content = data.content as TaskT
                                                sectionContent = [content.type, content.name, content.agentIp].join(' ')
                                            }

                                            let sectionTime =
                                                content && content?.startTime > 0 && content?.finishTime > 0
                                                    ? durationToStr(content?.finishTime - content?.startTime)
                                                    : ''

                                            return (
                                                <foreignObject height={event.height} width={event.width} x={0} y={0}>
                                                    <Link to={`tasks?id=${data?.entityId}`}>
                                                        <div className='flow-card'>
                                                            <div className='flow-title'>
                                                                {data?.type} {content?.status}
                                                            </div>
                                                            {conf.icon ?? ''}
                                                            <div className='flow-content'>{sectionContent}</div>
                                                            <div className='flow-time'>{sectionTime}</div>
                                                        </div>
                                                    </Link>
                                                    {/* <div className='flow-card'>
                                                        <div className='flow-title'>{data?.type}</div>
                                                    </div> */}
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
