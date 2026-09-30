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
package ch.bedag.dap.hellodata.portal.base.config;

import ch.bedag.dap.hellodata.portal.user.UserAlreadyExistsException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUserAlreadyExistsException_returnsConflict() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException();
        ProblemDetail problem = handler.handleUserAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("User Already Exists", problem.getTitle());
        assertEquals("@User email already exists", problem.getDetail());
    }

    @Test
    void handleNotFoundException_returnsNotFound() {
        NotFoundException ex = new NotFoundException("User 123 not found");
        ProblemDetail problem = handler.handleNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
        assertEquals("User 123 not found", problem.getDetail());
    }

    @Test
    void handleIllegalArgumentException_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input data");
        ProblemDetail problem = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Invalid input data", problem.getDetail());
    }

    @Test
    void handleAllUncaughtExceptions_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ProblemDetail problem = handler.handleAllUncaughtExceptions(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
    }

    @Test
    void handleAccessDeniedException_rethrows() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        assertThrows(AccessDeniedException.class, () -> handler.handleAccessDeniedException(ex));
    }

    @Test
    void handleAuthenticationException_rethrows() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        assertThrows(BadCredentialsException.class, () -> handler.handleAuthenticationException(ex));
    }

    @Test
    void handleMethodArgumentNotValid_extractsInvalidParams() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        WebRequest request = mock(WebRequest.class);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatusCode.valueOf(400), request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ProblemDetail);
        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertNotNull(problemDetail.getProperties());
        assertTrue(problemDetail.getProperties().containsKey("invalidParams"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> invalidParams = (List<Map<String, String>>) problemDetail.getProperties().get("invalidParams");
        assertEquals(1, invalidParams.size());
        assertEquals("email", invalidParams.get(0).get("name"));
        assertEquals("must not be blank", invalidParams.get(0).get("reason"));
    }
}
