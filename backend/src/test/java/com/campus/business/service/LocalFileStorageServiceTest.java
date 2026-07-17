package com.campus.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campus.business.common.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageServiceTest {
  @TempDir Path directory;

  @Test
  void usesDetectedJpegExtensionInsteadOfClientFilename() throws Exception {
    LocalFileStorageService storage = new LocalFileStorageService(directory.toString());
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "attack.html",
            "text/html",
            new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01});

    String url = storage.store(file);

    assertThat(url).endsWith(".jpg");
    assertThat(Files.exists(directory.resolve(url.substring("/uploads/".length())))).isTrue();
  }

  @Test
  void rejectsSvgAndHtmlEvenWhenContentTypeClaimsImage() {
    LocalFileStorageService storage = new LocalFileStorageService(directory.toString());
    MockMultipartFile svg =
        new MockMultipartFile(
            "file", "image.png", "image/png", "<svg><script>alert(1)</script></svg>".getBytes());

    assertThatThrownBy(() -> storage.store(svg))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("JPEG、PNG或WebP");
  }

  @Test
  void recognizesPngAndWebpMagicBytes() {
    LocalFileStorageService storage = new LocalFileStorageService(directory.toString());
    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    byte[] webp = new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    assertThat(storage.store(new MockMultipartFile("file", "x", null, png))).endsWith(".png");
    assertThat(storage.store(new MockMultipartFile("file", "x", null, webp))).endsWith(".webp");
  }
}
