/* eslint-disable jsx-a11y/no-static-element-interactions */
import React, { useCallback } from 'react'
import classNames from 'classnames'
import { Skeleton } from 'baseui/skeleton'
import { createUseStyles } from 'react-jss'
import Text from '@/components/Text'
import type { IconType } from 'react-icons/lib'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import styles from './index.module.scss'

const useStyles = createUseStyles({
    card: (props: IThemedStyleProps) => {
        const linkStyle = {
            color: props.theme.brandLink,
        }

        return {
            'background': props.theme.colors.backgroundPrimary,
            'marginTop': '12px',
            '& a': linkStyle,
            '& a:link': linkStyle,
            '& a:visited': linkStyle,
        }
    },
    cardBody: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
    },
})

export interface ICardProps {
    title?: string | React.ReactNode
    outTitle?: string
    titleIcon?: IconType
    titleTail?: React.ReactNode
    style?: React.CSSProperties
    headStyle?: React.CSSProperties
    bodyStyle?: React.CSSProperties
    bodyClassName?: string
    children?: React.ReactNode
    className?: string
    middle?: React.ReactNode
    extra?: React.ReactNode
    loading?: boolean
    onMountCard?: React.RefCallback<HTMLDivElement>
    onClick?: () => void
}

export default function Card({
    title,
    outTitle,
    titleIcon: TitleIcon,
    titleTail,
    middle,
    extra,
    className,
    style,
    headStyle,
    bodyStyle,
    bodyClassName,
    children,
    loading,
    onMountCard,
    onClick,
}: ICardProps) {
    const mountCard = useCallback(
        (card) => {
            if (card) {
                // eslint-disable-next-line no-param-reassign
                card.style.transform = 'translate3d(0, 0, 0)'
                onMountCard?.(card)
            }
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        []
    )

    let c = children
    if (loading) {
        c = <Skeleton rows={3} animation />
    }

    const [, theme] = themedUseStyletron()

    const dynamicStyles = useStyles({ theme })

    return (
        <div
            ref={mountCard}
            onClick={onClick}
            className={classNames(styles.card, dynamicStyles.card, className)}
            style={{
                ...style,
                marginTop: outTitle ? '56px' : '0',
            }}
        >
            {outTitle && (
                <div
                    style={{
                        position: 'absolute',
                        top: '-36px',
                        left: 0,
                    }}
                >
                    {outTitle}
                </div>
            )}
            {(title || extra) && (
                <div
                    className={styles.cardHeadWrapper}
                    style={{
                        ...headStyle,
                        color: theme.brandFontPrimary,
                        borderBottomWidth: 0,
                    }}
                >
                    <div className={styles.cardHead}>
                        {title && (
                            <div className={styles.cardHeadTitle}>
                                {TitleIcon && <TitleIcon size={13} />}
                                {typeof title === 'string' ? (
                                    <Text
                                        // size='large'
                                        style={{
                                            fontSize: '18px',
                                            fontWeight: 'bold',
                                            lineHeight: '32px',
                                        }}
                                    >
                                        {title}
                                    </Text>
                                ) : (
                                    title
                                )}
                                {titleTail}
                            </div>
                        )}
                        <div className={styles.cardHeadTail}>
                            {middle}
                            {extra && <div className={styles.cardExtra}>{extra}</div>}
                        </div>
                    </div>
                </div>
            )}
            <div className={classNames(dynamicStyles.cardBody, styles.cardBody, bodyClassName)} style={bodyStyle}>
                {c}
            </div>
        </div>
    )
}
