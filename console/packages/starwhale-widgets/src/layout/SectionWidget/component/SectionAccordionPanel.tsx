/* eslint-disable */
/* @ts-nocheck */

import Button from '@/components/Button'
import IconFont from '@/components/IconFont'
import { Panel, PanelProps } from 'baseui/accordion'
import React, { useCallback } from 'react'
import { expandBorder, expandMargin, expandPadding } from '@/utils'
import SectionPopover from './SectionPopover'

// @FIXME type define
const Header = React.forwardRef((props, ref) => {
    // console.log('Header', props)
    const { $expanded, children, onClick, onPanelAdd } = props as any

    const actions = {
        rename: props.onSectionRename,
        addAbove: props.onSectionAddAbove,
        addBelow: props.onSectionAddBelow,
        delete: props.onSectionDelete,
    }

    return (
        <div
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
                            ':hover': { color: '#02102B' },
                        },
                    },
                }}
                as='transparent'
            >
                {children}
            </Button>
            <div style={{ flex: 1 }} />
            <SectionPopover
                onOptionSelect={(item) => {
                    console.log(item)
                    actions[item.type]?.()
                }}
            />
            <Button
                kind='secondary'
                onClick={onPanelAdd}
                overrides={{
                    BaseButton: {
                        style: {
                            backgroundColor: '#fff',
                            fontSize: '14px',
                            color: '#02102B',
                            height: '32px',
                            ...expandMargin('0', '0', '0', '0'),
                        },
                    },
                }}
            >
                Add Chart
            </Button>
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
                Header: (headerProps: any) => (
                    <Header {...rest} {...headerProps}>
                        {title}
                        {childNums ? (
                            <span
                                style={{
                                    backgroundColor: '#F0F5FF',
                                    color: 'rgba(2,16,43,0.60)',
                                    fontWeight: 'normal',
                                    borderRadius: '12px',
                                    padding: '3px 10px',
                                    marginLeft: '8px',
                                    fontSize: '12px ',
                                }}
                            >
                                {childNums}
                            </span>
                        ) : null}
                    </Header>
                ),
                PanelContainer: {
                    style: {
                        backgroundColor: '#FAFBFC;',
                        ...expandPadding('0', '0', '0', '0'),
                        borderBottomWidth: '1px',
                        ...expandBorder('1px', 'solid', '#E2E7F0'),
                        marginBottom: '-1px',
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
