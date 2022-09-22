import React from 'react'
import { useQueryDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { useHistory, useParams } from 'react-router-dom'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { useAuth } from '@/api/Auth'
import { getMetaRow } from '@/domain/dataset/utils'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { usePage } from '@/hooks/usePage'
import Button from '@/components/Button'
import DatasetViewer from '@/components/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { getReadableStorageQuantityStr } from '@/utils'
import IconFont from '@/components/IconFont/index'
import { createUseStyles } from 'react-jss'
import qs from 'qs'
import { DatasetObject } from '@/domain/dataset/sdk'
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
        'flexBasis': '161px',
        'width': '161px',
        'height': '137px',
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
    },
    cardImg: {
        position: 'relative',
        height: '90px',
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
})

const PAGE_TABLE_SIZE = 10
const PAGE_CARD_SIZE = 50

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
                <Tab title={<IconFont type='grid' />} />
                <Tab title={<IconFont type='view' />} />
            </Tabs>
        </div>
    )
}

export default function DatasetVersionFiles() {
    const { projectId, fileId, datasetId, datasetVersionId } = useParams<{
        projectId: string
        datasetId: string
        datasetVersionId: string
        fileId: string
    }>()
    const [page, setPage] = usePage()
    const { token } = useAuth()
    const history = useHistory()
    const styles = useCardStyles()
    const { datasetVersion } = useDatasetVersion()

    const tables = useQueryDatasetList(datasetVersion?.indexTable, page, true)

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

    const columnTypes = React.useMemo(() => {
        return tables.data?.columnTypes ?? {}
    }, [tables.data])

    const [layoutKey, setLayoutKey] = React.useState('1')
    const [isFullscreen, setIsFullscreen] = React.useState(false)

    const datasets = React.useMemo(
        () =>
            tables?.data?.records?.map((record) => {
                const dObj = new DatasetObject(record, columnTypes)
                dObj.setDataSrc(
                    projectId,
                    datasetVersion?.name as string,
                    datasetVersion?.versionName as string,
                    token as string
                )
                return dObj ?? []
            }) ?? [],
        [tables?.data, columnTypes, projectId, datasetVersion, token]
    )

    const Records = React.useMemo(() => {
        if (fileId || !tables.data) return <></>
        const { summary = {} } = datasets?.[0] ?? {}

        const rowAction = [
            {
                label: 'data',
                overrides: {
                    TableBodyCell: {
                        style: {
                            verticalAlign: 'middle',
                            paddingTop: '4px',
                            paddingBottom: '4px',
                            position: 'relative',
                            height: '60px',
                        },
                    },
                },
                renderItem: (row: any) => {
                    return (
                        <Button
                            as='link'
                            onClick={() => {
                                setIsFullscreen(false)
                                history.push(
                                    `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files/${
                                        row.id
                                    }?${qs.stringify(page)}`
                                )
                            }}
                        >
                            <DatasetViewer data={row} />
                        </Button>
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
                renderItem: (row: any) => row?.summary?.[key],
            })),
        ]

        if (layoutKey === '0') {
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
                                {/* <div className={styles.cardLabel}>label: {rowAction.label(row)}</div> */}
                                <div className={styles.cardSize}>{rowAction[1].renderItem(row)}</div>
                                <div
                                    className={styles.cardFullscreen}
                                    role='button'
                                    tabIndex={0}
                                    onClick={() => {
                                        setIsFullscreen(true)
                                        history.push(
                                            `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files/${
                                                row.id
                                            }?${qs.stringify(page)}`
                                        )
                                    }}
                                >
                                    <IconFont type='fullscreen' />
                                </div>
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
    }, [layoutKey, fileId, tables.data, datasets, styles, datasetVersionId, history, projectId, datasetId, page])

    return (
        <div className={styles.wrapper}>
            <div className={styles.icon}>
                <LayoutControl
                    value={layoutKey}
                    onChange={(key) => {
                        setLayoutKey(key)
                        const newSize = key === '1' ? PAGE_TABLE_SIZE : PAGE_CARD_SIZE

                        history.push(
                            `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files/?${qs.stringify(
                                {
                                    pageNum: Math.floor((page.pageSize * page.pageNum) / newSize),
                                    pageSize: newSize,
                                }
                            )}`
                        )
                    }}
                />
            </div>
            {Records}
            {!fileId && paginationProps && (
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
            {fileId && <DatasetVersionFilePreview datasets={datasets} fileId={fileId} fullscreen={isFullscreen} />}
        </div>
    )
}
