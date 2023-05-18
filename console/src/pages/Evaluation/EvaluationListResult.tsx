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

function prefixColumn(row: any, prefix: string | number) {
    return `${[row?.['sys/model_name']?.value, prefix].filter((v) => v !== undefined).join('/')}@`
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

    const queries = React.useMemo(
        () =>
            rows?.map((row, i) => {
                return {
                    tableName: tableNameOfResult(projectId, val(row.id)),
                    columnPrefix: prefixColumn(row, i),
                }
            }) ?? [],
        [rows, projectId]
    )
    const getId = useCallback(
        (row) => {
            return rows?.map((v, i) => getPrefixId(row, prefixColumn(rows[i], i))).filter((v) => !!v)[0]
        },
        [rows]
    )

    const { records, columnTypes, recordInfo } = useFetchDatastoreByTables({
        tables: queries,
    })

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
                // queryable
                title={t('evaluation.detail.title')}
                titleOfCompare={t('evaluation.detail.compare')}
                selectable
                records={records}
                columnTypes={columnTypes}
                getId={getId}
                previewable
            />
        </Card>
    )
}
