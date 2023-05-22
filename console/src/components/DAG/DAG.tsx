import React, { useRef } from 'react'
import { Canvas, Node, Edge, Port, MarkerArrow, Label, CanvasPosition, CanvasRef } from 'reaflow'
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch'
// eslint-disable-next-line
import { Spinner } from 'baseui/spinner'
import { Link } from 'react-router-dom'
import { durationToStr } from '@/utils/datetime'
import IconFont from '@starwhale/ui/IconFont'
import './index.scss'
import { ErrorBoundary } from '@starwhale/ui/ErrorBoundary'

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

const CANVAS_DEFAULT_SIZE = 2000

export default function DAG({ nodes = [], edges = [] }: any) {
    const ref = useRef<CanvasRef | null>(null)

    const getStatusMap = (status: string) => {
        let icon
        let edge = { className: '' }
        switch (status) {
            case Status.SUCCESS:
                icon = <IconFont type='success' style={{ color: 'green' }} />
                break
            case Status.FAILED:
            case Status.FAIL:
                icon = <IconFont type='clear' style={{ color: 'red' }} />
                break
            case Status.CREATED:
                icon = <IconFont type='success' style={{ color: 'green' }} />
                break
            case Status.CANCELED:
                icon = <IconFont type='clear' />
                break
            case Status.RUNNING:
            case Status.ASSIGNING:
                icon = <Spinner $size={16} />
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

    const $nodes = nodes.map((node: any) => {
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

    const $edges = edges.map((edge: any) => {
        return {
            ...edge,
            fromPort: `${edge.from}-from`,
            toPort: `${edge.to}-to`,
        }
    })

    const [rect, setRect] = React.useState({ width: CANVAS_DEFAULT_SIZE, height: CANVAS_DEFAULT_SIZE })

    return (
        <div
            className='flowContainer'
            style={{
                position: 'absolute',
                top: 0,
                bottom: 0,
                left: 0,
                right: 0,
            }}
        >
            <ErrorBoundary>
                <TransformWrapper wheel={{ disabled: true }} limitToBounds={false} maxScale={1.2} minScale={0.8}>
                    {({ zoomIn, zoomOut, resetTransform }) => (
                        <>
                            <div className='flow-tools' style={{ top: rect.height + 200 }}>
                                <button type='button' onClick={() => zoomIn()}>
                                    +
                                </button>
                                <button type='button' onClick={() => zoomOut()}>
                                    -
                                </button>
                                <button type='button' onClick={() => resetTransform()}>
                                    x
                                </button>
                            </div>
                            <TransformComponent>
                                <Canvas
                                    fit
                                    ref={ref}
                                    maxHeight={rect.height + 200}
                                    maxWidth={rect.width}
                                    zoomable={false}
                                    nodes={$nodes}
                                    edges={$edges}
                                    direction='RIGHT'
                                    defaultPosition={CanvasPosition.LEFT}
                                    onLayoutChange={(layout) => {
                                        if (
                                            rect.width !== layout.width &&
                                            rect.height !== layout.height &&
                                            rect.width > 0 &&
                                            rect.height > 0
                                        ) {
                                            setRect({
                                                width: layout.width ?? CANVAS_DEFAULT_SIZE,
                                                height: layout.height ?? CANVAS_DEFAULT_SIZE,
                                            })
                                        }
                                    }}
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
                                                let sectionContent = ''

                                                if (data?.type === Type.JOB) {
                                                    content = data.content as JobT
                                                    sectionContent = [content.jobType].join(' ')
                                                } else if (data?.type === Type.STEP) {
                                                    content = data.content as StepT
                                                    sectionContent = [content.name].join(' ')
                                                } else if (data?.type === Type.TASK) {
                                                    content = data.content as TaskT
                                                    sectionContent = [content.type, content.name, content.agentIp].join(
                                                        ' '
                                                    )
                                                }

                                                const sectionTime =
                                                    content && content?.startTime > 0 && content?.finishTime > 0
                                                        ? durationToStr(content?.finishTime - content?.startTime)
                                                        : ''

                                                return (
                                                    <foreignObject
                                                        height={event.height}
                                                        width={event.width}
                                                        x={0}
                                                        y={0}
                                                    >
                                                        <Link to={`tasks?id=${data?.entityId}`}>
                                                            <div className='flow-card'>
                                                                <div className='flow-title'>
                                                                    {data?.type} {content?.status}
                                                                </div>
                                                                {conf.icon ?? ''}
                                                                <div className='flow-content' title={sectionContent}>
                                                                    {sectionContent}
                                                                </div>
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
                        </>
                    )}
                </TransformWrapper>
            </ErrorBoundary>
        </div>
    )
}
