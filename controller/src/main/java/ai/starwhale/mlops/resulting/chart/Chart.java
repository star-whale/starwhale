package ai.starwhale.mlops.resulting.chart;

import lombok.Data;

/**
 * 2d chart
 */
@Data
public class Chart {

    Long id;

    /**
     * chart title
     */
    String title;

    /**
     * X axis description
     */
    String descAxisX;

    /**
     * unit on X axis description
     */
    String descUnitX;

    /**
     * Y axis description
     */
    String descAxisY;

    /**
     * unit on Y axis description
     */
    String descUnitY;
}
