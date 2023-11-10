/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export interface IUserUpdateStateRequest {
    isEnabled: boolean
}

export interface IResponseMessageString {
    code: string
    message: string
    data: string
}

export interface IUserUpdatePasswordRequest {
    currentUserPwd: string
    newPwd: string
}

export interface IUserRoleUpdateRequest {
    currentUserPwd: string
    roleId: string
}

export interface IUpdateProjectRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
    projectName?: string
    privacy?: string
    description?: string
    readme?: string
}

export interface IResponseMessageObject {
    code: string
    message: string
    data: object
}

export interface IRuntimeTagRequest {
    force?: boolean
    tag: string
}

export interface IUpdateReportRequest {
    /**
     * @minLength 0
     * @maxLength 255
     */
    title?: string
    /**
     * @minLength 0
     * @maxLength 255
     */
    description?: string
    content?: string
    shared?: boolean
}

export interface IModelUpdateRequest {
    tag?: string
    built_in_runtime?: string
}

export interface IJobModifyRequest {
    comment: string
}

export interface IApplySignedUrlRequest {
    flag?: string
    pathPrefix: string
    /** @uniqueItems true */
    files: string[]
}

export interface IResponseMessageSignedUrlResponse {
    code: string
    message: string
    data: ISignedUrlResponse
}

export interface ISignedUrlResponse {
    pathPrefix?: string
    signedUrls?: Record<string, string>
}

export interface IUserRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{3,32}$ */
    userName: string
    userPwd: string
    salt?: string
}

export interface IUserCheckPasswordRequest {
    currentUserPwd: string
}

export interface IResource {
    name?: string
    /** @format float */
    max?: number
    /** @format float */
    min?: number
    /** @format float */
    defaults?: number
}

export interface IResourcePool {
    name?: string
    nodeSelector?: Record<string, string>
    resources?: IResource[]
    tolerations?: IToleration[]
    metadata?: Record<string, string>
    isPrivate?: boolean
    visibleUserIds?: number[]
}

export interface IToleration {
    key?: string
    operator?: string
    value?: string
    effect?: string
    /** @format int64 */
    tolerationSeconds?: number
}

export interface IUserRoleAddRequest {
    currentUserPwd: string
    userId: string
    roleId: string
}

export interface ICreateProjectRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
    projectName: string
    privacy: string
    description: string
}

export interface ICreateModelVersionRequest {
    metaBlobId: string
    builtInRuntime?: string
    force?: boolean
}

export interface ICreateJobTemplateRequest {
    name: string
    jobUrl: string
}

export interface IModelServingRequest {
    modelVersionUrl: string
    runtimeVersionUrl: string
    resourcePool?: string
    /**
     * @deprecated
     * @format int64
     */
    ttlInSeconds?: number
    spec?: string
}

/**
 * Model Serving
 * Model Serving object
 */
export interface IModelServingVo {
    id: string
    baseUri?: string
}

export interface IResponseMessageModelServingVo {
    code: string
    message: string
    /** Model Serving object */
    data: IModelServingVo
}

/** user defined running configurations such environment variables */
export interface IRunEnvs {
    envVars?: Record<string, string>
}

/**
 * Runtime
 * Build runtime image result
 */
export interface IBuildImageResult {
    success?: boolean
    message?: string
}

export interface IResponseMessageBuildImageResult {
    code: string
    message: string
    /** Build runtime image result */
    data: IBuildImageResult
}

export interface IRuntimeRevertRequest {
    versionUrl: string
}

export interface IClientRuntimeRequest {
    runtime?: string
    project?: string
    force?: string
    manifest?: string
}

export interface ICreateReportRequest {
    /**
     * @minLength 1
     * @maxLength 255
     */
    title: string
    /**
     * @minLength 0
     * @maxLength 255
     */
    description?: string
    /**
     * @minLength 1
     * @maxLength 2147483647
     */
    content: string
}

export interface ITransferReportRequest {
    targetProjectUrl: string
}

export interface IModelTagRequest {
    force?: boolean
    tag: string
}

export interface IRevertModelVersionRequest {
    versionUrl: string
}

export interface IJobRequest {
    modelVersionId?: string
    datasetVersionIds?: string[]
    runtimeVersionId?: string
    /** @format int64 */
    timeToLiveInSec?: number
    /** @deprecated */
    modelVersionUrl?: string
    /** @deprecated */
    datasetVersionUrls?: string
    /** @deprecated */
    runtimeVersionUrl?: string
    comment?: string
    resourcePool: string
    handler?: string
    stepSpecOverWrites?: string
    type?: 'EVALUATION' | 'TRAIN' | 'FINE_TUNE' | 'SERVING' | 'BUILT_IN'
    devMode?: boolean
    devPassword?: string
    devWay?: 'VS_CODE'
}

export interface IExecRequest {
    command: string[]
}

export interface IExecResponse {
    stdout?: string
    stderr?: string
}

export interface IResponseMessageExecResponse {
    code: string
    message: string
    data: IExecResponse
}

export interface IJobModifyPinRequest {
    pinned: boolean
}

export interface IEventRequest {
    eventType: 'INFO' | 'WARNING' | 'ERROR'
    source: 'CLIENT' | 'SERVER' | 'NODE'
    relatedResource: IRelatedResource
    message: string
    data?: string
    /** @format int64 */
    timestamp?: number
}

export interface IRelatedResource {
    eventResourceType: 'JOB' | 'TASK' | 'RUN'
    /** @format int64 */
    id: number
}

export interface IResponseMessageMapStringString {
    code: string
    message: string
    data: Record<string, string>
}

export interface IConfigRequest {
    name: string
    content: string
}

export interface IDatasetTagRequest {
    force?: boolean
    tag: string
}

export interface IDataConsumptionRequest {
    sessionId?: string
    consumerId?: string
    /** @format int32 */
    batchSize?: number
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
    processedData?: IDataIndexDesc[]
    /** @deprecated */
    serial?: boolean
}

export interface IDataIndexDesc {
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
}

export interface INullableResponseMessageDataIndexDesc {
    code: string
    message: string
    data?: IDataIndexDesc
}

export interface IRevertDatasetRequest {
    versionUrl: string
}

export interface IDatasetUploadRequest {
    /** @format int64 */
    uploadId: number
    partName?: string
    signature?: string
    uri?: string
    desc?: 'MANIFEST' | 'SRC_TAR' | 'SRC' | 'MODEL' | 'DATA' | 'UNKNOWN'
    phase: 'MANIFEST' | 'BLOB' | 'END' | 'CANCEL'
    force?: string
    project: string
    swds: string
}

export interface IResponseMessageUploadResult {
    code: string
    message: string
    data: IUploadResult
}

export interface IUploadResult {
    /** @format int64 */
    uploadId?: number
}

export interface IDatasetBuildRequest {
    type: 'IMAGE' | 'VIDEO' | 'AUDIO'
    shared?: boolean
    storagePath: string
}

export interface IResponseMessageMapObjectObject {
    code: string
    message: string
    data: Record<string, object>
}

export interface IColumnSchemaDesc {
    name?: string
    /** @format int32 */
    index?: number
    type?: string
    pythonType?: string
    elementType?: IColumnSchemaDesc
    keyType?: IColumnSchemaDesc
    valueType?: IColumnSchemaDesc
    attributes?: IColumnSchemaDesc[]
    sparseKeyValuePairSchema?: Record<string, IKeyValuePairSchema>
}

export interface IKeyValuePairSchema {
    keyType?: IColumnSchemaDesc
    valueType?: IColumnSchemaDesc
}

export interface IRecordDesc {
    values: IRecordValueDesc[]
}

export interface IRecordValueDesc {
    key: string
    value?: object
}

export interface ITableSchemaDesc {
    keyColumn?: string
    columnSchemaList?: IColumnSchemaDesc[]
}

export interface IUpdateTableRequest {
    tableName?: string
    tableSchemaDesc?: ITableSchemaDesc
    records?: IRecordDesc[]
}

export interface IColumnDesc {
    columnName?: string
    alias?: string
}

export interface IScanTableRequest {
    tables?: ITableDesc[]
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
}

export interface ITableDesc {
    tableName?: string
    columnPrefix?: string
    columns?: IColumnDesc[]
    keepNone?: boolean
    revision?: string
}

export interface IColumnHintsDesc {
    typeHints?: string[]
    columnValueHints?: string[]
    elementHints?: IColumnHintsDesc
    keyHints?: IColumnHintsDesc
    valueHints?: IColumnHintsDesc
}

export interface IRecordListVo {
    columnTypes?: IColumnSchemaDesc[]
    columnHints?: Record<string, IColumnHintsDesc>
    records?: Record<string, object>[]
    lastKey?: string
}

export interface IResponseMessageRecordListVo {
    code: string
    message: string
    data: IRecordListVo
}

export interface IOrderByDesc {
    columnName?: string
    descending?: boolean
}

export interface IQueryTableRequest {
    tableName?: string
    columns?: IColumnDesc[]
    orderBy?: IOrderByDesc[]
    descending?: boolean
    filter?: ITableQueryFilterDesc
    /** @format int32 */
    start?: number
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
    revision?: string
}

export interface ITableQueryFilterDesc {
    operator: string
    operands?: ITableQueryOperandDesc[]
}

export interface ITableQueryOperandDesc {
    filter?: ITableQueryFilterDesc
    columnName?: string
    boolValue?: boolean
    /** @format int64 */
    intValue?: number
    /** @format double */
    floatValue?: number
    stringValue?: string
    bytesValue?: string
}

export interface IListTablesRequest {
    prefix?: string
    /** @uniqueItems true */
    prefixes?: string[]
}

export interface IResponseMessageTableNameListVo {
    code: string
    message: string
    data: ITableNameListVo
}

export interface ITableNameListVo {
    tables?: string[]
}

export type IFlushRequest = object

export interface IInitUploadBlobRequest {
    contentMd5: string
    /** @format int64 */
    contentLength: number
}

export interface IInitUploadBlobResult {
    status?: 'OK' | 'EXISTED'
    blobId?: string
    signedUrl?: string
}

export interface IResponseMessageInitUploadBlobResult {
    code: string
    message: string
    data: IInitUploadBlobResult
}

export interface ICompleteUploadBlobResult {
    blobId?: string
}

export interface IResponseMessageCompleteUploadBlobResult {
    code: string
    message: string
    data: ICompleteUploadBlobResult
}

export interface IPageInfoUserVo {
    /** @format int64 */
    total?: number
    list?: IUserVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoUserVo {
    code: string
    message: string
    data: IPageInfoUserVo
}

/**
 * User
 * User object
 */
export interface IUserVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    isEnabled: boolean
    systemRole?: string
    projectRoles?: Record<string, string>
}

export interface IResponseMessageUserVo {
    code: string
    message: string
    /** User object */
    data: IUserVo
}

/**
 * Role
 * Project Role object
 */
export interface IProjectMemberVo {
    id: string
    /** User object */
    user: IUserVo
    /** Project object */
    project: IProjectVo
    /** Role object */
    role: IRoleVo
}

/**
 * Project
 * Project object
 */
export interface IProjectVo {
    id: string
    name: string
    description?: string
    privacy: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    statistics?: IStatisticsVo
}

export interface IResponseMessageListProjectMemberVo {
    code: string
    message: string
    data: IProjectMemberVo[]
}

/**
 * Role
 * Role object
 */
export interface IRoleVo {
    id: string
    name: string
    code: string
    description?: string
}

export interface IStatisticsVo {
    /** @format int32 */
    modelCounts: number
    /** @format int32 */
    datasetCounts: number
    /** @format int32 */
    runtimeCounts: number
    /** @format int32 */
    memberCounts: number
    /** @format int32 */
    evaluationCounts: number
}

export interface IResponseMessageSystemVersionVo {
    code: string
    message: string
    /** System version */
    data: ISystemVersionVo
}

/**
 * Version
 * System version
 */
export interface ISystemVersionVo {
    id?: string
    version?: string
}

export interface IResponseMessageListResourcePool {
    code: string
    message: string
    data: IResourcePool[]
}

/**
 * Features
 * System features
 */
export interface IFeaturesVo {
    disabled: string[]
}

export interface IResponseMessageFeaturesVo {
    code: string
    message: string
    /** System features */
    data: IFeaturesVo
}

/**
 * Device
 * Device information
 */
export interface IDeviceVo {
    name: string
}

export interface IResponseMessageListDeviceVo {
    code: string
    message: string
    data: IDeviceVo[]
}

export interface IResponseMessageListRoleVo {
    code: string
    message: string
    data: IRoleVo[]
}

/**
 * Report
 * Report object
 */
export interface IReportVo {
    /** @format int64 */
    id: number
    uuid: string
    title: string
    content?: string
    description?: string
    shared?: boolean
    /** User object */
    owner: IUserVo
    /** @format int64 */
    createdTime: number
    /** @format int64 */
    modifiedTime: number
}

export interface IResponseMessageReportVo {
    code: string
    message: string
    /** Report object */
    data: IReportVo
}

export interface IPageInfoProjectVo {
    /** @format int64 */
    total?: number
    list?: IProjectVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoProjectVo {
    code: string
    message: string
    data: IPageInfoProjectVo
}

export interface IFileNode {
    name?: string
    signature?: string
    flag?: 'added' | 'updated' | 'deleted' | 'unchanged'
    mime?: string
    type?: 'directory' | 'file'
    desc?: string
    size?: string
}

export interface IListFilesResult {
    files?: IFileNode[]
}

export interface IResponseMessageListFilesResult {
    code: string
    message: string
    data: IListFilesResult
}

export interface IResponseMessageProjectVo {
    code: string
    message: string
    /** Project object */
    data: IProjectVo
}

export interface IPageInfoTrashVo {
    /** @format int64 */
    total?: number
    list?: ITrashVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoTrashVo {
    code: string
    message: string
    data: IPageInfoTrashVo
}

export interface ITrashVo {
    id?: string
    name?: string
    type?: string
    /** @format int64 */
    trashedTime?: number
    /** @format int64 */
    size?: number
    trashedBy?: string
    /** @format int64 */
    lastUpdatedTime?: number
    /** @format int64 */
    retentionTime?: number
}

export interface IJobTemplateVo {
    id: string
    name: string
    jobId: string
}

export interface IResponseMessageListJobTemplateVo {
    code: string
    message: string
    data: IJobTemplateVo[]
}

export interface IResponseMessageJobTemplateVo {
    code: string
    message: string
    data: IJobTemplateVo
}

export interface IPageInfoRuntimeVo {
    /** @format int64 */
    total?: number
    list?: IRuntimeVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoRuntimeVo {
    code: string
    message: string
    data: IPageInfoRuntimeVo
}

/**
 * RuntimeVersion
 * Runtime version object
 */
export interface IRuntimeVersionVo {
    tags?: string[]
    latest: boolean
    id: string
    runtimeId: string
    name: string
    alias: string
    meta?: string
    image: string
    builtImage?: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
}

/**
 * Runtime
 * Runtime object
 */
export interface IRuntimeVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    /** Runtime version object */
    version: IRuntimeVersionVo
}

/**
 * StorageFile
 * Storage file object
 */
export interface IFlattenFileVo {
    name?: string
    size?: string
}

export interface IResponseMessageRuntimeInfoVo {
    code: string
    message: string
    /** Runtime information object */
    data: IRuntimeInfoVo
}

/**
 * RuntimeInfo
 * Runtime information object
 */
export interface IRuntimeInfoVo {
    /** Runtime version object */
    versionInfo: IRuntimeVersionVo
    id: string
    name: string
    versionId: string
    versionName: string
    versionAlias: string
    versionTag?: string
    versionMeta?: string
    manifest: string
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
    files?: IFlattenFileVo[]
}

export interface IPageInfoRuntimeVersionVo {
    /** @format int64 */
    total?: number
    list?: IRuntimeVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoRuntimeVersionVo {
    code: string
    message: string
    data: IPageInfoRuntimeVersionVo
}

export interface IResponseMessageListString {
    code: string
    message: string
    data: string[]
}

export interface IResponseMessageLong {
    code: string
    message: string
    /** @format int64 */
    data: number
}

export interface IResponseMessageListRuntimeViewVo {
    code: string
    message: string
    data: IRuntimeViewVo[]
}

/**
 * Runtime
 * Runtime Version View object
 */
export interface IRuntimeVersionViewVo {
    id: string
    versionName: string
    alias: string
    latest: boolean
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
}

/**
 * Runtime
 * Runtime View object
 */
export interface IRuntimeViewVo {
    ownerName: string
    projectName: string
    runtimeId: string
    runtimeName: string
    shared: boolean
    versions: IRuntimeVersionViewVo[]
}

export interface IPageInfoReportVo {
    /** @format int64 */
    total?: number
    list?: IReportVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoReportVo {
    code: string
    message: string
    data: IPageInfoReportVo
}

export interface IContainerSpec {
    image?: string
    cmds?: string[]
    entrypoint?: string[]
}

export interface IEnv {
    name?: string
    value?: string
}

/**
 * Model
 * Model Version View Object
 */
export interface IModelVersionViewVo {
    id: string
    versionName: string
    alias: string
    latest: boolean
    tags?: string[]
    /** @format int32 */
    shared: number
    stepSpecs: IStepSpec[]
    builtInRuntime?: string
    /** @format int64 */
    createdTime: number
}

/**
 * Model View
 * Model View Object
 */
export interface IModelViewVo {
    ownerName: string
    projectName: string
    modelId: string
    modelName: string
    shared: boolean
    versions: IModelVersionViewVo[]
}

export interface IParameterSignature {
    name: string
    required?: boolean
    multiple?: boolean
}

export interface IResponseMessageListModelViewVo {
    code: string
    message: string
    data: IModelViewVo[]
}

export interface IRuntimeResource {
    type?: string
    /** @format float */
    request?: number
    /** @format float */
    limit?: number
}

export interface IStepSpec {
    name: string
    /** @format int32 */
    concurrency?: number
    /** @format int32 */
    replicas: number
    /** @format int32 */
    backoffLimit?: number
    needs?: string[]
    resources?: IRuntimeResource[]
    env?: IEnv[]
    /** @format int32 */
    expose?: number
    virtual?: boolean
    job_name?: string
    show_name: string
    require_dataset?: boolean
    container_spec?: IContainerSpec
    ext_cmd_args?: string
    parameters_sig?: IParameterSignature[]
}

/**
 * Dataset
 * Dataset Version View object
 */
export interface IDatasetVersionViewVo {
    id: string
    versionName: string
    alias?: string
    latest: boolean
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
}

/**
 * Dataset
 * Dataset View object
 */
export interface IDatasetViewVo {
    ownerName: string
    projectName: string
    datasetId: string
    datasetName: string
    shared: boolean
    versions: IDatasetVersionViewVo[]
}

export interface IResponseMessageListDatasetViewVo {
    code: string
    message: string
    data: IDatasetViewVo[]
}

/**
 * ModelVersion
 * Model version object
 */
export interface IModelVersionVo {
    latest: boolean
    tags?: string[]
    stepSpecs: IStepSpec[]
    id: string
    name: string
    alias: string
    /** @format int64 */
    size?: number
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
    builtInRuntime?: string
}

/**
 * Model
 * Model object
 */
export interface IModelVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    /** Model version object */
    version: IModelVersionVo
}

export interface IPageInfoModelVo {
    /** @format int64 */
    total?: number
    list?: IModelVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoModelVo {
    code: string
    message: string
    data: IPageInfoModelVo
}

/**
 * ModelInfo
 * Model information object
 */
export interface IModelInfoVo {
    /** Model version object */
    versionInfo: IModelVersionVo
    id: string
    name: string
    versionAlias: string
    versionId: string
    versionName: string
    versionTag?: string
    /** @format int64 */
    createdTime: number
    /** @format int32 */
    shared: number
}

export interface IResponseMessageModelInfoVo {
    code: string
    message: string
    /** Model information object */
    data: IModelInfoVo
}

export interface IPageInfoModelVersionVo {
    /** @format int64 */
    total?: number
    list?: IModelVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoModelVersionVo {
    code: string
    message: string
    data: IPageInfoModelVersionVo
}

export interface IResponseMessageMapStringListFileNode {
    code: string
    message: string
    data: Record<string, IFileNode[]>
}

/**
 * DatasetVersion
 * Dataset version object
 */
export interface IDatasetVersionVo {
    tags?: string[]
    latest: boolean
    indexTable?: string
    id: string
    name: string
    alias?: string
    meta?: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
}

/**
 * Dataset
 * Dataset object
 */
export interface IDatasetVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    /** Dataset version object */
    version: IDatasetVersionVo
}

export interface IExposedLinkVo {
    type: 'DEV_MODE' | 'WEB_HANDLER'
    name: string
    link: string
}

/**
 * Job
 * Job object
 */
export interface IJobVo {
    exposedLinks: IExposedLinkVo[]
    id: string
    uuid: string
    modelName: string
    modelVersion: string
    /** Model object */
    model: IModelVo
    jobName?: string
    datasets?: string[]
    datasetList?: IDatasetVo[]
    /** Runtime object */
    runtime: IRuntimeVo
    isBuiltinRuntime?: boolean
    device?: string
    /** @format int32 */
    deviceAmount?: number
    /** User object */
    owner: IUserVo
    /** @format int64 */
    createdTime: number
    /** @format int64 */
    stopTime?: number
    jobStatus: 'CREATED' | 'READY' | 'PAUSED' | 'RUNNING' | 'CANCELLING' | 'CANCELED' | 'SUCCESS' | 'FAIL' | 'UNKNOWN'
    comment?: string
    stepSpec?: string
    resourcePool: string
    /** @format int64 */
    duration?: number
    /** @format int64 */
    pinnedTime?: number
}

export interface IPageInfoJobVo {
    /** @format int64 */
    total?: number
    list?: IJobVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoJobVo {
    code: string
    message: string
    data: IPageInfoJobVo
}

export interface IResponseMessageJobVo {
    code: string
    message: string
    /** Job object */
    data: IJobVo
}

export interface IPageInfoTaskVo {
    /** @format int64 */
    total?: number
    list?: ITaskVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoTaskVo {
    code: string
    message: string
    data: IPageInfoTaskVo
}

export interface IRunVo {
    /** @format int64 */
    id?: number
    /** @format int64 */
    taskId?: number
    status?: 'PENDING' | 'RUNNING' | 'FINISHED' | 'FAILED'
    ip?: string
    /** @format int64 */
    startTime?: number
    /** @format int64 */
    finishTime?: number
    failedReason?: string
}

/**
 * Task
 * Task object
 */
export interface ITaskVo {
    id: string
    uuid: string
    /** @format int64 */
    startedTime?: number
    /** @format int64 */
    finishedTime?: number
    taskStatus:
        | 'CREATED'
        | 'READY'
        | 'ASSIGNING'
        | 'PAUSED'
        | 'PREPARING'
        | 'RUNNING'
        | 'RETRYING'
        | 'SUCCESS'
        | 'CANCELLING'
        | 'CANCELED'
        | 'FAIL'
        | 'UNKNOWN'
    /** @format int32 */
    retryNum?: number
    resourcePool: string
    stepName: string
    exposedLinks?: IExposedLinkVo[]
    failedReason?: string
    runs?: IRunVo[]
}

export interface IResponseMessageTaskVo {
    code: string
    message: string
    /** Task object */
    data: ITaskVo
}

export interface IResponseMessageListRunVo {
    code: string
    message: string
    data: IRunVo[]
}

export interface IEventVo {
    eventType: 'INFO' | 'WARNING' | 'ERROR'
    source: 'CLIENT' | 'SERVER' | 'NODE'
    relatedResource: IRelatedResource
    message: string
    data?: string
    /** @format int64 */
    timestamp?: number
    /** @format int64 */
    id?: number
}

export interface IResponseMessageListEventVo {
    code: string
    message: string
    data: IEventVo[]
}

export interface IGraph {
    /** @format int64 */
    id?: number
    groupingNodes?: Record<string, IGraphNode[]>
    edges?: IGraphEdge[]
}

export interface IGraphEdge {
    /** @format int64 */
    from?: number
    /** @format int64 */
    to?: number
    content?: string
}

export interface IGraphNode {
    /** @format int64 */
    id?: number
    type?: string
    content?: object
    group?: string
    /** @format int64 */
    entityId?: number
}

export interface IResponseMessageGraph {
    code: string
    message: string
    data: IGraph
}

/**
 * Evaluation
 * Evaluation View Config object
 */
export interface IConfigVo {
    name?: string
    content?: string
    /** @format int64 */
    createTime?: number
}

export interface IResponseMessageConfigVo {
    code: string
    message: string
    /** Evaluation View Config object */
    data: IConfigVo
}

export interface IPageInfoDatasetVo {
    /** @format int64 */
    total?: number
    list?: IDatasetVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoDatasetVo {
    code: string
    message: string
    data: IPageInfoDatasetVo
}

/**
 * DatasetInfo
 * SWDataset information object
 */
export interface IDatasetInfoVo {
    indexTable?: string
    /** Dataset version object */
    versionInfo?: IDatasetVersionVo
    id: string
    name: string
    versionId: string
    versionName: string
    versionAlias?: string
    versionTag?: string
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
    files?: IFlattenFileVo[]
    versionMeta: string
}

export interface IResponseMessageDatasetInfoVo {
    code: string
    message: string
    /** SWDataset information object */
    data: IDatasetInfoVo
}

export interface IPageInfoDatasetVersionVo {
    /** @format int64 */
    total?: number
    list?: IDatasetVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoDatasetVersionVo {
    code: string
    message: string
    data: IPageInfoDatasetVersionVo
}

export interface IBuildRecordVo {
    id: string
    projectId: string
    taskId: string
    datasetName: string
    status:
        | 'CREATED'
        | 'READY'
        | 'ASSIGNING'
        | 'PAUSED'
        | 'PREPARING'
        | 'RUNNING'
        | 'RETRYING'
        | 'SUCCESS'
        | 'CANCELLING'
        | 'CANCELED'
        | 'FAIL'
        | 'UNKNOWN'
    type: 'IMAGE' | 'VIDEO' | 'AUDIO'
    /** @format int64 */
    createTime: number
}

export interface IPageInfoBuildRecordVo {
    /** @format int64 */
    total?: number
    list?: IBuildRecordVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoBuildRecordVo {
    code: string
    message: string
    data: IPageInfoBuildRecordVo
}

/**
 * Model Serving Status
 * Model Serving Status object
 */
export interface IModelServingStatusVo {
    /** @format int32 */
    progress?: number
    events?: string
}

export interface IResponseMessageModelServingStatusVo {
    code: string
    message: string
    /** Model Serving Status object */
    data: IModelServingStatusVo
}

export interface IPageInfoPanelPluginVo {
    /** @format int64 */
    total?: number
    list?: IPanelPluginVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IPanelPluginVo {
    id: string
    name: string
    version: string
}

export interface IResponseMessagePageInfoPanelPluginVo {
    code: string
    message: string
    data: IPageInfoPanelPluginVo
}

export interface IResponseMessageRuntimeSuggestionVo {
    code: string
    message: string
    /** Model Serving object */
    data: IRuntimeSuggestionVo
}

/**
 * Model Serving
 * Model Serving object
 */
export interface IRuntimeSuggestionVo {
    runtimes?: IRuntimeVersionVo[]
}

export interface IUserRoleDeleteRequest {
    currentUserPwd: string
}

export interface IFileDeleteRequest {
    pathPrefix: string
    /** @uniqueItems true */
    files: string[]
}

export type IUpdateUserStateData = IResponseMessageString

export type IUpdateUserPwdData = IResponseMessageString

export type IUpdateCurrentUserPasswordData = IResponseMessageString

export type ICheckCurrentUserPasswordData = IResponseMessageString

export type IUpdateUserSystemRoleData = IResponseMessageString

export type IDeleteUserSystemRoleData = IResponseMessageString

export type IGetProjectByUrlData = IResponseMessageProjectVo

export type IUpdateProjectData = IResponseMessageString

export type IDeleteProjectByUrlData = IResponseMessageString

export type IRecoverTrashData = IResponseMessageString

export type IDeleteTrashData = IResponseMessageString

export type IUpdateRuntimeData = IResponseMessageObject

export type IModifyRuntimeData = IResponseMessageString

export type IShareRuntimeVersionData = IResponseMessageString

export type IRecoverRuntimeData = IResponseMessageString

export type IModifyProjectRoleData = IResponseMessageString

export type IDeleteProjectRoleData = IResponseMessageString

export type IGetReportData = IResponseMessageReportVo

export type IModifyReportData = IResponseMessageString

export type IDeleteReportData = IResponseMessageString

export type ISharedReportData = IResponseMessageString

export type IModifyModelData = IResponseMessageString

export type IHeadModelData = object

export type IShareModelVersionData = IResponseMessageString

export type IRecoverModelData = IResponseMessageString

export type IGetJobData = IResponseMessageJobVo

export type IModifyJobCommentData = IResponseMessageString

export type IRemoveJobData = IResponseMessageString

export type IShareDatasetVersionData = IResponseMessageString

export type IRecoverDatasetData = IResponseMessageString

export type IRecoverProjectData = IResponseMessageString

export type IApplySignedGetUrlsData = IResponseMessageSignedUrlResponse

export type IApplySignedPutUrlsData = IResponseMessageSignedUrlResponse

export type IListUserData = IResponseMessagePageInfoUserVo

export type ICreateUserData = IResponseMessageString

export type IInstanceStatusData = IResponseMessageString

export type IQuerySettingData = IResponseMessageString

export type IUpdateSettingData = IResponseMessageString

export type IListResourcePoolsData = IResponseMessageListResourcePool

export type IUpdateResourcePoolsData = IResponseMessageString

export type IListSystemRolesData = IResponseMessageListProjectMemberVo

export type IAddUserSystemRoleData = IResponseMessageString

export type IListProjectData = IResponseMessagePageInfoProjectVo

export type ICreateProjectData = IResponseMessageString

export type ICreateModelVersionData = any

export type ISelectAllInProjectData = IResponseMessageListJobTemplateVo

export type IAddTemplateData = IResponseMessageString

export type ICreateModelServingData = IResponseMessageModelServingVo

export type IListRuntimeVersionTagsData = IResponseMessageListString

export type IAddRuntimeVersionTagData = IResponseMessageString

export type IBuildRuntimeImageData = IResponseMessageBuildImageResult

export type IRevertRuntimeVersionData = IResponseMessageString

export type IUploadData = IResponseMessageString

export type IListProjectRoleData = IResponseMessageListProjectMemberVo

export type IAddProjectRoleData = IResponseMessageString

export type IListReportsData = IResponseMessagePageInfoReportVo

export type ICreateReportData = IResponseMessageString

export type ITransferData = IResponseMessageString

export type IListModelVersionTagsData = IResponseMessageListString

export type IAddModelVersionTagData = IResponseMessageString

export type IRevertModelVersionData = IResponseMessageString

export type IListJobsData = IResponseMessagePageInfoJobVo

export type ICreateJobData = IResponseMessageString

export type IActionData = IResponseMessageString

export type IExecData = IResponseMessageExecResponse

export type IRecoverJobData = IResponseMessageString

export type IModifyJobPinStatusData = IResponseMessageString

export type IGetEventsData = IResponseMessageListEventVo

export type IAddEventData = IResponseMessageString

export type ISignLinksData = IResponseMessageMapStringString

export type IGetHashedBlobData = any

export type IUploadHashedBlobData = IResponseMessageString

export type IHeadHashedBlobData = object

export type IGetViewConfigData = IResponseMessageConfigVo

export type ICreateViewConfigData = IResponseMessageString

export type IListDatasetVersionTagsData = IResponseMessageListString

export type IAddDatasetVersionTagData = IResponseMessageString

export type IConsumeNextDataData = INullableResponseMessageDataIndexDesc

export type IRevertDatasetVersionData = IResponseMessageString

export type IUploadDsData = IResponseMessageUploadResult

export type IBuildDatasetData = IResponseMessageString

export type ISignLinks1Data = IResponseMessageMapObjectObject

export type IGetHashedBlob1Data = any

export type IUploadHashedBlob1Data = IResponseMessageString

export type IHeadHashedBlob1Data = object

export type IGetPanelSettingData = IResponseMessageString

export type ISetPanelSettingData = IResponseMessageString

export type IPluginListData = IResponseMessagePageInfoPanelPluginVo

export type IInstallPluginData = IResponseMessageString

export type ISignLinks2Data = IResponseMessageMapStringString

export type IUpdateTableData = IResponseMessageString

export type IScanTableData = IResponseMessageRecordListVo

export type IScanAndExportData = any

export type IQueryTableData = IResponseMessageRecordListVo

export type IQueryAndExportData = any

export type IListTablesData = IResponseMessageTableNameListVo

export type IFlushData = IResponseMessageString

export type IInitUploadBlobData = IResponseMessageInitUploadBlobResult

export type ICompleteUploadBlobData = IResponseMessageCompleteUploadBlobResult

export type IHeadRuntimeData = object

export type IHeadDatasetData = object

export type IGetUserByIdData = IResponseMessageUserVo

export type IUserTokenData = IResponseMessageString

export type IGetCurrentUserData = IResponseMessageUserVo

export type IGetCurrentUserRolesData = IResponseMessageListProjectMemberVo

export type IGetCurrentVersionData = IResponseMessageSystemVersionVo

export type IQueryFeaturesData = IResponseMessageFeaturesVo

export type IListDeviceData = IResponseMessageListDeviceVo

export type IListRolesData = IResponseMessageListRoleVo

export type IPreviewData = IResponseMessageReportVo

export type IGetModelMetaBlobData = IResponseMessageString

export type IListFilesData = IResponseMessageListFilesResult

/** @format binary */
export type IGetFileDataData = File

export type IListTrashData = IResponseMessagePageInfoTrashVo

export type IGetTemplateData = IResponseMessageJobTemplateVo

export type IDeleteTemplateData = IResponseMessageString

export type IListRuntimeData = IResponseMessagePageInfoRuntimeVo

export type IGetRuntimeInfoData = IResponseMessageRuntimeInfoVo

export type IDeleteRuntimeData = IResponseMessageString

export type IListRuntimeVersionData = IResponseMessagePageInfoRuntimeVersionVo

export type IPullData = any

export type IGetRuntimeVersionTagData = IResponseMessageLong

export type IListRuntimeTreeData = IResponseMessageListRuntimeViewVo

export type ISelectRecentlyInProjectData = IResponseMessageListJobTemplateVo

export type IRecentRuntimeTreeData = IResponseMessageListRuntimeViewVo

export type IRecentModelTreeData = IResponseMessageListModelViewVo

export type IRecentDatasetTreeData = IResponseMessageListDatasetViewVo

export type IGetProjectReadmeByUrlData = IResponseMessageString

export type IListModelData = IResponseMessagePageInfoModelVo

export type IGetModelInfoData = IResponseMessageModelInfoVo

export type IDeleteModelData = IResponseMessageString

export type IListModelVersionData = IResponseMessagePageInfoModelVersionVo

export type IGetModelVersionTagData = IResponseMessageLong

export type IGetModelDiffData = IResponseMessageMapStringListFileNode

export type IListModelTreeData = IResponseMessageListModelViewVo

export type IListTasksData = IResponseMessagePageInfoTaskVo

export type IGetTaskData = IResponseMessageTaskVo

export type IGetRunsData = IResponseMessageListRunVo

export type IGetJobDagData = IResponseMessageGraph

export type IListDatasetData = IResponseMessagePageInfoDatasetVo

export type IGetDatasetInfoData = IResponseMessageDatasetInfoVo

export type IDeleteDatasetData = IResponseMessageString

export type IListDatasetVersionData = IResponseMessagePageInfoDatasetVersionVo

export type IPullDsData = any

export type IGetDatasetVersionTagData = IResponseMessageLong

export type IListBuildRecordsData = IResponseMessagePageInfoBuildRecordVo

export type IListDatasetTreeData = IResponseMessageListDatasetViewVo

export type IPullUriContentData = any

export type IGetModelServingStatusData = IResponseMessageModelServingStatusVo

export type IOfflineLogsData = IResponseMessageListString

export type ILogContentData = string

export type IGetRuntimeSuggestionData = IResponseMessageRuntimeSuggestionVo

export type IApplyPathPrefixData = IResponseMessageString

export type IPullUriContent1Data = any

export type IDeletePathData = IResponseMessageString

export type IDeleteRuntimeVersionTagData = IResponseMessageString

export type IDeleteModelVersionTagData = IResponseMessageString

export type IDeleteDatasetVersionTagData = IResponseMessageString

export type IUninstallPluginData = IResponseMessageString
