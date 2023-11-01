import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { StyledTable, StyledHead, StyledHeadCell, StyledBody, StyledRow, StyledCell } from 'baseui/table'
import { ExtendButton, IconFont, Input, FormSelect } from '@starwhale/ui'
import { IJobEventSchema } from '@/domain/job/schemas/job'
import { expandBorder } from '../../../packages/starwhale-ui/src/utils/index'
import { formatTimestampDateTime } from '@/utils/datetime'
import { useFullscreen, useToggle } from 'react-use'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

export interface ITaskEventListCardProps {
    sources: Record<string, IJobEventSchema[]>
}

export default function TaskEventListCard({ sources }: ITaskEventListCardProps) {
    const [t] = useTranslation()
    const [expandId, setExpandId] = React.useState<number | undefined>(undefined)
    const [current, setCurrent] = React.useState<string | undefined>(undefined)
    const ref = React.useRef<HTMLDivElement>(null)

    const [css] = themedUseStyletron()
    const cls = css({
        'borderRadius': '12px',
        'padding': '3px 10px',
        'fontSize': '12px',
        'lineHeight': '12px',
        'width': 'max-content',
        ':before': {
            content: '"\u2022"',
            display: 'inline-block',
            marginRight: '4px',
        },
    })

    const EVENT_LEVEL = {
        INFO: (
            <p className={cls} style={{ color: '#2B65D9', backgroundColor: '#EBF1FF' }}>
                {t('job.event.level.info')}
            </p>
        ),
        ERROR: (
            <p className={cls} style={{ color: '#CC3D3D', backgroundColor: '#F3EDFF' }}>
                {t('job.event.level.error')}
            </p>
        ),
        WARNING: (
            <p className={cls} style={{ color: '#E67F17', backgroundColor: '#FFF3E8' }}>
                {t('job.event.level.warning')}
            </p>
        ),
    }
    const $current = current ?? Object.keys(sources)[0]

    const list = sources[$current] ?? []

    const attrs = [
        {
            key: 'timestamp',
            label: t('Created'),
            render: (str) => str && formatTimestampDateTime(str),
            headerStyle: {
                flex: '1.3',
            },
        },
        {
            key: 'eventType',
            label: t('job.event.level'),
            render: (str) => EVENT_LEVEL[str ?? 'INFO'],
            headerStyle: {
                flex: '0.7',
            },
        },
        {
            key: 'source',
            label: t('job.event.source'),
            render: (str) => str,
            headerStyle: {
                flex: '0.5',
            },
        },
        {
            key: 'message',
            label: t('job.event.message'),
            render: (str) => str,
            headerStyle: {
                flex: '3',
            },
        },
        {
            key: 'data',
            label: t('job.event.description'),
            render: (str) => str,
            headerStyle: {
                flex: '2',
            },
        },
    ]

    const attrHeader = attrs.map((attr) => {
        return (
            <StyledHeadCell
                key={attr.key}
                $style={{
                    ...expandBorder('0px'),
                    paddingLeft: '32px',
                    backgroundColor: '#F3F5F9',
                    ...(attr.headerStyle ?? {}),
                }}
            >
                {attr.label}
            </StyledHeadCell>
        )
    })

    const attrRow = list.map((item) => {
        const isExpand = expandId === item.id

        return (
            <StyledRow
                key={item.id}
                $style={{
                    ...expandBorder('0px'),
                    backgroundColor: isExpand ? '#EBF1FF' : '',
                }}
                onClick={() => {
                    if (expandId === item.id) {
                        setExpandId(undefined)
                    } else {
                        setExpandId(item.id)
                    }
                }}
            >
                {attrs.map((attr, index) => {
                    return (
                        <StyledCell
                            key={attr.key}
                            $style={{
                                ...expandBorder('0px'),
                                paddingLeft: '32px',
                                paddingTop: '15px',
                                paddingBottom: '15px',
                                position: 'relative',
                                overflow: 'hidden',
                                ...(isExpand
                                    ? {
                                          height: 'auto',
                                      }
                                    : {
                                          height: '44px',
                                          whiteSpace: 'nowrap',
                                          textOverflow: 'ellipsis',
                                          display: 'block',
                                      }),
                                ...(attr.headerStyle ?? {}),
                            }}
                        >
                            {index === 0 && (
                                <div className='absolute left-[10px] cursor-pointer'>
                                    {isExpand ? (
                                        <IconFont type='arrow2' kind='gray' />
                                    ) : (
                                        <IconFont type='play' kind='gray' />
                                    )}
                                </div>
                            )}
                            {attr.render(item[attr.key])}
                        </StyledCell>
                    )
                })}
            </StyledRow>
        )
    })

    const [show, toggle] = useToggle(false)
    useFullscreen(ref, show, {
        onClose: () => {
            toggle(false)
        },
    })

    const options = Object.keys(sources).map((key) => {
        return {
            label: key,
            id: key,
        }
    })

    return (
        <div ref={ref} className='task-event-list overflow-hidden h-full'>
            <div className='grid gap-20px grid-cols-[280px_1fr_16px] mb-20px'>
                <FormSelect options={options} onChange={setCurrent as any} value={$current} />
                <div className='flex-1'>
                    <Input />
                </div>
                {/* <ExtendButton iconnormal nopadding icon='download' tooltip={t('download')} /> */}
                <ExtendButton
                    iconnormal
                    nopadding
                    icon='fullscreen'
                    tooltip={t('fullscreen')}
                    onClick={() => toggle(true)}
                />
            </div>
            <StyledTable
                $style={{
                    ...expandBorder('0px'),
                    overflow: 'hidden',
                }}
            >
                <StyledHead
                    $style={{
                        boxShadow: 'none',
                    }}
                >
                    {attrHeader}
                </StyledHead>
                <StyledBody
                    $style={{
                        overflow: 'auto',
                    }}
                >
                    {attrRow}
                </StyledBody>
            </StyledTable>
        </div>
    )
}
