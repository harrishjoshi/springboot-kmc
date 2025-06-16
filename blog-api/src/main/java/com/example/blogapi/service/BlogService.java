package com.example.blogapi.service;

import com.example.blogapi.dto.BlogCreateRequest;
import com.example.blogapi.dto.BlogResponse;
import com.example.blogapi.dto.BlogUpdateRequest;
import com.example.blogapi.entity.Blog;
import com.example.blogapi.repository.BlogRepository;
import com.example.blogapi.util.BlogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);
    private final BlogRepository blogRepository;

    public BlogService(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    public void create(BlogCreateRequest request) {
        log.debug("Debug BlogCreateRequest: {}", request);
        log.info("Info BlogCreateRequest: {}", request);

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setSlug(BlogUtil.generateSlug(request.getTitle()));
        blog.setTags(String.join(",", request.getTags()));

        blogRepository.save(blog);
    }

    public List<BlogResponse> getAllBlogs() {
        List<BlogResponse> blogResponses = new ArrayList<>();
        List<Blog> blogs = blogRepository.findAll();

        for (Blog blog : blogs) {
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
        BlogResponse blogResponse = null;
        Optional<Blog> blogOpt = blogRepository.findById(id);

        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();
            blogResponse = new BlogResponse(
                    blog.getId(), blog.getTitle(), blog.getSlug(),
                    blog.getContent(), BlogUtil.convertToTags(blog.getTags()),
                    blog.isDeleted()
            );
        }

        return blogResponse;
    }

    public void update(Long id, BlogUpdateRequest request) {
        Optional<Blog> blogOpt = blogRepository.findById(id);

        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();

            blog.setTitle(request.getTitle());
            blog.setSlug(BlogUtil.generateSlug(request.getTitle()));
            blog.setContent(request.getContent());
            blog.setTags(String.join(",", request.getTags()));

            blogRepository.save(blog);
        }
    }

    public void delete(Long id) {
        Optional<Blog> blogOpt = blogRepository.findById(id);
        blogOpt.ifPresent(blogRepository::delete);
    }
}
