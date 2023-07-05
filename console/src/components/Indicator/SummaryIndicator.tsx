import { LabelMedium } from 'baseui/typography'
import _ from 'lodash'
import React from 'react'
import { createUseStyles } from 'react-jss'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { IconFont } from '@starwhale/ui'
import JSONView from '@starwhale/ui/JSONView'

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

    return <JSONView data={data} collapsed={2} collapseStringsAfterLength={100} />
}
