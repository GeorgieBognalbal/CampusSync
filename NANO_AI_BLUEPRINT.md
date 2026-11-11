# CampusSense: Local Nano AI Engine & UI Blueprint
**COESC x OCTA : Hack4Success 2026**  
**Track:** Student Around Services (LSPU-SPCC)  
**Assigned Module:** Local Nano AI Engine & UI Navigation (Member 1)  
**Selected Architecture:** Path 3 (Hybrid Edge-First Nano RAG with Cloud Fallback)

---

## 1. System Architecture & Query Pipeline
CampusSense utilizes an on-device **Retrieval-Augmented Generation (RAG)** pipeline executing in 4 lightweight stages:

1. **Intent & Tokenizer**: Bilingual parser (English + Tagalog/Taglish) detects intents:
   - `SUSPENSION` (e.g., *"may pasok ba"*, *"walang klase"*, *"bagyo"*, *"typhoon"*)
   - `SCHOLARSHIP` (e.g., *"tdp"*, *"ched"*, *"allowance"*, *"osas requirements"*)
   - `ACADEMICS` (e.g., *"midterm"*, *"final exam"*, *"permit"*, *"grades"*)
   - `EVENTS` (e.g., *"intramurals"*, *"coesc"*, *"octa"*, *"webinar"*)
   - `GENERAL` / `UNKNOWN` (fallback handling)
2. **Context Ranker**: Evaluates cached Room SQLite announcements with scoring:
   $$\text{Score} = (\text{IntentWeight} \times 0.4) + (\text{KeywordHits} \times 0.4) + (\text{Recency} \times 0.2)$$
3. **Answer Synthesizer**: Extracts operative facts (status, dates, guidelines) and compiles a natural language response. Prevents hallucinations by strictly grounding in local cached posts.
4. **Source Citation**: Attaches original post metadata (page name, date, and Facebook permalink) for instant verification.

---

## 2. Hardware & Performance Target
- **Target Device:** Android 12 (API 31), Qualcomm Snapdragon 460 (8x Kryo 240 cores), 3–4GB RAM.
- **Memory Consumption:** < 20 MB RAM during query execution.
- **Inference Latency:** < 15 milliseconds (sub-second instant response).
- **Network Dependency:** 0% for core queries (100% functional in Airplane Mode).

---

## 3. Class Interface Specifications

### A. `LocalNanoAIEngine.java` (`com.example.campussync.ai`)
```java
package com.example.campussync.ai;

import com.example.campussync.data.local.AnnouncementEntity;
import java.util.List;

public class LocalNanoAIEngine {

    public enum Intent {
        SUSPENSION, SCHOLARSHIP, ACADEMICS, EVENTS, GENERAL, UNKNOWN
    }

    public static class NanoAIResult {
        public final String answer;
        public final Intent detectedIntent;
        public final float confidence;
        public final AnnouncementEntity primarySource;
        public final boolean isOfflineAnswer;

        public NanoAIResult(String answer, Intent intent, float confidence, 
                            AnnouncementEntity source, boolean isOffline) {
            this.answer = answer;
            this.detectedIntent = intent;
            this.confidence = confidence;
            this.primarySource = source;
            this.isOfflineAnswer = isOffline;
        }
    }

    public NanoAIResult processQuery(String userQuery, List<AnnouncementEntity> cachedPosts) {
        // 1. Normalize and extract intent
        // 2. Score candidate posts from Room DB
        // 3. Synthesize natural language answer with citation
        // Returns structured result ready for Chat UI
    }
}
```

---

## 4. UI Navigation & Feed Updates (`MainActivity.java`)
- **Branding:** Rebrand all references to **CampusSense** (*"Intelligent Campus Information Assistant"*).
- **Status Indicator:** Display `● NANO AI READY (OFFLINE)` in green (`#247A57`) to visually prove edge autonomy.
- **Category Filter Pills on Feed:**
  `[ All ]` `[ 🌧️ Suspensions ]` `[ 🎓 Scholarships ]` `[ 📚 Academics ]` `[ 📢 Events ]`
- **Chat Hook:** Connect user inputs to `LocalNanoAIEngine.processQuery()` and render answers with a direct `View Original Post ↗` button.

---

## 5. Golden Test Cases for Live Demonstration
| Query | Intent | Expected Nano AI Response |
| :--- | :--- | :--- |
| *"May pasok ba bukas sa LSPU?"* | `SUSPENSION` | "Classes remain normal as of today." (or "Classes are suspended tomorrow due to Tropical Cyclone Signal #2 according to LSPU Official. [View Post]") |
| *"Kailan deadline ng TDP scholarship?"* | `SCHOLARSHIP` | "According to SPCC OSAS, the submission deadline for TDP documents is Friday, Nov 14 at 5:00 PM at the OSAS office." |
| *"Midterm exam schedule"* | `ACADEMICS` | "Midterm examinations are scheduled on Nov 24–28 per the LSPU Academic Calendar advisory." |

---

## 6. Live Pitch Climax Strategy (CTE Function Hall)
1. Mirror phone screen to projector using `scrcpy`.
2. Swipe down the Android status bar and toggle **AIRPLANE MODE**.
3. Ask the chatbot: *"May pasok ba bukas?"*
4. Show the instant response generated in < 15ms with full source citation.
5. Pitch line: *"While traditional chatbots fail when campus Wi-Fi drops, CampusSense's Local Nano AI ensures no student ever misses a life-critical announcement."*
