package ai.starwhale.mlops.configuration.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * format double value when exposed to web. For example: 1.000 -> 1
 */
public class DecimalJsonSerializer extends JsonSerializer<Double> {
    @Override
    public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        if(null == value){
            jgen.writeNull();
            return;
        }
        if (value % 1 == 0) {
            jgen.writeNumber(value.intValue());
        } else {
            jgen.writeNumber(value);
        }

    }
}
