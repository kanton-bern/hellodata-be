package ch.bedag.dap.hellodata.portal.faq.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode
public class FaqMessages {
    private List<FaqMessage> messages;
}
