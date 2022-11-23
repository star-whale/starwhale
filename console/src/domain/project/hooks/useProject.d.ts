/// <reference types="react" />
export declare const useProject: () => {
    project: import("../schemas/project").IProjectSchema | undefined;
    setProject: (u: import("react").SetStateAction<import("../schemas/project").IProjectSchema | undefined>) => void;
};
export declare const useProjectLoading: () => {
    projectLoading: boolean;
    setProjectLoading: (u: import("react").SetStateAction<boolean>) => void;
};
