package com.campus.business.service;

import com.campus.business.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    private final Path root;

    public LocalFileStorageService(@Value("${campus.upload-dir:uploads}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片不能超过5MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + extension;
        Path destination = root.resolve(filename).normalize();
        if (!destination.getParent().equals(root)) {
            throw new BusinessException("文件名不安全");
        }

        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new BusinessException("图片保存失败");
        }
    }

    private String detectExtension(MultipartFile file) {
        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException e) {
            throw new BusinessException("无法读取图片");
        }

        if (header.length >= 3 && unsigned(header[0]) == 0xff && unsigned(header[1]) == 0xd8 && unsigned(header[2]) == 0xff) {
            return ".jpg";
        }
        if (startsWith(header, PNG_SIGNATURE)) {
            return ".png";
        }
        if (header.length >= 12
                && ascii(header, 0, "RIFF")
                && ascii(header, 8, "WEBP")) {
            return ".webp";
        }
        throw new BusinessException("只允许上传真实的JPEG、PNG或WebP图片");
    }

    private boolean startsWith(byte[] value, byte[] signature) {
        if (value.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (value[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean ascii(byte[] value, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (value[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }
}
