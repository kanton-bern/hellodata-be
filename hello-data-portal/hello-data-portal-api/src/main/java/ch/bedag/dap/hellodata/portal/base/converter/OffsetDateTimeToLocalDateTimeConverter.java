package ch.bedag.dap.hellodata.portal.base.converter;

import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class OffsetDateTimeToLocalDateTimeConverter implements Converter<OffsetDateTime, LocalDateTime> {
    @Override
    public LocalDateTime convert(MappingContext<OffsetDateTime, LocalDateTime> context) {
        OffsetDateTime source = context.getSource();
        return (source == null) ? null : source.toLocalDateTime();
    }
}