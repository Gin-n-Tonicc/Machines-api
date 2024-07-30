package com.machines.machines_api.exceptions.files;

import com.machines.machines_api.exceptions.common.UnsupportedMediaTypeException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Exception thrown to indicate that the requested file type is not supported.
 * Extends UnsupportedMediaTypeException and sets the appropriate message using MessageSource (the messages are in src/main/resources/messages).
 */
public class UnsupportedFileTypeException extends UnsupportedMediaTypeException {
    public UnsupportedFileTypeException() {
        super("Неподдържан тип файл!");
    }
}
