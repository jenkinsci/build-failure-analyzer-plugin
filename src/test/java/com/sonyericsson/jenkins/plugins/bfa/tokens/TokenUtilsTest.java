package com.sonyericsson.jenkins.plugins.bfa.tokens;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the plugin can wrap token macro output.
 */
class TokenUtilsTest {
    private static final String TEST_TEXT = """
                    Lorem ipsum dolor sit amet,
                    consectetur adipiscing elit. Nulla euismod sapien ligula,
                    
                        ac euismod quam aliquet vel.
                        Duis quam augue, tristique in mi ac, scelerisque
                        euismod nibh.
                    
                    Nulla accumsan velit nec neque sollicitudin,
                    eget sagittis purus vestibulum. Nunc cursus ornare sapien
                    sit amet hendrerit. Proin non nisi sapien.""";

    /**
     * Test that wrap() with no additional wrapping works appropriately.
     *
     */
    @Test
    void testNoAdditionalWrap() {
        final int noWrapping = 0;
        final List<String> unwrappedLines = TokenUtils.wrap(TEST_TEXT, noWrapping);
        System.out.println("Unwrapped lines:");
        for (final String line : unwrappedLines) {
            System.out.println(line);
        }
        final int expectedNoWrappingLineCount = 10;
        assertEquals(expectedNoWrappingLineCount, unwrappedLines.size());
    }

    /**
     * Test that wrap() works appropriately.
     *
     */
    @Test
    void testWrap() {
        final int wrapAt35 = 35;
        final List<String> wrappedAt35 = TokenUtils.wrap(TEST_TEXT, wrapAt35);
        System.out.println("Wrapped at 35:");
        for (final String line : wrappedAt35) {
            System.out.println(line);
        }
        final int expectedWrapAt35LineCount = 15;
        assertEquals(expectedWrapAt35LineCount, wrappedAt35.size());
    }
}
