import ejs from 'ejs'

import QuickStartNewModelZH from '@/assets/docs/QuickStartNewModelZH.md.ejs?raw'
import QuickStartNewModelEN from '@/assets/docs/QuickStartNewModelEN.md.ejs?raw'
import { getCurrentHost } from '@/utils'

export const headerHeight = 50
export const sidebarExpandedWidth = 200
export const sidebarFoldedWidth = 68
export const textVariant = 'smallPlus'
export const dateFormat = 'YYYY-MM-DD'
export const dateWithZeroTimeFormat = 'YYYY-MM-DD 00:00:00'
export const dateTimeFormat = 'YYYY-MM-DD HH:mm:ss'
export const drawerExpandedWidthOfColumnManage = 520
export const passwordMinLength = 8
export const SignupStepStranger = 'strange_user'
export const SignupStepEmailNeedVerify = 'email_not_verified'
export const SignupStepNeedCreateAccount = 'account_not_created'
export const SignupStepAccountCreated = 'account_created'
export const CreateAccountPageUri = '/create-account'
export const FakeLocalToken = 'local'
export const languages = [
    {
        label: 'English',
        id: 'en',
    },
    {
        label: '简体中文',
        id: 'zh',
    },
]
export const cliMateServer = 'http://127.0.0.1:8007'
export const docsZH = 'https://starwhale.cn/docs'
export const docsEN = 'https://doc.starwhale.ai'

export const localeConst = {
    zh: {
        quickstart: 'https://starwhale.cn/docs/getting-started/cloud',
        model: 'https://starwhale.cn/docs/model/',
        quickStartNewModel: ejs.render(QuickStartNewModelZH, { consoleUrl: getCurrentHost() }),
    },
    en: {
        quickstart: 'https://doc.starwhale.ai/getting-started/cloud',
        model: 'https://doc.starwhale.ai/model/',
        quickStartNewModel: ejs.render(QuickStartNewModelEN, { consoleUrl: getCurrentHost() }),
    },
}
