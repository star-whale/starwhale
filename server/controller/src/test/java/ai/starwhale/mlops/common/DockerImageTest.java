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

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DockerImageTest {

    @Test
    public void testGhcrConstructor() {
        Assertions.assertEquals(
                new DockerImage("ghcr.io/star-whale//", "starwhale:latest").toString(),
                "ghcr.io/star-whale/starwhale:latest");

        DockerImage dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("ghcr.io/star-whale", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("ghcr.io/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("ghcr.io/star-whale", "starwhale"), dockerImage);
    }

    @Test
    public void testLocalhostPortConstructor() {
        DockerImage dockerImage = new DockerImage(
                "localhost:8083/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("localhost:8083/star-whale", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("localhost:8083/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("localhost:8083/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("localhost:8083/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("localhost:8083/star-whale", "starwhale"), dockerImage);
    }

    @Test
    public void testLocalhostConstructor() {
        DockerImage dockerImage = new DockerImage(
                "localhost/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("localhost/star-whale", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("localhost/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("localhost/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("localhost/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("localhost/star-whale", "starwhale"), dockerImage);
    }

    @Test
    public void testDockerConstructor() {
        DockerImage dockerImage = new DockerImage(
                "docker.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("docker.io/starwhaleai", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("ghcr.io/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("ghcr.io/star-whale", "starwhale"), dockerImage);

    }

    @Test
    public void testHostPortConstructor() {
        DockerImage dockerImage = new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale",
                    "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"), dockerImage);

        dockerImage = new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:latest");
        Assertions.assertEquals(
                new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale");
        Assertions.assertEquals(
                new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale", "starwhale"), dockerImage);

        dockerImage = new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale",
                "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"), dockerImage);
    }

    @Test
    public void testIpPortConstructor() {
        DockerImage dockerImage = new DockerImage(
                "131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("131.0.1.8:5000/star-whale", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("131.0.1.8:5000/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("131.0.1.8:5000/star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("131.0.1.8:5000/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("131.0.1.8:5000/star-whale", "starwhale"), dockerImage);
    }

    @Test
    public void testOnlyNameConstructor() {
        var dockerImage = new DockerImage("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("star-whale", "starwhale:0.3.0-rc.6-nightly-20220920-016d5507"), dockerImage);

        dockerImage = new DockerImage("star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("star-whale", "starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("star-whale", "starwhale"), dockerImage);

    }

    @Test
    public void testResolve() {
        Map<String, String> images = Map.of(
                "docker.io/star-whale", "docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "docker.io/star-whale/", "docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "ghcr.io/star-whale", "ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "ghcr.io/star-whale/", "ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "131.0.1.8:5000/star-whale", "131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "131.0.1.8:5000/star-whale/",
                "131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "homepage-ca.intra.starwhale.ai:5000/star-whale",
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "homepage-ca.intra.starwhale.ai:5000/star-whale/",
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                "localhost:5000/star-whale", "localhost:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"
        );
        images.forEach((k, v) -> Assertions.assertEquals(v,
            new DockerImage("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507").resolve(k)));

        images.forEach((k, v) -> Assertions.assertEquals(v,
            new DockerImage("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507").resolve(k)));

        images.forEach((k, v) -> Assertions.assertEquals(v,
            new DockerImage("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507").resolve(k)));

        images.forEach((k, v) -> Assertions.assertEquals(v,
            new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507")
                .resolve(k)
        ));

        images.forEach((k, v) -> Assertions.assertEquals(v, new DockerImage(v).resolve("")));

    }


}
