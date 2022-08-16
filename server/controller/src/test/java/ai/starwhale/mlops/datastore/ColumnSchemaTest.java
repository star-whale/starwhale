package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.exception.SWValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ColumnSchemaTest {

    private ColumnSchema schema;

    @Test
    public void testConstructor() {
        new ColumnSchema(new ColumnSchemaDesc("k", "STRING"));
    }

    @Test
    public void testConstructorException() {
        assertThrows(NullPointerException.class, () -> new ColumnSchema(null), "null schema");

        assertThrows(SWValidationException.class,
                () -> new ColumnSchema(new ColumnSchemaDesc(null, "STRING")),
                "null column name");

        assertThrows(SWValidationException.class,
                () -> new ColumnSchema(new ColumnSchemaDesc("k", null)),
                "null type");

        assertThrows(SWValidationException.class,
                () -> new ColumnSchema(new ColumnSchemaDesc("k", "invalid")),
                "invalid type");
    }
}
