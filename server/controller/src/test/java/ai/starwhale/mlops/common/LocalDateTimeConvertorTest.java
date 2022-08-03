/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.common;

import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link LocalDateTimeConvertor}
 */
public class LocalDateTimeConvertorTest {

    static final Long timeStamp = 1655446440000L;
    static final LocalDateTime utcTime = LocalDateTime.of(2022, 6, 17, 6, 14);
    static final LocalDateTime shanghaiTime = LocalDateTime.of(2022, 6, 17, 14, 14);

    @Test
    public void testLocalDateTimeConvertorGMT(){
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        LocalDateTimeConvertor localDateTimeConvertor = new LocalDateTimeConvertor();
        Long convertedTimestamp = localDateTimeConvertor.convert(utcTime);
        Assertions.assertEquals(timeStamp,convertedTimestamp);
        LocalDateTime revert = localDateTimeConvertor.revert(convertedTimestamp);
        Long convert = localDateTimeConvertor.convert(revert);
        Assertions.assertEquals(timeStamp,convert);
    }

    @Test
    public void testLocalDateTimeConvertorCN(){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        LocalDateTimeConvertor localDateTimeConvertor = new LocalDateTimeConvertor();
        Long convertedTimestamp = localDateTimeConvertor.convert(shanghaiTime);
        Assertions.assertEquals(timeStamp,convertedTimestamp);
        LocalDateTime revert = localDateTimeConvertor.revert(convertedTimestamp);
        Long convert = localDateTimeConvertor.convert(revert);
        Assertions.assertEquals(timeStamp,convert);
    }
}
