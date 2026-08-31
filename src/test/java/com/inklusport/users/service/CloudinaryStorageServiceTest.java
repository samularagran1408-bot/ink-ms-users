package com.inklusport.users.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudinaryStorageServiceTest {

    @Test
    void extractPublicId_desdeUrlSimple() {
        String url = "https://res.cloudinary.com/vo3sxeso/image/upload/v1710000000/inklusport/profiles/user-1.jpg";
        assertEquals("inklusport/profiles/user-1", CloudinaryStorageService.extractPublicId(url));
    }

    @Test
    void extractPublicId_ignoraTransformaciones() {
        String url = "https://res.cloudinary.com/vo3sxeso/image/upload/c_fill,g_auto,w_400,h_400/v12/inklusport/profiles/abc.png";
        assertEquals("inklusport/profiles/abc", CloudinaryStorageService.extractPublicId(url));
    }

    @Test
    void extractPublicId_base64OVacio() {
        assertEquals("", CloudinaryStorageService.extractPublicId("data:image/jpeg;base64,abc"));
        assertEquals("", CloudinaryStorageService.extractPublicId(null));
        assertEquals("", CloudinaryStorageService.extractPublicId("https://example.com/foto.jpg"));
    }
}
