import IconFont from '../IconFont'
import Input from '../Input'
import { themedStyled } from '../theme/styletron'

export const TreeContainer = themedStyled('div', () => ({
    display: 'flex',
    flex: 1,
    flexDirection: 'column',
}))

export const TreeNodeContainer = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    wdith: '100%',
}))

export const TreeSearch = (props: any) => {
    return (
        <Input
            startEnhancer={() => <IconFont type='search' style={{ color: 'rgba(2,16,43,0.40)' }} />}
            overrides={{
                Root: {
                    style: {
                        marginBottom: '20px',
                        maxWidth: '320px',
                    },
                },
            }}
            {...props}
        />
    )
}
