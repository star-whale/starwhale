package ai.starwhale.mlops.domain.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.api.protocol.project.StatisticsVO;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectConvertorTest {

    private ProjectConvertor projectConvertor;

    @BeforeEach
    public void setUp() {
        IDConvertor idConvertor = new IDConvertor();
        LocalDateTimeConvertor localDateTimeConvertor = new LocalDateTimeConvertor();
        UserConvertor userConvertor = new UserConvertor(idConvertor, localDateTimeConvertor);
        projectConvertor = new ProjectConvertor(idConvertor, userConvertor, localDateTimeConvertor);
    }

    @Test
    public void testConvert() {
        var res = projectConvertor.convert(null);
        assertThat(res ,allOf(
            notNullValue(),
            is(hasProperty("id", emptyString())),
            is(hasProperty("name", emptyString())),
            is(hasProperty("privacy", is("PRIVATE"))),
            is(hasProperty("owner", isA(UserVO.class))),
            is(hasProperty("statistics", isA(StatisticsVO.class)))
        ));

        res = projectConvertor.convert(ProjectEntity.builder().id(0L).build());
        assertThat(res ,allOf(
            notNullValue(),
            is(hasProperty("id", is("0"))),
            is(hasProperty("name", is("SYSTEM"))),
            is(hasProperty("privacy", is("PUBLIC"))),
            is(hasProperty("owner", isA(UserVO.class))),
            is(hasProperty("statistics", isA(StatisticsVO.class)))
        ));

        res = projectConvertor.convert(ProjectEntity.builder()
                .id(1L)
                .projectName("p1")
                .privacy(1)
                .description("project for test")
                .build());
        assertThat(res ,allOf(
            notNullValue(),
            is(hasProperty("id", is("1"))),
            is(hasProperty("name", is("p1"))),
            is(hasProperty("privacy", is("PUBLIC"))),
            is(hasProperty("description", is("project for test"))),
            is(hasProperty("owner", isA(UserVO.class))),
            is(hasProperty("statistics", isA(StatisticsVO.class)))
        ));

        res = projectConvertor.convert(ProjectEntity.builder()
            .id(2L)
            .projectName("p2")
            .build());

        assertThat(res ,allOf(
            notNullValue(),
            is(hasProperty("id", is("2"))),
            is(hasProperty("name", is("p2"))),
            is(hasProperty("privacy", is("PRIVATE"))),
            is(hasProperty("owner", isA(UserVO.class))),
            is(hasProperty("statistics", isA(StatisticsVO.class)))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(NullPointerException.class,
            () -> projectConvertor.revert(null));

        var res = projectConvertor.revert(ProjectVO.builder()
                .id("1")
                .name("p1")
                .description("project for test")
                .build());
        assertThat(res, allOf(
            notNullValue(),
            is(hasProperty("id", is(1L))),
            is(hasProperty("projectName", is("p1"))),
            is(hasProperty("description", is("project for test")))
        ));
    }

}
