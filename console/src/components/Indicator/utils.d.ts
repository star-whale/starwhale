export interface IRocAuc {
    fpr: number[];
    tpr: number[];
    thresholds: number[];
    auc: number;
}
export declare function getRocAucConfig(title: string | undefined, labels: string[], data: {
    fpr: IRocAuc['fpr'][];
    tpr: IRocAuc['tpr'][];
}): {
    data: ({
        x: number[][];
        y: number[][];
        mode: string;
        name: string;
        type: string;
        line?: undefined;
    } | {
        x: number[];
        y: number[];
        mode: string;
        name: string;
        line: {
            dash: string;
            width: number;
        };
        type?: undefined;
    })[];
    layout: {
        title: string;
        xaxis: {
            title: string;
            autotick: boolean;
            ticks: string;
            tickcolor: string;
            tickwidth: number;
            ticklen: number;
            tickfont: {
                family: string;
                size: number;
                color: string;
            };
        };
        yaxis: {
            title: string;
            autotick: boolean;
            ticks: string;
            tickcolor: string;
            tickwidth: number;
            ticklen: number;
            tickfont: {
                family: string;
                size: number;
                color: string;
            };
        };
        autosize: boolean;
        annotations: any[];
        font: {
            family: string;
            size: number;
        };
    };
};
export declare function getHeatmapConfig(title: string | undefined, labels: string[], heatmap: number[][]): {
    data: {
        x: string[];
        y: string[];
        z: number[][];
        colorscale: (string | number)[][];
        type: string;
    }[];
    layout: {
        title: string;
        autosize: boolean;
        annotations: any[];
        xaxis: {
            autotick: boolean;
            ticks: string;
            tickcolor: string;
            tickwidth: number;
            ticklen: number;
            tickfont: {
                family: string;
                size: number;
                color: string;
            };
        };
        yaxis: {
            autotick: boolean;
            ticks: string;
            tickcolor: string;
            tickwidth: number;
            ticklen: number;
            tickfont: {
                family: string;
                size: number;
                color: string;
            };
        };
        font: {
            family: string;
            size: number;
        };
    };
};
