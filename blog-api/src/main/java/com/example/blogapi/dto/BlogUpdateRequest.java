package com.example.blogapi.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class BlogUpdateRequest {

    private String title;
    private String content;
    private String slug;
    private List<String> tags;
}
