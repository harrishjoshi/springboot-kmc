package com.example.blogapp.service;

import com.example.blogapp.entity.Blog;
import com.example.blogapp.entity.User;
import com.example.blogapp.repository.BlogRepository;
import com.example.blogapp.util.BlogUtil;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    private final BlogRepository blogRepository;
    private final UserService userService;

    public BlogService(BlogRepository blogRepository, UserService userService) {
        this.blogRepository = blogRepository;
        this.userService = userService;
    }

    public List<Blog> findAll() {
        return blogRepository.findAll();
    }

    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id);
    }

    public void save(Blog blog, List<Long> authorIds) {
        if (authorIds != null && !authorIds.isEmpty()) {
            List<User> validAuthors = userService.findAllByIds(authorIds);
            blog.setAuthors(new HashSet<>(validAuthors));
        }

        blog.setSlug(BlogUtil.generateSlug(blog.getTitle()));

        blogRepository.save(blog);
    }

    public void deleteById(Long id) {
        blogRepository.deleteById(id);
    }
}
