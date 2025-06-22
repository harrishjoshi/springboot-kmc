package com.example.blogapp.controller;

import com.example.blogapp.entity.Blog;
import com.example.blogapp.service.BlogService;
import com.example.blogapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        model.addAttribute("users", userService.findAll());

        return "blog/form";
    }

    @PostMapping
    public String saveBlog(@ModelAttribute Blog blog, @RequestParam(required = false) Long userId) {
        blogService.save(blog, userId);

        return "redirect:/blogs";
    }

    @GetMapping("edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Blog blog = blogService.findById(id).orElseThrow();
        model.addAttribute("blog", blog);
        model.addAttribute("users", userService.findAll());
        return "blog/form";
    }

    @GetMapping("delete/{id}")
    public String deleteBlog(@PathVariable Long id) {
        blogService.deleteById(id);
        return "redirect:/blogs";
    }
}
