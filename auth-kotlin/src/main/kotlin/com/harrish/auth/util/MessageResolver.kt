package com.harrish.auth.util

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class MessageResolver(
    private val messageSource: MessageSource
) {

    fun getMessage(code: String): String {
        return getMessage(code, emptyArray())
    }

    fun getMessage(code: String, vararg args: Any): String {
        val locale: Locale = LocaleContextHolder.getLocale()
        return messageSource.getMessage(code, args, code, locale)
    }
}
