package com.harrish.auth.dto;

import com.harrish.auth.validation.ValidBlogContent;
import com.harrish.auth.validation.ValidBlogTitle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBlogPostRequest {

    @ValidBlogTitle
    private String title;

    @ValidBlogContent
    private String content;
}