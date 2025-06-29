package com.example.blogapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
@ToString
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;
    
    private String description;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Blog> blogs = new HashSet<>();
}