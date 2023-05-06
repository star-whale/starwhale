import { StatefulTooltip } from 'baseui/tooltip'
import IconFont, { IconTypesT } from '../IconFont'
import { themedUseStyletron } from '../theme/styletron'

function IconTooltip({ content, icon, ...props }: { content: React.ReactNode; icon: IconTypesT }) {
    const [css] = themedUseStyletron()

    return (
        <StatefulTooltip content={content} showArrow placement='top' {...props}>
            <p
                className={css({
                    'cursor': 'pointer',
                    'color': 'rgba(2,16,43,0.40)',
                    ':hover': {
                        color: '#5181E0',
                    },
                })}
            >
                <IconFont type={icon} />
            </p>
        </StatefulTooltip>
    )
}

export { IconTooltip }
