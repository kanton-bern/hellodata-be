package ch.bedag.dap.hellodata.portal.base.converter;

import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class LocalDateTimeToOffsetDateTimeConverter implements Converter<LocalDateTime, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(MappingContext<LocalDateTime, OffsetDateTime> context) {
        LocalDateTime source = context.getSource();
        return (source == null) ? null : source.atOffset(java.time.ZoneOffset.UTC);
    }
}
