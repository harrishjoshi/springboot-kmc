package com.example.blogapi.controller;


import com.example.blogapi.dto.BlogCreateRequest;
import com.example.blogapi.dto.BlogResponse;
import com.example.blogapi.dto.BlogUpdateRequest;
import com.example.blogapi.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping
    void create(@RequestBody @Valid BlogCreateRequest request) {
        blogService.create(request);
    }

    @GetMapping
    List<BlogResponse> getAllBlogs() {
        return blogService.getAllBlogs();
    }

    @GetMapping("{id}")
    BlogResponse getById(@PathVariable Long id) {
        return blogService.findById(id);
    }

    @GetMapping("{slug}")
    BlogResponse getBySlug(@PathVariable String slug) {
        return blogService.findBySlug(slug);
    }

    @PutMapping("{id}")
    void update(@PathVariable Long id,
                @RequestBody BlogUpdateRequest request) {
        blogService.update(id, request);
    }

    @DeleteMapping("{id}")
    void delete(@PathVariable Long id) {
        blogService.delete(id);
    }
}
