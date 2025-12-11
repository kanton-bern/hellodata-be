package ch.bedag.dap.hellodata.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HdHtmlSanitizerTest {


    static Stream<TestCase> htmlSanitizerCases() {
        return Stream.of(
                new TestCase(null, ""),
                new TestCase("<p class=\"text\">Hello <span class=\"highlight\" style=\"color:red\">World</span></p>", "<p class=\"text\">Hello <span class=\"highlight\" style=\"color:red\">World</span></p>"),
                new TestCase("<p>Safe text<script>alert('hack');</script></p>", "<p>Safe text</p>"),
                new TestCase("<p onclick=\"evil()\">Click me</p>", "<p>Click me</p>"),
                new TestCase("<iframe src=\"evil.com\"></iframe><object>hack</object><p>ok</p>", "hack<p>ok</p>"),
                new TestCase("<ul><li class=\"bullet\">Item</li></ul>", "<ul><li class=\"bullet\">Item</li></ul>")
        );
    }

    @ParameterizedTest(name = "{index}: sanitizeHtml({0}) == {1}")
    @MethodSource("htmlSanitizerCases")
    @DisplayName("Sanitizes HTML input according to custom policy")
    void testSanitizeHtml(TestCase testCase) {
        String result = HdHtmlSanitizer.sanitizeHtml(testCase.input);
        assertEquals(testCase.expected, result);
    }

    record TestCase(String input, String expected) {

        @Override
        public String toString() {
            return "input=" + input + ", expected=" + expected;
        }
    }
}