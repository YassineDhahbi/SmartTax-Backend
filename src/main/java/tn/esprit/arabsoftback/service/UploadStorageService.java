package tn.esprit.arabsoftback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UploadStorageService {

    @Value("${app.uploads.base-dir:uploads}")
    private String uploadsBaseDir;

    private Path basePath;

    @PostConstruct
    void init() throws IOException {
        basePath = Paths.get(uploadsBaseDir).toAbsolutePath().normalize();
        Files.createDirectories(basePath.resolve("users"));
        Files.createDirectories(basePath.resolve("publications"));
    }

    public String storeUserPhoto(MultipartFile file, String email) throws IOException {
        String safeEmail = email.replaceAll("[^a-zA-Z0-9.@]", "_");
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String fileName = safeEmail + "_" + System.currentTimeMillis() + "_"
                + original.replaceAll("[\\s()]+", "_");
        Path target = basePath.resolve("users").resolve(fileName);
        Files.write(target, file.getBytes());
        return publicUrl("users", fileName);
    }

    public String storePublicationImage(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
        String fileName = System.currentTimeMillis() + "_"
                + original.replaceAll("[^a-zA-Z0-9.]", "_");
        Path target = basePath.resolve("publications").resolve(fileName);
        file.transferTo(target.toFile());
        return publicUrl("publications", fileName);
    }

    public void deleteIfManaged(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String relative = toRelativeUploadPath(publicUrl);
        if (relative == null) {
            return;
        }
        try {
            Path path = basePath.resolve(relative).normalize();
            if (path.startsWith(basePath) && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    /** Chemin public servi via /uploads/** (ex. uploads/users/photo.jpg). */
    public String publicUrl(String category, String fileName) {
        return "uploads/" + category + "/" + fileName;
    }

    public Path getBasePath() {
        return basePath;
    }

    private String toRelativeUploadPath(String url) {
        String normalized = url.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            int idx = normalized.indexOf("/uploads/");
            if (idx >= 0) {
                normalized = normalized.substring(idx + "/uploads/".length());
                return normalized;
            }
            idx = normalized.indexOf("/assets/img/user/");
            if (idx >= 0) {
                return "users/" + normalized.substring(idx + "/assets/img/user/".length());
            }
            idx = normalized.indexOf("/assets/img/publication/");
            if (idx >= 0) {
                return "publications/" + normalized.substring(idx + "/assets/img/publication/".length());
            }
            return null;
        }
        if (normalized.startsWith("/uploads/")) {
            return normalized.substring("/uploads/".length());
        }
        if (normalized.startsWith("uploads/")) {
            return normalized.substring("uploads/".length());
        }
        if (normalized.startsWith("/assets/img/user/")) {
            return "users/" + normalized.substring("/assets/img/user/".length());
        }
        if (normalized.startsWith("/assets/img/publication/")) {
            return "publications/" + normalized.substring("/assets/img/publication/".length());
        }
        return null;
    }
}
