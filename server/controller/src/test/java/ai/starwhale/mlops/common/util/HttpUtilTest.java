package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.common.util.HttpUtil.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpUtilTest {

    @Test
    public void testGetResourceUrlFromPath() {
        Assertions.assertEquals("project_test_1",
            HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1", Resources.PROJECT));
        Assertions.assertEquals("project_test_1",
            HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1/model/1", Resources.PROJECT));
        Assertions.assertEquals("project_test_1",
            HttpUtil.getResourceUrlFromPath("/project/project_test_1?pageSize=1", Resources.PROJECT));
        Assertions.assertNull(
            HttpUtil.getResourceUrlFromPath("/project/project_test_1?pageSize=1", Resources.RUNTIME));
        Assertions.assertEquals("1",
            HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1/model/1", Resources.MODEL));
    }
}
