package ch.bedag.dap.hellodata.portal.faq.data;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Locale;

@Data
@EqualsAndHashCode
public class FaqMessage {
    @NotBlank
    private Locale locale;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
}
