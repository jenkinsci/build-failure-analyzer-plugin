package com.sonyericsson.jenkins.plugins.bfa.tokens;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests that the plugin can wrap token macro output.
 */
public class TokenUtilsTest {
    private static final String TEST_TEXT =
            "Lorem ipsum dolor sit amet,\n"
                    + "consectetur adipiscing elit. Nulla euismod sapien ligula,\n"
                    + "\n"
                    + "    ac euismod quam aliquet vel.\n"
                    + "    Duis quam augue, tristique in mi ac, scelerisque\n"
                    + "    euismod nibh.\n"
                    + "\n"
                    + "Nulla accumsan velit nec neque sollicitudin,\n"
                    + "eget sagittis purus vestibulum. Nunc cursus ornare sapien\n"
                    + "sit amet hendrerit. Proin non nisi sapien.";

    /**
     * Test that wrap() with no additional wrapping works appropriately.
     *
     * @throws Exception if necessary
     */
    @Test
    public void testNoAdditionalWrap() throws Exception {
        final int noWrapping = 0;
        final List<String> unwrappedLines = TokenUtils.wrap(TEST_TEXT, noWrapping);
        System.out.println("Unwrapped lines:");
        for (final String line : unwrappedLines) {
            System.out.println(line);
        }
        final int expectedNoWrappingLineCount = 10;
        Assert.assertEquals(expectedNoWrappingLineCount, unwrappedLines.size());
    }

    /**
     * Test that wrap() works appropriately.
     *
     * @throws Exception if necessary
     */
    @Test
    public void testWrap() throws Exception {
        final int wrapAt35 = 35;
        final List<String> wrappedAt35 = TokenUtils.wrap(TEST_TEXT, wrapAt35);
        System.out.println("Wrapped at 35:");
        for (final String line : wrappedAt35) {
            System.out.println(line);
        }
        final int expectedWrapAt35LineCount = 15;
        Assert.assertEquals(expectedWrapAt35LineCount, wrappedAt35.size());
    }
}
