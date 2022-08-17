package ai.starwhale.mlops.objectstore.impl;

import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileSystemObjectStoreTest {
    @TempDir
    private File rootDir;

    private SwBufferManager bufferManager;

    private FileSystemObjectStore objectStore;

    @BeforeEach
    public void setUp() {
        this.bufferManager = new SwByteBufferManager();
        this.objectStore = new FileSystemObjectStore(this.bufferManager, this.rootDir.getAbsolutePath());
    }

    @Test
    public void testAll() throws IOException {
        var buf = this.bufferManager.allocate(100);
        buf.setString(0, "c:t1");
        this.objectStore.put("t1", buf.slice(0, 4));
        buf.setString(0, "c:t2");
        this.objectStore.put("t2", buf.slice(0, 4));
        buf.setString(0, "c:t/t3");
        this.objectStore.put("t/t3", buf.slice(0, 6));
        buf.setString(0, "c:d/a");
        this.objectStore.put("d/a", buf.slice(0, 5));
        assertThat(ImmutableList.copyOf(this.objectStore.list("t")), is(List.of("t/t3", "t1", "t2")));
        assertThat(this.objectStore.get("t1").asByteBuffer(),
                is(ByteBuffer.wrap("c:t1".getBytes(StandardCharsets.UTF_8))));
        assertThat(this.objectStore.get("t/t3").asByteBuffer(),
                is(ByteBuffer.wrap("c:t/t3".getBytes(StandardCharsets.UTF_8))));
    }

}
