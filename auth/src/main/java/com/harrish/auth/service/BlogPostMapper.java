package com.harrish.auth.service;

import com.harrish.auth.dto.BlogPostResponse;
import com.harrish.auth.dto.UserDto;
import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for mapping BlogPost entities to DTOs.
 * This separates the mapping responsibility from business logic.
 */
@Component
public class BlogPostMapper {

    /**
     * Maps a BlogPost entity to a BlogPostResponse DTO.
     *
     * @param entity the BlogPost entity
     * @return the BlogPostResponse DTO
     */
    public BlogPostResponse toResponse(BlogPost entity) {
        return BlogPostResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(toUserDto(entity.getCreatedBy()))
                .updatedBy(toUserDto(entity.getUpdatedBy()))
                .build();
    }

    /**
     * Maps a list of BlogPost entities to a list of BlogPostResponse DTOs.
     *
     * @param entities the list of BlogPost entities
     * @return the list of BlogPostResponse DTOs
     */
    public List<BlogPostResponse> toResponseList(List<BlogPost> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Maps a User entity to a UserDto.
     *
     * @param user the User entity
     * @return the UserDto, or null if user is null
     */
    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
