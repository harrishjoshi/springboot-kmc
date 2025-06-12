package com.example.blogapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.blogapi.dto.BlogUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.blogapi.dto.BlogCreateRequest;

@Service
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);

    private List<BlogCreateRequest> DATA = new ArrayList<>();

    public void create(BlogCreateRequest request) {
        log.debug("Debug BlogCreateRequest: {}", request);
        log.info("Info BlogCreateRequest: {}", request);

        DATA.add(request);
    }

    public List<BlogCreateRequest> getAllBlogs() {
        return DATA;
    }

    public BlogCreateRequest findById(Long id) {
        BlogCreateRequest blogDetail = null;

        for (BlogCreateRequest blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                blogDetail = blog;
            }
        }

        return blogDetail;
    }

    public void update(Long id, BlogUpdateRequest request) {
        for (BlogCreateRequest blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                blog.setTitle(request.getTitle());
                blog.setContent(request.getContent());
                blog.setSlug(request.getSlug());
                blog.setTags(request.getTags());
            }
        }
    }

    public void delete(Long id) {
        for (BlogCreateRequest blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                DATA.remove(blog);
            }
        }
    }
}
