import React, { useCallback, useEffect, useMemo, useState } from 'react'
import _, { isPlainObject } from 'lodash'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { LazyLog, ScrollFollow } from 'react-lazylog'
import { Panel } from 'baseui/accordion'
import { fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { getToken } from '@/api'
import { ITaskSchema, TaskStatusType } from '@/domain/job/schemas/task'
import Accordion from '@starwhale/ui/Accordion'
import TaskListCard from './TaskListCard'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder, Button } from '@starwhale/ui'

const ComplexToolbarLogViewer = React.lazy(() => import('@/components/LogViewer/LogViewer'))

export interface IScrollProps {
    scrollTop: number
    scrollHeight: number
    clientHeight: number
}

export default function JobTasks() {
    const [t] = useTranslation()
    const [, theme] = themedUseStyletron()
    const [follow, setFollow] = useState(true)
    const [currentTask, setCurrentTask] = useState<ITaskSchema | undefined>(undefined)
    const [, setExpanded] = useState(false)
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, string>>({})
    const onAction = useCallback(async (type, task: ITaskSchema) => {
        setCurrentTask(task)
        const data = await fetchTaskOfflineLogFiles(task?.id)
        const files: Record<string, string> = {}
        if (!_.isEmpty(data)) {
            data.map(async (v: string) => {
                const content = await fetchTaskOfflineFileLog(task?.id, v)
                files[v] = content ?? ''
                setCurrentLogFiles({
                    ...files,
                })
            })
        } else {
            toaster.negative('No logs collected for this task', { autoHideDuration: 2000 })
        }
        if ([TaskStatusType.RUNNING].includes(task.taskStatus)) {
            files[task?.stepName] = 'ws'
            setCurrentLogFiles({
                ...files,
            })
        }

        setExpanded(true)
    }, [])

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

    const sources = React.useMemo(() => {
        return Object.entries(currentLogFiles).map(([fileName, content]) => {
            return {
                id: fileName,
                type: '',
                data: content.startsWith('ws') ? '' : content,
                ws: content.startsWith('ws') ? currentOnlineLogUrl : undefined,
            }
        })
    }, [currentLogFiles, currentOnlineLogUrl])

    console.log(sources)

    return (
        <>
            <div
                style={{
                    width: '100%',
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <TaskListCard header={null} onAction={onAction} />

                <Card outTitle={t('Logs collected')} style={{ padding: 0, flex: 1 }}>
                    <React.Suspense fallback={<BusyPlaceholder />}>
                        <ComplexToolbarLogViewer sources={sources} />
                    </React.Suspense>
                </Card>
            </div>
        </>
    )
}
