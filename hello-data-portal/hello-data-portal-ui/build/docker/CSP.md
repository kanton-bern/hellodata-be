### To set a CSP header set the CSP_HEADER_VALUE env variable. For example:

CSP_HEADER_VALUE=connect-src 'self' localhost; font-src 'self' localhost; frame-src 'self' localhost; script-src 'self' localhost 'unsafe-inline'; style-src 'self' localhost 'unsafe-inline'; frame-ancestors 'self' localhost; img-src 'self' data: localhost; manifest-src 'self' localhost; media-src 'self' localhost; object-src 'self' localhost; worker-src 'self';
