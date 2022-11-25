import React from 'react'
import Card from '@/components/Card'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { showTableName, tableNameOfSummary } from '@/domain/datastore/utils'
import { useQueryDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import { useProject } from '@/domain/project/hooks/useProject'
import Table from '@/components/Table'
import Editor from '@/components/Editor'
import { Panel } from 'baseui/accordion'
import Accordion from '@/components/Accordion'

const PAGE_TABLE_SIZE = 100

function Summary({ fetch }: any) {
    const record: Record<string, string> = fetch?.data?.records?.[0] ?? {}

    return (
        <div className='mb-20'>
            <Accordion accordion>
                <Panel title='Summary' expanded>
                    {fetch?.data?.records.length === 0 && (
                        <BusyPlaceholder type='notfound' style={{ minHeight: '300px' }} />
                    )}
                    <div
                        style={{
                            lineHeight: '32px',
                            fontSize: '14px',
                            gridTemplateColumns: 'minmax(160px, max-content) 1fr',
                            display: 'grid',
                        }}
                    >
                        {Object.keys(record)
                            .sort((a, b) => {
                                if (a === 'id') return -1
                                return a > b ? 1 : -1
                            })
                            .map((label) => (
                                <React.Fragment key={label}>
                                    <div
                                        style={{
                                            color: 'rgba(2,16,43,0.60)',
                                            borderBottom: '1px solid #EEF1F6',
                                        }}
                                    >
                                        {label}
                                    </div>
                                    <div
                                        style={{
                                            borderBottom: '1px solid #EEF1F6',
                                            paddingLeft: '20px',
                                        }}
                                    >
                                        {record[label]}
                                    </div>
                                </React.Fragment>
                            ))}
                    </div>
                </Panel>
            </Accordion>
        </div>
    )
}

function EvaluationViewer({ table, filter }: { table: string; filter?: Record<string, any> }) {
    const query = React.useMemo(
        () => ({
            tableName: table,
            start: 0,
            limit: PAGE_TABLE_SIZE,
            rawResult: true,
            ignoreNonExistingTable: true,
            filter,
        }),
        [table, filter]
    )

    const info = useQueryDatastore(query, true)

    const columns = React.useMemo(() => {
        return info.data?.columnTypes?.map((column) => column.name)?.sort((a) => (a === 'id' ? -1 : 1)) ?? []
    }, [info])
    const data = React.useMemo(() => {
        if (!info.data) return []

        return (
            info.data?.records?.map((item) => {
                return columns.map((k) => item?.[k])
            }) ?? []
        )
    }, [info.data, columns])

    if (info.isFetching) {
        return <BusyPlaceholder />
    }

    if (info.isError) {
        return <BusyPlaceholder type='notfound' />
    }

    if (table.includes('/summary')) return <Summary name={table} fetch={info} />

    return (
        <Card outTitle={showTableName(table)} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
            <React.Suspense fallback={<BusyPlaceholder />}>
                <Table columns={columns} data={data} />
            </React.Suspense>
        </Card>
    )
}

function EvaluationWidgetResults() {
    const { project } = useProject()

    const tables = React.useMemo(() => {
        const names = []
        if (project?.name) names.push(tableNameOfSummary(project?.name as string))

        return [...names]
    }, [project])

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(800px, 1fr))',
                    gridGap: '16px',
                }}
            >
                {tables.map((name) => {
                    return <EvaluationViewer table={name} key={name} />
                })}
            </div>
            <Editor />
        </div>
    )
}
export default EvaluationWidgetResults
