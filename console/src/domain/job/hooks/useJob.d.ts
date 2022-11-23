/// <reference types="react" />
export declare const useJob: () => {
    job: import("../schemas/job").IJobSchema | undefined;
    setJob: (u: import("react").SetStateAction<import("../schemas/job").IJobSchema | undefined>) => void;
};
export declare const useJobLoading: () => {
    jobLoading: boolean;
    setJobLoading: (u: import("react").SetStateAction<boolean>) => void;
};
