import React from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useAuth } from '@/api/Auth'
import { usePage } from '@/hooks/usePage'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import IconFont from '@starwhale/ui/IconFont/index'
import { createUseStyles } from 'react-jss'
import { parseDataSrc } from '@starwhale/core/dataset'
import { useSearchParam } from 'react-use'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { SpaceTabs, Tab } from '@starwhale/ui/Tab'
import { StyledTab } from 'baseui/tabs'
import { StatefulTooltip } from 'baseui/tooltip'
import { getMeta } from '@/domain/dataset/utils'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'

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
    const $columns = useDatastoreColumns(columnTypes as any, options)

    return (
        <div className={styles.wrapper}>
            <GridCombineTable
                compareable
                columnable
                // viewable
                // queryable
                selectable
                previewable
                records={records}
                columnTypes={columnTypes}
                columns={$columns}
            />
        </div>
    )
}
