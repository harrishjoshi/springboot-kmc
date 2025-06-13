package com.example.blogapi.util;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class BlogUtil {

    public static String generateSlug(String title) {
        return title.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-") // Replace spaces and tabs with single hyphen
                .replaceAll("-{2,}", "-"); // Replace multiple hyphens with single hyphen
    }

    public static List<String> convertToTags(String tags) {
        return List.of(tags.split(","));
    }
}
