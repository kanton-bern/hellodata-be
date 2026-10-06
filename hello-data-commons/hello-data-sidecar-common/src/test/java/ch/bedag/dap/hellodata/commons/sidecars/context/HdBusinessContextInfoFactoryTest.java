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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the logic extracted from the six {@code *AppInfoResourceProviderService} classes.
 */
class HdBusinessContextInfoFactoryTest {

    private static final String BUSINESS_DOMAIN_KEY = "HelloDATA_Product_Development";
    private static final String DATA_DOMAIN_KEY = "Default_Data_Domain";
    private static final String BUSINESS_CONTEXT = "Business Domain | HelloDATA_Product_Development | HelloDATA Product Development";
    private static final String DATA_DOMAIN = "Data Domain | Default_Data_Domain | Default Data Domain";
    private static final String EXTRA_DATA_DOMAIN = "Data Domain | Extra_Data_Domain | Extra Data Domain | true";

    private static HelloDataContextConfig config(String... contexts) {
        HelloDataContextConfig contextConfig = new HelloDataContextConfig();
        contextConfig.setBusinessContext(BUSINESS_CONTEXT);
        contextConfig.setContexts(contexts.length == 0 ? null : List.of(contexts));
        return contextConfig;
    }

    @Test
    void createBusinessContextInfo_mapsBusinessContextFromConfiguration() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(DATA_DOMAIN), true);

        assertEquals("Business Domain", result.getType());
        assertEquals(BUSINESS_DOMAIN_KEY, result.getKey());
        assertEquals("HelloDATA Product Development", result.getName());
        assertFalse(result.isExtra(), "the business context itself is never flagged as extra");
    }

    @Test
    void createBusinessContextInfo_attachesDataDomainAsSubContext() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(DATA_DOMAIN), true);

        HdBusinessContextInfo subContext = result.getSubContext();
        assertNotNull(subContext, "the configured data domain must be attached");
        assertEquals("Data Domain", subContext.getType());
        assertEquals(DATA_DOMAIN_KEY, subContext.getKey());
        assertEquals("Default Data Domain", subContext.getName());
        assertFalse(subContext.isExtra());
    }

    @Test
    void createBusinessContextInfo_propagatesTheExtraFlag() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(EXTRA_DATA_DOMAIN), true);

        assertNotNull(result.getSubContext());
        assertTrue(result.getSubContext().isExtra());
    }

    @Test
    void createBusinessContextInfo_usesOnlyTheFirstConfiguredContext() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(DATA_DOMAIN, EXTRA_DATA_DOMAIN), true);

        assertNotNull(result.getSubContext());
        assertEquals(DATA_DOMAIN_KEY, result.getSubContext().getKey());
    }

    /**
     * The CloudBeaver case: a data domain IS configured, but the sidecar must stay registered
     * against the business domain. Attaching the sub context here would move its app info resource
     * to the data domain context key.
     */
    @Test
    void createBusinessContextInfo_ignoresDataDomain_whenSubContextExcluded() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(DATA_DOMAIN), false);

        assertNull(result.getSubContext(), "CloudBeaver must not be filed under a data domain");
        assertEquals(BUSINESS_DOMAIN_KEY, result.getKey());
    }

    /**
     * Sidecars without a configured data domain (airflow, dbt-docs, sftpgo) must not blow up.
     */
    @Test
    void createBusinessContextInfo_leavesSubContextEmpty_whenNoneConfigured() {
        HdBusinessContextInfo result = HdBusinessContextInfoFactory.createBusinessContextInfo(config(), true);

        assertNull(result.getSubContext());
        assertEquals(BUSINESS_DOMAIN_KEY, result.getKey());
    }
}
