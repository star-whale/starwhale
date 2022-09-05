import React from 'react'
import { useDataset } from '@dataset/hooks/useDataset'
import { useQueryDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { useHistory, useParams } from 'react-router-dom'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { useAuth } from '@/api/Auth'
import { tableDataLink } from '@/domain/datastore/utils'
import { getMetaRow } from '@/domain/dataset/utils'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { usePage } from '@/hooks/usePage'
import Button from '@/components/Button'
import DatasetViewer from '@/components/Viewer/DatasetViewer'
import ImageViewer from '@/components/Viewer/ImageViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { getReadableStorageQuantityStr } from '@/utils'
import Typer from '@/domain/datastore/sdk'

export default function DatasetVersionFiles() {
    const { projectId, fileId } = useParams<{ projectId: string; datasetId: string; fileId: string }>()
    const [page, setPage] = usePage()
    const { dataset: datasetVersion } = useDataset()
    const { token } = useAuth()
    const history = useHistory()

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

    const preview: any = React.useMemo(() => {
        const row = tables.data?.records?.find((v) => v.id === fileId)
        if (!row) return
        const src = tableDataLink(projectId, datasetVersion?.name as string, datasetVersion?.versionName as string, {
            uri: row.data_uri,
            authName: row.auth_name,
            offset: Typer[columnTypes.data_offset]?.encode(row.data_offset),
            size: Typer[columnTypes.data_size]?.encode(row.data_size),
            Authorization: token as string,
        })
        // eslint-disable-next-line consistent-return
        return {
            ...row,
            src,
        }
    }, [tables, datasetVersion, projectId, token, fileId, columnTypes])

    const [activeKey, setActiveKey] = React.useState('1')

    return (
        <div style={{ flex: 1, position: 'relative' }}>
            {!preview && (
                <TableBuilder
                    data={tables.data?.records ?? []}
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
                    <TableBuilderColumn
                        header='Data'
                        overrides={{
                            TableBodyCell: {
                                style: {
                                    verticalAlign: 'middle',
                                    paddingTop: '4px',
                                    paddingBottom: '4px',
                                },
                            },
                        }}
                    >
                        {(row) => {
                            const src = tableDataLink(
                                projectId,
                                datasetVersion?.name as string,
                                datasetVersion?.versionName as string,
                                {
                                    uri: row.data_uri,
                                    authName: row.auth_name,
                                    offset: Typer[columnTypes.data_offset]?.encode(row.data_offset),
                                    size: Typer[columnTypes.data_size]?.encode(row.data_size),
                                    Authorization: token as string,
                                }
                            )

                            return (
                                <div style={{ display: 'flex' }}>
                                    <Button as='link' onClick={() => history.push(`files/${row.id}`)}>
                                        <DatasetViewer
                                            data={{
                                                type: row?.data_mime_type,
                                                label: row?.label,
                                                name: row?.auth_name,
                                                src,
                                            }}
                                        />
                                    </Button>
                                </div>
                            )
                        }}
                    </TableBuilderColumn>
                    <TableBuilderColumn header='Label'>{(row) => row.label}</TableBuilderColumn>
                    <TableBuilderColumn header='Size'>
                        {(row) => getReadableStorageQuantityStr(row.data_size)}
                    </TableBuilderColumn>
                    <TableBuilderColumn header='Name'>{(row) => row.auth_name}</TableBuilderColumn>
                </TableBuilder>
            )}
            {!preview && paginationProps && (
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
                <div style={{ minHeight: '500px', borderRadius: '4px', border: '1px solid #E2E7F0', display: 'flex' }}>
                    <div
                        style={{
                            flexBasis: '320px',
                            padding: '20px',
                            borderRight: '1px solid #EEF1F6',
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
                                        paddingTop: '9px',
                                        paddingBottom: '9px',
                                    }),
                                },
                            }}
                            onChange={({ activeKey: activeKeyNew }) => {
                                setActiveKey(activeKeyNew as string)
                            }}
                            activeKey={activeKey}
                        >
                            <Tab title='Annotation'>
                                <div>
                                    <div
                                        style={{
                                            borderBottom: '1px solid #EEF1F6',
                                        }}
                                    >
                                        label: {preview?.label ?? '-'}
                                    </div>
                                </div>
                            </Tab>
                            <Tab title='Categories'>
                                <div>
                                    <div
                                        style={{
                                            borderBottom: '1px solid #EEF1F6',
                                        }}
                                    >
                                        label: {preview?.label ?? '-'}
                                    </div>
                                </div>
                            </Tab>
                        </Tabs>
                    </div>
                    <div
                        style={{
                            flex: 1,
                        }}
                    >
                        <ImageViewer data={preview} isZoom />
                    </div>
                </div>
            )}
        </div>
    )
}
