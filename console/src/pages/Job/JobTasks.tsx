import React, { useCallback, useMemo, useState } from 'react'
import _ from 'lodash'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { getToken } from '@/api'
import { ITaskSchema, TaskStatusType } from '@/domain/job/schemas/task'
import TaskListCard from './TaskListCard'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder } from '@starwhale/ui'

const ComplexToolbarLogViewer = React.lazy(() => import('@/components/LogViewer/LogViewer'))

export interface IScrollProps {
    scrollTop: number
    scrollHeight: number
    clientHeight: number
}

export default function JobTasks() {
    const [t] = useTranslation()
    const [currentTask, setCurrentTask] = useState<ITaskSchema | undefined>(undefined)
    const [, setExpanded] = useState(false)
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, string>>({})

    const onAction = useCallback(async (type, task: ITaskSchema) => {
        setCurrentTask(task)
        const files: Record<string, string> = {}

        if ([TaskStatusType.RUNNING].includes(task.taskStatus)) {
            const key = [task?.stepName, task?.id].join('@')
            files[key] = 'ws'
        }

        const data = await fetchTaskOfflineLogFiles(task?.id)
        if (!_.isEmpty(data)) {
            await Promise.all(
                data.map(async (v: string) => {
                    const content = await fetchTaskOfflineFileLog(task?.id, v)
                    const key = [task?.stepName, v].join('@')
                    files[key] = content ?? ''
                })
            )
        }

        if (Object.keys(files).length === 0) {
            toaster.negative('No logs collected for this task', { autoHideDuration: 2000 })
        }

        setCurrentLogFiles({
            ...files,
        })

        setExpanded(true)
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

    return (
        <div
            data-type='job-tasks'
            style={{
                width: '100%',
                flex: '1',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                minHeight: 0,
            }}
        >
            <TaskListCard header={null} onAction={onAction} />

            {sources.length > 0 && (
                <Card
                    outTitle={t('Logs collected')}
                    style={{ padding: 0, flex: 1, margin: 0, position: 'relative', flexDirection: 'column' }}
                >
                    <React.Suspense fallback={<BusyPlaceholder />}>
                        <ComplexToolbarLogViewer sources={sources} />
                    </React.Suspense>
                </Card>
            )}
        </div>
    )
}
