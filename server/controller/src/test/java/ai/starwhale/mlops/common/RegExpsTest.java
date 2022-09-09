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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class RegExpsTest {

    @Test
    public void testUserNameRegex() {
        Pattern pattern = Pattern.compile(RegExps.USER_NAME_REGEX);
        assertThat(pattern.matcher("star_whale").matches(), is(true));
        assertThat(pattern.matcher("star").matches(), is(true));
        assertThat(pattern.matcher("sta").matches(), is(false));
        assertThat(pattern.matcher("star123whale").matches(), is(true));
        assertThat(pattern.matcher("5starwhale").matches(), is(false));
        assertThat(pattern.matcher("_starwhale").matches(), is(false));
        assertThat(pattern.matcher("sta%$#rwhale").matches(), is(false));
        assertThat(pattern.matcher("star_whale_star_whale_star_whale_star_whale_star_whale")
                .matches(), is(false));
    }

    @Test
    public void testProjectNameRegex() {
        Pattern pattern = Pattern.compile(RegExps.PROJECT_NAME_REGEX);
        assertThat(pattern.matcher("star_whale").matches(), is(true));
        assertThat(pattern.matcher("star").matches(), is(true));
        assertThat(pattern.matcher("sta").matches(), is(true));
        assertThat(pattern.matcher("star123whale").matches(), is(true));
        assertThat(pattern.matcher("5starwhale").matches(), is(false));
        assertThat(pattern.matcher("_starwhale").matches(), is(false));
        assertThat(pattern.matcher("sta%$#rwhale").matches(), is(false));
        assertThat(pattern.matcher("star_whale_star_whale_star_whale_star_whale_star_whale")
                .matches(), is(true));
        assertThat(pattern.matcher("star_whale_star_whale_star_whale_star_whale_star_whale"
                        + "_star_whale_star_whale_star_whale_star_whale_star_whale")
                .matches(), is(false));
    }

    @Test
    public void testBundleNameRegex() {
        Pattern pattern = Pattern.compile(RegExps.BUNDLE_NAME_REGEX);
        assertThat(pattern.matcher("star_whale").matches(), is(true));
        assertThat(pattern.matcher("star").matches(), is(true));
        assertThat(pattern.matcher("sta").matches(), is(true));
        assertThat(pattern.matcher("star123whale").matches(), is(true));
        assertThat(pattern.matcher("5starwhale").matches(), is(false));
        assertThat(pattern.matcher("_starwhale").matches(), is(false));
        assertThat(pattern.matcher("sta%$#rwhale").matches(), is(false));
        assertThat(pattern.matcher("star_whale_star_whale_star_whale_star_whale_star_whale")
                .matches(), is(true));
        assertThat(pattern.matcher("star_whale_star_whale_star_whale_star_whale_star_whale"
                        + "_star_whale_star_whale_star_whale_star_whale_star_whale")
                .matches(), is(false));
    }
}
