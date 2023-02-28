import AdminLayout from '@/pages/Admin/AdminLayout'
import UserManagement from '@/pages/Admin/UserManagement'
import React from 'react'
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom'
import ProjectLayout from '@/pages/Project/ProjectLayout'
import { createUseStyles } from 'react-jss'
import Login from '@/pages/Home/Login'
import ProjectMembers from '@/pages/Project/ProjectMembers'
import ProjectListCard from '@/pages/Project/ProjectListCard'
import ProjectModels from '@/pages/Project/Models'
import ProjectDatasets from '@/pages/Project/Datasets'
import ProjectJobs from '@/pages/Project/Jobs'
import ModelVersionListCard from '@/pages/Model/ModelVersionListCard'
import DatasetVersionListCard from '@/pages/Dataset/DatasetVersionListCard'
import DatasetOverviewLayout from '@/pages/Dataset/DatasetOverviewLayout'
import JobNewCard from '@/pages/Project/JobNewCard'
import ApiHeader from '@/api/ApiHeader'
import JobTasks from '@/pages/Job/JobTasks'
import JobResults from '@/pages/Job/JobResults'
import JobOverviewLayout from '@/pages/Job/JobOverviewLayout'
import SettingsOverviewLayout from '@/pages/Settings/SettingsOverviewLayout'
import SettingAgentListCard from '@/pages/Settings/SettingAgentListCard'
import RuntimeVersionListCard from '@/pages/Runtime/RuntimeVersionListCard'
import RuntimeVersionOverviewFiles from '@/pages/Runtime/RuntimeVersionOverviewFiles'
import ProjectRuntimes from '@/pages/Project/Runtimes'
import ProjectEvaluations from '@/pages/Project/Evaluations'
import EvaluationOverviewLayout from '@/pages/Evaluation/EvaluationOverviewLayout'
import Header from '@/components/Header'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import LoginLayout from '@/pages/Home/LoginLayout'
import ResetPassword from '@/pages/Home/ResetPassword'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import CenterLayout from '@/pages/CenterLayout'
import ProjectOverview from '@/pages/Project/Overview'
import Pending, { NoneBackgroundPending } from '@/pages/Home/Pending'
import { useAuth } from '@/api/Auth'
import DatasetVersionOverview from '@/pages/Dataset/DatasetVersionOverview'
import DatasetVersionOverviewMeta from '@/pages/Dataset/DatasetVersionOverviewMeta'
import DatasetVersionOverviewFiles from '@/pages/Dataset/DatasetVersionOverviewFiles'
import RuntimeVersionOverviewMeta from '@/pages/Runtime/RuntimeVersionOverviewMeta'
import RuntimeVersionOverview from '@/pages/Runtime/RuntimeVersionOverview'
import RuntimeOverviewLayout from '@/pages/Runtime/RuntimeOverviewLayout'
import SystemSettings from '@/pages/Admin/SystemSettings'
import Panel from '@/components/Editor'
import EvaluationWidgetResults from '@/pages/Evaluation/EvaluationWidgetResults'
import ModelVersionOverviewFiles from '@/pages/Model/ModelVersionOverviewFiles'
import ModelVersionOverview from '@/pages/Model/ModelVersionOverview'
import ModelOverviewLayout from '@/pages/Model/ModelOverviewLayout'
import ProjectTrashes from '@/pages/Project/Trashes'
import TrashLayout from '@/pages/Trash/TrashLayout'
import TrashListCard from '@/pages/Trash/TrashListCard'
import OnlineEval from '@/pages/Project/OnlineEval'
import _ from 'lodash'
import { getUnauthedRoutes } from './routesUtils'

const JobDAG = React.lazy(() => import('@/pages/Job/JobDAG'))

const useStyles = createUseStyles({
    root: ({ theme }: IThemedStyleProps) => ({
        display: 'flex',
        flexFlow: 'column nowrap',
        height: '100vh',
        width: '100vw',
        position: 'relative',
        color: theme.colors.contentPrimary,
    }),
})

const defaultRoutes = [
    {
        auth: false,
        component: LoginLayout,
        routes: [
            {
                path: '/reset',
                component: ResetPassword,
            },
            {
                path: '/login',
                component: Login,
            },
            {
                from: '/',
                to: '/login',
                component: Redirect,
            },
        ],
    },
]

const unauthedRoutes = getUnauthedRoutes(defaultRoutes[0])

const Routes = () => {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })
    const { token } = useAuth()

    if (!token) {
        return (
            <React.Suspense fallback={<Pending />}>
                <BrowserRouter>
                    <div className={styles.root}>
                        <Route>
                            <ApiHeader />
                            {unauthedRoutes}
                        </Route>
                    </div>
                </BrowserRouter>
            </React.Suspense>
        )
    }

    return (
        <React.Suspense fallback={<NoneBackgroundPending />}>
            <BrowserRouter>
                <div className={styles.root}>
                    <Route>
                        <ApiHeader />
                        <Header />
                        <Switch>
                            {/* setting */}
                            <Route exact path='/settings/:path?'>
                                <SettingsOverviewLayout>
                                    <Switch>
                                        <Route exact path='/settings/agents' component={SettingAgentListCard} />
                                        <Redirect from='/settings/:path?' to='/settings/agents' />
                                    </Switch>
                                </SettingsOverviewLayout>
                            </Route>
                            {/* project */}
                            <Route exact path='/projects/:projectId/members'>
                                <CenterLayout>
                                    <Route exact path='/projects/:projectId/members' component={ProjectMembers} />
                                </CenterLayout>
                            </Route>
                            {/* evaluation */}
                            <Route exact path='/projects/:projectId/evaluations/:jobId/:path?'>
                                <EvaluationOverviewLayout>
                                    <Switch>
                                        <Route
                                            exact
                                            path='/projects/:projectId/evaluations/:jobId/results'
                                            component={EvaluationWidgetResults}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/evaluations/:jobId/tasks'
                                            component={JobTasks}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/evaluations/:jobId/actions'
                                            component={JobDAG}
                                        />
                                        {/* <Redirect
                                            from='/projects/:projectId/evaluations/:jobId'
                                            to='/projects/:projectId/evaluations/:jobId/results'
                                        /> */}
                                    </Switch>
                                </EvaluationOverviewLayout>
                            </Route>
                            {/* job & task */}
                            <Route exact path='/projects/:projectId/jobs/:jobId/:path?'>
                                <JobOverviewLayout>
                                    <Switch>
                                        <Route
                                            exact
                                            path='/projects/:projectId/jobs/:jobId/tasks'
                                            component={JobTasks}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/jobs/:jobId/results'
                                            component={JobResults}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/jobs/:jobId/actions'
                                            component={JobDAG}
                                        />
                                        <Redirect
                                            from='/projects/:projectId/jobs/:jobId'
                                            to='/projects/:projectId/jobs/:jobId/actions'
                                        />
                                    </Switch>
                                </JobOverviewLayout>
                            </Route>
                            {/* datasets */}
                            <Route
                                exact
                                path='/projects/:projectId/datasets/:datasetId/:path?/:datasetVersionId?/:path?/:fileId?'
                            >
                                <DatasetOverviewLayout>
                                    <Switch>
                                        <Route
                                            exact
                                            path='/projects/:projectId/datasets/:datasetId'
                                            component={DatasetVersionListCard}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/overview'
                                            component={DatasetVersionOverview}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/meta'
                                            component={DatasetVersionOverviewMeta}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/files'
                                            component={DatasetVersionOverviewFiles}
                                        />

                                        <Route
                                            exact
                                            path='/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/files/:fileId?/:path?'
                                            component={DatasetVersionOverviewFiles}
                                        />
                                        <Redirect to='/projects/:projectId/datasets/:datasetId' />
                                    </Switch>
                                </DatasetOverviewLayout>
                            </Route>
                            {/* runtime */}
                            <Route
                                exact
                                path='/projects/:projectId/runtimes/:runtimeId/:path?/:runtimeVersionId?/:path?/:fileId?'
                            >
                                <RuntimeOverviewLayout>
                                    <Switch>
                                        <Route
                                            exact
                                            path='/projects/:projectId/runtimes/:runtimeId'
                                            component={RuntimeVersionListCard}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/overview'
                                            component={RuntimeVersionOverview}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/meta'
                                            component={RuntimeVersionOverviewMeta}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/files'
                                            component={RuntimeVersionOverviewFiles}
                                        />
                                        <Redirect to='/projects/:projectId/runtimes/:runtimeId' />
                                    </Switch>
                                </RuntimeOverviewLayout>
                            </Route>
                            {/* model */}
                            <Route
                                exact
                                path='/projects/:projectId/models/:modelId/:path?/:modelVersionId?/:path?/:fileId?'
                            >
                                <ModelOverviewLayout>
                                    <Switch>
                                        <Route
                                            exact
                                            path='/projects/:projectId/models/:modelId'
                                            component={ModelVersionListCard}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/models/:modelId/versions/:modelVersionId/overview'
                                            component={ModelVersionOverview}
                                        />
                                        <Route
                                            exact
                                            path='/projects/:projectId/models/:modelId/versions/:modelVersionId/files'
                                            component={ModelVersionOverviewFiles}
                                        />
                                        <Redirect to='/projects/:projectId/models/:modelId' />
                                    </Switch>
                                </ModelOverviewLayout>
                            </Route>

                            {/* trash */}
                            <Route exact path='/projects/:projectId/trashes'>
                                <TrashLayout>
                                    <Switch>
                                        <Route exact path='/projects/:projectId/trashes' component={TrashListCard} />
                                        <Redirect to='/projects/:projectId/trashes' />
                                    </Switch>
                                </TrashLayout>
                            </Route>
                            {/* admin */}
                            <Route exact path='/admin/:path?'>
                                <AdminLayout>
                                    <Switch>
                                        <Route exact path='/admin/users' component={UserManagement} />
                                        <Route exact path='/admin/settings' component={SystemSettings} />

                                        <Redirect exact from='/admin' to='/admin/users' />
                                    </Switch>
                                </AdminLayout>
                            </Route>
                            <Route exact path='/projects/:projectId/:path*'>
                                <ProjectLayout>
                                    <Switch>
                                        <Route exact path='/projects/:projectId/models' component={ProjectModels} />
                                        <Route exact path='/projects/:projectId/trashes' component={ProjectTrashes} />
                                        <Route exact path='/projects/:projectId/datasets' component={ProjectDatasets} />
                                        <Route exact path='/projects/:projectId/jobs' component={ProjectJobs} />
                                        <Route
                                            exact
                                            path='/projects/:projectId/evaluations'
                                            component={ProjectEvaluations}
                                        />
                                        <Route exact path='/projects/:projectId/runtimes' component={ProjectRuntimes} />
                                        <Route exact path='/projects/:projectId/new_job' component={JobNewCard} />
                                        <Route
                                            exact
                                            path='/projects/:projectId/online_eval/:modelId/:modelVersionId?'
                                            component={OnlineEval}
                                        />
                                        <Route exact path='/projects/:projectId/members' component={ProjectMembers} />
                                        <Route exact path='/projects/:projectId/overview' component={ProjectOverview} />
                                        <Redirect from='/projects/:projectId' to='/projects/:projectId/overview' />
                                    </Switch>
                                </ProjectLayout>
                            </Route>
                            {/* default */}
                            <Route>
                                <CenterLayout>
                                    <Switch>
                                        <Route path='/projects' component={ProjectListCard} />
                                        <Route path='/panels' component={Panel} />
                                        <Redirect path='/' to='/projects' />
                                    </Switch>
                                </CenterLayout>
                            </Route>
                        </Switch>
                    </Route>
                </div>
            </BrowserRouter>
        </React.Suspense>
    )
}

export default Routes
