import React, { useCallback, useEffect, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createProject } from '@project/services/project'
import { usePage } from '@/hooks/usePage'
import { ICreateProjectSchema } from '@project/schemas/project'
import ProjectForm from '@project/components/ProjectForm'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { Link } from 'react-router-dom'
import { useFetchProjects } from '@project/hooks/useFetchProjects'
import IconFont from '@/components/IconFont'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { useStyletron } from 'baseui'
import { QueryInput } from '@/components/data-table/stateful-data-table'
import cn from 'classnames'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { StatefulTooltip } from 'baseui/tooltip'
import { IProjectSchema } from '../../domain/project/schemas/project'

type IProjectCardProps = {
    project: IProjectSchema
}

const ProjectCard = ({ project }: IProjectCardProps) => {
    const [css] = useStyletron()
    const [t] = useTranslation()

    return (
        <div
            className={css({
                'display': 'flex',
                'height': '116px',
                'gap': '10px',
                'background': '#FFFFFF',
                'border': '1px solid #E2E7F0',
                'borderRadius': '4px',
                'padding': '20px',
                'flexDirection': 'column',
                'alignItems': 'space-between',
                'justifyContent': 'space-between',
                'textDecoration': 'none',
                'color': ' rgba(2,16,43,0.60)',
                ':hover': {
                    boxShadow: '0 2px 8px 0 rgba(0,0,0,0.20)',
                },
            })}
        >
            <div
                className={css({
                    display: 'flex',
                    justifyContent: 'space-between',
                    flex: 1,
                    flexGrow: 0,
                    lineHeight: '18px',
                })}
            >
                <Link
                    className={css({
                        display: 'flex',
                        width: '80%',
                        textDecoration: 'none',
                    })}
                    to={`/projects/${project.id}`}
                >
                    <p
                        className={cn(
                            'text-ellipsis',
                            css({
                                'fontSize': '14px',
                                'fontWeight': 'bold',
                                'color': '#02102B',
                                ':hover': {
                                    textDecoration: 'underline',
                                    color: ' #5181E0',
                                },
                            })
                        )}
                    >
                        {[project.owner?.name, project.name].join('/')}
                    </p>
                </Link>
                <div
                    className={css({
                        display: 'flex',
                        lineHeight: '12px',
                    })}
                >
                    <p
                        className={cn(
                            css({
                                display: 'flex',
                                fontSize: '12px',
                                color: '#00B368',
                                // private
                                // color: '#4848B3',
                                background: '#E6FFF4',
                                borderRadius: '9px',
                                padding: '3px 10px',
                            })
                        )}
                    >
                        public
                    </p>
                </div>
            </div>
            <div
                className={css({
                    display: 'flex',
                    justifyContent: 'space-between',
                    color: ' rgba(2,16,43,0.60)',
                })}
            >
                <StatefulTooltip content='desc' placement='bottom'>
                    desc
                </StatefulTooltip>
            </div>
            <div
                className={css({
                    display: 'flex',
                    justifyContent: 'space-between',
                })}
            >
                <div />
                <StatefulTooltip content={t('Manage Member')} placement='bottom'>
                    <Link
                        key={project.id}
                        to={`/projects/${project.id}/members`}
                        className={cn(
                            'flex-row-center',
                            css({
                                'display': 'flex',
                                'fontSize': '12px',
                                'background': '#F4F5F7',
                                'borderRadius': '2px',
                                'width': '20px',
                                'height': '20px',
                                'textDecoration': 'none',
                                'color': 'gray !important',
                                ':hover span': {
                                    color: ' #5181E0  !important',
                                },
                            })
                        )}
                    >
                        <IconFont type='setting' size={12} />
                    </Link>
                </StatefulTooltip>
            </div>
        </div>
    )
}

export default function ProjectListCard() {
    const [page] = usePage()
    const projectsInfo = useFetchProjects({ ...page, pageNum: 1, pageSize: 10000 })
    const [filter, setFilter] = useState('')
    const [isCreateProjectOpen, setIsCreateProjectOpen] = useState(false)
    const handleCreateProject = useCallback(
        async (data: ICreateProjectSchema) => {
            await createProject(data)
            await projectsInfo.refetch()
            setIsCreateProjectOpen(false)
        },
        [projectsInfo]
    )
    const [data, setData] = useState<IProjectSchema[]>([])
    const [css] = useStyletron()
    const [t] = useTranslation()
    // eslintd-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()

    useEffect(() => {
        const items = projectsInfo.data?.list ?? []
        setData(items.filter((i) => (filter && i.name.includes(filter)) || filter === ''))
    }, [filter, projectsInfo.data])

    const projectCards = useMemo(() => {
        if (data.length === 0 && filter) {
            return <BusyPlaceholder type='notfound' />
        }
        if (data.length === 0) {
            return <BusyPlaceholder type='empty' />
        }
        return data.map((project) => {
            return <ProjectCard key={project.id} project={project} />
        })
    }, [data, filter])

    return (
        <Card
            title={currentUser?.name ?? t('projects')}
            titleIcon={undefined}
            extra={
                <Button
                    startEnhancer={<IconFont type='add' kind='white' />}
                    size={ButtonSize.compact}
                    onClick={() => setIsCreateProjectOpen(true)}
                >
                    {t('create')}
                </Button>
            }
        >
            <div className={css({ marginBottom: '20px', width: '280px' })}>
                <QueryInput
                    onChange={(val: string) => {
                        setFilter(val.trim())
                    }}
                />
            </div>
            <div
                className={css({
                    marginBottom: '20px',
                    display: 'grid',
                    width: '100%',
                    flexWrap: 'wrap',
                    gap: '20px',
                    gridTemplateColumns:
                        data.length >= 3 || data.length === 0
                            ? 'repeat(auto-fit, minmax(334px, 1fr))'
                            : 'repeat(3, minmax(334px, 1fr))',
                })}
            >
                {projectCards}
            </div>
            <Modal
                isOpen={isCreateProjectOpen}
                onClose={() => setIsCreateProjectOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('create sth', [t('Project')])}</ModalHeader>
                <ModalBody>
                    <ProjectForm onSubmit={handleCreateProject} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
