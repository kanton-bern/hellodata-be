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
package ch.bedag.dap.hellodata.portal.email.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailTemplateModelKeys {
    public static final String BUSINESS_DOMAIN_NAME_PARAM = "BUSINESS_DOMAIN_NAME";
    public static final String BUSINESS_DOMAIN_ROLE_NAME_PARAM = "BUSINESS_DOMAIN_ROLE_NAME";
    public static final String DATA_DOMAIN_ROLES_PARAM = "DATA_DOMAIN_ROLES";
    public static final String AFFECTED_USER_FIRST_NAME_PARAM = "AFFECTED_USER_FIRST_NAME";
    public static final String FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE_PARAM = "FIRST_NAME_LAST_NAME_OF_USER_THAT_MADE_CHANGE";
    public static final String COMMENT_TEXT_PARAM = "COMMENT_TEXT";
    public static final String COMMENT_NEW_STATUS_PARAM = "COMMENT_NEW_STATUS";
    public static final String COMMENT_DECLINE_REASON_PARAM = "COMMENT_DECLINE_REASON";
    public static final String COMMENT_DELETION_REASON_PARAM = "COMMENT_DELETION_REASON";
    public static final String DASHBOARD_NAME_PARAM = "DASHBOARD_NAME";
}
