package com.unbidden.telegramcoursesbot.controller;

import com.unbidden.telegramcoursesbot.dao.ImageDao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/invoiceimages")
public class PhotoController {
    private final ImageDao imageDao;

    @GetMapping(value = "/{courseName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getImage(@PathVariable String courseName) {
        return imageDao.read(courseName);
    }
}
