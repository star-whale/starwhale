import _ from 'lodash'
import React from 'react'
import Button from '@starwhale/ui/Button'
import DatasetViewer from '@starwhale/ui/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import { DatasetObject } from '../../domain/dataset/sdk'
import { RAW_COLORS } from '../../../packages/starwhale-ui/src/Viewer/utils'

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
        marginTop: 0,
        marginBottom: 0,
        border: '1px solid #CFD7E6',
    },
    layoutFullscreen: {
        position: 'fixed',
        background: '#fff',
        left: 0,
        right: 0,
        bottom: 0,
        top: 0,
        zIndex: 20,
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
    preview,
    fileId,
    isFullscreen = false,
    setIsFullscreen = () => {},
}: {
    datasets: DatasetObject[]
    preview: any
    fileId: string
    isFullscreen?: boolean
    setIsFullscreen?: any
}) {
    const data: DatasetObject | undefined = React.useMemo(() => {
        const row = datasets?.find((v) => v.id === fileId)
        if (!row) return undefined
        return row
    }, [datasets, fileId])
    const previewData = preview
    const styles = useStyles()
    const [activeKey, setActiveKey] = React.useState('0')
    const [hiddenLabels, setHiddenLabels] = React.useState<Set<number>>(new Set())

    console.log(previewData)

    const Panel = React.useMemo(() => {
        if (previewData && previewData?.cocos?.length > 0) {
            return (
                // eslint-disable-next-line @typescript-eslint/no-use-before-define
                <TabControl
                    value={activeKey}
                    onChange={setActiveKey}
                    data={previewData}
                    hiddenLabels={hiddenLabels}
                    setHiddenLabels={setHiddenLabels}
                />
            )
        }
        if (!data?.summary || Object.keys(data?.summary).length === 0) return undefined

        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        return <Summary data={data?.summary ?? {}} />
    }, [previewData, data, activeKey, setHiddenLabels, hiddenLabels])

    if (!isFullscreen) return <></>

    return (
        <Modal
            isOpen={isFullscreen}
            onClose={() => setIsFullscreen(false)}
            closeable
            animate
            autoFocus
            overrides={{
                Dialog: {
                    style: {
                        width: '90vw',
                        maxWidth: '1200px',
                        maxHeight: '640px',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <ModalHeader />
            <ModalBody
                style={{ display: 'flex', gap: '30px', flex: 1, overflow: 'auto' }}
                className={styles.layoutNormal}
            >
                <div className={styles.wrapper}>
                    {Panel && <div className={styles.panel}>{Panel}</div>}
                    <div className={styles.card}>
                        {!isFullscreen && (
                            <div
                                role='button'
                                tabIndex={0}
                                className={styles.cardFullscreen}
                                onClick={() => setIsFullscreen((v: boolean) => !v)}
                            >
                                <IconFont type='fullscreen' />
                            </div>
                        )}

                        <DatasetViewer dataset={previewData} isZoom hiddenLabels={hiddenLabels} />
                    </div>
                </div>
            </ModalBody>
            <ModalFooter />
        </Modal>
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
                    {data?.cocos?.map((coco, index) => {
                        return (
                            <div className={styles.cocoAnnotation} key={index}>
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
            <Tab title={`Categories(${data?.cocoCats.length})`}>
                <div>
                    {data?.cocoCats.map((v) => {
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
            {Object.entries(data)
                .filter(([, value]) => !_.isObject(value))
                .map(([key, value]) => {
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
