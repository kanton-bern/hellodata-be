/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.commons.sidecars.context;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Data
@Validated
@ConfigurationProperties("hello-data")
public class HelloDataContextConfig {

    private static final String CODE_DELIMITER = "\\|";

    @NotNull
    private String businessContext;
    private List<String> contexts;

    @PostConstruct
    public void validateProperties() {
        String[] parts = businessContext.split(CODE_DELIMITER);
        if (parts == null || parts.length != 3) {
            throw new IllegalArgumentException(String.format("Business context config must have 3 parts separated by a '|' sign. The current config is: %s", businessContext));
        }

        log.info("Initiated context config, business context: {}", businessContext);
        if (!CollectionUtils.isEmpty(contexts)) {
            for (String context : contexts) {
                log.info("Validating context: {}", context);
                parts = context.split(CODE_DELIMITER);
                if (parts == null || parts.length < 3) {
                    throw new IllegalArgumentException(String.format("Context config must have 3 or 4 parts separated by a '|' sign. The current config is: %s", context));
                }
                log.info("Context OK: {}", context);
            }
        }
    }

    public BusinessContext getBusinessContext() {
        BusinessContext businessContextObj = new BusinessContext();
        String[] parts = businessContext.split(CODE_DELIMITER);
        businessContextObj.setType(parts[0].trim());
        businessContextObj.setKey(parts[1].trim());
        businessContextObj.setName(parts[2].trim());
        return businessContextObj;
    }

    public Context getContext() {
        if (CollectionUtils.isEmpty(contexts)) {
            return null;
        }
        return createContextObj(contexts.get(0));
    }

    public List<Context> getContexts() {
        if (contexts == null) {
            return Collections.emptyList();
        }
        List<Context> result = new ArrayList<>();
        for (String context : contexts) {
            result.add(createContextObj(context));
        }
        return result;
    }

    private Context createContextObj(String context) {
        Context contextObj = new Context();
        String[] parts = context.split(CODE_DELIMITER);
        contextObj.setType(parts[0].trim());
        contextObj.setKey(parts[1].trim());
        contextObj.setName(parts[2].trim());
        contextObj.setExtra(parts.length > 3 && Boolean.parseBoolean(parts[3].trim()));
        return contextObj;
    }

    @Data
    public static class BusinessContext implements HdContext {
        private String type;
        private String key;
        private String name;
    }

    @Data
    public static class Context extends BusinessContext {
        private boolean extra;
    }
}
