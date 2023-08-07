import React from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '@/api/Auth'
import { usePage } from '@/hooks/usePage'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import { createUseStyles } from 'react-jss'
import { parseDataSrc } from '@starwhale/core/dataset'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import { getMeta } from '@/domain/dataset/utils'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { ITableState, useDatasetStore } from '@starwhale/ui/GridTable/store'
import shallow from 'zustand/shallow'

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

const selector = (s: ITableState) => ({
    currentView: s.currentView,
})

export default function DatasetVersionFiles() {
    const { projectId } = useParams<{
        projectId: string
    }>()
    const [page] = usePage()
    const { token } = useAuth()
    const styles = useCardStyles()
    const { datasetVersion } = useDatasetVersion()
    const { revision } = React.useMemo(() => {
        return getMeta(datasetVersion?.versionInfo.meta as string)
    }, [datasetVersion?.versionInfo.meta])
    const { query } = useQueryArgs()
    const extra = React.useMemo(() => {
        return {
            revision,
        }
    }, [revision])

    const { currentView } = useDatasetStore(selector, shallow)

    const {
        page: tablePage,
        setPage,
        getQueryParams,
    } = useDatastorePage({
        pageNum: page.pageNum,
        pageSize: 50,
        queries: query.filter,
        sortBy: currentView?.sortBy || 'id',
        sortDirection: currentView?.sortDirection || 'DESC',
    })

    const { records, columnTypes } = useFetchDatastoreByTable(
        getQueryParams(datasetVersion?.versionInfo.indexTable, extra),
        !!datasetVersion?.versionInfo.indexTable
    )

    const options = React.useMemo(
        () => ({
            parseLink: parseDataSrc(
                projectId,
                datasetVersion?.name as string,
                datasetVersion?.versionInfo.name as string,
                token as string
            ),
            showPrivate: false,
            showLink: false,
        }),
        [projectId, datasetVersion, token]
    )
    const $columns = useDatastoreColumns(columnTypes as any, options)

    return (
        <div className={styles.wrapper}>
            <GridCombineTable
                compareable
                columnable
                // viewable
                // queryable
                fillable
                selectable
                previewable
                records={records}
                columnTypes={columnTypes}
                columns={$columns}
                paginationable
                page={tablePage}
                onPageChange={setPage}
                rowHeight={80}
            />
        </div>
    )
}
