import React, { useCallback } from 'react'
import Card from '@/components/Card'
import { useParams } from 'react-router-dom'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import { useEvaluationDetailStore } from '@starwhale/ui/base/data-table/store'
import { tableNameOfResult } from '@starwhale/core/datastore/utils'
import { useProject } from '@project/hooks/useProject'
import useFetchDatastoreByTables from '@starwhale/core/datastore/hooks/useFetchDatastoreByTables'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { val } from '@starwhale/ui/GridTable/utils'

function prefixColumn(row: any, prefix: string | number) {
    return `${[row?.['sys/model_name']?.value, prefix].filter((v) => v !== undefined).join('-')}-`
}

function getPrefixId(row: any, prefix: string | number) {
    const key = `${prefix}id`
    return _.get(row, [key, 'value'], _.get(row, key, '')) as string
}

export default function DatastoreDiffTables({ rows }) {
    const { expandedWidth, expanded } = useDrawer()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const queries = React.useMemo(
        () =>
            rows.map((row, i) => {
                return {
                    tableName: tableNameOfResult(projectId, val(row.id)),
                    columnPrefix: prefixColumn(row, i),
                }
            }),
        [rows]
    )
    const getId = useCallback(
        (row) => {
            return getPrefixId(row, prefixColumn(rows[0], 0))
        },
        [rows]
    )

    // console.log(queries, rows, prefixColumn(rows[0], 0))

    const { records, columnTypes, recordInfo } = useFetchDatastoreByTables({
        tables: queries,
    })

    return (
        <Card
            style={{
                marginRight: expanded ? expandedWidth : '0',
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
                queryable
                selectable
                records={records}
                columnTypes={columnTypes}
                getId={getId}
            />
        </Card>
    )
}
