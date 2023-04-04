import React, { useCallback, useEffect, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { changeProject, createProject, removeProject } from '@project/services/project'
import { usePage } from '@/hooks/usePage'
import { ICreateProjectSchema } from '@project/schemas/project'
import ProjectForm from '@project/components/ProjectForm'
import useTranslation from '@/hooks/useTranslation'
import { SIZE as ButtonSize } from 'baseui/button'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import { useFetchProjects } from '@project/hooks/useFetchProjects'
import IconFont from '@starwhale/ui/IconFont'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { useStyletron } from 'baseui'
import Input, { QueryInput } from '@starwhale/ui/Input'
import cn from 'classnames'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { StatefulTooltip } from 'baseui/tooltip'
import { createUseStyles } from 'react-jss'
import { IProjectSchema } from '@/domain/project/schemas/project'
import { IconLink, TextLink } from '@/components/Link'
import WithAuth from '@/api/WithAuth'
import { toaster } from 'baseui/toast'
import { useFetchProjectRole } from '@/domain/project/hooks/useFetchProjectRole'
import { LabelMedium } from 'baseui/typography'
import { Button } from '@starwhale/ui'
import { expandMargin, expandPadding } from '@starwhale/ui/utils'
import VisitSelector, { VisitBy } from '@/domain/project/components/VisitSelector'
import { useLocalStorage } from 'react-use'
import { useEventCallback } from '@starwhale/core'
import { formatTimestampDateTime } from '@/utils/datetime'

type IProjectCardProps = {
    project: IProjectSchema
    onRefresh?: () => void
    onEdit?: () => void
}

const useCardStyles = createUseStyles({
    projectCard: {
        'display': 'flex',
        'height': '144px',
        'gap': '0px',
        'background': '#FFFFFF',
        'border': '1px solid #E2E7F0',
        'borderRadius': '4px',
        'padding': '16px',
        'flexDirection': 'column',
        'alignItems': 'space-between',
        'justifyContent': 'start',
        'textDecoration': 'none',
        'color': ' rgba(2,16,43,0.60)',
        '&:hover': {
            'boxShadow': '0 2px 8px 0 rgba(0,0,0,0.20)',
            '& $actions': {
                display: 'flex',
            },
            '& $text': {
                '&:hover span': {
                    textDecoration: 'underline',
                    color: ' #5181E0 ',
                },
                '&:visited': {
                    color: '#1C4CAD ',
                },
            },
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
    name: {
        textOverflow: 'ellipsis',
        display: '-webkit-box',
        WebkitLineClamp: 1,
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        flexBasis: '80%',
        marginBottom: '12px',
    },
    time: {
        background: '#F2F7FF',
        borderRadius: '3px',
        fontSize: '12px',
        lineHeight: '12px',
        color: 'rgba(2,16,43,0.40)',
        display: 'flex',
        justifyContent: 'left',
        alignItems: 'center',
        gap: '4px',
        width: 'fit-content',
        padding: '2px 6px',
        marginBottom: '5px',
    },
    description: {
        margin: 'auto 0',
        display: 'flex',
        justifyContent: 'space-between',
        color: ' rgba(2,16,43,0.60)',
        // justifySelf: 'center',
    },
    descriptionText: {
        lineHeight: '12px',
        fontSize: '12px',
        whiteSpace: 'normal',
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical',
        alignItems: 'center',
        overflow: 'hidden',
        wordWrap: 'break-word',
    },
    statisticsWrapper: {
        display: 'flex',
        justifyContent: 'space-between',
        marginTop: 'auto',
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
    text: {
        'display': 'initial',
        'fontSize': '14px',
        'color': '#02102B',
        'fontWeight': 'bold',
        '&:hover': {
            textDecoration: 'underline',
            color: ' #5181E0 ',
        },
        '&:hover span': {
            textDecoration: 'underline',
            color: ' #5181E0 ',
        },
        '&:visited': {
            color: '#1C4CAD ',
        },
    },
    actions: {
        display: 'none',
        gap: '12px',
    },
    edit: {
        display: 'flex',
        justifyContent: 'center',
    },
    delete: {
        display: 'flex',
        justifyContent: 'center',
    },
})

const ProjectCard = ({ project, onEdit, onRefresh }: IProjectCardProps) => {
    const [css] = useStyletron()
    const [t] = useTranslation()
    const styles = useCardStyles()
    const { role } = useFetchProjectRole(project?.id)
    const [name, setName] = useState('')
    const [isRemoveProjectOpen, setIsRemoveProjectOpen] = useState(false)

    return (
        <div className={styles.projectCard}>
            <div className={styles.row}>
                <div className={styles.name}>
                    <TextLink className={styles.text} to={`/projects/${project.id}/evaluations`}>
                        {[project.owner?.name, project.name].join('/')}
                    </TextLink>
                </div>
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
                                height: '18px',
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
            <div className={cn(styles.time)}>
                <IconFont type='runtime' /> {project.createdTime && formatTimestampDateTime(project.createdTime)}
            </div>
            <div className={cn(styles.description, 'text-ellipsis')}>
                <StatefulTooltip
                    content={() => (
                        <p style={{ maxWidth: '300px', wordWrap: 'break-word' }}>{project.description ?? ''}</p>
                    )}
                    placement='bottom'
                >
                    <p className={cn(styles.descriptionText)}>{project.description ?? ''}</p>
                </StatefulTooltip>
            </div>
            <div className={styles.statisticsWrapper}>
                <div className={styles.statistics}>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}/evaluations`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={{
                                content: `${t('Evaluations')}:${project?.statistics.evaluationCounts}`,
                            }}
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
                            to={`/projects/${project.id}/datasets`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={{
                                content: `${t('Datasets')}:${project?.statistics.datasetCounts}`,
                            }}
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
                            to={`/projects/${project.id}/models`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={{
                                content: `${t('Models')}:${project?.statistics.modelCounts}`,
                            }}
                        >
                            <IconFont
                                type='Model'
                                size={12}
                                style={{ color: 'rgba(2,16,43,0.20)', marginRight: '4px' }}
                            />
                            <span>{project?.statistics.modelCounts}</span>
                        </IconLink>
                    </div>
                    <div className={styles.statisticsItem}>
                        <IconLink
                            to={`/projects/${project.id}/overview`}
                            style={{ backgroundColor: 'transparent', color: 'rgba(2,16,43,0.60)' }}
                            tooltip={{
                                content: `${t('Members')}:${project?.statistics.memberCounts}`,
                            }}
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
                <div className={styles.actions}>
                    <IconLink
                        to={`/projects/${project.id}/members`}
                        tooltip={{
                            content: t('Manage Member'),
                        }}
                    >
                        <IconFont type='setting' size={12} style={{ color: 'gray' }} />
                    </IconLink>
                    <WithAuth role={role} id='project.update'>
                        <StatefulTooltip content={t('edit sth', [t('Project')])} placement='top'>
                            <div className={styles.edit}>
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
                            </div>
                        </StatefulTooltip>
                    </WithAuth>
                    <WithAuth role={role} id='project.delete'>
                        <StatefulTooltip content={t('delete sth', [t('Project')])} placement='top'>
                            <div className={styles.delete}>
                                <Button
                                    icon='delete'
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
                                                'marginRight': '0',
                                                ':hover span': {
                                                    color: ' #5181E0  !important',
                                                },

                                                ':hover': {
                                                    backgroundColor: '#F0F4FF',
                                                },
                                            },
                                        },
                                    }}
                                    onClick={() => setIsRemoveProjectOpen(true)}
                                />
                            </div>
                        </StatefulTooltip>
                    </WithAuth>
                </div>
            </div>

            <Modal
                isOpen={isRemoveProjectOpen}
                onClose={() => setIsRemoveProjectOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader $style={{ display: 'flex', gap: '5px', fontWeight: 'normal' }}>
                    {t('project.remove.confirm.start')}
                    <strong>{project?.name ?? ''}</strong>
                    {t('project.remove.confirm.end')}
                </ModalHeader>
                <ModalBody>
                    <div>
                        <div
                            style={{
                                display: 'flex',
                                marginTop: '20px',
                                marginBottom: '20px',
                                gap: '12px',
                                alignItems: 'center',
                            }}
                        >
                            <Input value={name} onChange={(e) => setName(e.target.value)} />
                        </div>
                        <LabelMedium $style={{ color: ' rgba(2,16,43,0.60)', fontSize: '14px' }}>
                            <IconFont type='info' style={{ color: ' #E67F17', marginRight: '8px' }} size={14} />
                            {t(
                                'All the evaluations, datasets, models, and runtimes belong to the project will be removed.'
                            )}
                        </LabelMedium>
                    </div>
                </ModalBody>
                <ModalFooter
                    $style={{
                        ...expandMargin('30px', '30px', '30px', '30px'),
                        ...expandPadding('0', '0', '0', '0'),
                        fontSize: '16px',
                    }}
                >
                    <div style={{ display: 'grid', gap: '20px', gridTemplateColumns: '1fr 79px 79px' }}>
                        <div style={{ flexGrow: 1 }} />
                        <Button
                            size='default'
                            isFull
                            kind='secondary'
                            onClick={() => {
                                setIsRemoveProjectOpen(false)
                            }}
                        >
                            {t('Cancel')}
                        </Button>
                        <Button
                            size='default'
                            isFull
                            disabled={name !== project?.name}
                            onClick={async () => {
                                setIsRemoveProjectOpen(false)
                                await removeProject(project?.id)
                                toaster.positive(t('Remove Project Success'), { autoHideDuration: 1000 })
                                onRefresh?.()
                            }}
                        >
                            {t('Confirm')}
                        </Button>
                    </div>
                </ModalFooter>
            </Modal>
        </div>
    )
}

export default function ProjectListCard() {
    const [visit, setVisit] = useLocalStorage('projectVisit', VisitBy.Visited)
    const [page] = usePage({
        query: {
            pageNum: 1,
            pageSize: 10000,
            sort: visit,
        },
    })
    const projectsInfo = useFetchProjects(page)
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
        setData(
            items.filter((i) => {
                if (filter) return [i.name, i.owner?.name].join('/').includes(filter)
                return filter === ''
            })
        )
    }, [filter, projectsInfo.data])

    const handleRefresh = useEventCallback(async () => {
        await projectsInfo.refetch()
    })

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
                    onRefresh={handleRefresh}
                    onEdit={() => {
                        setEditProject(project)
                        setIsCreateProjectOpen(true)
                    }}
                />
            )
        })
    }, [handleRefresh, data, filter])

    return (
        <Card
            title={currentUser?.name ?? t('projects')}
            titleIcon={undefined}
            extra={
                <Button
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
            <div
                className={css({
                    marginBottom: '20px',
                    display: 'flex',
                    justifyContent: 'space-between',
                })}
            >
                <div style={{ maxWidth: '280px' }}>
                    <QueryInput
                        over
                        onChange={(val: string) => {
                            setFilter(val.trim())
                        }}
                    />
                </div>
                <div className={css({ display: 'flex', alignItems: 'center', minWidth: '100px', fontSize: '12px' })}>
                    {t('project.visit.orderby')}
                    <VisitSelector value={visit} onChange={setVisit as any} />
                </div>
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
