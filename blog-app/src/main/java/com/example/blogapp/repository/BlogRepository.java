package com.example.blogapp.repository;

import com.example.blogapp.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    @Query("SELECT b FROM Blog b LEFT JOIN FETCH b.authors WHERE b.id = :id")
    Optional<Blog> findByIdWithAuthors(@Param("id") Long id);

    @Query("SELECT b FROM Blog b LEFT JOIN FETCH b.authors")
    List<Blog> findAllWithAuthors();
}
