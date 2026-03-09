package ch.bedag.dap.hellodata.portal.faq.data;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Rejects any string containing base64-encoded image data (e.g. from pasting images into a rich-text editor).
 * Only external image URLs should be used in FAQ messages.
 */
public class NoBase64ImagesValidator implements ConstraintValidator<NoBase64Images, String> {

    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // @NotBlank handles null/blank
        }
        return !BASE64_IMAGE_PATTERN.matcher(value).find();
    }
}
