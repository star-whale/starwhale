import React, { useCallback } from 'react'
import Card from '@/components/Card'
import { useParams } from 'react-router-dom'
import _ from 'lodash'
import { tableNameOfResult } from '@starwhale/core/datastore/utils'
import { useProject } from '@project/hooks/useProject'
import useFetchDatastoreByTables from '@starwhale/core/datastore/hooks/useFetchDatastoreByTables'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { val } from '@starwhale/ui/GridTable/utils'
import { ITableProps } from '@starwhale/ui/GridTable/types'
import { useEvaluationDetailStore } from '@starwhale/ui/GridTable/store'
import useTranslation from '@/hooks/useTranslation'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useEventCallback } from '@starwhale/core/utils'

function getPrefixFromRecord(row: any) {
    return `${[row?.['sys/model_name']?.value, row?.['sys/id']?.value].filter((v) => v !== undefined).join('-')}@`
}

function getPrefixId(row: any, prefix: string | number) {
    const key = `${prefix}id`
    return _.get(row, [key, 'value'], _.get(row, key, '')) as string
}

export default function DatastoreDiffTables({ rows }: { rows: ITableProps['records'] }) {
    const [t] = useTranslation()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const tables = React.useMemo(
        () =>
            rows?.map((row) => {
                return {
                    tableName: tableNameOfResult(projectId, val(row.id)),
                    columnPrefix: getPrefixFromRecord(row),
                }
            }) ?? [],
        [rows, projectId]
    )

    const getId = useCallback(
        (row) => {
            return rows?.map((v, i) => getPrefixId(row, getPrefixFromRecord(rows[i]))).filter(Boolean)?.[0]
        },
        [rows]
    )
    const { page, setPage, getScanParams } = useDatastorePage({
        pageNum: 1,
        pageSize: 50,
    })

    const { lastKey, records, columnTypes, recordInfo, columnHints } = useFetchDatastoreByTables(getScanParams(tables))
    const handlePageChange = useEventCallback((tmp: any) => setPage(tmp, lastKey))

    return (
        <Card
            style={{
                flexShrink: 1,
                marginBottom: 0,
                width: '100%',
                flex: 1,
            }}
        >
            <GridCombineTable
                isLoading={recordInfo.isLoading}
                store={useEvaluationDetailStore}
                columnable
                compareable
                title={t('evaluation.detail.title')}
                titleOfCompare={t('evaluation.detail.compare')}
                selectable
                fillable
                records={records}
                columnTypes={columnTypes}
                columnHints={columnHints}
                getId={getId}
                previewable
                paginationable
                page={page}
                onPageChange={handlePageChange}
            />
        </Card>
    )
}
