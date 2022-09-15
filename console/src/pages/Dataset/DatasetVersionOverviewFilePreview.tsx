import React from 'react'
import Button from '@/components/Button'
import DatasetViewer from '@/components/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { createUseStyles } from 'react-jss'
import { headerHeight } from '@/consts'
import useTranslation from '../../hooks/useTranslation'
import IconFont from '../../components/IconFont/index'
import { DatasetObject } from '../../domain/dataset/sdk'
import { RAW_COLORS } from '../../components/Viewer/utils'

const useStyles = createUseStyles({
    cardImg: {
        'position': 'relative',
        'flex': 1,
        'display': 'grid',
        'placeContent': 'center',
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
        display: 'flex',
        flexDirection: 'column',
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
        borderBottom: '1px solid #E2E7F0',
    },
    wrapper: {
        minHeight: '500px',
        height: '100%',
        borderRadius: '4px',
        // border: '1px solid #E2E7F0',
        display: 'flex',
    },
    card: {
        'position': 'relative',
        'flex': 1,
        'display': 'grid',
        'placeContent': 'center',
        '&:hover $cardFullscreen': {
            display: 'grid',
        },
    },
    panel: {
        flexBasis: '320px',
        padding: '20px',
        borderRight: '1px solid #EEF1F6',
    },
    summary: { display: 'flex', gap: '12px' },
    summaryLabel: {
        lineHeight: '24px',
        borderRadius: '4px',
        color: 'rgba(2,16,43,0.60)',
    },
    summaryValue: {},
    cocoAnnotation: {
        height: '32px',
        lineHeight: '32px',
        color: 'rgba(2,16,43,0.40)',
        display: 'flex',
        borderBottom: '1px solid #EEF1F6',
        paddingLeft: '8px',
        gap: '8px',
        alignItems: 'center',
    },
    cocoAnnotationShow: {
        marginLeft: 'auto',
    },
    cocoAnnotationColor: {
        width: '10px',
        height: '10px',
    },
})

export default function DatasetVersionFilePreview({
    datasets,
    fileId,
    fullscreen = false,
}: {
    datasets: DatasetObject[]
    fileId: string
    fullscreen?: boolean
}) {
    const data: any = React.useMemo(() => {
        const row = datasets?.find((v) => v.id === fileId)
        if (!row) return undefined
        return row
    }, [datasets, fileId])

    const styles = useStyles()
    const [t] = useTranslation()
    const [isFullscreen, setIsFullscreen] = React.useState(fullscreen)
    const [activeKey, setActiveKey] = React.useState('0')
    const [hiddenLabels, setHiddenLabels] = React.useState<Set<number>>(new Set())

    const Panel = React.useMemo(() => {
        if (data?.cocos.length > 0) {
            return (
                // eslint-disable-next-line @typescript-eslint/no-use-before-define
                <TabControl
                    value={activeKey}
                    onChange={setActiveKey}
                    data={data}
                    hiddenLabels={hiddenLabels}
                    setHiddenLabels={setHiddenLabels}
                />
            )
        }

        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        return <Summary data={data?.summary ?? {}} />
    }, [data, activeKey, setHiddenLabels, hiddenLabels])

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
            <div className={styles.wrapper}>
                <div className={styles.panel}>{Panel}</div>
                <div className={styles.card}>
                    <div
                        role='button'
                        tabIndex={0}
                        className={styles.cardFullscreen}
                        onClick={() => setIsFullscreen((v) => !v)}
                    >
                        <IconFont type='fullscreen' />
                    </div>
                    <DatasetViewer data={data} isZoom hiddenLabels={hiddenLabels} />
                </div>
            </div>
        </div>
    )
}

function TabControl({
    value,
    onChange = () => {},
    hiddenLabels,
    setHiddenLabels,
    data,
}: {
    value: string
    onChange: (str: string) => void
    hiddenLabels: Set<number>
    setHiddenLabels: (ids: Set<number>) => void
    data: DatasetObject
}) {
    const styles = useStyles()
    const allIds = React.useMemo(() => {
        return new Set(data?.cocos?.map((coco) => coco.id) ?? [])
    }, [data])

    return (
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
                        fontSize: '14px',
                        lineHeight: '14px',
                    }),
                },
            }}
            onChange={({ activeKey }) => {
                onChange?.(activeKey as string)
            }}
            activeKey={value}
        >
            <Tab title={`Annotation(${data?.cocos.length})`}>
                <div>
                    <div className={styles.cocoAnnotation} style={{ color: ' rgba(2,16,43,0.60)', marginTop: '20px' }}>
                        MAPPEDBOX({data?.cocos?.length})
                        <div className={styles.cocoAnnotationShow}>
                            <Button
                                as='transparent'
                                onClick={() =>
                                    setHiddenLabels(hiddenLabels.size !== allIds.size ? new Set(allIds) : new Set())
                                }
                            >
                                {hiddenLabels.size === allIds.size ? (
                                    <IconFont type='eye_off' />
                                ) : (
                                    <IconFont type='eye' />
                                )}
                            </Button>
                        </div>
                    </div>
                    {data?.cocos?.map((coco) => {
                        return (
                            <div className={styles.cocoAnnotation} key={coco.id}>
                                <div
                                    className={styles.cocoAnnotationColor}
                                    style={{
                                        backgroundColor: RAW_COLORS[coco.id % RAW_COLORS.length],
                                    }}
                                />
                                {coco.id}
                                <div className={styles.cocoAnnotationShow}>
                                    <Button
                                        as='transparent'
                                        onClick={() => {
                                            if (hiddenLabels.has(coco.id)) {
                                                hiddenLabels.delete(coco.id)
                                            } else {
                                                hiddenLabels.add(coco.id)
                                            }
                                            setHiddenLabels(new Set(hiddenLabels))
                                        }}
                                    >
                                        {hiddenLabels.has(coco.id) ? (
                                            <IconFont type='eye_off' />
                                        ) : (
                                            <IconFont type='eye' />
                                        )}
                                    </Button>
                                </div>
                            </div>
                        )
                    })}
                </div>
            </Tab>
            <Tab title={`Categories(${data?.getCOCOCategories().length})`}>
                <div>
                    {data?.getCOCOCategories().map((v) => {
                        return (
                            <div key={v} className={styles.cocoAnnotation}>
                                {v}
                            </div>
                        )
                    })}
                </div>
            </Tab>
        </Tabs>
    )
}
function Summary({ data }: { data: Record<string, any> }) {
    const styles = useStyles()

    return (
        <div>
            {Object.entries(data).map(([key, value]) => {
                return (
                    <div className={styles.summary} key={key}>
                        <span className={styles.summaryLabel}>{key}</span>
                        <span className={styles.summaryValue}>{value}</span>
                    </div>
                )
            })}
        </div>
    )
}
