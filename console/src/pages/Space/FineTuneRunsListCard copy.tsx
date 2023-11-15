import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { StyledTable, StyledHead, StyledHeadCell, StyledBody, StyledRow, StyledCell } from 'baseui/table'
import {
    ExtendButton,
    IconFont,
    FormSelect,
    Search,
    createBuilder,
    FilterDatetime,
    FilterStrinWithContains,
    Operators,
} from '@starwhale/ui'
import { expandBorder } from '../../../packages/starwhale-ui/src/utils/index'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { useFullscreen, useToggle } from 'react-use'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { CSVLink } from 'react-csv'
import { IFineTuneVo, api } from '@/api'
import { useHistory, useParams } from 'react-router-dom'
import { usePage } from '@/hooks/usePage'
import User from '@/domain/user/components/User'

type QueryT = {
    property: string
    op: string
    value: string
}

export default function FineTuneRunsListCard() {
    const [t] = useTranslation()
    const [expandId, setExpandId] = React.useState<number | undefined>(undefined)
    const [current, setCurrent] = React.useState<string | undefined>(undefined)
    const ref = React.useRef<HTMLDivElement>(null)
    const [page] = usePage()
    const history = useHistory()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const info = api.useListFineTune(projectId, spaceId, {
        ...page,
    })
    const sources = info?.data?.list ?? []

    const [queries, setQueries] = React.useState<QueryT[]>([])

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

    const $listRaw = sources

    const $listFiltered = React.useMemo(() => {
        const set = new Set($listRaw.map((_, idx) => idx))
        Array.from(queries || new Set(), (f) => f).forEach(({ property, op, value }: any) => {
            // @ts-ignore
            const filterFn = Operators[op]?.buildFilter({ value })
            Array.from(set).forEach((idx) => {
                if (filterFn && !filterFn($listRaw[idx][property])) {
                    set.delete(idx)
                }
            })
        })

        return [...set].map((idx) => $listRaw[idx])
    }, [$listRaw, queries])

    const fields = ['id', 'timestamp', 'Created at', 'End at', 'duration']

    const attrs = [
        {
            key: 'id',
            label: t('Runs ID'),
            render: (data: IFineTuneVo) => data.id,
            getFilters: () =>
                createBuilder({
                    fields,
                    list: $listRaw,
                    key: 'id',
                })(FilterDatetime),
        },
        {
            key: 'owner',
            label: t('Owner'),
            render: ({ job }) => job.owner && <User user={job.owner} />,
            getFilters: () =>
                createBuilder({
                    fields,
                    list: $listRaw,
                    key: 'timestamp',
                })(FilterDatetime),
        },
        {
            key: 'Created at',
            label: t('Created at'),
            render: ({ job }) => job?.createdTime && job?.createdTime > 0 && formatTimestampDateTime(job?.createdTime),
            getFilters: () =>
                createBuilder({
                    fields,
                    list: $listRaw,
                    key: 'timestamp',
                })(FilterDatetime),
        },
        {
            key: 'End At',
            label: t('End at'),
            render: ({ job }) => job?.stopTime && job?.stopTime > 0 && formatTimestampDateTime(job?.stopTime),
            getFilters: () =>
                createBuilder({
                    fields,
                    list: $listRaw,
                    key: 'timestamp',
                })(FilterDatetime),
        },

        {
            key: 'duration',
            label: t('Duration'),
            render: ({ job }) => durationToStr(job.duration),
            getFilters: () =>
                createBuilder({
                    fields,
                    list: $listRaw,
                    key: 'timestamp',
                })(FilterDatetime),
        },
        // {
        //     key: 'eventType',
        //     label: t('job.event.level'),
        //     render: (str) => EVENT_LEVEL[str ?? 'INFO'],
        //     headerStyle: {
        //         flex: '0.7',
        //     },
        //     getFilters: () =>
        //         createBuilder({
        //             fields,
        //             list: $listRaw,
        //             key: 'eventType',
        //         })(FilterStrinWithContains),
        // },
    ]

    const attrHeader = attrs.map((attr) => {
        return (
            <StyledHeadCell
                key={attr.key}
                $style={{
                    ...expandBorder('0px'),
                    paddingLeft: '32px',
                    backgroundColor: '#F3F5F9',
                    ...(attr?.headerStyle ?? {}),
                }}
            >
                {attr.label}
            </StyledHeadCell>
        )
    })

    const attrRow = $listFiltered.map((item) => {
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
                            {attr.render(item)}
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

    return (
        <div ref={ref} className='task-event-list overflow-hidden h-full'>
            <div className='grid gap-20px grid-cols-[280px_1fr_16px_16px] mb-20px'>
                <div className='flex-1'>
                    <Search
                        value={queries}
                        getFilters={(key) => (attrs.find((v) => v.key === key) || attrs[0])?.getFilters()}
                        onChange={setQueries as any}
                    />
                </div>
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
