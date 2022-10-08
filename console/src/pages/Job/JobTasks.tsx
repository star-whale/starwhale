import React, { useCallback, useMemo, useState } from 'react'
import _, { isPlainObject } from 'lodash'
import { toaster } from 'baseui/toast'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { LazyLog } from 'react-lazylog'
import { Panel } from 'baseui/accordion'
import { fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { getToken } from '@/api'
import { ITaskSchema, TaskStatusType } from '@/domain/job/schemas/task'
import Accordion from '@/components/Accordion'
import TaskListCard from './TaskListCard'

export interface IScrollProps {
    scrollTop: number
    scrollHeight: number
    clientHeight: number
}

export default function JobTasks() {
    const [t] = useTranslation()
    const [follow, setFollow] = useState(true)
    const [currentTask, setCurrentTask] = useState<ITaskSchema | undefined>(undefined)
    const [, setExpanded] = useState(false)
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, string>>({})
    const onAction = useCallback(
        async (type, task: ITaskSchema) => {
            setCurrentTask(task)
            if ([TaskStatusType.RUNNING].includes(task.taskStatus)) {
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
                    files[v] = content ?? ''
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
        let obj
        try {
            obj = JSON.parse(part)
            if (!isPlainObject(obj)) {
                return part
            }
        } catch (e) {
            return part
        }

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
            <span style={{ whiteSpace: 'nowrap' }}>
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
            </span>
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
                }}
            >
                <TaskListCard header={null} onAction={onAction} />

                <Card outTitle={t('View Log')} style={{ padding: 0 }}>
                    <Accordion
                        overrides={{
                            Header: {
                                style: {
                                    borderRadius: '8px',
                                },
                            },
                            Content: {
                                style: {
                                    height: '800px',
                                    paddingBottom: '0px',
                                    paddingTop: '0px',
                                    backgroundColor: 'var(--color-brandBgSecondary)',
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
                                        text={content || ' '}
                                        follow={follow}
                                        formatPart={formatContent}
                                        onScroll={handleScroll}
                                        extraLines={1}
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
                </Card>
            </div>
        </>
    )
}
