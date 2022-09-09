import React from 'react'
import { useDataset } from '@dataset/hooks/useDataset'
import { useQueryDatasetList } from '@/domain/datastore/hooks/useFetchDatastore'
import { useParams } from 'react-router-dom'
import { useAuth } from '@/api/Auth'
import { tableDataLink } from '@/domain/datastore/utils'
import Button from '@/components/Button'
import DatasetViewer from '@/components/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import Typer from '@/domain/datastore/sdk'
import { createUseStyles } from 'react-jss'
import { headerHeight } from '@/consts'
import useTranslation from '../../hooks/useTranslation'
import IconFont from '../../components/IconFont/index'

const useCardStyles = createUseStyles({
    cardImg: {
        'position': 'relative',
        'flex': 1,
        '&:hover $cardFullscreen': {
            display: 'grid',
        },
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
        'zIndex': '5',
        '&:hover': {
            backgroundColor: '#5181E0',
        },
    },
    layoutNormal: {
        flex: 1,
        position: 'relative',
    },
    layoutFullscreen: {
        position: 'fixed',
        background: '#fff',
        left: 0,
        right: 0,
        bottom: 0,
        top: headerHeight,
    },
    exitFullscreen: {
        height: '56px',
        paddingLeft: '24px',
        display: 'flex',
        alignItems: 'center',
    },
})

export default function DatasetVersionFilePreview({
    api,
    fileId,
    fullscreen = false,
}: {
    api: ReturnType<typeof useQueryDatasetList>
    fileId: string
    fullscreen?: boolean
}) {
    const { projectId } = useParams<{
        projectId: string
        datasetId: string
        datasetVersionId: string
    }>()
    const { dataset: datasetVersion } = useDataset()
    const { token } = useAuth()

    const columnTypes = React.useMemo(() => {
        return api.data?.columnTypes ?? {}
    }, [api.data])

    const preview: any = React.useMemo(() => {
        const row = api.data?.records?.find((v) => v.id === fileId)
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
    }, [api, datasetVersion, projectId, token, fileId, columnTypes])

    const [activeKey, setActiveKey] = React.useState('1')
    const styles = useCardStyles()
    const [t] = useTranslation()
    const [isFullscreen, setIsFullscreen] = React.useState(fullscreen)

    return (
        <div className={isFullscreen ? styles.layoutFullscreen : styles.layoutNormal}>
            {isFullscreen && (
                <div className={styles.exitFullscreen}>
                    <Button
                        as='link'
                        startEnhancer={() => <IconFont type='close' />}
                        onClick={() => setIsFullscreen(false)}
                    >
                        {t('Exit Fullscreen')}
                    </Button>
                </div>
            )}
            <div
                style={{
                    minHeight: '500px',
                    height: '100%',
                    borderRadius: '4px',
                    border: '1px solid #E2E7F0',
                    display: 'flex',
                }}
            >
                {/* eslint-disable-next-line @typescript-eslint/no-use-before-define */}
                <TabControl value={activeKey} onChange={setActiveKey} preview={preview} />
                <div className={styles.cardImg}>
                    <div
                        role='button'
                        tabIndex={0}
                        className={styles.cardFullscreen}
                        onClick={() => setIsFullscreen((v) => !v)}
                    >
                        <IconFont type='fullscreen' />
                    </div>
                    <DatasetViewer
                        data={{
                            type: preview?.data_mime_type,
                            label: preview?.label,
                            name: preview?.auth_name,
                            src: preview?.src,
                        }}
                        isZoom
                    />
                </div>
            </div>
        </div>
    )
}

function TabControl({
    value,
    onChange = () => {},
    preview,
}: {
    value: string
    onChange: (str: string) => void
    preview: any
}) {
    return (
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
                    onChange?.(activeKeyNew as string)
                }}
                activeKey={value}
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
    )
}
