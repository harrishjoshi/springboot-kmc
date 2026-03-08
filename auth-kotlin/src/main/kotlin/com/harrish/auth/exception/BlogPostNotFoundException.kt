package com.harrish.auth.exception

import com.harrish.auth.exception.error.BlogErrorCode

class BlogPostNotFoundException : BaseException(
    BlogErrorCode.BLOG_POST_NOT_FOUND.messageKey,
    "Blog post not found"
)
