import React from 'react'

import projectSvg from '@/assets/fonts/project.svg'

interface IIconFontProps {
    size?: number
    kind?: 'white' | 'gray' | 'white2'
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
}

export default function IconFont({ size = 14, type = 'user', kind = 'gray' }: IIconFontProps) {
    const colors = {
        gray: 'var(--color-brandFontTip)',
        white: 'var(--color-brandFontWhite)',
        white2: 'var(--color-brandUserIcon)',
    }

    return (
        <div
            className='icon-container'
            style={{
                width: size,
                height: size,
                fontSize: size,
                color: colors[kind],
                padding: 0,
                display: 'flex',
                alignItems: 'center',
                fontWeight: 'normal',
            }}
        >
            {type == 'project' ? (
                <img src={projectSvg} alt='project' width={20} />
            ) : (
                <span className={`iconfont icon-${type}`}></span>
            )}
        </div>
    )
}
