export const CONFIG = {
    screenshotDir: 'test-video',
}
export const USERS = [
    { role: 'admin', username: 'starwhale', password: 'abcd1234' },
    { role: 'maintainer', username: 'lwzlwz', password: 'abcd1234' },
]
export const CONST = {
    user: {
        userName: 'lwzlwz',
        projectName: 'e2e',
        projectDescription: 'testing',
    },
    admin: {
        userName: 'starwhale',
        projectName: 'starwhale',
        projectDescription: 'testing',
    },
    adminSettings: 'Admin Settings',
    projectId: '3',
}
export const ROUTES = {
    evaluations: `/projects/${CONST.projectId}/evaluations`,
    evaluationResult: `/projects/${CONST.projectId}/evaluations/5/results`,
    evaluationActions: `/projects/${CONST.projectId}/evaluations/5/actions`,
    evaluationTasks: `/projects/${CONST.projectId}/evaluations/5/tasks`,
    evaluationNewJob: `/projects/${CONST.projectId}/new_job`,
}
export const SELECTOR = {
    loginName: 'input[type="text"]',
    loginPassword: 'input[type="password"]',
    // --- homepage ----
    userWrapper: '[class^=userNameWrapper]',
    userAvtarName: '[class^=userAvatarName]',
    authAdminSetting: '[class^=userMenuItems] >> :has-text("Admin Settings")',
    projectCreate: 'button:has-text("Create")',
    // --- project form ---
    projectForm: 'form[class^=project]',
    projectName: 'form[class^=project] >> input[type="text"]',
    projectPrivacy: 'form[class^=project] >> label:has-text("Private")',
    projectDescription: 'form[class^=project] >> textarea[type="textarea"]',
    projectSubmit: 'form[class^=project] >> button:has-text("Submit")',
    projectClose: 'role=button[name="Close"]',
    // --- project list ---
    projectCard: '[class*=projectCard]',
    projectCardLink: `[class*=projectCard] >> [class^=name] >> a[href="${ROUTES.evaluations}"]`,
    projectCardActions: '[class^=actions]',
    projectCardActionDelete: '[class^=actions] >> [class^=delete]',
    projectCardActionEdit: '[class^=actions] >> [class^=edit]',
    projectCardDeleteConfirm: 'role=button[name="Continue"]',
    // --- table ---
    // table: '[class^=table]',
    table: '[class*=tablePinnable]',
    tableCompare: '[class*=tableComparable]',
    headerFirst: '.table-headers .header-cell >> nth=0',
    headerFocused: '.header-cell--focused',
    row1column1: '[data-row-index="0"] [data-column-index="0"]',
    row2column1: '[data-row-index="1"] [data-column-index="0"]',
    // --- list ----
    listCreate: '[class*=cardHeadWrapper] >> :has-text("Create")',
    // --- evaluation result ----
    confusionMatrix: '[class*=card]:has(:has-text("Confusion Matrix")) >> .plotly',
}
