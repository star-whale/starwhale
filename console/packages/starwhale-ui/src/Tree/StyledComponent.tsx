import IconFont from '../IconFont'
import Input from '../Input'
import { themedStyled } from '../theme/styletron'

export const TreeContainer = themedStyled('div', () => ({
    display: 'flex',
    padding: '20px 20px 20px 0',
    flex: 1,
    flexDirection: 'column',
}))

export const TreeNodeContainer = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
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
