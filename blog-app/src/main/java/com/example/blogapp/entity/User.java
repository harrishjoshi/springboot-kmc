package com.example.blogapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
public class User extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToMany(mappedBy = "authors")
    private Set<Blog> blogs = new HashSet<>();
}
