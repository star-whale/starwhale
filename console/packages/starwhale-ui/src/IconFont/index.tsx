import React from 'react'
import _ from 'lodash'
import './fonts/iconfont.css'

import projectSvg from './svg/project.svg'
import settingSvg from './svg/setting.svg'
import emptySvg from './svg/empty.svg'
import emptyChart from './svg/empty-chart.svg'
import searchEmptySvg from './svg/search-empty.svg'
import googleSvg from './svg/google.svg'
import invalidFile from './svg/invalid-file.svg'
import { themedUseStyletron } from '../theme/styletron'

export type IconTypesT =
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
    | 'filter'
    | 'error'
    | 'edit'
    | 'info'
    | 'item-reduce'
    | 'item-add'
    | 'emptyChart'
    | 'download'
    | 'searchEmpty'
    | 'empty'
    | 'reset'
    | 'delete'
    | 'a-passwordresets'
    | 'file'
    | 'file2'
    | 'check'
    | 'invalidFile'
    | 'group'
    | 'library'

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
    emptyChart,
    searchEmpty: searchEmptySvg,
    invalidFile,
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
