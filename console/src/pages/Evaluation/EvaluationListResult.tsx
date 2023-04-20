import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import { useParams } from 'react-router-dom'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import { ITableState, useEvaluationDetailStore, useEvaluationCompareStore } from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { tableNameOfResult } from '@starwhale/core/datastore/utils'
import { BusyPlaceholder } from '@starwhale/ui'
import { useProject } from '@project/hooks/useProject'
import useFetchDatastoreByTables from '@starwhale/core/datastore/hooks/useFetchDatastoreByTables'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { val } from '@starwhale/ui/GridTable/utils'

function prefixColumn(row: any, prefix: string | number) {
    return `${[row?.['sys/model_name']?.value, prefix].filter((v) => v !== undefined).join('-')}-`
}

function getPrefixAttr(row: any, prefix: string | number, attr: string) {
    const key = `${prefixColumn(row, prefix)}${attr}`
    return _.get(row, [key, 'value'], _.get(row, key, '')) as string
}

function getPrefixId(row: any, prefix: string | number) {
    const key = `${prefix}id`
    return _.get(row, [key, 'value'], _.get(row, key, '')) as string
}

export default function DatastoreDiffTables({ rows }) {
    const { expandedWidth, expanded } = useDrawer()
    const [t] = useTranslation()
    const { projectId: projectFromUri } = useParams<{ projectId: string }>()
    const { project } = useProject()
    const projectId = project?.id || projectFromUri

    const store = useEvaluationDetailStore()

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
    const getId = useCallback((row) => getPrefixId(row, prefixColumn(rows[0], 0)), [rows])

    const { records, columnTypes } = useFetchDatastoreByTables({
        tables: queries,
    })
    const evaluationViewConfig = useFetchViewConfig(projectId, 'evaluation-detail')

    const $ready = evaluationViewConfig.isSuccess

    // NOTICE: use isinit to make sure view config is loading into store
    const initRef = React.useRef(false)
    React.useEffect(() => {
        if (!evaluationViewConfig.isSuccess) return
        if (initRef.current) return

        let $rawConfig
        try {
            $rawConfig = JSON.parse(evaluationViewConfig.data?.content, undefined) ?? {}
        } catch (e) {
            // console.log(e)
        }
        // eslint-disable-next-line no-console
        console.log('init store')
        store.initStore($rawConfig)
        initRef.current = true
        // store should not be used as a deps, it's will trigger cycle render
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [evaluationViewConfig.isSuccess, evaluationViewConfig.data?.content])

    React.useEffect(() => {
        const unsub = useEvaluationCompareStore.subscribe(
            (state: ITableState) => state.rowSelectedIds,
            (state: any[]) => store.onSelectMany(state)
        )
        return unsub
    }, [store])

    if (!$ready)
        return (
            <Card
                title={t('Evaluations')}
                style={{
                    marginRight: expanded ? expandedWidth : '0',
                    flexShrink: 1,
                    marginBottom: 0,
                    width: '100%',
                    flex: 1,
                }}
            >
                <BusyPlaceholder />
            </Card>
        )

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
                store={useEvaluationDetailStore}
                queryable
                selectable
                records={records}
                columnTypes={columnTypes}
                getId={getId}
            />
        </Card>
    )
}
