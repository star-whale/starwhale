package ai.starwhale.mlops.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public final class TarFileUtil {
    /**
     * Find the contents of a specified file in a tar file.
     */
    public static byte[] getContentFromTarFile(InputStream tarFileInputStream, String targetFilePath, String targetFileName) {
        ArchiveInputStream archiveInputStream = null;
        try {
            archiveInputStream = getArchiveInputStream(tarFileInputStream);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                if (entry.getSize() <= 0) {
                    continue;
                }
                if (!StringUtils.isEmpty(targetFilePath) && !entry.getName().startsWith(targetFilePath)) {
                    continue;
                }
                if (!StringUtils.isEmpty(targetFileName) && !entry.getName().endsWith(targetFileName)) {
                    continue;
                }
                return getContent(archiveInputStream);
            }

        } catch (Exception e) {
            log.error("get tar file failed!", e);
        } finally {
            if (null != archiveInputStream) {
                try {
                    archiveInputStream.close();
                } catch (IOException e) {
                    log.error("file close error!", e);
                }
            }
        }

        return null;
    }

    private static ArchiveInputStream getArchiveInputStream(InputStream in) throws ArchiveException {
        return new ArchiveStreamFactory()
            .createArchiveInputStream("tar", new BufferedInputStream(in));
    }

    public static byte[] getContent(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int len = is.read(buffer);

                if (len == -1) {
                    break;
                }

                baos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();

    }

}
