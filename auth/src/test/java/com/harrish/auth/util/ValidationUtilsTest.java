package com.harrish.auth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidationUtils")
class ValidationUtilsTest {

    @Nested
    @DisplayName("requireNonNull")
    class RequireNonNullTests {

        @Test
        @DisplayName("Should return object when it is not null")
        void shouldReturnObjectWhenNotNull() {
            // Arrange
            String testObject = "test value";

            // Act
            String result = ValidationUtils.requireNonNull(testObject, "testObject");

            // Assert
            assertThat(result)
                    .as("Should return the same object")
                    .isEqualTo(testObject);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when object is null")
        void shouldThrowExceptionWhenObjectIsNull() {
            // Arrange
            String paramName = "testParam";

            // Act & Assert
            assertThatThrownBy(() -> ValidationUtils.requireNonNull(null, paramName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(paramName)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("Should include parameter name in exception message")
        void shouldIncludeParameterNameInExceptionMessage() {
            // Arrange
            String paramName = "customParameter";

            // Act & Assert
            assertThatThrownBy(() -> ValidationUtils.requireNonNull(null, paramName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("customParameter must not be null");
        }

        @Test
        @DisplayName("Should work with different object types")
        void shouldWorkWithDifferentTypes() {
            // Arrange
            Integer intValue = 123;
            Long longValue = 456L;
            Object complexObject = new Object();

            // Act & Assert
            assertThat(ValidationUtils.requireNonNull(intValue, "int"))
                    .isEqualTo(intValue);
            assertThat(ValidationUtils.requireNonNull(longValue, "long"))
                    .isEqualTo(longValue);
            assertThat(ValidationUtils.requireNonNull(complexObject, "object"))
                    .isEqualTo(complexObject);
        }
    }

    @Nested
    @DisplayName("requireNonBlank")
    class RequireNonBlankTests {

        @Test
        @DisplayName("Should return string when it is not blank")
        void shouldReturnStringWhenNotBlank() {
            // Arrange
            String testString = "valid string";

            // Act
            String result = ValidationUtils.requireNonBlank(testString, "testString");

            // Assert
            assertThat(result)
                    .as("Should return the same string")
                    .isEqualTo(testString);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when string is null")
        void shouldThrowExceptionWhenStringIsNull() {
            // Arrange
            String paramName = "testParam";

            // Act & Assert
            assertThatThrownBy(() -> ValidationUtils.requireNonBlank(null, paramName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(paramName)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when string is empty")
        void shouldThrowExceptionWhenStringIsEmpty() {
            // Arrange
            String emptyString = "";
            String paramName = "emptyParam";

            // Act & Assert
            assertThatThrownBy(() -> ValidationUtils.requireNonBlank(emptyString, paramName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("emptyParam must not be blank");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when string contains only whitespace")
        void shouldThrowExceptionWhenStringIsWhitespace() {
            // Arrange
            String whitespaceString = "   ";
            String paramName = "whitespaceParam";

            // Act & Assert
            assertThatThrownBy(() -> ValidationUtils.requireNonBlank(whitespaceString, paramName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("whitespaceParam must not be blank");
        }

        @Test
        @DisplayName("Should accept string with leading/trailing whitespace if it contains text")
        void shouldAcceptStringWithWhitespaceIfHasContent() {
            // Arrange
            String stringWithWhitespace = "  valid content  ";

            // Act
            String result = ValidationUtils.requireNonBlank(stringWithWhitespace, "param");

            // Assert
            assertThat(result).isEqualTo(stringWithWhitespace);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException with different parameter names")
        void shouldIncludeCorrectParameterNameInException() {
            // Act & Assert - test multiple parameter names
            assertThatThrownBy(() -> ValidationUtils.requireNonBlank("", "firstName"))
                    .hasMessage("firstName must not be blank");

            assertThatThrownBy(() -> ValidationUtils.requireNonBlank(" ", "email"))
                    .hasMessage("email must not be blank");

            assertThatThrownBy(() -> ValidationUtils.requireNonBlank(null, "password"))
                    .hasMessage("password must not be blank");
        }
    }
}
