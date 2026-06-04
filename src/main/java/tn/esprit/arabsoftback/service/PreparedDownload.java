package tn.esprit.arabsoftback.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.nio.file.Path;

/**
 * Fichier pret pour envoi HTTP (endpoint public de telechargement).
 */
public record PreparedDownload(Resource resource, String fileName, String contentType) {

    public static PreparedDownload of(Path path, String fileName, String contentType) throws MalformedURLException {
        Resource resource = new UrlResource(path.toUri());
        return new PreparedDownload(resource, fileName, contentType);
    }
}
