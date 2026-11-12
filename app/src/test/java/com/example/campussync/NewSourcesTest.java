package com.example.campussync;

import com.example.campussync.ai.CampusKnowledgeBase;
import com.example.campussync.ai.LocalNanoAIEngine;
import com.example.campussync.data.local.AnnouncementEntity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NewSourcesTest {

    private LocalNanoAIEngine engine;

    @Before
    public void setup() {
        engine = new LocalNanoAIEngine();
    }

    @Test
    public void testOctaStaticKnowledge() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("Ano ang CCSOCTA?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertNotNull(result.answer);
        Assert.assertTrue(result.answer.contains("CCSOCTA") || result.answer.contains("Organization of Computer Technology Advocates"));
        Assert.assertTrue(result.answer.contains("College of Computer Studies"));
    }

    @Test
    public void testTechAdStaticKnowledge() {
        LocalNanoAIEngine.NanoAIResult result =
                engine.processQuery("What is TechAd?", Collections.emptyList());

        Assert.assertTrue(result.isOfflineAnswer);
        Assert.assertNotNull(result.answer);
        Assert.assertTrue(result.answer.contains("The Technology Advocate") || result.answer.contains("TechAd"));
        Assert.assertTrue(result.answer.contains("student publication") || result.answer.contains("journalism"));
    }

    @Test
    public void testDynamicPostRetrievalForTechAdAndOcta() {
        AnnouncementEntity octaPost = new AnnouncementEntity(
                "octa-1",
                "OCTA",
                "CodeChum National Programming Challenge 2026 Sprint Edition Nov 14!",
                "https://www.facebook.com/CCSOCTA/posts/1",
                System.currentTimeMillis()
        );
        AnnouncementEntity techAdPost = new AnnouncementEntity(
                "techad-1",
                "TechAd",
                "Results for the Editorial Board Examination 2026 released by The Technology Advocate.",
                "https://www.facebook.com/techadspc/posts/1",
                System.currentTimeMillis()
        );

        LocalNanoAIEngine.NanoAIResult resultOcta =
                engine.processQuery("May event ba ang OCTA para sa programming challenge?", Arrays.asList(octaPost, techAdPost));
        Assert.assertNotNull(resultOcta.answer);
        Assert.assertEquals("OCTA", resultOcta.primarySource.pageName);

        LocalNanoAIEngine.NanoAIResult resultTechAd =
                engine.processQuery("May latest announcement ba ang TechAd tungkol sa editorial board?", Arrays.asList(octaPost, techAdPost));
        Assert.assertNotNull(resultTechAd.answer);
        Assert.assertNotNull("Expected primarySource to not be null", resultTechAd.primarySource);
        Assert.assertEquals("TechAd", resultTechAd.primarySource.pageName);
    }
}
