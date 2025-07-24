package com.harrish.auth.exception;

import com.harrish.auth.exception.error.BlogErrorCode;

public class BlogPostNotFoundException extends BaseException {

    public BlogPostNotFoundException() {
        super(BlogErrorCode.BLOG_POST_NOT_FOUND.getMessageKey(),
                "Blog post not found");
    }
}
