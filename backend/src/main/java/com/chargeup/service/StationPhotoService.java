package com.chargeup.service;

import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StationPhotoService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Path photoRoot = Path.of("uploads", "stations").toAbsolutePath().normalize();

    public String store(MultipartFile file) {
        if (file.isEmpty() || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Station photos must be non-empty JPEG, PNG, or WebP files");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Station photos must be 5MB or smaller");
        }

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BadRequestException("Unsupported station photo type");
        };
        String filename = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(photoRoot);
            file.transferTo(photoRoot.resolve(filename));
            return "/api/stations/photos/" + filename;
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store station photo");
        }
    }

    public Resource load(String filename) {
        try {
            Path target = photoRoot.resolve(filename).normalize();
            if (!target.startsWith(photoRoot)) {
                throw new ResourceNotFoundException("Photo not found");
            }
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Photo not found");
            }
            return resource;
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Photo not found");
        }
    }
}
