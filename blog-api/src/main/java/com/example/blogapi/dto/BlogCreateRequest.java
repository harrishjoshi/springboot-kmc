package com.example.blogapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class BlogCreateRequest {

    @NotBlank(message = "Title is required")
    @Length(max = 100)
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotEmpty
    private List<@NotBlank String> tags;
}


