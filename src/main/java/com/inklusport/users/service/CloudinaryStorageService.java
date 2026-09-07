package com.inklusport.users.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Almacenamiento de fotos de perfil en Cloudinary.
 */
@Service
@Slf4j
public class CloudinaryStorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private final Cloudinary cloudinary;
    private final String folder;

    /**
     * Inicializa el cliente y normaliza la carpeta de destino.
     */
    public CloudinaryStorageService(
            Cloudinary cloudinary,
            @Value("${cloudinary.folder:inklusport/profiles}") String folder) {
        this.cloudinary = cloudinary;
        this.folder = folder.endsWith("/") ? folder.substring(0, folder.length() - 1) : folder;
    }

    /**
     * Indica si Cloudinary tiene credenciales válidas y no está deshabilitado.
     */
    public boolean isConfigured() {
        Object cloudName = cloudinary.config.cloudName;
        Object apiKey = cloudinary.config.apiKey;
        return cloudName != null
                && !"disabled".equals(String.valueOf(cloudName))
                && apiKey != null
                && !String.valueOf(apiKey).isBlank();
    }

    /**
     * Construye el publicId del asset de perfil para el usuario.
     */
    public String publicIdFor(String userId) {
        return folder + "/" + userId;
    }

    /**
     * Valida y sube una imagen de perfil; devuelve la URL segura.
     */
    public String uploadImage(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Selecciona una imagen de perfil.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuntimeException("La imagen no puede superar 2 MB.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Formato de imagen no válido. Usa JPG, PNG, WEBP o GIF.");
        }
        try {
            return uploadBytes(userId, file.getBytes());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo leer la imagen de perfil: {}", e.getMessage());
            throw new RuntimeException("No se pudo leer la imagen de perfil.");
        }
    }

    /**
     * Decodifica un data URL de imagen y lo sube a Cloudinary.
     */
    public String uploadDataUrl(String userId, String dataUrl) {
        if (dataUrl == null || !dataUrl.regionMatches(true, 0, "data:image/", 0, 11)) {
            throw new RuntimeException("Formato de foto de perfil no válido.");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0 || comma == dataUrl.length() - 1) {
            throw new RuntimeException("Formato de foto de perfil no válido.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1).replace("\n", "").replace("\r", ""));
            if (bytes.length > MAX_BYTES) {
                throw new RuntimeException("La imagen no puede superar 2 MB.");
            }
            return uploadBytes(userId, bytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("La foto de perfil no es una imagen válida.");
        }
    }

    /**
     * Elimina el asset del usuario y, si aplica, el publicId extraído de la URL.
     */
    public void deleteStored(String userId, String storedValue) {
        if (!isConfigured()) {
            return;
        }
        destroyQuietly(publicIdFor(userId));
        String extracted = extractPublicId(storedValue);
        if (!extracted.isBlank() && !extracted.equals(publicIdFor(userId))) {
            destroyQuietly(extracted);
        }
    }

    /**
     * Extrae el publicId de una URL de Cloudinary; vacío si no aplica.
     */
    static String extractPublicId(String stored) {
        if (stored == null || stored.isBlank() || stored.startsWith("data:")) {
            return "";
        }
        if (!stored.contains("res.cloudinary.com")) {
            return "";
        }
        int uploadIdx = stored.indexOf("/upload/");
        if (uploadIdx < 0) {
            return "";
        }
        String rest = stored.substring(uploadIdx + "/upload/".length());
        int query = rest.indexOf('?');
        if (query >= 0) {
            rest = rest.substring(0, query);
        }
        String[] parts = rest.split("/");
        int i = 0;
        while (i < parts.length) {
            String part = parts[i];
            if (part.contains(",") || isVersionSegment(part)) {
                i++;
                continue;
            }
            break;
        }
        if (i >= parts.length) {
            return "";
        }
        String joined = String.join("/", Arrays.copyOfRange(parts, i, parts.length));
        int slash = joined.lastIndexOf('/');
        int dot = joined.lastIndexOf('.');
        if (dot > slash) {
            joined = joined.substring(0, dot);
        }
        return joined;
    }

    /**
     * Sube los bytes de la imagen y devuelve la URL segura de Cloudinary.
     */
    private String uploadBytes(String userId, byte[] bytes) {
        ensureConfigured();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "public_id", publicIdFor(userId),
                    "overwrite", true,
                    "invalidate", true,
                    "resource_type", "image",
                    "unique_filename", false,
                    "transformation", "c_fill,g_auto,w_400,h_400,q_auto,f_jpg"
            ));
            Object url = result.get("secure_url");
            if (url == null || url.toString().isBlank()) {
                throw new RuntimeException("Cloudinary no devolvió una URL de imagen.");
            }
            return url.toString();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error subiendo foto a Cloudinary: {}", e.getMessage());
            throw new RuntimeException("No se pudo subir la foto de perfil.");
        }
    }

    /**
     * Lanza error si Cloudinary no está configurado.
     */
    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new RuntimeException("Cloudinary no está configurado. Define CLOUDINARY_URL o las claves API.");
        }
    }

    /**
     * Intenta borrar un asset sin interrumpir el flujo si falla.
     */
    private void destroyQuietly(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "invalidate", true,
                    "resource_type", "image"
            ));
        } catch (Exception e) {
            log.warn("No se pudo borrar el asset {} en Cloudinary: {}", publicId, e.getMessage());
        }
    }

    /**
     * Indica si el segmento de URL es una versión (v + dígitos).
     */
    private static boolean isVersionSegment(String part) {
        if (part == null || part.length() < 2 || part.charAt(0) != 'v') {
            return false;
        }
        for (int i = 1; i < part.length(); i++) {
            if (!Character.isDigit(part.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
