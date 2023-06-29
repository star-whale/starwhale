import React from 'react'
import { createUseStyles } from 'react-jss'
import { StatefulTooltip } from 'baseui/tooltip'

const useStyles = createUseStyles({
    member: {
        display: 'grid',
        placeItems: 'center',
        backgroundColor: '#8B9FC7',
        borderRadius: '50%',
        color: '#fff',
        border: '1px solid #D0DDF7',
    },
})

export default function Avatar({ name = '', size = 34, isTooltip = true }) {
    const styles = useStyles()

    return (
        <StatefulTooltip content={isTooltip ? name : ''} placement='bottom'>
            <div
                className={styles.member}
                style={{
                    width: size,
                    height: size,
                }}
            >
                {name?.substr(0, 2)}
            </div>
        </StatefulTooltip>
    )
}
