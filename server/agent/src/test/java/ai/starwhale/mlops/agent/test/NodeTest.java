package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.node.UniqueID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class NodeTest {
    @Test
    public void uniqueIDTest() throws IOException {

        UniqueID uniqueID = new UniqueID("/opt/starwhale");
        Assertions.assertTrue(StringUtils.isNotEmpty(uniqueID.id()));
        // clear local dir
        FileUtils.forceDeleteOnExit(new File("/opt/starwhale" + File.separator + UniqueID.UNIQUE_FILE));
    }
}
