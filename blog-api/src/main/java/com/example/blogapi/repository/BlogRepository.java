package com.example.blogapi.repository;

import com.example.blogapi.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findBySlugAndDeletedIsFalse(String slug);

    @Query("""
            SELECT b FROM Blog b
                        WHERE b.deleted = FALSE
            """)
    Optional<Blog> findBySlug(String slug);

    // table emp
    // table stu
    // table coll
    // select * from coll c
    // left join emp e on e.id = c.emp_id
    // left join stu s on s.id = c.stu_id
}
