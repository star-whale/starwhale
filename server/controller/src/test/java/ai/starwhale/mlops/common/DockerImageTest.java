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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DockerImageTest {

    @Test
    public void testGhcrConstructor() {
        DockerImage dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage("ghcr.io", "star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("ghcr.io", "star-whale/starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("ghcr.io", "star-whale/starwhale"), dockerImage);
    }

    @Test
    public void testDockerConstructor() {
        DockerImage dockerImage = new DockerImage(
                "docker.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("docker.io", "starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("ghcr.io", "star-whale/starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("ghcr.io", "star-whale/starwhale"), dockerImage);

    }

    @Test
    public void testHostPortConstructor() {
        DockerImage dockerImage = new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000",
                "star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"), dockerImage);

        dockerImage = new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000", "star-whale/starwhale:latest"),
                dockerImage);

        dockerImage = new DockerImage("homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000", "star-whale/starwhale"),
                dockerImage);

        dockerImage = new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage("homepage-ca.intra.starwhale.ai:5000",
                "star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"), dockerImage);
    }

    @Test
    public void testIpPortConstructor() {
        DockerImage dockerImage = new DockerImage(
                "131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(
                new DockerImage("131.0.1.8:5000", "star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("131.0.1.8:5000/star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage("131.0.1.8:5000", "star-whale/starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("131.0.1.8:5000/star-whale/starwhale");
        Assertions.assertEquals(new DockerImage("131.0.1.8:5000", "star-whale/starwhale"), dockerImage);
    }

    @Test
    public void testOnlyNameConstructor() {
        DockerImage dockerImage = new DockerImage("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals(new DockerImage(null, "star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507"),
                dockerImage);

        dockerImage = new DockerImage("star-whale/starwhale:latest");
        Assertions.assertEquals(new DockerImage(null, "star-whale/starwhale:latest"), dockerImage);

        dockerImage = new DockerImage("star-whale/starwhale");
        Assertions.assertEquals(new DockerImage(null, "star-whale/starwhale"), dockerImage);

    }

    @Test
    public void testResolve() {
        DockerImage dockerImage = new DockerImage(
                "docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507", dockerImage.resolve(null));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io"));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io/"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000/"));

        dockerImage = new DockerImage("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507", dockerImage.resolve(null));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io"));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io/"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000/"));

        dockerImage = new DockerImage("starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals("starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve(null));
        Assertions.assertEquals("docker.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io"));
        Assertions.assertEquals("docker.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io/"));
        Assertions.assertEquals("ghcr.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io"));
        Assertions.assertEquals("ghcr.io/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io/"));
        Assertions.assertEquals("131.0.1.8:5000/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000/"));
        Assertions.assertEquals("131.0.1.8:5000/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/starwhaleai/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000/"));

        dockerImage = new DockerImage(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507");
        Assertions.assertEquals("star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507", dockerImage.resolve(null));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io"));
        Assertions.assertEquals("docker.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("docker.io/"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io"));
        Assertions.assertEquals("ghcr.io/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("ghcr.io/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000/"));
        Assertions.assertEquals("131.0.1.8:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("131.0.1.8:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000"));
        Assertions.assertEquals(
                "homepage-ca.intra.starwhale.ai:5000/star-whale/starwhale:0.3.0-rc.6-nightly-20220920-016d5507",
                dockerImage.resolve("homepage-ca.intra.starwhale.ai:5000/"));

    }

}
