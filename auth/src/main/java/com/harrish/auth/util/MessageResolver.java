package com.harrish.auth.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code) {
        return getMessage(code, new Object[0]);
    }

    public String getMessage(String code, Object... args) {
        var locale = LocaleContextHolder.getLocale();

        return messageSource.getMessage(code, args, code, locale);
    }
}