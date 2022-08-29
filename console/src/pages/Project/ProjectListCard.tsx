import React, { useCallback, useEffect, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { changeProject, createProject } from '@project/services/project'
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
import { createUseStyles } from 'react-jss'
import { IProjectSchema } from '../../domain/project/schemas/project'
import { IconLink, TextLink } from '@/components/Link'

type IProjectCardProps = {
    project: IProjectSchema
    onEdit?: () => void
}

const useCardStyles = createUseStyles({
    card: {
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
    },
    row: {
        display: 'flex',
        justifyContent: 'space-between',
        flexGrow: 0,
        lineHeight: '18px',
    },
    rowKey: {
        color: 'rgba(2,16,43,0.60)',
        marginRight: '8px',
    },
    rowValue: {
        display: 'flex',
        alignItems: 'center',
        color: '#02102B',
    },
    rowEnd: {
        marginLeft: 'auto',
    },
    description: {
        display: 'flex',
        justifyContent: 'space-between',
        color: ' rgba(2,16,43,0.60)',
    },
    statistics: {
        display: 'flex',
        justifyContent: 'flex-start',
        color: ' rgba(2,16,43,0.60)',
        gap: '12px',
    },
    statisticsItem: {
        display: 'flex',
        gap: '4px',
    },
    tag: {
        fontSize: '12px',
        color: '#00B368',
        backgroundColor: '#E6FFF4',
        borderRadius: '9px',
        padding: '3px 10px',
    },
})

const ProjectCard = ({ project, onEdit }: IProjectCardProps) => {
    const [css] = useStyletron()
    const [t] = useTranslation()
    const styles = useCardStyles()

    return (
        <div className={styles.card}>
            <div className={styles.row}>
                <TextLink to={`/projects/${project.id}`} style={{ width: '80%' }}>
                    {[project.owner?.name, project.name].join('/')}
                </TextLink>
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
                                color: project?.privacy === 'PRIVATE' ? '#4848B3' : '#00B368',
                                backgroundColor: project?.privacy === 'PRIVATE' ? '#EDEDFF' : '#E6FFF4',
                                borderRadius: '9px',
                                padding: '3px 10px',
                            })
                        )}
                    >
                        {project.privacy === 'PRIVATE' ? t('Private') : t('Public')}
                    </p>
                </div>
            </div>
            <div className={styles.description}>
                <StatefulTooltip content={project.description ?? ''} placement='bottom'>
                    {project.description ?? ''}
                </StatefulTooltip>
            </div>
            <div
                className={css({
                    display: 'flex',
                    justifyContent: 'space-between',
                })}
            >
                <div className={styles.statistics}>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={`${t('Evaluations')}:${project?.statistics.evaluationCounts}`}
                        >
                            <IconFont
                                type='evaluation'
                                size={12}
                                style={{ color: 'rgba(2,16,43,0.20)', marginRight: '4px' }}
                            />
                            <span>{project?.statistics.evaluationCounts}</span>
                        </IconLink>
                    </div>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={`${t('Datasets')}:${project?.statistics.datasetCounts}`}
                        >
                            <IconFont
                                type='dataset'
                                size={12}
                                style={{ color: 'rgba(2,16,43,0.20)', marginRight: '4px' }}
                            />
                            <span>{project?.statistics.datasetCounts}</span>
                        </IconLink>
                    </div>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={`${t('Models')}:${project?.statistics.modelCounts}`}
                        >
                            <IconFont
                                type='model'
                                size={12}
                                style={{ color: 'rgba(2,16,43,0.20)', marginRight: '4px' }}
                            />
                            <span>{project?.statistics.modelCounts}</span>
                        </IconLink>
                    </div>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={`${t('Members')}:${project?.statistics.memberCounts}`}
                        >
                            <IconFont
                                type='a-managemember'
                                size={12}
                                style={{ color: 'rgba(2,16,43,0.20)', marginRight: '4px' }}
                            />
                            <span>{project?.statistics.memberCounts}</span>
                        </IconLink>
                    </div>
                </div>
                <div
                    style={{
                        display: 'flex',
                        gap: '12px',
                    }}
                >
                    <IconLink to={`/projects/${project.id}/members`} tooltip={t('Manage Member')}>
                        <IconFont type='setting' size={12} style={{ color: 'rgba(2,16,43,0.60)' }} />
                    </IconLink>
                    <StatefulTooltip content={t('edit sth', [t('Project')])} placement='top'>
                        <Button
                            onClick={onEdit}
                            size='compact'
                            kind='secondary'
                            overrides={{
                                BaseButton: {
                                    style: {
                                        'display': 'flex',
                                        'fontSize': '12px',
                                        'backgroundColor': '#F4F5F7',
                                        'width': '20px',
                                        'height': '20px',
                                        'textDecoration': 'none',
                                        'color': 'gray !important',
                                        'paddingLeft': '10px',
                                        'paddingRight': '10px',
                                        ':hover span': {
                                            color: ' #5181E0  !important',
                                        },
                                        ':hover': {
                                            backgroundColor: '#F0F4FF',
                                        },
                                    },
                                },
                            }}
                        >
                            <IconFont type='edit' size={10} />
                        </Button>
                    </StatefulTooltip>
                </div>
            </div>
        </div>
    )
}

export default function ProjectListCard() {
    const [page] = usePage()
    const projectsInfo = useFetchProjects({ ...page, pageNum: 1, pageSize: 10000 })
    const [filter, setFilter] = useState('')
    const [isCreateProjectOpen, setIsCreateProjectOpen] = useState(false)
    const [editProject, setEditProject] = useState<IProjectSchema>()

    const handleCreateProject = useCallback(
        async (data: ICreateProjectSchema) => {
            await createProject(data)
            await projectsInfo.refetch()
            setIsCreateProjectOpen(false)
        },
        [projectsInfo]
    )
    const handleEditProject = useCallback(
        async (data: ICreateProjectSchema) => {
            if (!editProject) return
            await changeProject(editProject.id, data)
            await projectsInfo.refetch()
            setIsCreateProjectOpen(false)
        },
        [projectsInfo, editProject]
    )

    const [data, setData] = useState<IProjectSchema[]>([])
    const [css] = useStyletron()
    const [t] = useTranslation()
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
            return (
                <ProjectCard
                    key={project.id}
                    project={project}
                    onEdit={() => {
                        setEditProject(project)
                        setIsCreateProjectOpen(true)
                    }}
                />
            )
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
                    onClick={() => {
                        setEditProject(undefined)
                        setIsCreateProjectOpen(true)
                    }}
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
                <ModalHeader>
                    {editProject ? t('edit sth', [t('Project')]) : t('create sth', [t('Project')])}
                </ModalHeader>
                <ModalBody>
                    <ProjectForm
                        project={editProject}
                        onSubmit={editProject ? handleEditProject : handleCreateProject}
                    />
                </ModalBody>
            </Modal>
        </Card>
    )
}
