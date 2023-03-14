import React from 'react'
import Card from '@/components/Card'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { showTableName, tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { useQueryDatastore } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import Table from '@/components/Table'
import { Panel, StatelessAccordion } from 'baseui/accordion'
import { QueryTableRequest } from '@starwhale/core/datastore'
import { FullTablesEditor } from '@/components/Editor/FullTablesEditor'
import { useParams } from 'react-router-dom'
import { Button, IconFont } from '@starwhale/ui'
import { useFetchPanelSetting } from '@/domain/panel/hooks/useSettings'
import { useJob } from '@/domain/job/hooks/useJob'
import { updatePanelSetting } from '@/domain/panel/services/panel'
import { toaster } from 'baseui/toast'

const PAGE_TABLE_SIZE = 100

function Summary({ fetch }: any) {
    const record: Record<string, string> = fetch?.data?.records?.[0]
    const [expanded, setExpanded] = React.useState<boolean>(false)

    return (
        <div className='mb-20'>
            <StatelessAccordion
                accordion
                expanded={['summary']}
                overrides={{
                    PanelContainer: {
                        style: {
                            borderTop: '1px solid #CFD7E6',
                            borderLeft: '1px solid #CFD7E6',
                            borderRight: '1px solid #CFD7E6',
                            borderRadius: '3px',
                            borderBottomColor: '#CFD7E6',
                            display: 'flex',
                            flexDirection: 'column',
                        },
                    },
                    Header: {
                        style: {
                            backgroundColor: '#F7F8FA',
                            paddingTop: '0px',
                            paddingBottom: '0px',
                            fontSize: '14px',
                        },
                    },
                    Content: {
                        style: {
                            flex: 1,
                            display: 'flex',
                            flexDirection: 'column',
                            paddingTop: '20px',
                            paddingBottom: '20px',
                            paddingLeft: '20px',
                            paddingRight: '20px',
                        },
                    },
                    Root: {
                        style: {
                            flex: 1,
                            display: 'flex',
                            fontSize: '14px',
                        },
                    },
                    ToggleIcon: {
                        component: () => <span />,
                    },
                }}
            >
                <Panel
                    title={
                        <Button
                            onClick={() => setExpanded(!expanded)}
                            as='transparent'
                            overrides={{
                                BaseButton: {
                                    style: {
                                        flex: 1,
                                        justifyContent: 'flex-start',
                                    },
                                },
                            }}
                            isFull
                        >
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px',
                                    fontWeight: 'bold',
                                    color: 'rgba(2,16,43,0.60)',
                                }}
                            >
                                Summary
                                <IconFont type={!expanded ? 'arrow_down' : 'arrow_top'} />
                            </div>
                        </Button>
                    }
                    key='summary'
                >
                    {!record && <BusyPlaceholder type='notfound' style={{ height: '148px', minHeight: 'auto' }} />}
                    <div
                        style={{
                            lineHeight: '32px',
                            fontSize: '14px',
                            gridTemplateColumns: 'minmax(160px, max-content) 1fr',
                            display: 'grid',
                            maxHeight: expanded ? undefined : '248px',
                            overflow: 'auto',
                        }}
                    >
                        {record &&
                            Object.keys(record)
                                .sort((a, b) => {
                                    if (a === 'id') return -1
                                    return a > b ? 1 : -1
                                })
                                .filter((label) => typeof record[label] !== 'object')
                                .map((label) => {
                                    return (
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
                                    )
                                })}
                    </div>
                </Panel>
            </StatelessAccordion>
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

    const info = useQueryDatastore(query as QueryTableRequest)

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

    // if (info.isFetching) {
    //     return <BusyPlaceholder />
    // }

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
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const { job } = useJob()
    const storeKey = job?.modelName ? ['evaluation-model', job?.modelName].join('-') : ''
    const settingInfo = useFetchPanelSetting(projectId, storeKey)
    const onStateChange = async (data: any) => {
        await updatePanelSetting(projectId, storeKey, data)
        toaster.positive('Panel setting saved', { autoHideDuration: 2000 })
    }

    const tables = React.useMemo(() => {
        const names = []
        if (projectId) names.push(tableNameOfSummary(projectId))

        return [...names]
    }, [projectId])

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
                    let filter
                    if (name.includes('/summary') && jobId)
                        filter = {
                            operator: 'EQUAL',
                            operands: [
                                {
                                    intValue: jobId,
                                },
                                {
                                    columnName: 'sys/id',
                                },
                            ],
                        }
                    return <EvaluationViewer table={name} key={name} filter={filter} />
                })}
            </div>
            <FullTablesEditor initialState={settingInfo.data} onStateChange={onStateChange} />
        </div>
    )
}
export default EvaluationWidgetResults
