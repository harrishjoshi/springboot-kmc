package com.harrish.auth.repository

import com.harrish.auth.model.BlogPost
import com.harrish.auth.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface BlogPostRepository : JpaRepository<BlogPost, Long> {

    fun findByCreatedByOrderByCreatedAtDesc(user: User): List<BlogPost>
}
