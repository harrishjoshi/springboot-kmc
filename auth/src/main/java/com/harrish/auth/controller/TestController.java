package com.harrish.auth.controller;

import com.harrish.auth.dto.MessageResponse;
import com.harrish.auth.dto.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test", description = "Test endpoints for demonstrating authentication and authorization")
class TestController {

    @Operation(
            summary = "Public test endpoint",
            description = "A public endpoint that doesn't require authentication"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully accessed public endpoint",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    @GetMapping("/public")
    ResponseEntity<MessageResponse> publicEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This is a public endpoint"));
    }

    @Operation(
            summary = "Protected test endpoint",
            description = "A protected endpoint that requires authentication",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully accessed protected endpoint",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content)
    })
    @GetMapping("/protected")
    ResponseEntity<UserInfoResponse> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(new UserInfoResponse(
                "This is a protected endpoint",
                authentication.getName(),
                authentication.getAuthorities()
        ));
    }

    @Operation(
            summary = "Admin test endpoint",
            description = "An admin endpoint that requires ADMIN role",
            security = {@SecurityRequirement(name = "bearerAuth")},
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully accessed admin endpoint",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role",
                    content = @Content)
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<MessageResponse> adminEndpoint() {
        return ResponseEntity.ok(new MessageResponse("This is an admin endpoint"));
    }
}
