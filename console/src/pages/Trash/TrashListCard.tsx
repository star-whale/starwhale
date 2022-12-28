import React, { useEffect, useState } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { useFetchTrashs } from '@/domain/trash/hooks/useFetchTrashs'
import { useStyletron } from 'baseui'
import { QueryInput } from '@starwhale/ui/base/data-table/stateful-data-table'
import { ITrashSchema } from '@/domain/trash/schemas/trash'
import { useParams } from 'react-router'
import { recoverTrash, removeTrash } from '@/domain/trash/services/trash'
import Table from '@/components/Table'
import { formatTimestampDateTime } from '@/utils/datetime'
import { getReadableStorageQuantityStr } from '@/utils'
import { ConfirmButton } from '@/components/Modal/confirm'
import { toaster } from 'baseui/toast'

export default function TrashListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ trashId: string; projectId: string }>()
    const info = useFetchTrashs(projectId, page)
    const [filter, setFilter] = useState('')
    const [data, setData] = useState<ITrashSchema[]>([])
    const [css] = useStyletron()
    const [t] = useTranslation()

    useEffect(() => {
        const items = info.data?.list ?? []
        setData(
            items.filter((i) => {
                if (filter) return [i.name, i.trashedBy, i.type].join('/').includes(filter)
                return filter === ''
            })
        )
    }, [filter, info.data])

    return (
        <Card title={t('trash.title')}>
            <div className={css({ marginBottom: '20px', width: '280px' })}>
                <QueryInput
                    onChange={(val: string) => {
                        setFilter(val.trim())
                    }}
                />
            </div>
            <Table
                isLoading={info.isLoading}
                columns={[
                    t('trash.name'),
                    t('trash.type'),
                    t('trash.trashedTime'),
                    t('trash.size'),
                    t('trash.trashedBy'),
                    t('trash.updatedTime'),
                    t('trash.retentionTime'),
                    t('Action'),
                ]}
                data={
                    data?.map((trash) => {
                        return [
                            trash.name,
                            trash.type,
                            trash.trashedTime && formatTimestampDateTime(trash.trashedTime),
                            trash.size && getReadableStorageQuantityStr(trash.size),
                            trash.trashedBy,
                            trash.lastUpdatedTime && formatTimestampDateTime(trash.lastUpdatedTime),
                            trash.retentionTime && formatTimestampDateTime(trash.retentionTime),
                            <>
                                <ConfirmButton
                                    as='link'
                                    title={t('trash.remove.confirm')}
                                    onClick={async () => {
                                        await removeTrash(projectId, trash.id)
                                        toaster.positive(t('trash.remove.success'), { autoHideDuration: 1000 })
                                        await info.refetch()
                                    }}
                                >
                                    {t('trash.remove.button')}
                                </ConfirmButton>
                                &nbsp;&nbsp;
                                <ConfirmButton
                                    as='link'
                                    title={t('trash.restore.confirm')}
                                    onClick={async () => {
                                        await recoverTrash(projectId, trash.id)
                                        toaster.positive(t('trash.restore.success'), { autoHideDuration: 1000 })
                                        await info.refetch()
                                    }}
                                >
                                    {t('trash.restore.button')}
                                </ConfirmButton>
                            </>,
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: info.data?.pageNum,
                    count: info.data?.pageSize,
                    total: info.data?.total,
                    afterPageChange: () => {
                        info.refetch()
                    },
                }}
            />
        </Card>
    )
}
