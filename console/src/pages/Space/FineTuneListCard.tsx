import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { Link, MemoryRouter, Route, StaticRouter, useHistory, useParams } from 'react-router-dom'
import User from '@/domain/user/components/User'
import { Button, ExtendButton, GridResizer, Toggle } from '@starwhale/ui'
import { useAccess } from '@/api/WithAuth'
import { IFineTuneSpaceVo, api } from '@/api'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import SpaceForm from '@/domain/space/components/SpaceForm'
import { useEventCallback } from '@starwhale/core'
import { useToggle } from 'ahooks'
import ProjectListCard from '../Project/ProjectListCard'
import { useRouteContext } from '@/contexts/RouteContext'
import FineTuneRunsListCard from './FineTuneRunsListCard'

const Right = () => {
    const { RoutesInline } = useRouteContext()

    if (!RoutesInline) return null

    return (
        <RoutesInline>
            <Link to='/projects'>1</Link>
            <Link to='/projects2'>2</Link>
        </RoutesInline>
    )
}

export default function FineTuneListCard() {
    const [page] = usePage()
    const history = useHistory()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const [isOpen, setIsOpen] = React.useState(false)
    const [editRow, setEditRow] = React.useState<IFineTuneSpaceVo>()

    const info = api.useListFineTune(projectId, spaceId, {
        ...page,
    })

    const [t] = useTranslation()

    const isAccessCreate = useAccess('ft.space.create')
    const isAccessUpdate = useAccess('ft.space.update')
    // const isAccessDelete = useAccess('ft.space.delete')

    const getActions = (data: IFineTuneSpaceVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/spaces/${data.id}`)}
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
                    {hasText ? t('ft.space.action.edit') : undefined}
                </ExtendButton>
            ),
        },
    ]

    const handleCreate = useEventCallback(async (data: IFineTuneSpaceVo) => {
        await api.createSpace(projectId, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })
    const handleEdit = useEventCallback(async ({ id, ...data }: IFineTuneSpaceVo) => {
        if (!id) return
        await api.updateSpace(projectId, id, {
            name: data.name as string,
            description: data.description as string,
        })
        await info.refetch()
        setIsOpen(false)
    })

    const [expand, { toggle }] = useToggle(false)
    const [fullscreen, { toggle: toggleFullscreen }] = useToggle(false)

    const right = <Right />

    if (fullscreen) {
        return right
    }

    return (
        <>
            <Toggle onChange={toggle} />
            <Toggle onChange={toggleFullscreen} />
            <GridResizer
                left={() => (
                    <div className='flex-col content-full'>
                        <FineTuneRunsListCard />
                    </div>
                )}
                right={() => right}
                isResizeable={expand}
            />
        </>
    )
}
