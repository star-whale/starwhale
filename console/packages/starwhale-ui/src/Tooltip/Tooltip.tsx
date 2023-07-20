import { StatefulTooltip as BaseToolTip, StatefulTooltipProps } from 'baseui/tooltip'
import IconFont, { IconTypesT } from '../IconFont'
import { themedUseStyletron } from '../theme/styletron'
import { mergeOverrides } from '../base/helpers/overrides'
import { expandBorderRadius, expandPadding } from '../utils'

export type ITooltipProps = StatefulTooltipProps

function Tooltip({ children, ...props }: ITooltipProps) {
    const overrides = mergeOverrides({
        Body: {
            style: {
                opacity: 0.8,
                zIndex: 20,
            },
        },
        Inner: {
            style: {
                backgroundColor: '#000',
                color: '#fff',
                ...expandBorderRadius('2px'),
                ...expandPadding('6px', '8px', '6px', '8px'),
            },
        },
        Arrow: {
            style: {
                backgroundColor: '#000',
            },
        },
    })

    return (
        <BaseToolTip showArrow {...props} overrides={overrides}>
            {children}
        </BaseToolTip>
    )
}

function IconTooltip({
    content,
    icon,
    style = {},
    ...props
}: {
    content: React.ReactNode
    icon: IconTypesT
    style?: React.CSSProperties
}) {
    const [css] = themedUseStyletron()

    return (
        <Tooltip content={content} showArrow placement='top' {...props}>
            <p
                // @ts-ignore
                className={css({
                    'cursor': 'pointer',
                    'color': 'rgba(2,16,43,0.40)',
                    ':hover': {
                        color: '#5181E0',
                    },
                    ...style,
                })}
            >
                <IconFont type={icon} />
            </p>
        </Tooltip>
    )
}

export { IconTooltip }
export default Tooltip
