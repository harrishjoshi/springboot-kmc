package com.example.blogapi.service;

import com.example.blogapi.dto.BlogCreateRequest;
import com.example.blogapi.dto.BlogResponse;
import com.example.blogapi.dto.BlogUpdateRequest;
import com.example.blogapi.entity.Blog;
import com.example.blogapi.util.BlogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);

    private List<Blog> DATA = new ArrayList<>();

    public void create(BlogCreateRequest request) {
        log.debug("Debug BlogCreateRequest: {}", request);
        log.info("Info BlogCreateRequest: {}", request);

        Blog blog = new Blog();
        blog.setId((long) DATA.size() + 1);
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setSlug(BlogUtil.generateSlug(request.getTitle()));
        blog.setTags(String.join(",", request.getTags()));

        DATA.add(blog);
    }

    public List<BlogResponse> getAllBlogs() {
        List<BlogResponse> blogResponses = new ArrayList<>();

        for (Blog blog : DATA) {
            BlogResponse response = new BlogResponse(
                    blog.getId(), blog.getTitle(), blog.getSlug(),
                    blog.getContent(), BlogUtil.convertToTags(blog.getTags()),
                    blog.isDeleted()
            );

            blogResponses.add(response);
        }

        return blogResponses;
    }

    public BlogResponse findById(Long id) {
        BlogResponse response = null;

        for (Blog blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                response = new BlogResponse(
                        blog.getId(), blog.getTitle(), blog.getSlug(),
                        blog.getContent(), BlogUtil.convertToTags(blog.getTags()),
                        blog.isDeleted()
                );
            }
        }

        return response;
    }

    public void update(Long id, BlogUpdateRequest request) {
        for (Blog blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                blog.setTitle(request.getTitle());
                blog.setSlug(BlogUtil.generateSlug(request.getTitle()));
                blog.setContent(request.getContent());
                blog.setTags(String.join(",", request.getTags()));
            }
        }
    }

    public void delete(Long id) {
        for (Blog blog : DATA) {
            if (Objects.equals(blog.getId(), id)) {
                DATA.remove(blog);
            }
        }
    }
}
