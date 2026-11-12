package com.example.campussync;

import com.example.campussync.ai.CampusKnowledgeBase;
import com.example.campussync.ai.LocalNanoAIEngine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class StudentHandbookTest {

    private LocalNanoAIEngine engine;

    @Before
    public void setup() {
        engine = new LocalNanoAIEngine();
    }

    @Test
    public void testCheatingSanctionsGrounded() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano parusa sa cheating sa exam?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertNotNull(result.answer);
        Assert.assertTrue(result.answer.contains("failed"));
        Assert.assertTrue(result.answer.contains("5.0"));
        Assert.assertTrue(result.answer.contains("Dismissal"));
        Assert.assertEquals("Chapter 6, Article 3, Sec. 2", result.handbookCitation);
    }

    @Test
    public void testDressCodeAndProhibitedAttire() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Bawal ba ang shorts at tsinelas sa campus?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("Shorts"));
        Assert.assertTrue(result.answer.contains("Slippers"));
        Assert.assertEquals("Chapter 6, Article 4, Sec. 1", result.handbookCitation);
    }

    @Test
    public void testMaleHaircutRegulations() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang rules sa haircut ng lalaki?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("2x3"));
        Assert.assertTrue(result.answer.contains("1x2"));
        Assert.assertEquals("Chapter 6, Article 4, Sec. 2", result.handbookCitation);
    }

    @Test
    public void testLatinHonorsCriteria() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang criteria para sa Cum Laude?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("1.46 to 1.75"));
        Assert.assertTrue(result.answer.contains("2.00"));
        Assert.assertEquals("Appendix B, Article 1", result.handbookCitation);
    }

    @Test
    public void testMaximumResidenceRule() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang maximum residency rule (MRR)?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("1.5 times"));
        Assert.assertTrue(result.answer.contains("six (6) years"));
        Assert.assertEquals("Chapter 3, Article 11", result.handbookCitation);
    }

    @Test
    public void testINCResolutionAndAutomaticFive() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Paano maayos ang INC grade at kailan nagiging 5.0?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("one (1) academic year"));
        Assert.assertTrue(result.answer.contains("5.00"));
        Assert.assertEquals("Chapter 3, Article 7, Sec. 5.4", result.handbookCitation);
    }

    @Test
    public void testLGBTQGenderRepresentation() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang policy sa transgender at LGBT uniform?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("Transwomen"));
        Assert.assertTrue(result.answer.contains("Transmen"));
        Assert.assertEquals("Chapter 6, Article 4, Sec. 6", result.handbookCitation);
    }

    @Test
    public void testMandatoryDrugTesting() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Paano ang mandatory drug testing sa LSPU?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("RA 9165"));
        Assert.assertTrue(result.answer.contains("confidential"));
        Assert.assertEquals("Appendix C, Sec. 1-8", result.handbookCitation);
    }

    @Test
    public void testGraduationTreePlanting() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Kailangan ba ng tree planting bago grumaduate?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("ten (10) seedlings"));
        Assert.assertEquals("Chapter 3, Article 14", result.handbookCitation);
    }

    @Test
    public void testGrade4RemovalPolicy() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Paano maalis ang 4.00 conditional grade?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("one (1) academic year"));
        Assert.assertTrue(result.answer.contains("3.00"));
        Assert.assertEquals("Chapter 3, Article 7, Sec. 5.3", result.handbookCitation);
    }

    @Test
    public void testScholasticDelinquencyProbationDismissal() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang rules sa scholastic delinquency at academic probation?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("Probation"));
        Assert.assertTrue(result.answer.contains("Dismissal"));
        Assert.assertEquals("Chapter 3, Article 5, Sec. 4", result.handbookCitation);
    }

    @Test
    public void testStudentRightsBatasPambansa232() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang mga student rights sa LSPU batay sa Batas Pambansa 232?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("Batas Pambansa Blg. 232"));
        Assert.assertEquals("Appendix A, Article 1 & 2", result.handbookCitation);
    }

    @Test
    public void testMaleEarringsPolicy() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Bawal ba mag hikaw ang lalaki sa LSPU?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertTrue(result.answer.contains("strictly prohibited"));
        Assert.assertEquals("Chapter 6, Article 4, Sec. 3-5", result.handbookCitation);
    }
}
