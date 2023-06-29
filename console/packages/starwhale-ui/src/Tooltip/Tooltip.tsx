import { StatefulTooltip } from 'baseui/tooltip'
import IconFont, { IconTypesT } from '../IconFont'
import { themedUseStyletron } from '../theme/styletron'
import { expandBorderRadius, expandPadding } from '../utils'

function IconTooltip({ content, icon, ...props }: { content: React.ReactNode; icon: IconTypesT }) {
    const [css] = themedUseStyletron()

    return (
        <StatefulTooltip
            overrides={{
                Inner: {
                    style: {
                        backgroundColor: 'rgba(0,0,0,0.80);',
                        color: '#fff',
                        ...expandBorderRadius('2px'),
                        ...expandPadding('6px', '8px', '6px', '8px'),
                    },
                },
            }}
            content={content}
            showArrow
            placement='top'
            {...props}
        >
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
