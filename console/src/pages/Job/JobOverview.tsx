import React, { useCallback, useMemo, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useJob, useJobLoading } from '@job/hooks/useJob'
import TaskListCard from './TaskListCard'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import Card from '@/components/Card'
import { ScrollFollow, LazyLog } from 'react-lazylog'
import { Accordion, Panel } from 'baseui/accordion'
import { Grid, Cell } from 'baseui/layout-grid'
import useWebSocket from '../../hooks/useWebSocket'
import { useQuery } from 'react-query'
import { fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { ITaskSchema, TaskStatusType } from '../../domain/job/schemas/task'
import { getToken } from '@/api'

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
            value: job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
        },
        {
            label: t('Created time'),
            value: job?.createTime && formatTimestampDateTime(job.createTime),
        },
        ,
        {
            label: t('End time'),
            value: job?.createTime && formatTimestampDateTime(job.createTime),
        },
    ]

    const [currentTask, setCurrentTask] = useState<ITaskSchema | undefined>(undefined)
    const [expanded, setExpanded] = useState(false)
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, string>>({})
    const onAction = useCallback(async (type, task: ITaskSchema) => {
        setCurrentTask(task)
        if ([TaskStatusType.SUCCESS, TaskStatusType.SUCCESS].includes(task.taskStatus)) {
            const data = await fetchTaskOfflineLogFiles(task?.id)

            let files: Record<string, string> = {}
            data.map(async (v: string, k: number) => {
                const content = await fetchTaskOfflineFileLog(task?.id, v)
                files[v] = content
                setCurrentLogFiles({
                    ...files,
                })
            })
        } else if ([TaskStatusType.RUNNING, TaskStatusType.PREPARING].includes(task.taskStatus)) {
            setCurrentLogFiles({
                [task?.uuid]: 'ws',
            })
        }
        setExpanded(true)
    }, [])

    const currentOnlineLogUrl = useMemo(() => {
        return `${window.location.protocol === 'http:' ? 'ws:' : 'wss:'}//${window.location.host}/api/v1/log/online/${
            currentTask?.id
        }?Authorization=${getToken()}`
    }, [currentTask, currentTask?.id])

    // useWebSocket({
    //     debug: true,
    //     wsUrl: currentOnlineLogUrl,
    //     onMessage: (e) => {
    //         console.log('self', e)
    //     },
    // })

    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr 380px',
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
                                <ScrollFollow
                                    startFollowing
                                    render={({ follow }) => {
                                        console.log(fileName)
                                        if (content) {
                                            return (
                                                <LazyLog
                                                    enableSearch
                                                    selectableLines
                                                    text={content || ''}
                                                    follow={follow}
                                                    formatPart={(part) => {
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
                                                        ]

                                                        return (
                                                            <>
                                                                {columns
                                                                    .filter((v) => !!v.value)
                                                                    .map((v, k) => (
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
                                                                    ))}
                                                            </>
                                                        )
                                                    }}
                                                    // scrollToLine={scrollToLine}
                                                    // onScroll={handleScroll}
                                                />
                                            )
                                        }
                                        return (
                                            <LazyLog
                                                enableSearch
                                                selectableLines
                                                url={currentOnlineLogUrl}
                                                websocket
                                                websocketOptions={{
                                                    formatMessage: (e): any => {
                                                        const msg = JSON.parse(e) as any
                                                        console.log(msg)
                                                        return e
                                                    },
                                                }}
                                                follow={follow}
                                                // onScroll={handleScroll}
                                            />
                                        )
                                    }}
                                />
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
