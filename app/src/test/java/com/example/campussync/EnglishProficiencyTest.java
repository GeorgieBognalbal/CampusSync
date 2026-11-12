package com.example.campussync;

import com.example.campussync.ai.CampusKnowledgeBase;
import com.example.campussync.ai.LocalNanoAIEngine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class EnglishProficiencyTest {
    private LocalNanoAIEngine engine;

    @Before
    public void setup() {
        engine = new LocalNanoAIEngine();
    }

    @Test
    public void testEnglishProficiencyCheck() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("What is the English proficiency check?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertNotNull(result.answer);
        Assert.assertTrue(result.answer.contains("85 in English"));
        Assert.assertTrue(result.answer.contains("Board Examination"));
        Assert.assertEquals("Chapter 3, Article 2, Sec. 1-2 & 2.6", result.handbookCitation);
    }

    @Test
    public void testEnglishBoardRequirement() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Is there an English grade requirement for board programs?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertNotNull(result.answer);
        Assert.assertTrue(result.answer.contains("85"));
        Assert.assertEquals("Chapter 3, Article 2, Sec. 1-2 & 2.6", result.handbookCitation);
    }

    @Test
    public void testEnglishSuspensionQueries() {
        String[] suspensionQueries = {
            "Are there classes tomorrow?",
            "Are classes suspended today?",
            "Is there school tomorrow?",
            "Do we have classes tomorrow?",
            "Are classes suspended tomorrow?"
        };

        for (String q : suspensionQueries) {
            LocalNanoAIEngine.NanoAIResult result = engine.processQuery(q, Collections.emptyList());
            Assert.assertEquals(LocalNanoAIEngine.Intent.SUSPENSION, result.detectedIntent);
            Assert.assertFalse("Should not be fallback rejection for: " + q,
                    result.answer.contains("LSPU Campus Inquiries Only") || result.answer.contains("Hindi ko mahanap"));
        }
    }

    @Test
    public void testEnglishDressCodeQueries() {
        String[] dressQueries = {
            "Can I wear shorts?",
            "Can I wear slippers?",
            "What is the dress code?",
            "What are the rules on dress code?"
        };

        for (String q : dressQueries) {
            LocalNanoAIEngine.NanoAIResult result = engine.processQuery(q, Collections.emptyList());
            Assert.assertEquals("Chapter 6, Article 4, Sec. 1", result.handbookCitation);
            Assert.assertTrue(result.answer.contains("PROHIBITED ATTIRE"));
        }
    }

    @Test
    public void testEnglishCampusFacilities() {
        Assert.assertEquals("Chapter 5", engine.processQuery("Where is the registrar?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5", engine.processQuery("Where is the OSAS office?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5, Article 4, Sec. 1", engine.processQuery("Where is the clinic?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5, Article 5, Sec. 1", engine.processQuery("Where is the library?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5, Article 5, Sec. 1", engine.processQuery("What are the library hours?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5, Article 4, Sec. 1", engine.processQuery("What are the clinic hours?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 5, Article 3, Sec. 1", engine.processQuery("How to get a good moral certificate?", Collections.emptyList()).handbookCitation);
    }

    @Test
    public void testEnglishAcademicsAndRules() {
        Assert.assertEquals("Chapter 6, Article 3, Sec. 2", engine.processQuery("What happens if I get caught cheating?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 6, Article 4, Sec. 3-5", engine.processQuery("Can male students wear earrings?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 6, Article 4, Sec. 2", engine.processQuery("What are the rules on male haircut?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 6, Article 4, Sec. 2", engine.processQuery("Can guys have long hair?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 7, Sec. 1", engine.processQuery("What is the passing grade?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 7, Sec. 5.3", engine.processQuery("What happens if I get a 4.0 grade?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 7, Sec. 5.4", engine.processQuery("How do I resolve an INC grade?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 6, Sec. 4", engine.processQuery("How to drop a subject?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 2, Sec. 2.10", engine.processQuery("How to shift course?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Appendix B, Article 1", engine.processQuery("What is the criteria for cum laude?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 11", engine.processQuery("What is the maximum residency rule?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Chapter 3, Article 10", engine.processQuery("What are the requirements for TDP scholarship?", Collections.emptyList()).handbookCitation);
        Assert.assertEquals("Appendix A, Article 1 & 2", engine.processQuery("What are the student rights under Batas Pambansa 232?", Collections.emptyList()).handbookCitation);
    }

    @Test
    public void testEnglishConversationalGreetingsAndGratitude() {
        LocalNanoAIEngine.NanoAIResult morning = engine.processQuery("Good morning!", Collections.emptyList());
        Assert.assertEquals(LocalNanoAIEngine.Intent.GREETING, morning.detectedIntent);
        Assert.assertTrue(morning.answer.contains("Good morning"));

        LocalNanoAIEngine.NanoAIResult thanks = engine.processQuery("Thank you so much!", Collections.emptyList());
        Assert.assertEquals(LocalNanoAIEngine.Intent.GRATITUDE, thanks.detectedIntent);
        Assert.assertTrue(thanks.answer.contains("You're very welcome"));
    }
}
