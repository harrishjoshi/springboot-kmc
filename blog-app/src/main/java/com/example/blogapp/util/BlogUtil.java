package com.example.blogapp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BlogUtil {

    public static String generateSlug(String title) {
        return title.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-") // Replace spaces and tabs with single hyphen
                .replaceAll("-{2,}", "-"); // Replace multiple hyphens with single hyphen
    }
}
