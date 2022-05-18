import React from 'react'
import { useStyletron } from 'baseui'
import LoginLayout from './LoginLayout'
import BusyPlaceholder from '../../components/BusyLoaderWrapper/BusyPlaceholder'

export default function Login() {
    const [, theme] = useStyletron()

    return (
        <LoginLayout>
            <div
                style={{
                    display: 'flex',
                    width: '100%',
                    height: '100%',
                    flexDirection: 'row',
                    justifyContent: 'center',
                }}
            >
                <div
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                    }}
                >
                    <BusyPlaceholder />
                </div>
            </div>
        </LoginLayout>
    )
}
