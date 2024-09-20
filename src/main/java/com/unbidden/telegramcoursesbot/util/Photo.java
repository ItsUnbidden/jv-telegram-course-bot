package com.unbidden.telegramcoursesbot.util;

import lombok.Data;

/**
 * Photo defining file must look like: 
 * <p>`url`
 * <p>`size`
 * <p>`width`
 * <p>`height`
 */
@Data
public class Photo {
    private String url;

    private Integer size;

    private Integer width;

    private Integer height;
}
