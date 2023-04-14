import React from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { useAuth } from '@/api/Auth'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { usePage } from '@/hooks/usePage'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import DatasetViewer from '@starwhale/ui/Viewer/DatasetViewer'
import IconFont from '@starwhale/ui/IconFont/index'
import { createUseStyles } from 'react-jss'
import qs from 'qs'
import { ArtifactType, isAnnotationHiddenInTable, parseDataSrc } from '@starwhale/core/dataset'
import { useSearchParam } from 'react-use'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { SpaceTabs, Tab } from '@starwhale/ui/Tab'
import { StyledTab } from 'baseui/tabs'
import { StatefulTooltip } from 'baseui/tooltip'
import { useDatasets } from '@starwhale/core/dataset/hooks/useDatasets'
import Preview from '@starwhale/ui/Dataset/Preview'
import { getMeta } from '@/domain/dataset/utils'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'

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
const HAS_TABLE_CONTROL = false
enum LAYOUT {
    GRID = '1',
    LIST = '0',
}

function TabOverride({ children, ...rest }: any) {
    return (
        <StatefulTooltip placement='top' content={rest.id === LAYOUT.GRID ? 'Grid View' : 'List View'} showArrow>
            <StyledTab {...rest}>
                <div>{children}</div>
            </StyledTab>
        </StatefulTooltip>
    )
}

function LayoutControl({ value, onChange = () => {} }: { value: string; onChange: (str: string) => void }) {
    return (
        <div
            style={{
                padding: '0px',
            }}
        >
            <SpaceTabs
                onChange={({ activeKey: activeKeyNew }) => {
                    onChange(activeKeyNew as string)
                }}
                activeKey={value}
                overrides={{
                    Tab: {
                        component: TabOverride,
                    },
                }}
            >
                <Tab title={<IconFont type='grid' />} key={LAYOUT.GRID} />
                <Tab title={<IconFont type='view' />} key={LAYOUT.LIST} />
            </SpaceTabs>
        </div>
    )
}

export default function DatasetVersionFiles() {
    const { projectId, datasetId, datasetVersionId } = useParams<{
        projectId: string
        datasetId: string
        datasetVersionId: string
    }>()
    const [, theme] = themedUseStyletron()
    // @FIXME layoutParam missing when build
    const layoutParam = useSearchParam('layout') as string
    const [layoutKey, setLayoutKey] = React.useState(layoutParam ?? '0')
    const [page, setPage] = usePage()
    const { token } = useAuth()
    const history = useHistory()
    const styles = useCardStyles()
    const { datasetVersion } = useDatasetVersion()
    const { revision, rows: rowCount } = React.useMemo(() => {
        return getMeta(datasetVersion?.versionMeta as string)
    }, [datasetVersion?.versionMeta])

    const [preview, setPreview] = React.useState('')
    const [previewKey, setPreviewKey] = React.useState('')
    const { query } = useQueryArgs()

    const $page = React.useMemo(() => {
        return {
            ...page,
            layout: layoutKey,
            filter: query.filter,
            revision,
        }
    }, [page, layoutKey, query.filter, revision])

    React.useEffect(() => {
        setLayoutKey(layoutParam ?? '0')
    }, [layoutParam])

    const { records, columnTypes } = useFetchDatastoreByTable(datasetVersion?.indexTable, $page, true)

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

    const options = React.useMemo(
        () => ({
            parseLink: parseDataSrc(
                projectId,
                datasetVersion?.name as string,
                datasetVersion?.versionName as string,
                token as string
            ),
            showPrivate: false,
            showLink: false,
        }),
        [projectId, datasetVersion, token]
    )

    const { records: datasets } = useDatasets(records, columnTypes, options)

    const Records = React.useMemo(() => {
        if (!datasets?.[0]) return <></>
        const { summary } = datasets?.[0]
        const rowAction: any[] = []
        summary.forEach((value, key) => {
            if (isAnnotationHiddenInTable(value)) return

            rowAction.push({
                label: key,
                overrides: {
                    TableHeadCell: {
                        style: {
                            backgroundColor: theme.brandTableHeaderBackground,
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

                    switch (row?.summary?.get('_extendtype')) {
                        case ArtifactType.Image:
                            wrapperStyle = {
                                minWidth: '90px',
                                height: '90px',
                                textAlign: 'center',
                            }
                            break
                        case ArtifactType.Audio:
                            wrapperStyle = { height: '90px', maxWidth: '100%', width: '200px' }
                            break
                        case ArtifactType.Video:
                            wrapperStyle = { maxWidth: '300px' }
                            break
                        default:
                        case ArtifactType.Text:
                            wrapperStyle = { minHeight: '60px', maxWidth: '400px' }
                            break
                    }

                    return (
                        <div className={styles.tableCell} style={wrapperStyle}>
                            <DatasetViewer dataset={row} showKey={key} />
                            <div
                                className={styles.cardFullscreen}
                                role='button'
                                tabIndex={0}
                                onClick={() => {
                                    setIsFullscreen(true)
                                    setPreview(row)
                                    setPreviewKey(key)
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
            })
        })

        rowAction.sort((ca, cb) => {
            if (ca.label === 'id') return -1
            if (cb.label === 'id') return 1
            return ca.label.localeCompare(cb.label)
        })
        // if (layoutKey === LAYOUT.GRID) {
        //     return (
        //         <div
        //             style={{
        //                 display: 'grid',
        //                 gap: '9px',
        //                 gridTemplateColumns: 'repeat(auto-fit, minmax(161px, 1fr))',
        //                 placeItems: 'center',
        //             }}
        //         >
        //             {datasets.map((row, index) => {
        //                 return (
        //                     <div className={styles.card} key={index}>
        //                         <div className={styles.cardImg}>{rowAction[0].renderItem(row)}</div>
        //                         <div className={styles.cardSize}>{rowAction[1].renderItem(row)}</div>
        //                     </div>
        //                 )
        //             })}
        //         </div>
        //     )
        // }

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
                            backgroundColor: theme.brandTableHeaderBackground,
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
    }, [layoutKey, datasets, styles, datasetVersionId, history, projectId, datasetId, $page, theme])

    return (
        <div className={styles.wrapper}>
            {HAS_TABLE_CONTROL && (
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
                                        pageNum: Math.max(
                                            Math.floor((page.pageSize * (page.pageNum - 1)) / newSize),
                                            1
                                        ),
                                        pageSize: newSize,
                                        layout: key,
                                    }
                                )}`
                            )
                        }}
                    />
                </div>
            )}
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
                <Preview
                    preview={preview}
                    previewKey={previewKey}
                    isFullscreen={isFullscreen}
                    setIsFullscreen={setIsFullscreen}
                />
            )}
        </div>
    )
}
