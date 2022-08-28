import React from 'react'
import Table from '@/components/Table'
import useTranslation from '@/hooks/useTranslation'
import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import { IDatasetFileSchema } from '@dataset/schemas/dataset'
import { useQueryDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { useParams } from 'react-router-dom'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import { TableBuilder, TableBuilderColumn } from 'baseui/table-semantic'
import { RecordDesc } from '@/domain/datastore/schemas/datastore'
import { RecordListVO } from '../../domain/datastore/schemas/datastore'
import { getToken } from '@/api'
import { useAuth } from '@/api/Auth'
import { tableDataLink } from '@/domain/datastore/utils'
import { getMetaRow } from '@/domain/dataset/utils'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { usePage } from '@/hooks/usePage'

export default function DatasetVersionFiles() {
    const { projectId, datasetId } = useParams<{ projectId: string; datasetId: string }>()
    const [page, setPage] = usePage()
    const { dataset: datasetVersion } = useDataset()
    const { token } = useAuth()

    const tables = useQueryDatasetList(datasetVersion?.indexTable, page)

    const datasetVersionId = React.useMemo(() => datasetVersion?.id, [datasetVersion])
    const rowCount = React.useMemo(() => {
        return getMetaRow(datasetVersion?.versionMeta as string)
    }, [datasetVersion])
    const paginationProps: IPaginationProps = React.useMemo(() => {
        return {
            start: page.pageNum,
            count: page.pageSize,
            total: rowCount,
            afterPageChange: () => {
                tables.refetch()
            },
        }
    }, [page, rowCount])

    return (
        <>
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
                            },
                        },
                    }}
                >
                    {(row) => {
                        const src = tableDataLink(projectId, datasetId as string, datasetVersionId as string, {
                            uri: row.data_uri,
                            authName: row.auth_name,
                            offset: row.data_offset,
                            size: row.data_size,
                            Authorization: token as string,
                        })
                        return <img src={src} />
                    }}
                </TableBuilderColumn>
                <TableBuilderColumn header='Name'>{(row) => row.auth_name}</TableBuilderColumn>
                <TableBuilderColumn header='Label'>{(row) => row.label}</TableBuilderColumn>
            </TableBuilder>
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
                        size={'mini'}
                        numPages={
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
        </>
    )
}
