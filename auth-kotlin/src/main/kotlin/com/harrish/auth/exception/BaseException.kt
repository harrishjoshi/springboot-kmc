package com.harrish.auth.exception

abstract class BaseException(
    private val errorCode: String,
    message: String,
    vararg params: Any
) : RuntimeException(message) {

    fun getErrorCode(): String = errorCode

    fun getParams(): Array<out Any> = params
}
