import React, { useCallback, useMemo, useState } from 'react'
import _ from 'lodash'
import { toaster } from 'baseui/toast'
import useTranslation from '@/hooks/useTranslation'
import { useJob } from '@job/hooks/useJob'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import Card from '@/components/Card'
import { LazyLog } from 'react-lazylog'
import { Accordion, Panel } from 'baseui/accordion'
import { fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { getToken } from '@/api'
// import useWebSocket from '@/hooks/useWebSocket'
import TaskListCard from './TaskListCard'
import { ITaskSchema, TaskStatusType } from '../../domain/job/schemas/task'

export interface IScrollProps {
    scrollTop: number
    scrollHeight: number
    clientHeight: number
}

export default function JobOverview() {
    const { job } = useJob()
    const [t] = useTranslation()

    const items = [
        {
            label: t('Job ID'),
            value: job?.id ?? '',
        },
        {
            label: t('Owner'),
            value: job?.owner?.name ?? '-',
        },
        {
            label: t('Run time'),
            value: job?.duration && job?.duration > 0 ? durationToStr(job?.duration * 1000) : '-',
        },
        {
            label: t('Created time'),
            value: job?.createdTime && formatTimestampDateTime(job.createdTime),
        },
        {
            label: t('End time'),
            value: job?.stopTime && formatTimestampDateTime(job.stopTime),
        },
    ]

    const [follow, setFollow] = useState(true)
    const [currentTask, setCurrentTask] = useState<ITaskSchema | undefined>(undefined)
    const [, setExpanded] = useState(false)
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, string>>({})
    const onAction = useCallback(
        async (type, task: ITaskSchema) => {
            setCurrentTask(task)
            if ([TaskStatusType.RUNNING, TaskStatusType.PREPARING].includes(task.taskStatus)) {
                setCurrentLogFiles({
                    [task?.uuid]: 'ws',
                })
            } else {
                const data = await fetchTaskOfflineLogFiles(task?.id)
                if (_.isEmpty(data)) {
                    toaster.negative(t('no logs found'), { autoHideDuration: 2000 })
                }
                const files: Record<string, string> = {}
                data.map(async (v: string) => {
                    const content = await fetchTaskOfflineFileLog(task?.id, v)
                    files[v] = content
                    setCurrentLogFiles({
                        ...files,
                    })
                })
            }
            setExpanded(true)
        },
        [t]
    )

    const handleScroll = ({ scrollTop, scrollHeight, clientHeight }: IScrollProps) => {
        const delta = scrollHeight - scrollTop
        if (follow && delta > clientHeight && clientHeight >= 0) {
            setFollow(false)
        } else if (!follow && delta <= clientHeight && clientHeight >= 0) {
            setFollow(true)
        }
        // setScrollToLine(0)
    }

    const formatContent = useCallback((part) => {
        const obj = JSON.parse(part)
        const columns = [
            {
                key: 'time',
                value: obj?.time,
                styles: {
                    width: '190px',
                } as React.CSSProperties,
            },
            {
                key: 'stream',
                value: obj?.stream,
                styles: {
                    width: '50px',
                } as React.CSSProperties,
            },
            {
                key: 'log',
                value: obj?.log,
                styles: {
                    width: 'auto',
                } as React.CSSProperties,
            },
            {
                key: 'text',
                value: obj?.text,
                styles: {
                    width: 'auto',
                } as React.CSSProperties,
            },
            {
                key: 'logIncrement',
                value: obj?.logIncrement,
                styles: {
                    width: 'auto',
                } as React.CSSProperties,
            },
        ]
        if (obj?.logIncrement) {
            return obj?.logIncrement.filter((v: string) => v !== '').join('\n')
        }

        return (
            <>
                {columns
                    .filter((v) => !!v.value)
                    .map((v, k) => {
                        return (
                            <span
                                key={v.key + k}
                                style={{
                                    ...v.styles,
                                    display: 'inline-block',
                                    margin: '0 5px',
                                }}
                            >
                                {' '}
                                {v.value}{' '}
                            </span>
                        )
                    })}
            </>
        ) as any
    }, [])

    const currentOnlineLogUrl = useMemo(() => {
        return `${window.location.protocol === 'http:' ? 'ws:' : 'wss:'}//${window.location.host}/api/v1/log/online/${
            currentTask?.id
        }?Authorization=${getToken()}`
    }, [currentTask])

    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr 430px',
                    gridAutoRows: '200px',
                    // gridAutoColumns: 'minmax(200px, 1fr)',
                    gridGap: '16px',
                }}
            >
                <div style={{ gridColumnStart: 'span 2' }}>
                    <TaskListCard header={null} onAction={onAction} />

                    <Accordion
                        overrides={{
                            Content: {
                                style: {
                                    height: '800px',
                                    paddingBottom: '20px',
                                },
                            },
                        }}
                        onChange={({ expanded }) => {
                            setExpanded(expanded.includes('0'))
                        }}
                    >
                        {Object.entries(currentLogFiles).map(([fileName, content]) => (
                            <Panel key={fileName} title={`Log: ${fileName}`}>
                                {!content.startsWith('ws') ? (
                                    <LazyLog
                                        enableSearch
                                        selectableLines
                                        text={content || ''}
                                        follow={follow}
                                        formatPart={formatContent}
                                        // scrollToLine={scrollToLine}
                                        onScroll={handleScroll}
                                    />
                                ) : (
                                    <LazyLog
                                        enableSearch
                                        selectableLines
                                        url={currentOnlineLogUrl}
                                        websocket
                                        websocketOptions={{
                                            formatMessage: formatContent,
                                        }}
                                        follow={follow}
                                        onScroll={handleScroll}
                                    />
                                )}
                            </Panel>
                        ))}
                    </Accordion>
                </div>

                <Card
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                        gap: '12px',
                        fontSize: '16px',
                    }}
                >
                    {items.map((v) => (
                        <div key={v?.label} style={{ display: 'flex' }}>
                            <div style={{ flexBasis: '130px' }}>{v?.label}</div>
                            <div>: {v?.value}</div>
                        </div>
                    ))}
                </Card>
            </div>
        </>
    )
}
