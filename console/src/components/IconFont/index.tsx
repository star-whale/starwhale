import React from 'react'

import projectSvg from '@/assets/fonts/project.svg'
import settingSvg from '@/assets/fonts/setting.svg'

interface IIconFontProps {
    style?: React.CSSProperties
    size?: number
    kind?: 'inherit' | 'white' | 'gray' | 'white2' | 'primary'
    type:
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
        | 'dataset'
        | 'close'
        | 'results'
        | 'model'
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
        | 'fold2'
        | 'unfold2'
}

export default function IconFont({ size = 14, type = 'user', kind = 'inherit', style = {} }: IIconFontProps) {
    const colors = {
        gray: 'var(--color-brandFontTip)',
        white: 'var(--color-brandFontWhite)',
        white2: 'var(--color-brandUserIcon)',
        primary: 'var(--color-brandPrimary)',
    }

    return (
        <div
            className='icon-container row-center'
            style={{
                width: size,
                height: size,
                fontSize: size,
                color: kind === 'inherit' ? 'inherit' : colors[kind],
                padding: 0,
                fontWeight: 'normal',
                ...style,
            }}
        >
            {type === 'project' && <img src={projectSvg} alt={type} width={20} />}
            {type === 'setting2' && <img src={settingSvg} alt={type} width={20} />}
            {!['project', 'setting2'].includes(type) && (
                <span className={`iconfont icon-${type}`} style={{ fontSize: size }} />
            )}
        </div>
    )
}
