package ch.bedag.dap.hellodata.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HdHtmlSanitizerTest {

    @Test
    @DisplayName("Should return empty string when input is null")
    void testNullInput() {
        String result = HdHtmlSanitizer.sanitizeHtml(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Should keep allowed tags and attributes")
    void testAllowedTagsAndAttributes() {
        String input = "<p class=\"text\">Hello <span class=\"highlight\" style=\"color:red\">World</span></p>";
        String expected = "<p class=\"text\">Hello <span class=\"highlight\" style=\"color:red\">World</span></p>";
        String result = HdHtmlSanitizer.sanitizeHtml(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should remove disallowed tags like script")
    void testRemoveScriptTag() {
        String input = "<p>Safe text<script>alert('hack');</script></p>";
        String expected = "<p>Safe text</p>";
        String result = HdHtmlSanitizer.sanitizeHtml(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should strip disallowed attributes")
    void testRemoveDisallowedAttributes() {
        String input = "<p onclick=\"evil()\">Click me</p>";
        String expected = "<p>Click me</p>";
        String result = HdHtmlSanitizer.sanitizeHtml(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should remove disallowed tags like iframe and object")
    void testRemoveIframeAndObject() {
        String input = "<iframe src=\"evil.com\"></iframe><object>hack</object><p>ok</p>";
        String expected = "hack<p>ok</p>";
        String result = HdHtmlSanitizer.sanitizeHtml(input);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should allow list item with class")
    void testAllowLiWithClass() {
        String input = "<ul><li class=\"bullet\">Item</li></ul>";
        String expected = "<ul><li class=\"bullet\">Item</li></ul>";
        String result = HdHtmlSanitizer.sanitizeHtml(input);
        assertEquals(expected, result);
    }
}