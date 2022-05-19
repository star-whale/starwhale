import React from 'react'

import projectSvg from '@/assets/fonts/project.svg'

interface IIconFontProps {
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
}

export default function IconFont({ size = 14, type = 'user', kind = 'inherit' }: IIconFontProps) {
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
            }}
        >
            {type === 'project' ? (
                <img src={projectSvg} alt='project' width={20} />
            ) : (
                <span className={`iconfont icon-${type}`} />
            )}
        </div>
    )
}
