import React from 'react'
import _ from 'lodash'

import projectSvg from '@/assets/fonts/project.svg'
import settingSvg from '@/assets/fonts/setting.svg'
import emptySvg from '@/assets/empty.svg'
import emptyChartSvg from '@/assets/empty-chart.svg'
import searchEmptySvg from '@/assets/search-empty.svg'
import googleSvg from '@/assets/fonts/google.svg'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

type IconTypesT =
    | 'arrow2_down'
    | 'arrow2_right'
    | 'clear2'
    | 'arrow_left'
    | 'arrow_down'
    | 'arrow_top'
    | 'arrow_right'
    | 'eye_off'
    | 'eye'
    | 'clear'
    | 'fold'
    | 'fold2'
    | 'unfold'
    | 'unfold2'
    | 'job'
    | 'logout'
    | 'password'
    | 'passwordresets'
    | 'dataset'
    | 'close'
    | 'results'
    | 'Model'
    | 'project'
    | 'show'
    | 'revert'
    | 'user'
    | 'search'
    | 'tasks'
    | 'add'
    | 'setting2'
    | 'success'
    | 'runtime'
    | 'decline'
    | 'rise'
    | 'pin'
    | 'setting'
    | 'more'
    | 'a-sortasc'
    | 'a-sortdesc'
    | 'email'
    | 'warning'
    | 'Facebook'
    | 'Twitter'
    | 'Instagram'
    | 'google'
    | 'Github'
    | 'a-managemember'
    | 'overview'
    | 'evaluation'
    | 'excel'
    | 'text'
    | 'audio'
    | 'view'
    | 'grid'
    | 'fullscreen'
    | 'token'
    | 'drag'
    | 'a-Addabove'
    | 'a-Addbelow'
    | 'arrow2'

interface IIconFontProps {
    style?: React.CSSProperties
    size?: number
    kind?: 'inherit' | 'white' | 'gray' | 'white2' | 'primary'
    type: IconTypesT
}

const hijacked = {
    project: projectSvg,
    setting2: settingSvg,
    google: googleSvg,
    empty: emptySvg,
    searchEmpty: searchEmptySvg,
    emptyChart: emptyChartSvg,
}

export default function IconFont({ size = 14, type = 'user', kind = 'inherit', style = {} }: IIconFontProps) {
    const [, theme] = themedUseStyletron()

    const colors = {
        gray: theme.brandFontTip,
        white: theme.brandFontWhite,
        white2: theme.brandUserIcon,
        primary: theme.brandPrimary,
    }

    return (
        <i
            className='icon-container row-center'
            style={{
                width: size,
                height: size,
                lineHeight: `${size}px`,
                color: kind === 'inherit' ? 'inherit' : colors[kind],
                padding: 0,
                display: 'inline-block',
                fontWeight: 'normal',
                ...style,
            }}
        >
            {type in hijacked ? (
                <img src={_.get(hijacked, type)} alt={type} width={size ?? 20} />
            ) : (
                <span className={`iconfont icon-${type}`} style={{ fontSize: size }} />
            )}
        </i>
    )
}
