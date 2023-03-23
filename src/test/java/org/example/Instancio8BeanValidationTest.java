package org.example;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.UUID;
import org.instancio.Instancio;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.WithSettings;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.all;

/**
 * Example of using Bean Validation to produce valid objects.
 * Supported constraints:
 *
 * <ul>
 *   <li>javax.validation.constraints.*</li>
 *   <li>jakarta.validation.constraints.*</li>
 *   <li>org.hibernate.validator.constraints.*</li>
 * </ul>
 */
@ExtendWith(InstancioExtension.class)
class Instancio8BeanValidationTest {

    /**
     * Bean validation is an experimental feature and is disabled by default.
     * It can be enabled using {@code Settings} or {@code instancio.properties} file.
     */
    @WithSettings
    private final Settings settings = Settings.create()
            .set(Keys.BEAN_VALIDATION_ENABLED, true);

    private static class Student {
        @UUID
        private String id;

        @Digits(integer = 10, fraction = 0)
        private String studentId;

        @Length(min = 3, max = 50)
        String name;

        @Email
        String email;

        @Range(min = 18, max = 29)
        int age;

        @Past
        Instant lastModified;

        @Size(min = 1, max = 10)
        List<String> hobbies;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    /**
     * Creates a valid object based on Bean Validation annotations.
     */
    @Test
    void createValidObject() {
        Student result = Instancio.create(Student.class);

        assertThat(getValidationErrors(result)).isEmpty();
    }

    /**
     * Generated values can be overridden if needed,
     * for example, to verify validation errors.
     */
    @Test
    void validationError() {
        Student result = Instancio.of(Student.class)
                .generate(all(Instant.class), gen -> gen.temporal().instant().future())
                .create();

        assertThat(getValidationErrors(result))
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("must be a past date");
    }

    private static <T> Set<ConstraintViolation<T>> getValidationErrors(T obj) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(obj);
        }
    }
}
