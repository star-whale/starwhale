/* eslint-disable */
/* @ts-nocheck */

import Button from '@starwhale/ui/Button'
import IconFont from '@starwhale/ui/IconFont'
import { Panel } from 'baseui/accordion'
import React, { useCallback } from 'react'
import { expandBorder, expandPadding } from '@starwhale/ui/utils'
import SectionPopover from './SectionPopover'
import useTranslation from '@/hooks/useTranslation'

// @FIXME type define
const Header = React.forwardRef((props, ref) => {
    // console.log('Header', props)
    const { $expanded, children, onClick, onPanelChartAdd, editable } = props as any
    const [t] = useTranslation()

    const actions = {
        // @ts-ignore
        rename: props.onSectionRename,
        // @ts-ignore
        delete: props.onSectionDelete,
    }

    return (
        <div
            // @ts-ignore
            ref={ref}
            style={{
                height: '48px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0 20px 0 8px',
                gap: '12px',
            }}
        >
            <Button
                onClick={onClick}
                isFull
                startEnhancer={() => (
                    <IconFont
                        type='arrow2'
                        style={{
                            transform: $expanded ? 'rotate(0)' : 'rotate(-90deg)',
                            transition: 'ease 1s',
                            color: 'rgba(2,16,43,0.40)',
                        }}
                    />
                )}
                overrides={{
                    BaseButton: {
                        style: {
                            'fontSize': '14px',
                            'color': '#02102B',
                            'fontWeight': 'bold',
                            ':hover': { color: '#5181E0' },
                            ':active': { color: '#5181E0' },
                            'justifyContent': 'flex-start',
                        },
                    },
                }}
                as='transparent'
            >
                {children}
            </Button>
            <div style={{ flex: 1 }} />
            {editable && (
                <SectionPopover
                    actions={actions}
                    // // @ts-ignore
                    onOptionSelect={(item: any) => {
                        // @ts-ignore
                        actions[item.type]?.()
                    }}
                />
            )}
            {editable && (
                <Button kind='secondary' onClick={onPanelChartAdd} disabled={!!!onPanelChartAdd}>
                    {t('panel.chart.add')}
                </Button>
            )}
        </div>
    )
})

export default function SectionAccordionPanel(props: any) {
    const { title, children, childNums, expanded, onExpanded: setExpanded, ...rest } = props
    // console.log('Section', props, children)

    // const [expanded, setExpanded] = useState(false)
    const handleChange = useCallback(
        ({ expanded }) => {
            setExpanded(expanded)
        },
        [setExpanded]
    )

    return (
        <Panel
            {...rest}
            overrides={{
                Header: (headerProps: any) => {
                    return (
                        <Header {...rest} {...headerProps}>
                            {title}
                            {childNums ? (
                                <span
                                    style={{
                                        backgroundColor: '#F0F5FF',
                                        color: 'rgba(2,16,43,0.60)',
                                        fontWeight: 'normal',
                                        borderRadius: '12px',
                                        padding: '3px 20px',
                                        marginLeft: '8px',
                                        fontSize: '12px ',
                                    }}
                                >
                                    {childNums}
                                </span>
                            ) : null}
                        </Header>
                    )
                },
                PanelContainer: {
                    style: {
                        backgroundColor: '#FAFBFC;',
                        ...expandPadding('0', '0', '0', '0'),
                        borderBottomWidth: '1px',
                        ...expandBorder('1px', 'solid', '#E2E7F0'),
                        marginBottom: '-1px',
                        borderLeftWidth: 0,
                        borderRightWidth: 0,
                    },
                },
                Content: {
                    style: {
                        backgroundColor: '#FAFBFC;',
                        borderBottomWidth: '0px',
                        ...expandPadding('0', '0', '20px', '0'),
                    },
                },
            }}
            expanded={expanded}
            onChange={handleChange}
        >
            {children}
        </Panel>
    )
}
