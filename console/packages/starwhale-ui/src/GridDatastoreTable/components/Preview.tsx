import _, { toUpper } from 'lodash'
import React from 'react'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import DataViewer from '@starwhale/ui/Viewer/DataViewer'
import { Tabs, Tab } from 'baseui/tabs'
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'baseui/modal'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import { RAW_COLORS } from '@starwhale/ui/Viewer/utils'
import { LabelMedium } from 'baseui/typography'
import Checkbox from '@starwhale/ui/Checkbox'
import { useDatasetTableAnnotations } from '@starwhale/core/dataset'
import JSONView from '@starwhale/ui/JSONView'
import { useHover } from 'react-use'
import { RecordAttr } from '../recordAttrModel'

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
        borderTop: '1px solid #eef1f6',
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
        flex: 1,
    },
    card: {
        'position': 'relative',
        'flex': 1,
        'display': 'flex',
        'placeContent': 'center',
        'overflow': 'auto',
        '&:hover $cardFullscreen': {
            display: 'grid',
        },
        'padding': '20px',
        '& > pre': {
            borderRadius: '4px',
            width: '100%',
        },
    },
    panel: {
        flexBasis: '370px',
        padding: '20px 20px 20px 30px',
        borderRight: '1px solid #EEF1F6',
        overflow: 'auto',
        flexShrink: 0,
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
    prev,
    next,
    current: preview,
    previewKey,
    isFullscreen = false,
    setIsFullscreen = () => {},
    onPreviewNext,
    onPreviewPrev,
}: {
    current?: RecordAttr
    prev: any
    next: any
    previewKey: string
    isFullscreen?: boolean
    setIsFullscreen?: any
    onPreviewNext?: any
    onPreviewPrev?: any
}) {
    const styles = useStyles()
    const [activeKey, setActiveKey] = React.useState('0')
    const [hiddenLabels, setHiddenLabels] = React.useState<Set<number>>(new Set())
    const isSimpleView = React.useMemo(() => {
        if (!preview?.summary?.get(previewKey)) return false
        return !_.isObject(preview?.summary?.get(previewKey))
    }, [preview, previewKey])

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

    const hoverable = useHover((hovered) => {
        return (
            <ModalBody
                style={{ display: 'flex', gap: '30px', flex: 1, overflow: 'auto', marginLeft: 0, marginRight: 0 }}
                className={styles.layoutNormal}
            >
                {hovered && prev && (
                    <div className='prev-row absolute z-10 left-4px bg-[rgba(2,16,43,0.30)] hover:bg-[rgba(2,16,43,0.60)] w-26px h-80px rounded-4px top-1/2 -mt-37px flex justify-stretch items-stretch'>
                        <ExtendButton isFull as='transparent' onClick={onPreviewPrev}>
                            <IconFont type='arrow_left' kind='white' />
                        </ExtendButton>
                    </div>
                )}
                {hovered && next && (
                    <div className='prev-row absolute z-10 right-4px bg-[rgba(2,16,43,0.30)] hover:bg-[rgba(2,16,43,0.60)] w-26px h-80px rounded-4px top-1/2 -mt-37px flex justify-stretch items-stretch'>
                        <ExtendButton isFull as='transparent' onClick={onPreviewNext}>
                            <IconFont type='arrow_right' kind='white' />
                        </ExtendButton>
                    </div>
                )}
                <div className={styles.wrapper}>
                    {Panel && <div className={[styles.panel, 'content-full flex-grow-0'].join(' ')}>{Panel}</div>}
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

                        <DataViewer data={preview} showKey={previewKey} isZoom hiddenLabels={hiddenLabels} />
                    </div>
                </div>
            </ModalBody>
        )
    })

    if (!isFullscreen) return <></>

    return (
        <Modal
            isOpen={isFullscreen}
            onClose={() => setIsFullscreen(false)}
            closeable
            animate
            autoFocus
            returnFocus={false}
            overrides={{
                Dialog: {
                    style: {
                        width: '90vw',
                        maxWidth: '1200px',
                        minHeight: '640px',
                        maxHeight: '90vh',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
        >
            <ModalHeader />
            {hoverable}
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
        return Array.from(annotationTypeMap).map(([type, list], _index) => {
            if (hiddenTypes.has(type)) return <span key={_index} />

            const allIds = annotationTypeMap.get(type)
            const hiddenIds = allIds.filter((id: number) => hiddenLabels.has(id))
            const otherIds = _.without([...hiddenLabels], ...allIds)
            const isAllHidden = hiddenIds.length === allIds.length

            return (
                <div key={_index} className={styles.annotationList}>
                    <div className={styles.annotationItem} style={{ color: ' rgba(2,16,43,0.60)', marginTop: '20px' }}>
                        {toUpper(type)}({list.length})
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
        <div className='flex-content'>
            {!$isSimpleView && (
                <div className={styles.annotation}>
                    <LabelMedium>Annotation Type</LabelMedium>
                    <div className={styles.annotationTypes}>
                        {Array.from(annotationTypes).map((type: any) => {
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
            {!$isSimpleView && (
                <Tabs
                    overrides={{
                        Root: {
                            style: {
                                overflow: 'hidden',
                            },
                        },
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
                                overflow: 'auto',
                                backgroundColor: '#fff',
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
                                overflow: 'hidden',
                                backgroundColor: '#fff',
                                fontWeight: 600,
                            }),
                        },
                    }}
                    onChange={({ activeKey }) => onChange?.(activeKey as string)}
                    activeKey={$activeKey}
                >
                    <Tab title={`Annotation(${count})`}>
                        <div>{Anno}</div>
                    </Tab>
                    <Tab title='Meta'>
                        <JSONView data={record} />
                    </Tab>
                </Tabs>
            )}
            {$isSimpleView && (
                <div className='content-full-scroll'>
                    <JSONView data={record} />{' '}
                </div>
            )}
        </div>
    )
}
