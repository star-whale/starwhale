package ai.starwhale.mlops.resulting.chart;

import ai.starwhale.mlops.configuration.json.DecimalJsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

/**
 * data point in a chart
 */
@Data
public class ChartData {

    Long id;
    /**
     * which chart does this data belong to
     */
    Long chartId;

    /**
     * where this data locates on X axis
     */
    @JsonSerialize(using = DecimalJsonSerializer.class)
    Double valueX;

    /**
     * where this data locates on Y axis
     */
    @JsonSerialize(using = DecimalJsonSerializer.class)
    Double valueY;

    /**
     * extra display info of the data
     */
    String display;

}
