package com.example.blogapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@ToString
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String tags;

    @OneToOne
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
