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

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Builds the {@link HdBusinessContextInfo} every sidecar publishes with its app info resource.
 */
@Log4j2
@UtilityClass
public class HdBusinessContextInfoFactory {

    /**
     * @param includeDataDomainSubContext CloudBeaver passes {@code false}: it stays registered against
     *                                    the business domain even though a data domain is configured.
     */
    public static HdBusinessContextInfo createBusinessContextInfo(HelloDataContextConfig contextConfig, boolean includeDataDomainSubContext) {
        HdBusinessContextInfo businessContextInfo = new HdBusinessContextInfo();
        HelloDataContextConfig.BusinessContext businessContext = contextConfig.getBusinessContext();
        businessContextInfo.setType(businessContext.getType());
        businessContextInfo.setName(businessContext.getName());
        businessContextInfo.setKey(businessContext.getKey());
        businessContextInfo.setExtra(false);
        if (includeDataDomainSubContext) {
            HelloDataContextConfig.Context context = contextConfig.getContext();
            if (context != null) {
                HdBusinessContextInfo subContext = new HdBusinessContextInfo();
                businessContextInfo.setSubContext(subContext);
                subContext.setType(context.getType());
                subContext.setName(context.getName());
                subContext.setKey(context.getKey());
                subContext.setExtra(context.isExtra());
            }
        }
        log.debug("Created business context info {}", businessContextInfo);
        return businessContextInfo;
    }
}
