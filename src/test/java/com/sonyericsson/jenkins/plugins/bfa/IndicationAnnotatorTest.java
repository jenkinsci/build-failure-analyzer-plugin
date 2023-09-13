package com.sonyericsson.jenkins.plugins.bfa;

import static org.junit.Assert.assertTrue;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.indication.FoundIndication;
import hudson.MarkupText;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the IndicationAnnotator.
 *
 * @author Tomas Westling &lt;tomas.westling@axis.com&gt;
 */
public class IndicationAnnotatorTest {

    private static final String EXPECTED_ANNOTATED_TEXT = "tilt&quot; onmouseover=alert(1) foo=&quot;bar";

    /**
     * Tests that html is escaped correctly when annotating text.
     */
    @Issue("SECURITY-3244")
    @Test
    public void testAnnotate() {
        MarkupText text = new MarkupText("matchingString");
        FoundIndication fi = new FoundIndication(
                "pattern", "matchingFile", "matchingString");
        List<FoundIndication> fis = new ArrayList<>();
        fis.add(fi);
        FoundFailureCause ffc = new FoundFailureCause(
                new FailureCause("tilt\" onmouseover=alert(1) foo=\"bar", "description"), fis);
        List<FoundFailureCause> foundFailureCauses = new ArrayList<>();
        foundFailureCauses.add(ffc);
        IndicationAnnotator ia = new IndicationAnnotator(foundFailureCauses);
        ia.annotate(null, text);
        assertTrue(text.toString(false).indexOf(EXPECTED_ANNOTATED_TEXT) != -1);
    }
}
