package com.sonyericsson.jenkins.plugins.bfa.model.indication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for {@link Indication}.
 *
 * @author Ken Petti &lt;kpetti@constantcontact.com&gt;
 */
public class IndicationTest {

    /**
     * Test two {@link Indication} instances are equal.
     */
    @Test
    public void testEquals() {
        BuildLogIndication i1 = new BuildLogIndication("regex");
        BuildLogIndication i2 = new BuildLogIndication("regex");
        assertEquals(i1, i2);
    }

    /**
     * Test two {@link Indication} instances are not equal.
     */
    @Test
    public void testNotEquals() {
        BuildLogIndication i1 = new BuildLogIndication("foo");
        BuildLogIndication i2 = new BuildLogIndication("bar");
        assertNotEquals(i1, i2);
    }
}
