import React, { useCallback } from 'react'
import Button from '@starwhale/ui/Button'
import IconFont from '@starwhale/ui/IconFont'
import useTranslation from '@/hooks/useTranslation'
import { expandBorderRadius } from '@/utils'
import { expandPadding } from '@starwhale/ui/utils'

export interface IThirdPartyLoginButtonProps {
    isLogin: boolean
    vendorName: string
    vendor: string
    icon: React.ReactNode
}

export default function ThirdPartyLoginButton({ isLogin, vendorName, vendor, icon }: IThirdPartyLoginButtonProps) {
    const [t] = useTranslation()

    const handleClick = useCallback(
        (e) => {
            e.preventDefault()
            const base = `${window.location.protocol}//${window.location.host}`
            const feUrl = encodeURIComponent(`${base}/login`)
            const baseBe = `${base}/swcloud/api/v1/redirect/thirdparty`
            // redirect to our server and pass the front-end url to redirect when user auth done.
            window.location.href = `${baseBe}?callback=${feUrl}&vendor=${encodeURIComponent(vendor)}`
        },
        [vendor]
    )

    return (
        <div
            style={{
                display: 'inline-flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
            }}
        >
            <Button
                isFull
                type='button'
                startEnhancer={icon}
                kind='secondary'
                overrides={{
                    BaseButton: {
                        style: {
                            justifyContent: 'space-between',
                            fontSize: '16px',
                            width: '32px',
                            height: '32px',
                            backgroundColor: '#EEF1F6 !important',
                            color: 'black',
                            display: 'grid',
                            placeContent: 'center',
                            ...expandBorderRadius('50%'),
                            ...expandPadding('0px', '0px', '0px', '0px'),
                        },
                    },
                }}
                onClick={handleClick}
            ></Button>
            {t(isLogin ? 'Log In With' : 'Sign Up With', [vendorName])}
        </div>
    )
}
