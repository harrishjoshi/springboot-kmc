package com.example.blogapp.controller;

import com.example.blogapp.entity.BaseEntity;
import com.example.blogapp.entity.Blog;
import com.example.blogapp.entity.User;
import com.example.blogapp.service.BlogService;
import com.example.blogapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/blogs")
public class BlogController {

    private final BlogService blogService;
    private final UserService userService;

    public BlogController(BlogService blogService, UserService userService) {
        this.blogService = blogService;
        this.userService = userService;
    }

    @GetMapping
    public String listBlogs(Model model) {
        model.addAttribute("blogs", blogService.findAll());

        return "blog/list";
    }

    @GetMapping("new")
    public String showBlogForm(Model model) {
        model.addAttribute("blog", new Blog());
        model.addAttribute("authors", userService.findAll());

        return "blog/form";
    }

    @PostMapping
    public String saveBlog(@ModelAttribute Blog blog, @RequestParam(required = false) List<Long> authorIds) {
        blogService.save(blog, authorIds);

        return "redirect:/blogs";
    }

    @GetMapping("edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Blog blog = blogService.findById(id).orElseThrow();
        List<User> authors = userService.findAll();

        model.addAttribute("blog", blog);
        model.addAttribute("authors", authors);
        model.addAttribute("authorIds", blog.getAuthors().stream().map(BaseEntity::getId).toList());

        return "blog/form";
    }
}
