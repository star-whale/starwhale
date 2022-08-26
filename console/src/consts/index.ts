export const headerHeight = 56
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

const VERSION_PREFIX_CNT = 2

// const FORMATTER_TABLE_NAME_DATASET = 'project/%s/dataset/%s/%s/%s/meta'

// const FORMATTER_TABLE_NAME_EVAL_RESULTS = 'project/%s/eval/%s/results'

// const FORMATTER_TABLE_NAME_EVAL_SUMMARY = 'project/%s/eval/summary'

export function tableNameOfDataset(projectId: string, datasetId: string, datasetVersion: string) {
    return `project/${projectId}/dataset/${datasetId}/${datasetVersion.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${datasetVersion}/meta`
}

export function tableNameOfResult(projectId: string, evaluationId: string) {
    return `project/${projectId}/eval/${evaluationId}/summary`
}

export function tableNameOfSummary(projectId: string) {
    return `project/${projectId}/eval/summary`
}
