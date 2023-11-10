import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import User from '@/domain/user/components/User'
import { Button, ExtendButton } from '@starwhale/ui'
import { useAccess } from '@/api/WithAuth'
import { ISftSpaceVo, api } from '@/api'
import { useQuery } from 'react-query'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import SpaceForm from '@/domain/space/components/SpaceForm'
import { useEventCallback } from '@starwhale/core'

export default function SftSpaceListCard() {
    const [page] = usePage()
    const history = useHistory()
    const { projectId } = useParams<{ projectId: any }>()
    const [isOpen, setIsOpen] = React.useState(false)
    const [editRow, setEditRow] = React.useState<ISftSpaceVo>()

    const info = useQuery(
        ['listSftSpace', projectId],
        () =>
            api.listSftSpace(projectId, {
                ...page,
            }),
        {
            enabled: !!projectId,
        }
    )

    const [t] = useTranslation()

    const isAccessCreate = useAccess('sft.space.create')
    const isAccessUpdate = useAccess('sft.space.update')
    // const isAccessDelete = useAccess('sft.space.delete')

    const getActions = (data: ISftSpaceVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/spaces/sfts/${data.id}`)}
                >
                    {hasText ? t('View Details') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: isAccessUpdate,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='a-Versionhistory'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => {
                        setEditRow(data)
                        setIsOpen(true)
                    }}
                >
                    {hasText ? t('sft.space.action.edit') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const handleCreate = useEventCallback(async (data: ISftSpaceVo) => {
        await api.createSftSpace(projectId, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })
    const handleEdit = useEventCallback(async ({ id, ...data }: ISftSpaceVo) => {
        if (!id) return
        await api.updateSftSpace(projectId, id, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })

    return (
        <Card
            title={t('sft.space.title')}
            extra={
                isAccessCreate && (
                    <Button
                        size='compact'
                        onClick={() => {
                            setEditRow(undefined)
                            setIsOpen(true)
                        }}
                    >
                        {t('create')}
                    </Button>
                )
            }
        >
            <Table
                renderActions={(rowIndex) => {
                    const data = info.data?.list?.[rowIndex]
                    if (!data) return undefined
                    return getActions(data)
                }}
                isLoading={info.isLoading}
                columns={[t('sft.space.id'), t('sft.space.name'), t('Owner'), t('Created'), t('Description')]}
                data={
                    info.data?.list?.map((data) => {
                        return [
                            data.id,
                            data.name,
                            data.owner && <User user={data.owner} />,
                            data.createdTime && formatTimestampDateTime(data.createdTime),
                            data.description,
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
            <Modal isOpen={isOpen} onClose={() => setIsOpen(false)} closeable animate autoFocus>
                <ModalHeader>
                    {editRow ? t('edit sth', [t('sft.space.title')]) : t('create sth', [t('sft.space.title')])}
                </ModalHeader>
                <ModalBody>
                    <SpaceForm
                        label={t('sft.space.title')}
                        data={editRow}
                        onSubmit={editRow ? handleEdit : handleCreate}
                    />
                </ModalBody>
            </Modal>
        </Card>
    )
}
