package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.dao.ImageDao;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/invoiceimages/")
public class PhotoController {
    private final ImageDao imageDao;

    @GetMapping(value = "/{imageName}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getImage(@PathVariable String imageName) {
        return imageDao.read(Path.of(System.getProperty("user.dir"))
                .resolve("src/main/resources/image/").resolve(imageName));
    }
}
