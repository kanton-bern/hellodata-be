package ch.bedag.dap.hellodata.portal.faq.data;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string field does not contain embedded base64 image data.
 * Only external image URLs are allowed.
 */
@Documented
@Constraint(validatedBy = NoBase64ImagesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoBase64Images {
    String message() default "Embedded base64 images are not allowed. Please use an image URL instead.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
