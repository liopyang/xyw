package com.campus.business.service;

import com.campus.business.common.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ObjectMediaStorageService {
  private static final long MAX_SIZE = 5L * 1024 * 1024;
  private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private final MinioClient client;
  private final String bucket;
  private final boolean configured;

  public ObjectMediaStorageService(
      @Value("${campus.object-storage.endpoint:}") String endpoint,
      @Value("${campus.object-storage.access-key:}") String key,
      @Value("${campus.object-storage.secret-key:}") String secret,
      @Value("${campus.object-storage.bucket:}") String bucket) {
    this.bucket = bucket;
    this.configured =
        !endpoint.isBlank() && !key.isBlank() && !secret.isBlank() && !bucket.isBlank();
    this.client =
        configured
            ? MinioClient.builder().endpoint(endpoint).credentials(key, secret).build()
            : null;
  }

  public StoredObject store(MultipartFile file, String prefix) {
    if (!configured) throw new BusinessException("对象存储尚未配置");
    if (file == null || file.isEmpty()) throw new BusinessException("图片为空");
    if (file.getSize() > MAX_SIZE) throw new BusinessException("图片不能超过5MB");
    String type = file.getContentType();
    if (!TYPES.contains(type)) throw new BusinessException("只允许JPEG、PNG或WebP图片");
    verifyMagic(file, type);
    String ext =
        switch (type) {
          case "image/jpeg" -> ".jpg";
          case "image/png" -> ".png";
          default -> ".webp";
        };
    String key = prefix + "/" + UUID.randomUUID() + ext;
    try (InputStream in = file.getInputStream()) {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(key).stream(in, file.getSize(), -1)
              .contentType(type)
              .build());
      return new StoredObject(
          key,
          file.getOriginalFilename() == null ? "image" + ext : file.getOriginalFilename(),
          type,
          file.getSize());
    } catch (Exception e) {
      throw new BusinessException("对象存储上传失败");
    }
  }

  public String temporaryUrl(String key) {
    if (!configured) throw new BusinessException("对象存储尚未配置");
    try {
      return client.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucket)
              .object(key)
              .expiry(10, TimeUnit.MINUTES)
              .build());
    } catch (Exception e) {
      throw new BusinessException("生成图片访问地址失败");
    }
  }

  public void delete(String key) {
    if (!configured) throw new BusinessException("对象存储尚未配置");
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception e) {
      throw new BusinessException("删除对象失败");
    }
  }

  private void verifyMagic(MultipartFile file, String type) {
    try (InputStream in = file.getInputStream()) {
      byte[] h = in.readNBytes(12);
      boolean ok =
          ("image/jpeg".equals(type)
                  && h.length >= 3
                  && (h[0] & 255) == 255
                  && (h[1] & 255) == 216
                  && (h[2] & 255) == 255)
              || ("image/png".equals(type)
                  && h.length >= 8
                  && (h[0] & 255) == 137
                  && h[1] == 80
                  && h[2] == 78
                  && h[3] == 71)
              || ("image/webp".equals(type)
                  && h.length >= 12
                  && new String(h, 0, 4).equals("RIFF")
                  && new String(h, 8, 4).equals("WEBP"));
      if (!ok) throw new BusinessException("图片文件内容与格式不匹配");
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException("无法读取图片");
    }
  }

  public record StoredObject(
      String objectKey, String originalFilename, String contentType, long fileSize) {}
}
