import IconFont from '../IconFont'
import Input from '../Input'
import { themedStyled } from '../theme/styletron'

export const TreeContainer = themedStyled('div', () => ({
    display: 'flex',
    flex: 1,
    flexDirection: 'column',
    overflow: 'hidden',
}))

export const TreeNodeContainer = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    wdith: '100%',
    color: '#02102B',
}))

export const TreeSearch = (props: any) => {
    return (
        <Input
            ref={props.inputRef}
            startEnhancer={() => <IconFont type='search' style={{ color: 'rgba(2,16,43,0.40)' }} />}
            overrides={{
                Root: {
                    style: {
                        marginBottom: '12px',
                        maxWidth: '100%',
                        paddingLeft: 0,
                        height: '32px',
                        flexShrink: 0,
                    },
                },
                Input: {
                    style: {
                        paddingLeft: '0',
                    },
                },
            }}
            {...props}
        />
    )
}
