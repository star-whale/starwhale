import React from 'react'
import { useQueryDatasetList } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { useHistory, useParams } from 'react-router-dom'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { useAuth } from '@/api/Auth'
import { getMetaRow } from '@/domain/dataset/utils'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { usePage } from '@/hooks/usePage'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import DatasetViewer from '@/components/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { getReadableStorageQuantityStr } from '@/utils'
import IconFont from '@/components/IconFont/index'
import { createUseStyles } from 'react-jss'
import qs from 'qs'
import { DatasetObject, parseDataSrc, TYPES } from '@/domain/dataset/sdk'
import { useSearchParam } from 'react-use'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import DatasetVersionFilePreview from './DatasetVersionOverviewFilePreview'

const useCardStyles = createUseStyles({
    wrapper: {
        flex: 1,
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
    },
    icon: {
        position: 'absolute',
        top: '-60px',
        right: 0,
    },
    card: {
        'width': '100%',
        'height': '100%',
        'border': '1px solid #E2E7F0',
        'display': 'flex',
        'flexDirection': 'column',
        'justifyContent': 'space-between',
        'borderRadius': '4px',
        'position': 'relative',
        '&:hover': {
            boxShadow: '0 2px 8px 0 rgba(0,0,0,0.20);',
        },
        '&:hover $cardFullscreen': {
            display: 'grid',
        },
        'overflow': 'hidden',
    },
    cardImg: {
        'minHeight': '90px',
        '& > div': {
            margin: 'auto',
        },
    },
    cardLabel: {
        padding: '9px 9px 0px',
    },
    cardSize: {
        padding: '0 9px 9px',
        color: 'rgba(2, 16, 43, 0.4)',
    },
    cardFullscreen: {
        'position': 'absolute',
        'right': '5px',
        'top': '5px',
        'backgroundColor': 'rgba(2,16,43,0.40)',
        'height': '20px',
        'width': '20px',
        'borderRadius': '2px',
        'display': 'none',
        'color': '#FFF',
        'cursor': 'pointer',
        'placeItems': 'center',
        '&:hover': {
            backgroundColor: '#5181E0',
        },
    },
    tableCell: {
        'position': 'relative',
        'textAlign': 'left',
        '&:hover $cardFullscreen': {
            display: 'grid',
        },
    },
})

const PAGE_TABLE_SIZE = 10
const PAGE_CARD_SIZE = 50

enum LAYOUT {
    GRID = '1',
    LIST = '0',
}

function LayoutControl({ value, onChange = () => {} }: { value: string; onChange: (str: string) => void }) {
    return (
        <div
            style={{
                padding: '0px',
            }}
        >
            <Tabs
                overrides={{
                    TabBar: {
                        style: {
                            display: 'flex',
                            gap: '0',
                            paddingLeft: 0,
                            paddingRight: 0,
                            borderRadius: '4px',
                        },
                    },
                    TabContent: {
                        style: {
                            paddingLeft: 0,
                            paddingRight: 0,
                            borderRadius: '4px',
                        },
                    },
                    Tab: {
                        style: ({ $active }) => ({
                            flex: 1,
                            textAlign: 'center',
                            border: $active ? '1px solid #2B65D9' : '1px solid #CFD7E6',
                            color: $active ? ' #2B65D9' : 'rgba(2,16,43,0.60)',
                            marginLeft: '0',
                            marginRight: '0',
                            paddingTop: '6px',
                            paddingBottom: '9px',
                            height: '32px',
                            width: '40px',
                        }),
                    },
                }}
                onChange={({ activeKey: activeKeyNew }) => {
                    onChange(activeKeyNew as string)
                }}
                activeKey={value}
            >
                <Tab title={<IconFont type='grid' />} key={LAYOUT.GRID} />
                <Tab title={<IconFont type='view' />} key={LAYOUT.LIST} />
            </Tabs>
        </div>
    )
}

export default function DatasetVersionFiles() {
    const { projectId, datasetId, datasetVersionId } = useParams<{
        projectId: string
        datasetId: string
        datasetVersionId: string
    }>()
    // @FIXME layoutParam missing when build
    const layoutParam = useSearchParam('layout') as string
    const [layoutKey, setLayoutKey] = React.useState(layoutParam ?? '0')
    const [page, setPage] = usePage()
    const { token } = useAuth()
    const history = useHistory()
    const styles = useCardStyles()
    const { datasetVersion } = useDatasetVersion()

    const [preview, setPreview] = React.useState('')
    const { query } = useQueryArgs()

    const $page = React.useMemo(() => {
        return {
            ...page,
            layout: layoutKey,
            filter: query.filter,
        }
    }, [page, layoutKey, query.filter])

    React.useEffect(() => {
        setLayoutKey(layoutParam ?? '0')
    }, [layoutParam])

    const tables = useQueryDatasetList(datasetVersion?.indexTable, $page, true)

    const rowCount = React.useMemo(() => {
        return getMetaRow(datasetVersion?.versionMeta as string)
    }, [datasetVersion])

    const paginationProps: IPaginationProps = React.useMemo(() => {
        return {
            start: page.pageNum,
            count: page.pageSize,
            total: rowCount,
            afterPageChange: () => {},
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, rowCount])

    const [isFullscreen, setIsFullscreen] = React.useState(true)

    const datasets = React.useMemo(
        () =>
            tables?.data?.records?.map((record) => {
                const dObj = new DatasetObject(
                    record,
                    parseDataSrc(
                        projectId,
                        datasetVersion?.name as string,
                        datasetVersion?.versionName as string,
                        token as string
                    )
                )
                return dObj ?? []
            }) ?? [],
        [tables?.data, projectId, datasetVersion, token]
    )

    const Records = React.useMemo(() => {
        const { summary = {} } = datasets?.[0] ?? {}

        const rowAction = [
            {
                label: 'data',
                overrides: {
                    TableHeadCell: {
                        style: {
                            backgroundColor: 'var(--color-brandTableHeaderBackground)',
                            borderBottomWidth: '0',
                            fontWeight: 'bold',
                            fontSize: '14px',
                            lineHeight: '14px',
                        },
                    },
                    TableBodyCell: {
                        style: {
                            verticalAlign: 'middle',
                            paddingTop: '4px',
                            paddingBottom: '4px',
                            position: 'relative',
                        },
                    },
                },
                renderItem: (row: any) => {
                    let wrapperStyle = {}

                    switch (row.data._type) {
                        case TYPES.IMAGE:
                            wrapperStyle = {
                                minWidth: '90px',
                                height: '90px',
                                textAlign: 'center',
                            }
                            break
                        case TYPES.AUDIO:
                            wrapperStyle = { height: '90px', maxWidth: '100%', width: '200px' }
                            break
                        case TYPES.VIDEO:
                            wrapperStyle = { maxWidth: '300px' }
                            break
                        default:
                        case TYPES.TEXT:
                            wrapperStyle = { minHeight: '60px', maxWidth: '400px' }
                            break
                    }

                    return (
                        <div className={styles.tableCell} style={wrapperStyle}>
                            <DatasetViewer dataset={row} />
                            <div
                                className={styles.cardFullscreen}
                                role='button'
                                tabIndex={0}
                                onClick={() => {
                                    setIsFullscreen(true)
                                    setPreview(row.id)
                                    history.push(
                                        `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files?${qs.stringify(
                                            {
                                                ...$page,
                                                layout: layoutKey,
                                            }
                                        )}`
                                    )
                                }}
                            >
                                <IconFont type='fullscreen' />
                            </div>
                        </div>
                    )
                },
            },
            {
                label: 'size',
                renderItem: (row: any) =>
                    Number.isNaN(Number(row.size)) ? '-' : getReadableStorageQuantityStr(Number(row.size)),
            },
            ...Object.entries(summary).map(([key]) => ({
                label: key,
                renderItem: (row: any) => {
                    return <div className={styles.tableCell}>{row?.summary?.[key]}</div>
                },
            })),
        ]

        if (layoutKey === LAYOUT.GRID) {
            return (
                <div
                    style={{
                        display: 'grid',
                        gap: '9px',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(161px, 1fr))',
                        placeItems: 'center',
                    }}
                >
                    {datasets.map((row, index) => {
                        return (
                            <div className={styles.card} key={index}>
                                <div className={styles.cardImg}>{rowAction[0].renderItem(row)}</div>
                                <div className={styles.cardSize}>{rowAction[1].renderItem(row)}</div>
                            </div>
                        )
                    })}
                </div>
            )
        }

        return (
            <TableBuilder
                data={datasets}
                overrides={{
                    Root: { style: { maxHeight: '50vh' } },
                    TableBodyRow: {
                        style: {
                            cursor: 'pointer',
                        },
                    },
                    TableHeadCell: {
                        style: {
                            backgroundColor: 'var(--color-brandTableHeaderBackground)',
                            fontWeight: 'bold',
                            borderBottomWidth: 0,
                            fontSize: '14px',
                            lineHeight: '16px',
                            paddingTop: '15px',
                            paddingBottom: '15px',
                            paddingLeft: '20px',
                            paddingRight: '20px',
                        },
                    },
                    TableHeadRow: {
                        style: {
                            borderRadius: '4px',
                        },
                    },
                    TableBodyCell: {
                        style: {
                            paddingTop: 0,
                            paddingBottom: 0,
                            paddingLeft: '20px',
                            paddingRight: '20px',
                            lineHeight: '44px',
                            verticalAlign: 'middle',
                            textAlign: 'left',
                        },
                    },
                    // ...overrides,
                }}
            >
                {rowAction.map((row) => {
                    return (
                        // @ts-ignore
                        <TableBuilderColumn key={row.label} header={row.label} overrides={row?.overrides}>
                            {row.renderItem}
                        </TableBuilderColumn>
                    )
                })}
            </TableBuilder>
        )
    }, [layoutKey, datasets, styles, datasetVersionId, history, projectId, datasetId, $page])

    return (
        <div className={styles.wrapper}>
            <div className={styles.icon}>
                <LayoutControl
                    value={layoutKey}
                    onChange={(key) => {
                        const newSize = key === LAYOUT.LIST ? PAGE_TABLE_SIZE : PAGE_CARD_SIZE
                        setLayoutKey(key)
                        history.push(
                            `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files/?${qs.stringify(
                                {
                                    ...$page,
                                    pageNum: Math.max(Math.floor((page.pageSize * (page.pageNum - 1)) / newSize), 1),
                                    pageSize: newSize,
                                    layout: key,
                                }
                            )}`
                        )
                    }}
                />
            </div>
            {Records}
            {paginationProps && (
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        marginTop: 20,
                    }}
                >
                    <div
                        style={{
                            flexGrow: 1,
                        }}
                    />
                    <Pagination
                        size='mini'
                        numPages={
                            // esline-disable-next-line react/jsx-curly-brace-presence
                            paginationProps.total && paginationProps.count
                                ? Math.ceil(paginationProps.total / Math.max(paginationProps.count, 1))
                                : 0
                        }
                        currentPage={paginationProps.start ?? 1}
                        onPageChange={({ nextPage }) => {
                            if (paginationProps.onPageChange) {
                                paginationProps.onPageChange(nextPage)
                            }
                            if (paginationProps.afterPageChange) {
                                setPage({
                                    ...page,
                                    pageNum: nextPage,
                                })
                                paginationProps.afterPageChange(nextPage)
                            }
                        }}
                    />
                </div>
            )}
            {preview && (
                <DatasetVersionFilePreview
                    datasets={datasets}
                    fileId={preview}
                    isFullscreen={isFullscreen}
                    setIsFullscreen={setIsFullscreen}
                />
            )}
        </div>
    )
}
