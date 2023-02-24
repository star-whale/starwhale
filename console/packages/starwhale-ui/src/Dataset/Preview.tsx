import _ from 'lodash'
import React from 'react'
import Button from '@starwhale/ui/Button'
import DatasetViewer from '@starwhale/ui/Viewer/DatasetViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import { RAW_COLORS } from '@starwhale/ui/Viewer/utils'
import { LabelMedium } from 'baseui/typography'
import Checkbox from '@starwhale/ui/Checkbox'
import { JSONTree } from 'react-json-tree'
import { useDatasetTableAnnotations } from '@starwhale/core/dataset'

export const theme = {
    scheme: 'bright',
    author: 'chris kempson (http://chriskempson.com)',
    base00: '#000000',
    base01: '#303030',
    base02: '#505050',
    base03: '#b0b0b0',
    base04: '#d0d0d0',
    base05: '#e0e0e0',
    base06: '#f5f5f5',
    base07: '#ffffff',
    base08: '#fb0120',
    base09: '#fc6d24',
    base0A: '#fda331',
    base0B: '#a1c659',
    base0C: '#76c7b7',
    base0D: '#6fb3d2',
    base0E: '#d381c3',
    base0F: '#be643c',
}
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
        overflow: 'auto',
    },
    annotation: { display: 'flex', gap: '20px', flexDirection: 'column', marginBottom: '20px' },
    annotationTypes: {
        lineHeight: '24px',
        borderRadius: '4px',
        color: 'rgba(2,16,43,0.60)',
        display: 'flex',
        gap: '20px',
    },
    annotationList: { display: 'flex', gap: '20px', flexDirection: 'column' },
    annotationItem: {
        height: '32px',
        lineHeight: '32px',
        color: 'rgba(2,16,43,0.40)',
        display: 'flex',
        borderBottom: '1px solid #EEF1F6',
        paddingLeft: '8px',
        gap: '8px',
        alignItems: 'center',
    },
    annotationItemShow: {
        marginLeft: 'auto',
    },
    annotationItemColor: {
        width: '10px',
        height: '10px',
    },
})

export default function Preview({
    preview,
    previewKey,
    isFullscreen = false,
    setIsFullscreen = () => {},
}: {
    preview: any
    previewKey: string
    isFullscreen?: boolean
    setIsFullscreen?: any
}) {
    const styles = useStyles()
    const [activeKey, setActiveKey] = React.useState('0')
    const [hiddenLabels, setHiddenLabels] = React.useState<Set<number>>(new Set())
    const isSimpleView = React.useMemo(() => !_.isObject(preview.summary.get(previewKey)), [preview, previewKey])

    const Panel = React.useMemo(() => {
        return (
            // eslint-disable-next-line
            <TabControl
                isSimpleView={isSimpleView}
                value={activeKey}
                onChange={setActiveKey}
                data={preview}
                hiddenLabels={hiddenLabels}
                setHiddenLabels={setHiddenLabels}
            />
        )
    }, [preview, activeKey, setHiddenLabels, hiddenLabels, isSimpleView])

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

                        <DatasetViewer dataset={preview} showKey={previewKey} isZoom hiddenLabels={hiddenLabels} />
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
    isSimpleView,
}: {
    value: string
    onChange: (str: string) => void
    hiddenLabels: Set<any>
    setHiddenLabels: (ids: Set<any>) => void
    data: any
    isSimpleView: boolean
}) {
    const { record } = data
    const { annotationTypes, annotationTypeMap } = useDatasetTableAnnotations(data)
    const [hiddenTypes, setHiddenTypes] = React.useState<Set<string>>(new Set())
    const styles = useStyles()
    const count = React.useMemo(() => {
        return Array.from(annotationTypeMap)
            .map(([, list]) => list)
            .reduce((acc, cur) => acc + cur.length, 0)
    }, [annotationTypeMap])
    const $isSimpleView = React.useMemo(() => isSimpleView || count === 0, [count, isSimpleView])
    const $activeKey = React.useMemo(() => ($isSimpleView ? '1' : value), [$isSimpleView, value])

    const Anno = React.useMemo(() => {
        return Array.from(annotationTypeMap).map(([type, list]) => {
            if (hiddenTypes.has(type)) return <span key={type} />

            const allIds = annotationTypeMap.get(type)
            const hiddenIds = allIds.filter((id: number) => hiddenLabels.has(id))
            const otherIds = _.without([...hiddenLabels], ...allIds)
            const isAllHidden = hiddenIds.length === allIds.length

            return (
                <div key={type} className={styles.annotationList}>
                    <div className={styles.annotationItem} style={{ color: ' rgba(2,16,43,0.60)', marginTop: '20px' }}>
                        {type}({list.length})
                        <div className={styles.annotationItemShow}>
                            <Button
                                as='transparent'
                                onClick={() =>
                                    setHiddenLabels(isAllHidden ? new Set(otherIds) : new Set([...allIds, ...otherIds]))
                                }
                            >
                                {isAllHidden ? <IconFont type='eye_off' /> : <IconFont type='eye' />}
                            </Button>
                        </div>
                    </div>
                    {list.map((path: string, index: number) => {
                        return (
                            <div className={styles.annotationItem} key={index}>
                                <div
                                    className={styles.annotationItemColor}
                                    style={{
                                        backgroundColor: RAW_COLORS[index % RAW_COLORS.length],
                                    }}
                                />
                                {path}
                                <div className={styles.annotationItemShow}>
                                    <Button
                                        as='transparent'
                                        onClick={() => {
                                            if (hiddenLabels.has(path)) {
                                                hiddenLabels.delete(path)
                                            } else {
                                                hiddenLabels.add(path)
                                            }
                                            setHiddenLabels(new Set(hiddenLabels))
                                        }}
                                    >
                                        {hiddenLabels.has(path) ? <IconFont type='eye_off' /> : <IconFont type='eye' />}
                                    </Button>
                                </div>
                            </div>
                        )
                    })}
                </div>
            )
        })
    }, [hiddenLabels, setHiddenLabels, annotationTypeMap, styles, hiddenTypes])

    return (
        <div>
            {!$isSimpleView && (
                <div className={styles.annotation}>
                    <LabelMedium>Annotation Type</LabelMedium>
                    <div className={styles.annotationTypes}>
                        {Array.from(annotationTypes).map((type) => {
                            return (
                                <Checkbox
                                    key={type}
                                    checked={!hiddenTypes.has(type)}
                                    onChange={(e) => {
                                        const { checked } = e.target
                                        if (!checked) {
                                            setHiddenTypes((v) => new Set([...v, type]))
                                        } else {
                                            setHiddenTypes((v) => {
                                                const newV = new Set(v)
                                                newV.delete(type)
                                                return newV
                                            })
                                        }
                                    }}
                                >
                                    {type}
                                </Checkbox>
                            )
                        })}
                    </div>
                </div>
            )}
            {/* @ts-ignore */}
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
                onChange={({ activeKey }) => onChange?.(activeKey as string)}
                activeKey={$activeKey}
            >
                {/* @ts-ignore */}
                {!$isSimpleView && (
                    <Tab title={`Annotation(${count})`}>
                        <div>{Anno}</div>
                    </Tab>
                )}
                <Tab title='Meta'>
                    <div>
                        <JSONTree data={record} theme={theme} hideRoot shouldExpandNode={() => false} />
                    </div>
                </Tab>
            </Tabs>
        </div>
    )
}
