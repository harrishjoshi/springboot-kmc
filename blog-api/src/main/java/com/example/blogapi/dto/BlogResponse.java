package com.example.blogapi.dto;

import java.util.List;

public record BlogResponse(
        Long id,
        String title,
        String slug,
        String content,
        List<String> tags,
        boolean deleted
) {
}
