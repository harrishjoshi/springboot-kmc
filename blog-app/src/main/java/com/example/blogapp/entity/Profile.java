package com.example.blogapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@ToString
public class Profile extends BaseEntity {

    private String bio;
    
    private String location;
    
    private String website;
    
    private String avatarUrl;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}