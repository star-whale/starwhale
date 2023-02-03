import { LabelMedium } from 'baseui/typography'
import _ from 'lodash'
import React from 'react'
import { JSONTree } from 'react-json-tree'
import { createUseStyles } from 'react-jss'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { IconFont } from '@starwhale/ui'

const useStyles = createUseStyles({
    summaryRoot: {
        backgroundColor: '#F0F4FF',
        borderRadius: '4px',
        padding: '0 14px',
        gap: 12,
        display: 'flex',
        lineHeight: '24px',
        alignItems: 'center',
    },
    summaryLi: (props: IThemedStyleProps) => ({
        'borderRadius': '4px',
        'paddingLeft': '43px',
        'gap': 12,
        'display': 'flex',
        '&:before': {
            content: '"o"',
            color: props.theme.brandFontNote,
        },
    }),
})

export interface ISummaryIndicatorProps {
    data: Record<string, any>
    isTreeView?: boolean
}

export default function SummaryIndicator({ data, isTreeView = false }: ISummaryIndicatorProps) {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })

    if (!_.isObject(data)) {
        return (
            <LabelMedium
                $style={{
                    textOverflow: 'ellipsis',
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                }}
            >
                Sumary: {data}
            </LabelMedium>
        )
    }

    if (!isTreeView && _.isObject(data)) {
        return (
            <div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 6 }}>
                    {_(data)
                        .map((subV, subK) => (
                            <div key={subK}>
                                <div key={subK} className={styles.summaryRoot}>
                                    {_.isObject(subV) ? (
                                        <IconFont type='arrow2_down' kind='gray' />
                                    ) : (
                                        <div style={{ flexBasis: 14 }} />
                                    )}
                                    <p
                                        style={{
                                            color: 'rgba(2,16,43,0.60)',
                                        }}
                                    >
                                        {subK}
                                    </p>
                                    <p> {!_.isObject(subV) && subV}</p>
                                </div>
                                {_.isObject(subV) && (
                                    <ul key={`ul-${subK}`}>
                                        {_.map(subV, (subSubV, subSubK) => (
                                            <li className={styles.summaryLi} key={subSubK} style={{}}>
                                                <span
                                                    style={{
                                                        color: 'rgba(2,16,43,0.60)',
                                                    }}
                                                >
                                                    {subSubK}
                                                </span>
                                                {subSubV}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        ))
                        .value()}
                </div>
            </div>
        )
    }

    return (
        <JSONTree
            data={data}
            theme={{
                scheme: 'ocean',
                author: 'chris kempson (http://chriskempson.com)',
                base00: '#2b303b',
                base01: '#343d46',
                base02: '#4f5b66',
                base03: '#65737e',
                base04: '#a7adba',
                base05: '#c0c5ce',
                base06: '#dfe1e8',
                base07: '#eff1f5',
                base08: '#bf616a',
                base09: '#d08770',
                base0A: '#ebcb8b',
                base0B: '#a3be8c',
                base0C: '#96b5b4',
                base0D: '#8fa1b3',
                base0E: '#b48ead',
                base0F: '#ab7967',
            }}
            hideRoot
            shouldExpandNode={() => true}
            // labelRenderer={$labelRender}
            // valueRenderer={$valueRenderer}
        />
    )
}
