package com.harrish.auth.service

import com.harrish.auth.dto.AuthenticationRequest
import com.harrish.auth.dto.AuthenticationResponse
import com.harrish.auth.dto.RegisterRequest
import com.harrish.auth.dto.RegisterResponse
import com.harrish.auth.dto.TokenRefreshRequest
import com.harrish.auth.exception.EmailAlreadyExistsException
import com.harrish.auth.exception.InvalidTokenException
import com.harrish.auth.exception.UserNotFoundException
import com.harrish.auth.model.Role
import com.harrish.auth.model.User
import com.harrish.auth.repository.UserRepository
import com.harrish.auth.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {
    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val user = User(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = Role.USER
        )
        userRepository.save(user)
        return RegisterResponse(RegisterResponse.MESSAGE)
    }

    @Transactional(readOnly = true)
    fun authenticateUser(request: AuthenticationRequest): AuthenticationResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.email,
                request.password
            )
        )
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { UserNotFoundException() }
        val accessToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        val expiresIn = jwtService.getJwtExpirationInSeconds()
        return AuthenticationResponse(accessToken, refreshToken, expiresIn)
    }

    fun refreshToken(request: TokenRefreshRequest): AuthenticationResponse {
        val refreshToken = request.refreshToken
        val userEmail = jwtService.extractUsername(refreshToken)
        if (userEmail == null) {
            throw InvalidTokenException()
        }
        val userDetails = userRepository.findByEmail(userEmail)
            .orElseThrow { UserNotFoundException() }
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw InvalidTokenException()
        }
        val accessToken = jwtService.generateToken(userDetails)
        val expiresIn = jwtService.getJwtExpirationInSeconds()
        return AuthenticationResponse(accessToken, refreshToken, expiresIn)
    }
}
