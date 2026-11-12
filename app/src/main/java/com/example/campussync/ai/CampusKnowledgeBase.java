package com.example.campussync.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * LSPU Student Handbook 2021 & Campus Knowledge Base
 * (Doc. No.: LSPU-PM-LSH-01 • Approved pursuant to Board Resolution No. 111, series of 2021)
 *
 * Provides comprehensive, 100% on-device encyclopedic student guidance for
 * Laguna State Polytechnic University (LSPU-SPCC).
 *
 * Covers:
 * 1. Undergraduate Academic Policies (Grading, 4.00 removal, INC resolution, GWA, Retention,
 *    Scholastic Delinquency, MRR, Overload, Dropping, Refund, Attendance, Thesis, Graduation, Tree Planting)
 * 2. Code of Conduct & Student Discipline (Major Offenses, Minor Offenses, Sanctions Table, Cheating,
 *    Due Process & Discipline Committee)
 * 3. Norms, Decorum, Uniform & Grooming (Dress Code, Banned Attire, Haircut SOP, LGBTQ+ Accommodations,
 *    Earrings, Caps, Tattoos)
 * 4. Student Services & Welfare (OSAS, Guidance, Clinic, Library, Dorms, MDTP Drug Testing, OJT/SIPP,
 *    Mental Health & Psychological First Aid)
 * 5. Appendices (Batas Pambansa 232 Student Rights & Duties, Latin Honors Criteria, Leadership & Sports Awards)
 * 6. Campus Directory, Office Hours & Emergency Hotlines
 * 7. Weather Suspensions & Typhoon Protocols
 */
public class CampusKnowledgeBase {

    public static class KnowledgeEntry {
        public final String title;
        public final String content;
        public final LocalNanoAIEngine.Intent intent;
        public final List<String> triggerKeywords;
        public final List<String> keyPhrases;
        public final String citation;
        public final String category;

        public KnowledgeEntry(String title, String content, LocalNanoAIEngine.Intent intent,
                              List<String> triggerKeywords, List<String> keyPhrases,
                              String citation, String category) {
            this.title = title;
            this.content = content;
            this.intent = intent;
            this.triggerKeywords = triggerKeywords;
            this.keyPhrases = keyPhrases;
            this.citation = citation;
            this.category = category;
        }

        public KnowledgeEntry(String title, String content, LocalNanoAIEngine.Intent intent,
                              List<String> triggerKeywords, List<String> keyPhrases) {
            this(title, content, intent, triggerKeywords, keyPhrases,
                    "LSPU Student Handbook 2021", "General");
        }
    }

    private static final List<KnowledgeEntry> ENTRIES = new ArrayList<>();

    static {
        // =========================================================================
        // 1. ACADEMIC POLICIES & GRADING SYSTEM (Chapter 3)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "LSPU Official Grading System Scale",
                "📊 **LSPU Official Grading System Scale**\n\n" +
                "• **1.00** (99–100%) - Excellent\n" +
                "• **1.25** (96–98%) - Outstanding\n" +
                "• **1.50** (93–95%) - Very Satisfactory\n" +
                "• **1.75** (90–92%) - Good\n" +
                "• **2.00** (87–89%) - Satisfactory\n" +
                "• **2.25** (84–86%)\n" +
                "• **2.50** (81–83%) - Fairly Satisfactory\n" +
                "• **2.75** (78–80%)\n" +
                "• **3.00** (75–77%) - Passing (Lowest passing mark)\n" +
                "• **4.00** (70–74%) - Conditional Failure (needs removal exam within 1 yr)\n" +
                "• **5.00** (69% and below) - Failed\n" +
                "• **INC** - Incomplete (missed final exam or major requirement)\n" +
                "• **DRP** - Officially Dropped\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 7, Sec. 1*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("grading", "grades", "scale", "passing", "singko", "tres", "inc", "drp", "marka", "score", "grade", "4.00", "5.00", "1.00"),
                Arrays.asList("grading system", "passing grade", "anong passing", "grades scale", "ano ang tres", "ano ang singko", "lowest passing", "ano passing grade", "passing grade sa lspu", "grading scale"),
                "Chapter 3, Article 7, Sec. 1",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Policy on Grade 4.00 (Conditional Failure) Removal",
                "⚠️ **Removal Policy for Grade of 4.00 (Conditional Failure)**\n\n" +
                "1. **Removal Examination**: A grade of `4.00` may be removed either by re-examination or repeating the subject.\n" +
                "2. **Grade Ceiling**: The highest grade a student can obtain upon removal examination is **3.00**. If the removal examination is failed, the grade becomes **5.00**.\n" +
                "3. **Prescriptive Period**: Only one (1) removal examination is allowed, which must be taken within **one (1) academic year** from the date the 4.00 was received.\n" +
                "4. **Automatic 5.0**: If not removed within the 1-year period, it is automatically converted into a permanent **5.00**.\n" +
                "5. **Prerequisite Rule**: If the course is a prerequisite for the next semester, the 4.00 must be removed before next enrolment.\n" +
                "6. **Removal Fee**: ₱20.00 per unit during regular removal period (₱100.00 per unit for special removal exam).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 7, Sec. 5.3*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("4.00", "conditional", "removal", "tanggal", "re-examination", "alisin", "four"),
                Arrays.asList("grade of 4.00", "paano alisin ang 4", "removal exam", "removal of 4.00", "conditional grade", "ano gagawin sa 4.00", "paano maalis ang 4"),
                "Chapter 3, Article 7, Sec. 5.3",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Incomplete (INC) Grade Resolution & Automatic 5.0 Rule",
                "📝 **Incomplete (INC) Mark Resolution Procedure**\n\n" +
                "1. **Eligibility**: An `INC` is given ONLY when class standing is passing, but the student missed the final exam due to illness/valid reason (with approved excuse slip) or lacks a major course requirement.\n" +
                "2. **1-Year Time Limit**: An `INC` must be completed within **one (1) academic year** from the semester it was incurred.\n" +
                "3. **Automatic 5.0**: If NOT completed within the 1-year period, the grade of `INC` will **automatically be changed to 5.00 (Failed)** by the Registrar upon confirmation with the professor or Dean.\n" +
                "4. **Filing Procedure**: Obtain an official Completion Form from the Registrar, complete the lacking requirement with the instructor, and submit the signed form.\n" +
                "5. **Completion Fee**: ₱20.00 per unit (₱100.00 for special completion).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 7, Sec. 5.4 & 5.5*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("inc", "incomplete", "completion", "resolution", "kulang", "automatic 5", "singko"),
                Arrays.asList("paano maayos ang inc", "how to resolve inc", "incomplete grade", "ano gagawin sa inc", "pano ayusin ang inc", "resolving inc", "kailan nagiging 5 ang inc", "automatic 5.0 inc"),
                "Chapter 3, Article 7, Sec. 5.4",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Grade Rectification Period & Submission Deadline",
                "⏱️ **Grade Submission & Rectification Policy**\n\n" +
                "• **Faculty Submission Deadline**: Instructors must submit grades to the Registrar no later than **ten (10) working days** after the last day of the examination period.\n" +
                "• **Rectification Period**: A student who receives a grade believed to be erroneous may request in writing for change/rectification within **ten (10) days** after the official release of grades by the Registrar.\n" +
                "• **No Grade Improvement**: A student who has received a passing grade in a course is **not allowed** a re-examination to improve the grade.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 7, Sec. 2 & 3*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("rectification", "mali", "reklamo", "change grade", "ayos grado", "submission", "deadline"),
                Arrays.asList("maling grado", "grade rectification", "reklamo sa grade", "change of grade", "deadline ng grades", "rectification of grades"),
                "Chapter 3, Article 7, Sec. 2 & 3",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "LSPU Retention Policy & Minimum Passing Grades",
                "🎯 **LSPU Retention Policies & Academic Standards**\n\n" +
                "• **Overall Minimum GPA**: Must maintain a minimum Grade Point Average of **2.00** every semester.\n" +
                "• **Board Programs** (Engineering, IT, Nursing, Education, Accountancy, Criminology, Agriculture):\n" +
                "  - Major-related subjects: Minimum final grade of **2.00**\n" +
                "  - Professional subjects: Minimum final grade of **2.00**\n" +
                "  - General Education subjects: **1.75 - 1.00**\n" +
                "• **Non-Board Programs** (HRM, Tourism, Industrial Tech):\n" +
                "  - Major-required subjects: Minimum final grade of **2.25**\n" +
                "  - Professional subjects: Minimum final grade of **2.25**\n" +
                "  - General Education subjects: **2.00 - 1.00**\n" +
                "• **Retake Policy**:\n" +
                "  - Major subject below required grade: Retake allowed **ONCE**; maximum of 2 major subjects retaken from 3rd to 4th year.\n" +
                "  - Grade of 5.00 in ANY General Education / Elective: **Disqualified** from continuing in the chosen curriculum and advised to shift.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 5, Sec. 3*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("retention", "board", "non-board", "maintaining", "gpa", "bawal", "retake", "disqualified"),
                Arrays.asList("retention policy", "minimum gpa", "board program grade", "retake ng major", "retention sa board program", "maintaining grade"),
                "Chapter 3, Article 5, Sec. 3",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Scholastic Delinquency Matrix: Warning, Probation, Dismissal & Disqualification",
                "📉 **Scholastic Delinquency Standards**\n\n" +
                "• **Warning** (Fails 25% – 49% of academic units):\n" +
                "  - Warned by College Dean; allowable load reduced by **3 units** next semester.\n" +
                "• **Probation** (Fails 50% – 74% of units, 6+ units):\n" +
                "  - Placed on academic probation; allowable load capped at **15 units only**. Lifted upon passing all enrolled subjects.\n" +
                "• **Dismissal from College** (Fails 75% or more of units, 9+ units):\n" +
                "  - Dismissed from the College; not allowed to enroll in the program.\n" +
                "• **Permanent Disqualification** (100% Failure):\n" +
                "  - **Permanently barred** from enrollment in Laguna State Polytechnic University.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 5, Sec. 4 & Sec. 8*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("delinquency", "warning", "probation", "dismissal", "disqualification", "bagsak", "drop", "15 units"),
                Arrays.asList("scholastic delinquency", "academic probation", "dismissal sa lspu", "na probation", "permanent disqualification", "ilang bagsak ma dismiss"),
                "Chapter 3, Article 5, Sec. 4",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Maximum Residence Rule (MRR) for Undergraduates",
                "⏳ **Maximum Residence Rule (MRR)**\n\n" +
                "• **Standard Formula**: A student must finish all degree requirements within an actual residence equivalent to **1.5 times the normal length** of the course.\n" +
                "• **4-Year Degree Programs** (e.g. BSCS, BSIT, BSED, BSBA): Maximum of **six (6) years**.\n" +
                "• **5-Year Degree Programs** (e.g. BS Engineering): Maximum of **seven and a half (7.5) years**.\n" +
                "• **Penalty for Exceeding MRR**: The student shall **not be allowed to register** in the university.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 11*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("mrr", "residence", "maximum", "years", "ilang taon", "tagal", "limit"),
                Arrays.asList("maximum residence rule", "mrr policy", "hanggang ilang taon pwede mag aral", "maximum years to finish college", "mrr sa lspu"),
                "Chapter 3, Article 11",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Academic Overloading Guidelines (Graduating Students)",
                "📚 **Academic Overload Guidelines**\n\n" +
                "• **General Rule**: No student is allowed to take more than the normal prescribed semester load.\n" +
                "• **Graduating Students Exemption**: Only a graduating student with an academic record better than average may apply with the **Vice President for Academic Affairs (VPAA)** for an overload.\n" +
                "• **Maximum Overload Allowed**: Not more than **six (6) units or two (2) subjects**.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 6, Sec. 1.3*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("overload", "sobra", "units", "graduating", "vpaa", "magdagdag"),
                Arrays.asList("pwede ba mag overload", "academic overload", "overload guidelines", "ilang units pwede i-overload", "graduating overload"),
                "Chapter 3, Article 6, Sec. 1.3",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Adding, Dropping & Change of Registration Policy",
                "🔄 **Adding, Dropping & Change of Registration**\n\n" +
                "• **Change of Registration**: Taking a new subject is NOT allowed after **12% of regular class meetings** have been held.\n" +
                "• **Dropping Before Midterms**: Dropping any subject before the midterm examination permits a student to drop **without prejudice** (`DRP`).\n" +
                "• **Dropping After Midterms**: If dropped after the 1st day of midterms, the instructor gives a grade of **5.00** if the student's class standing is below 3.0.\n" +
                "• **Unauthorized Dropping**: Dropping without College Dean's approval = **automatic grade of 5.00**.\n" +
                "• **Absence Rule**: Accumulated absence of at least 25% of class hours = automatically dropped from the roll.\n" +
                "• **Dropping Fee**: ₱20.00 per unit (BOR Reso. 1046, s.2012).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 6, Sec. 3 & 4*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("dropping", "drop", "change of registration", "palit", "bawas", "20 per unit", "midterm drop"),
                Arrays.asList("paano mag drop", "drop subject", "dropping policy", "dropping fee", "change of registration", "huling araw ng dropping"),
                "Chapter 3, Article 6, Sec. 4",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Tuition Fee Refund Schedule & Late Registration Fine",
                "💰 **Registration Fees, Fines & Refund Schedule**\n\n" +
                "• **Late Registration**: Allowed up to one (1) week after regular registration period. Late fine: **₱500.00** (BOR Reso. 1046, s.2012).\n" +
                "• **Tuition Fee Refund Schedule** (upon approved LOA or Honorable Dismissal):\n" +
                "  - **75% refund**: Within the 1st week from opening of classes.\n" +
                "  - **50% refund**: Within the 2nd to 4th week from opening of classes.\n" +
                "  - **No refund (0%)**: After the 4th week from opening of classes.\n" +
                "• **Non-refundable Fees**: Laboratory, registration, library, and miscellaneous fees will NOT be refunded after registration.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 3, Sec. 1 & 3*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("refund", "late", "fine", "500", "matrikula", "bayad", "pera", "ibalik"),
                Arrays.asList("refund ng tuition", "may refund ba", "late registration fine", "paano mag refund", "tuition refund schedule"),
                "Chapter 3, Article 3, Sec. 1 & 3",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Class Attendance, Tardiness & Guidance Excuse Slips",
                "📋 **Class Attendance, Tardiness & Excuse Slips**\n\n" +
                "• **Attendance Rule**: Students must attend classes and required university activities regularly and on time.\n" +
                "• **Tardiness Equivalents**: Three (3) instances of tardiness in a class equal **one (1) absence**.\n" +
                "• **Excuse Slip Process**: Any student absent must secure an official **Excuse Slip from the Guidance Office / OSAS**.\n" +
                "• **Presentation Deadline**: Must present the excuse slip (with medical cert if due to illness) to the instructor **not later than the 2nd class session** following return.\n" +
                "• **Excessive Absences**: Accumulating unexcused absences equivalent to **25% of total class hours** (or 17% in specific courses) leads to being dropped from the roll.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Art. 8 & Chapter 6, Art. 3, Sec. 1*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("attendance", "absent", "tardiness", "excuse slip", "guidance", "late", "drop absences"),
                Arrays.asList("paano kumuha ng excuse slip", "excuse slip sa guidance", "rules sa absent", "ilang absent bago ma drop", "tardiness rule"),
                "Chapter 3, Article 8",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Course Shifting & Transferring to Another College",
                "🔀 **Course Shifting & Transfer Regulations**\n\n" +
                "• **Shifting Within LSPU** (to any BS Program):\n" +
                "  1. No failure of greater than **six (6) units** during the semester.\n" +
                "  2. No failing grade in major/course-related subjects stated in target curriculum retention policy.\n" +
                "  3. Must accomplish the official Shifting Form from the Office of the Registrar.\n" +
                "• **Transferring to Another College**:\n" +
                "  - Must obtain recommendation from previous Dean and approval from Registrar before **12% of class meetings**.\n" +
                "  - Must finish no less than **60% of units** in LSPU (and 75% if running for honors).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 2, Sec. 2.9 & 2.10*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("shifting", "shift", "transfer", "lipat", "kurso", "palit", "requirements"),
                Arrays.asList("paano mag shift", "shifting requirements", "rules sa pag shift", "transfer sa ibang college", "requirements sa pagpalit ng kurso"),
                "Chapter 3, Article 2, Sec. 2.10",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Undergraduate Thesis Guidelines & Manuscript Submission",
                "📖 **Undergraduate Thesis Guidelines**\n\n" +
                "• **Thesis Outline Panel**: Approved by a panel comprising Panel Chair, Statistician, English Critic, Subject Specialist, College Dean, and Research Director.\n" +
                "• **Defense**: Conducted under supervision of Thesis Adviser before final writing.\n" +
                "• **Manuscript Submission**: After approval, **five (5) typewritten copies** must be presented to the Registrar's Office at least **two (2) weeks** before faculty acts on candidacy for graduation.\n" +
                "• **Signatories on Approval Sheet**: Adviser, panel members, Dean/Associate Dean, Registrar, and Research Contribution Number.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 13*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("thesis", "manuscript", "defense", "outline", "panel", "undergraduate thesis"),
                Arrays.asList("thesis requirements", "thesis defense guidelines", "ilang kopya ng thesis", "thesis outline approval", "undergraduate thesis"),
                "Chapter 3, Article 13",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Graduation Requirements & Mandatory Tree Planting",
                "🎓 **Graduation Requirements & Mandatory Tree Planting**\n\n" +
                "No student shall graduate without satisfying all of the following:\n" +
                "1. **Curriculum Completion**: Completed all academic and prescribed university subjects.\n" +
                "2. **Mandatory Tree Planting**: Individually planted a minimum of **ten (10) seedlings per year** in designated college areas (National Greening Program EO No. 26). *(Deferred in times of pandemic)*.\n" +
                "3. **Clearance**: Full settlement of financial and property accountabilities.\n" +
                "4. **Residency**: Completed at least 50% of academic units and in residence for at least two (2) years in LSPU (including senior year).\n" +
                "5. **Commencement Exercises**: Mandatory attendance. Graduacion in absencia is allowed only via written approval from the University President for serious illness.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 14*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("graduation", "graduate", "tree planting", "seedlings", "tanim", "clearance", "residency", "graduacion in absencia"),
                Arrays.asList("requirements for graduation", "ano requirements para grumaduate", "tree planting requirement", "graduacion in absencia", "mandatory tree planting"),
                "Chapter 3, Article 14",
                "Academics"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Honorable Dismissal & Student Clearance Procedure",
                "📜 **Honorable Dismissal & Clearance Regulations**\n\n" +
                "• **Definition**: Voluntary withdrawal from the university with the consent of the Registrar.\n" +
                "• **Requirements**: Settle all property and financial indebtedness; submit written petition signed by parents/guardians to the Registrar.\n" +
                "• **Disqualification**: A student who leaves the university due to **suspension, dropping, or expulsion for disciplinary reasons is NOT entitled** to Honorable Dismissal.\n" +
                "• **Readmission Note**: A student granted an honorable dismissal may not be readmitted to the university.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 12*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("honorable dismissal", "clearance", "withdraw", "lipat school", "dismissal form"),
                Arrays.asList("paano kumuha ng honorable dismissal", "honorable dismissal requirements", "clearance para sa honorable dismissal"),
                "Chapter 3, Article 12",
                "Academics"
        ));

        // =========================================================================
        // 2. CODE OF CONDUCT & STUDENT DISCIPLINE (Chapter 6)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "Cheating in Examinations & Academic Dishonesty Sanctions",
                "⚖️ **Sanctions for Cheating & Academic Dishonesty**\n\n" +
                "• **Cheating in Examinations** (Kodigo, copying, unauthorized phone usage):\n" +
                "  - **1st Offense**: **Automatically failed** in the examination.\n" +
                "  - **2nd Offense**: **Academic probation** and automatic final grade of **5.00** in the subject.\n" +
                "  - **3rd Offense**: **Dismissal from the University**.\n\n" +
                "• **Other Acts of Dishonesty in Studies**:\n" +
                "  - **1st Offense**: Reprimand with community service.\n" +
                "  - **2nd Offense**: Promissory note signed by Guidance Counselor/Discipline Coordinator + 2 days community service.\n" +
                "  - **Subsequent**: Suspension for two (2) weeks.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 3, Sec. 2*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("cheating", "cheat", "kodigo", "exam", "dishonesty", "parusa", "sanction", "sanctions", "pangongopya", "kopya"),
                Arrays.asList("ano parusa sa cheating", "parusa sa pangongopya", "cheating in examination", "nahuli nag cheat", "cheating sanctions", "parusa kapag nag cheat"),
                "Chapter 6, Article 3, Sec. 2",
                "Discipline"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Major Student Offenses & Disciplinary Penalties Table",
                "🚨 **LSPU Major Student Offenses & Progressive Penalties**\n\n" +
                "• **Deadly Weapons** (Firearms, blades >2.5 inches, explosives):\n" +
                "  - 1st: 1 semester suspension | 2nd: 1 year suspension | 3rd: **Expulsion**\n" +
                "• **Physical Assault / Fist Fighting**:\n" +
                "  - 1st: 1 week suspension + 40 hrs service + damages | 2nd: 1 semester | 3rd: **Expulsion**\n" +
                "• **Stealing / Theft**:\n" +
                "  - 1st: 1 semester suspension + damages | 2nd: **Expulsion**\n" +
                "• **Drinking Alcohol / Drunkenness on Campus**:\n" +
                "  - Drinking: 1st: 1 wk + 40 hrs | 2nd: 1 sem | 3rd: **Expulsion**\n" +
                "  - Drunkenness with injury/property damage: 1st: 1 mo + damages | 2nd: 1 sem | 3rd: **Expulsion**\n" +
                "• **Narcotics / Prohibited Drugs** (RA 9165):\n" +
                "  - 1st Offense: 1 semester suspension or dismissal + mandatory rehab.\n" +
                "• **Vandalism & Destruction of Property**:\n" +
                "  - 1st: 1 month + damages | 2nd: 1 semester + damages | 3rd: **Expulsion**\n" +
                "• **Strikes / Boycotts without Permit**:\n" +
                "  - 1st: 1 month | 2nd: 1 semester | 3rd: **Expulsion**\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 2*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("major offense", "weapons", "baril", "patalim", "assault", "suntukan", "away", "stealing", "magnakaw", "alcohol", "alak", "vandalism", "expulsion"),
                Arrays.asList("major offenses", "parusa sa suntukan", "parusa sa alak", "parusa sa drugs", "parusa sa pagnanakaw", "major offenses and penalties", "list of major offenses"),
                "Chapter 6, Article 2",
                "Discipline"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Acts of Cybercrime Penalties (RA 10175)",
                "💻 **Sanctions for Cybercrime & Online Misconduct (RA 10175)**\n\n" +
                "• **Cyberbullying** (Harassment via electronic means):\n" +
                "  - 1st Offense: Suspension for one (1) month\n" +
                "  - 2nd Offense: Suspension for one (1) semester\n" +
                "  - 3rd Offense: **Expulsion from the University**\n" +
                "• **Hacking / Illegal Account Access**:\n" +
                "  - 1st: 1 month suspension | 2nd: 1 semester | 3rd: Expulsion\n" +
                "• **Immediate Expulsion Offenses (1st Offense)**:\n" +
                "  ❌ **Cybersex** or lascivious exhibition using computer systems\n" +
                "  ❌ **Child Pornography** (RA 9775)\n" +
                "  ❌ **Cyber Libel** under Article 355 of the Revised Penal Code\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 2, Sec. 1*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("cybercrime", "cyberbullying", "hacking", "cyber libel", "online bullying", "scandal", "expulsion"),
                Arrays.asList("parusa sa cyberbullying", "cybercrime rules", "hacking penalty", "cyber libel sa lspu", "cybercrime act"),
                "Chapter 6, Article 2, Sec. 1",
                "Discipline"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Anti-Bullying Policy & Physical Violence (RA 10627)",
                "🛡️ **Anti-Bullying Policies & Penalties (RA 10627)**\n\n" +
                "• **Verbal / Emotional Bullying & Intimidation**:\n" +
                "  - 1st Offense: Suspension for 1 week and 40 hours institutional service.\n" +
                "  - 2nd Offense: Suspension for one (1) month.\n" +
                "  - 3rd Offense: Suspension for one (1) semester.\n" +
                "• **Physical Bullying / Unwanted Contact** (punching, pushing, weapon use):\n" +
                "  - 1st Offense: Suspension for one (1) month.\n" +
                "  - 2nd Offense: Suspension for one (1) semester.\n" +
                "  - 3rd Offense: **Expulsion from the University**.\n" +
                "• **Slanderous Statements & Name-calling**:\n" +
                "  - 1st: 1 month | 2nd: 1 semester | 3rd: Expulsion.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 2, Sec. 5*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("bullying", "bully", "anti-bullying", "pananakot", "away", "panunukso", "foul language"),
                Arrays.asList("anti-bullying policy", "parusa sa bullying", "parusa sa pambu-bully", "physical bullying sanction"),
                "Chapter 6, Article 2, Sec. 5",
                "Discipline"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Minor Student Offenses: ID, Smoking, Littering & Trespassing",
                "⚠️ **Minor Student Offenses & Progressive Penalties**\n\n" +
                "• **Non-wearing of ID and School Uniform**:\n" +
                "  - 1st Action: Denied entry at university gates/classes without OSAS Excuse Slip.\n" +
                "  - 2nd Action: Submit filled-out Promissory Note Form.\n" +
                "  - Subsequent: Suspension for 1 week and 40 hours service.\n" +
                "• **Smoking on Campus**: Strictly prohibited.\n" +
                "  - 1st: Reprimand + community service | 2nd: Promissory note + 2 days service | 3rd: 2 weeks suspension.\n" +
                "• **Littering / Trash Scattering**:\n" +
                "  - 1st: Required to clean area | 2nd: 1 wk + 40 hrs service | 3rd: 2 weeks suspension.\n" +
                "• **Campus Trespassing** (after hours or off-school days without permit):\n" +
                "  - 1st: Reprimand + promissory note | 2nd: Parents informed + 1 wk + 40 hrs | 3rd: 1 month suspension.\n" +
                "• **Overspeeding Vehicles on Campus (>20 kph)**:\n" +
                "  - 1st: Reprimand | 2nd: 1 wk + 40 hrs | 3rd: 1 semester suspension.\n" +
                "• **Unauthorized Fence Jumping**:\n" +
                "  - 1st: Reprimand + community service | 2nd: 1 wk + 40 hrs | 3rd: 1 semester suspension.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 3*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("minor offense", "smoking", "yosi", "sigarilyo", "littering", "basura", "trespassing", "bakod", "gate", "speeding", "promissory note"),
                Arrays.asList("bawal manigarilyo", "parusa sa smoking", "parusa sa pagtapon ng basura", "trespassing sa school", "minor offenses"),
                "Chapter 6, Article 3",
                "Discipline"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Student Disciplinary Procedure & Due Process",
                "⚖️ **Student Disciplinary Due Process & Committee**\n\n" +
                "• **Procedure for Major Disciplinary Actions**:\n" +
                "  1. **1st Action**: Written complaint reported to OSAS through Coordinator of Student Discipline.\n" +
                "  2. **2nd Action**: OSAS investigates through the Student Discipline Committee.\n" +
                "  3. **3rd Action**: All parties concerned shall be afforded **due process**.\n" +
                "  4. **4th Action**: Final investigation report submitted to the **University President** (who has final authority in all cases).\n" +
                "  5. **5th Action**: Guidance Counselor conducts follow-up counseling.\n" +
                "• **Discipline Committee Composition**: OSAS Chairperson (Chair), Discipline Coordinator (Vice Chair), VPAA / Campus Director, College Dean, Chief Security Officer, SSC President, University Legal Consultant.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 6 & 7*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("discipline", "committee", "due process", "reklamo", "complaint", "hearing", "osas"),
                Arrays.asList("disciplinary procedure", "paano mag reklamo", "due process sa lspu", "student discipline committee", "reklamo sa osas"),
                "Chapter 6, Article 6 & 7",
                "Discipline"
        ));

        // =========================================================================
        // 3. NORMS, DECORUM, UNIFORM & GROOMING (Chapter 6, Article 4)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "School Uniform Schedule, Wash Days & Prohibited Attire List",
                "👔 **Official Uniform Policy, Wash Days & Dress Code**\n\n" +
                "• **Uniform Days**: Monday, Tuesday, Thursday, and Friday.\n" +
                "• **Wash / Civilian Days**:\n" +
                "  - **Wednesday**: University T-shirt with blue jeans/maong trousers.\n" +
                "  - **Friday**: Department Shirt with blue jeans/maong trousers.\n\n" +
                "🚫 **STRICTLY PROHIBITED ATTIRE (Barred at University Gates)**:\n" +
                "  ❌ Shorts / short pants, walking shorts, cycling shorts\n" +
                "  ❌ 'Puruntong' style or Cargo shorts\n" +
                "  ❌ Pedal pusher pants, Capri pants\n" +
                "  ❌ Mutilated / jazzed / heavily ripped jeans\n" +
                "  ❌ Faded, torn, wrinkled shirts\n" +
                "  ❌ Jogging pants / jogger pants\n" +
                "  ❌ Miniskirts\n" +
                "  ❌ Sleeveless tops, backless, one-sleeve, spaghetti straps, sandos\n" +
                "  ❌ Plunging necklines / halter tops\n" +
                "  ❌ Leggings\n" +
                "  ❌ Slippers / flip-flops\n\n" +
                "👖 **Female Pants Rule**: Must be slacks or loosely cut trousers, not denim/maong and not very tight fitting.\n" +
                "🏃 **P.E. Uniform**: Worn ONLY during P.E. classes; barred from regular academic classrooms.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 4, Sec. 1*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("uniform", "dress code", "wash day", "wednesday", "friday", "bawal", "shorts", "tsinelas", "slippers", "sando", "damit", "suot", "leggings", "jogging pants", "miniskirt"),
                Arrays.asList("uniform wash day", "kailan wash day", "dress code rules", "pwede ba mag civilian", "anong suot sa wednesday", "bawal ba shorts", "bawal ba tsinelas", "civilian day", "uniform policy", "bawal na damit", "list of prohibited attire"),
                "Chapter 6, Article 4, Sec. 1",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Male Haircut Regulations, Grooming & Inspection SOP",
                "✂️ **LSPU Haircut & Hair Grooming Guidelines**\n\n" +
                "• **Approved Male Haircuts**:\n" +
                "  - **Freshmen & Senior High School**: **2x3 haircut**.\n" +
                "  - **2nd to 4th Year College**: **Clean-cut or 1x2 barbers / decent cut**.\n" +
                "• **Prohibited Styles**: Funky short or fixie crop (shaven sides and long top fastened with ties or stiffened with gel).\n" +
                "• **Hair Color Policy**: Colored/tinted/dyed hair out of the ordinary is prohibited. Minimal aesthetic coloring requires approval from OSAS and Campus Director.\n" +
                "• **SOP Haircut Inspection**: Conducted every **first (1st) Monday of the month** by Guard-on-Duty, Chief of Security, and OSAS.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 4, Sec. 2*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("haircut", "gupit", "buhok", "barbers", "2x3", "1x2", "hair color", "kulay", "fixie", "inspection", "lalaki", "lalake", "male", "rules", "rule"),
                Arrays.asList("haircut rules", "rules sa haircut", "haircut ng lalaki", "haircut sa lalaki", "ano haircut sa lspu", "gupit ng lalaki", "hair inspection", "bawal ba kulay buhok", "2x3 haircut", "haircut policy", "pwede ba may kulay buhok"),
                "Chapter 6, Article 4, Sec. 2",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "LGBTQ+ Gender Representation & Uniform Policy",
                "🌈 **LGBTQ+ Clothing & Gender Representation Policy**\n\n" +
                "• **Gender Expression Right**: Transgender students have the right to dress in accordance with their gender identity and expression.\n" +
                "• **Uniform Specifications**:\n" +
                "  - **Transwomen**: Allowed male uniform pants and polo, but with **female cut blouses** of the same color as the male uniform.\n" +
                "  - **Transmen**: Allowed to wear pants and polo of the **same color as the female uniform**.\n" +
                "• **Grooming Accommodations**:\n" +
                "  - Transgender girls are **NOT** required to have a male haircut, provided their hair is clipped properly (men's tail or fold-over style).\n" +
                "  - Transgender boys are **NOT** required to maintain a female hairstyle, wear make-up, or wear feminine clothing.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 4, Sec. 6 (Gender and Development Unit)*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("lgbt", "lgbtq", "transgender", "gender", "transwoman", "transman", "identity", "gay", "lesbian"),
                Arrays.asList("lgbt uniform rules", "transgender uniform", "pwede ba trans mag suot", "gender representation policy", "lgbt dress code", "transgender haircut"),
                "Chapter 6, Article 4, Sec. 6",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Earrings, Caps & Body Tattoo Regulations",
                "🪪 **Earrings, Caps & Tattoo Guidelines**\n\n" +
                "• **Earrings**:\n" +
                "  - **Male Students**: Wearing of ANY kind of earrings is **strictly prohibited**.\n" +
                "  - **Female Students**: Ornamental, dropped, dangling, nose, eyebrow, tongue, and lip earrings are prohibited.\n" +
                "• **Caps / Headgear**: Wearing of caps or headgear inside any classroom, office, or hall is **strictly prohibited** for both male and female students.\n" +
                "• **Tattoos**: Permanent tattooing on exposed parts of the body is **strictly prohibited**.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 4, Sec. 3, 4, 5*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("earrings", "hikaw", "cap", "caps", "sombrero", "tattoo", "tattoos", "piercing", "bawal"),
                Arrays.asList("pwede ba mag hikaw", "bawal ba hikaw sa lalaki", "tattoo policy", "bawal ba tattoo", "bawal ba cap sa loob", "earrings policy", "hikaw rules"),
                "Chapter 6, Article 4, Sec. 3-5",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Student ID Regulations & Lost ID Replacement",
                "🪪 **Student ID Regulations & Lost ID Replacement**\n\n" +
                "• **'No ID, No Entry' Policy**: The official university ID with lanyard must be worn around the neck at all times upon entering campus premises.\n" +
                "• **ID Lending / Borrowing**: Using or lending someone else's ID or library card is a disciplinary offense.\n" +
                "• **What to do if your ID is Lost**:\n" +
                "  1. **Affidavit of Loss**: Secure a notarized Affidavit of Loss from a notary public.\n" +
                "  2. **OSAS Slip**: Present affidavit to OSAS / Security for an ID Replacement Slip.\n" +
                "  3. **Payment**: Pay the replacement fee at the University Cashier (Admin Ground Floor).\n" +
                "  4. **ID Re-issuance**: Submit receipt and form to MIS/ICT ID Unit.\n" +
                "  5. **Claiming**: Claim replacement ID upon release notice.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 3, Sec. 4 & Chapter 6, Article 4*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("id", "lost id", "nawala", "lanyard", "affidavit", "no id no entry", "replacement"),
                Arrays.asList("nawala ang id ko", "nawala id ko", "lost id", "how to replace lost id", "no id no entry rule", "paano kumuha ng bagong id", "id replacement procedure"),
                "Chapter 6, Article 3, Sec. 4",
                "Campus Life"
        ));

        // =========================================================================
        // 4. APPENDICES: HONORS, AWARDS, DRUG TESTING & INTERNSHIP
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "Latin Honors Criteria (Summa, Magna, Cum Laude) & Dean's List",
                "🎖️ **Latin Honors & Academic Awards Criteria**\n\n" +
                "• **Graduation Latin Honors Scale**:\n" +
                "  - **Summa Cum Laude**: GWA of **1.00 to 1.20**\n" +
                "  - **Magna Cum Laude**: GWA of **1.21 to 1.45**\n" +
                "  - **Cum Laude**: GWA of **1.46 to 1.75**\n\n" +
                "📜 **Mandatory Disqualifying Conditions**:\n" +
                "  1. **Grade Ceiling**: Must NOT have received a grade lower than **2.00 in ALL subjects** (including PE, NSTP/CWTS, and Euthenics).\n" +
                "  2. **Academic Load**: Must have carried a normal full load each semester (not less than 15 units).\n" +
                "  3. **Residency**: Must have completed at least **75% of units in LSPU** and in residence for at least two (2) years immediately prior to graduation.\n" +
                "  4. **No INC or DRP**: Candidate must NOT have any record of Incomplete (INC) or dropped (DRP) subjects.\n" +
                "  5. **Good Moral**: Must possess an unblemished disciplinary record.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix B, Article 1*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("latin", "honors", "cum laude", "magna", "summa", "deans list", "dean's list", "honor roll"),
                Arrays.asList("deans list requirements", "dean's list requirements", "how to be deans list", "latin honors criteria", "gwa for cum laude", "deans list", "magkano gwa sa deans list", "criteria sa cum laude"),
                "Appendix B, Article 1",
                "Awards & Honors"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Non-Academic Major & Minor Student Awards",
                "🏆 **Non-Academic Student Awards & Recognition**\n\n" +
                "• **President's Award for Leadership Excellence**:\n" +
                "  - Awarded to outstanding student leaders / Supreme Student Council (SSC) President.\n" +
                "  - Criteria: 20% Organizational Relevance, 30% Student Welfare, 20% Impact, and recommendation.\n" +
                "• **Athlete of the Year Award**:\n" +
                "  - Technical performance (70%) + Student achievement/modeling (30%). No derogatory records.\n" +
                "• **Student Journalist of the Year & Service Award**:\n" +
                "  - Active contribution in campus publications (min 5 articles/artworks in annuals, editorial board member).\n" +
                "• **Performance Award for On-the-Job Training**:\n" +
                "  - Given to students demonstrating exemplary industry performance in OJT/practicum, certified by superior and Dean.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix B, Article 2, 3 & 4*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("awards", "leadership", "athlete", "journalist", "service award", "ojt award", "parangal"),
                Arrays.asList("student awards", "athlete of the year", "presidents award for leadership", "service award criteria"),
                "Appendix B, Article 2-4",
                "Awards & Honors"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Mandatory Drug Testing Program (MDTP) & Student Rights",
                "🛡️ **Mandatory Drug Testing Program (MDTP) Guidelines**\n\n" +
                "• **Legal Basis**: RA 9165 (Comprehensive Dangerous Drugs Act) and CHED CMO No. 64 s.2017 / BOR Reso. 1362 s.2017.\n" +
                "• **Strict Confidentiality**: All test results are strictly confidential. The University shall NEVER publish or post test results.\n" +
                "• **Non-Incrimination Policy**: Positive confirmatory drug test results shall **NOT be a ground for disciplinary action** or criminal charges, and shall NOT be reflected in academic records.\n" +
                "• **Intervention for Positive Confirmatory Result**:\n" +
                "  1. Confidential conference with parents/guardians and Campus Director.\n" +
                "  2. **40 hours counseling** with the Guidance Counselor.\n" +
                "  3. Repeat drug testing for six (6) consecutive weeks at accredited DOH facility.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix C, Sec. 1–8*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("drug test", "mdtp", "drugs", "urine", "confirmatory", "counseling", "ra 9165"),
                Arrays.asList("mandatory drug testing", "drug test policy", "paano drug test sa lspu", "positive sa drug test", "drug testing guidelines"),
                "Appendix C, Sec. 1-8",
                "Student Services"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Student Internship Program (SIPP / OJT) Guidelines",
                "💼 **Student Internship / OJT Program (SIPP) Policies**\n\n" +
                "• **Legal Basis**: CHED CMO No. 104 s.2017 (Student Internship Program in the Philippines).\n" +
                "• **Maximum Duration**: Up to a maximum of **five (5) months** in accordance with curriculum.\n" +
                "• **Intern Eligibility**:\n" +
                "  1. Officially enrolled in internship subject.\n" +
                "  2. At least **18 years of age** from the start of the internship.\n" +
                "  3. Completed and passed all major/professional subjects before deployment.\n" +
                "  4. Medical Certificate indicating good physical and emotional fitness.\n" +
                "  5. Notarized written parent/guardian consent (waivers not permitted).\n" +
                "• **Concurrent Academic Load**: Allowed to take other subjects up to a **maximum of six (6) units only** during the internship period.\n" +
                "• **Intern Status**: The student is classified as an **intern and not an employee** of the Host Training Establishment (HTE).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix D, Article 1–5*",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("internship", "ojt", "sipp", "practicum", "host training", "hte", "intern"),
                Arrays.asList("ojt requirements", "internship guidelines", "rules sa ojt", "ilang months ang ojt", "parent consent ojt", "sipp guidelines"),
                "Appendix D, Article 1-5",
                "Internship"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Mental Health Services Policy & Psychological First Aid",
                "🧠 **Mental Health Services & Psychological First Aid (PFA)**\n\n" +
                "• **Legal Basis**: Republic Act No. 11036 (Mental Health Act).\n" +
                "• **Free Consultation**: The university allocates budget for free psychological/psychiatric consultations for students and employees.\n" +
                "• **Services Provided**:\n" +
                "  - Psychological evaluations and mental health counseling at the campus clinic/Guidance Office.\n" +
                "  - Peer Facilitators Program providing student-to-student support.\n" +
                "  - Psychological First Aid (PFA) for stabilizing students during crises.\n" +
                "  - Tele-Web online counseling via accredited platforms.\n" +
                "• **University Health Risk Management Team (UHRMT)**: Coordinates suicide prevention, safety plans, and confidential re-entry protocols.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix F, Article 1–9*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("mental health", "counseling", "depression", "anxiety", "pfa", "psychological", "guidance"),
                Arrays.asList("mental health services", "saan pwede magpa counseling", "psychological first aid", "guidance counseling services", "mental health policy"),
                "Appendix F, Article 1-9",
                "Student Services"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Student Rights & Responsibilities (Batas Pambansa Blg. 232)",
                "🕊️ **Student Rights and Duties (Batas Pambansa Blg. 232)**\n\n" +
                "• **Fundamental Student Rights**:\n" +
                "  - Right to competent, quality instruction.\n" +
                "  - Right to freely choose field of study and continue up to graduation.\n" +
                "  - Right to confidential academic records.\n" +
                "  - Right to issuance of official credentials (TOR, COR) within thirty (30) days from request.\n" +
                "  - Right to publish student newspapers and invite assembly resource speakers.\n" +
                "  - Right to free expression of opinions and channels to administration.\n" +
                "  - Right to form and join recognized student organizations.\n" +
                "  - Freedom from involuntary financial contributions.\n" +
                "• **Key Duties of Students**:\n" +
                "  - Uphold academic integrity and achieve academic excellence.\n" +
                "  - Observe university rules of discipline and maintain campus peace.\n" +
                "  - Exercise rights responsibly with respect for others.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Appendix A, Article 1 & 2*",
                LocalNanoAIEngine.Intent.RULES_DISCIPLINE,
                Arrays.asList("rights", "karapatan", "duties", "tungkulin", "batas pambansa", "bp 232", "responsibilities"),
                Arrays.asList("student rights", "karapatan ng estudyante", "tungkulin ng estudyante", "rights and obligations of students"),
                "Appendix A, Article 1 & 2",
                "Student Rights"
        ));

        // =========================================================================
        // 5. WEATHER SUSPENSIONS & TYPHOON PROTOCOLS (Chapter 6, Art. 5)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "Class Suspension Protocols & Typhoon Signals",
                "🌧️ **Official Class Suspension Matrix (DepEd/CHED/EO 66)**\n\n" +
                "• **PAGASA Typhoon Signal No. 1**: Pre-school to Senior High suspended. Tertiary/College classes subject to LGU / University President declaration.\n" +
                "• **PAGASA Typhoon Signal No. 2**: **Automatic suspension of High School classes**.\n" +
                "• **PAGASA Typhoon Signal No. 3, 4, 5**: **AUTOMATIC SUSPENSION OF ALL COLLEGE / TERTIARY & GRADUATE CLASSES**.\n" +
                "• **Heavy Rainfall Alerts (PAGASA / CDRRMO)**:\n" +
                "  - **Orange Warning**: Threat of flood; classes shifted to online/asynchronous.\n" +
                "  - **Red Warning**: Torrential rainfall; automatic suspension of in-person classes.\n" +
                "• **Extreme Heat Index (42°C and above)**: Shift to asynchronous or modular online modality.\n\n" +
                "🏛️ **Recognized Authorities Declaring Suspensions**:\n" +
                "1. LSPU University President (Final university-wide authority)\n" +
                "2. Governor of Laguna (Provincial level)\n" +
                "3. Mayor of San Pablo City (City LGU level, CMO 34 s.2010)\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 6, Article 5, Sec. 2*",
                LocalNanoAIEngine.Intent.SUSPENSION,
                Arrays.asList("signal", "typhoon", "bagyo", "pagasa", "baha", "ulan", "flood", "heat index", "protocol", "signal 1", "signal 2", "signal 3"),
                Arrays.asList("may pasok ba kapag signal 1", "ano ang suspension protocol", "automatic suspension signal", "sino nag dedeklara ng suspension", "signal 1 may pasok ba", "suspension protocol", "may pasok ba bukas"),
                "Chapter 6, Article 5, Sec. 2",
                "Suspensions"
        ));

        // =========================================================================
        // 6. SCHOLARSHIPS & FINANCIAL ASSISTANCE (Chapter 3 & 5)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "CHED Tulong Dunong Program (TDP) Guidelines",
                "🎓 **CHED Tulong Dunong Program (TDP-TES)**\n\n" +
                "• **Grant Amount**: ₱7,500 per semester (₱15,000 per academic year) for eligible beneficiaries.\n" +
                "• **Managing Office**: Handled on campus by **OSAS (Office of Student Affairs and Services)**.\n" +
                "• **Mandatory Validation Documents**:\n" +
                "  1. Duly accomplished TDP Application / Annex Form\n" +
                "  2. Certificate of Registration (COR) for current semester\n" +
                "  3. Official Grade Slip / True Copy of Grades without failing marks\n" +
                "  4. Barangay Certificate of Indigency / Proof of Low Family Income\n" +
                "  5. Valid Student ID (front and back photocopy)\n" +
                "• **Submission Point**: OSAS Building, LSPU-SPCC.\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 10 & Chapter 5, Art. 3, Sec. 2*",
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                Arrays.asList("tdp", "ched", "tulong dunong", "stipend", "allowance", "validation", "payout", "qualifiers"),
                Arrays.asList("tdp requirements", "tdp scholarship", "tdp scholarship requirements", "kailan release ng tdp", "tulong dunong requirements", "paano mag validate ng tdp"),
                "Chapter 3, Article 10",
                "Scholarship"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "UniFAST Free Higher Education (RA 10931)",
                "🆓 **UniFAST Free Higher Education Act (RA 10931)**\n\n" +
                "• **Coverage**: Covers 100% of tuition, miscellaneous, and mandatory school fees for qualified Filipino undergraduate students in State Universities.\n" +
                "• **Eligibility**: Regular and irregular undergraduate students without an existing bachelor's degree finishing within prescribed program duration + 1 year grace period.\n" +
                "• **Disqualification Conditions**:\n" +
                "  - Exceeding the maximum residency period (MRR)\n" +
                "  - Already holds a bachelor's degree or higher\n" +
                "  - Failure to meet university retention and admission standards\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 3, Article 2, Sec. 1.2*",
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                Arrays.asList("unifast", "free tuition", "ra 10931", "matrikula", "libre", "miscellaneous"),
                Arrays.asList("free tuition policy", "libre ba tuition", "unifast guidelines", "coverage ng free tuition", "may tuition fee ba"),
                "Chapter 3, Article 2, Sec. 1.2",
                "Scholarship"
        ));

        // =========================================================================
        // 7. CAMPUS DIRECTORY, OFFICES & OPERATING HOURS (Chapter 5)
        // =========================================================================

        ENTRIES.add(new KnowledgeEntry(
                "LSPU San Pablo Campus Directory & Key Offices",
                "🏢 **LSPU-SPCC Office Directory & Locations**\n\n" +
                "• **Administration Building**:\n" +
                "  - **Registrar's Office**: Ground Floor (Records, TOR, COR, Enrollment evaluation)\n" +
                "  - **Cashier & Accounting**: Ground Floor (Fee settlements, receipts, clearance)\n" +
                "  - **Campus Director's Office**: 2nd Floor\n" +
                "  - **HR & Administrative Services**: 2nd Floor\n" +
                "• **Student Services Building**:\n" +
                "  - **OSAS (Student Affairs & Scholarships)**: 1st Floor\n" +
                "  - **Guidance & Counseling Office**: 2nd Floor (Good Moral, counseling, excuse slips)\n" +
                "  - **Supreme Student Council (SSC) Headquarters**: 2nd Floor\n" +
                "• **Health Services Unit (Campus Clinic)**: Adjacent to Physical Plant Office\n" +
                "• **Campus Library**: Academic Building 2, 2nd & 3rd Floors\n" +
                "• **MIS / ICT Center**: Admin Building Annex (Student portal & Wi-Fi support)\n\n" +
                "⏰ **Operating Hours**: Monday to Friday, 8:00 AM – 5:00 PM (Closed weekends & holidays).\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Chapter 5*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("directory", "saan", "location", "building", "opisina", "office", "registrar", "cashier", "accounting", "guidance"),
                Arrays.asList("saan ang opisina", "campus directory", "saan ang registrar", "saan ang accounting", "location ng mga office", "registrar location", "saan makikita ang registrar"),
                "Chapter 5",
                "Campus Life"
        ));

        ENTRIES.add(new KnowledgeEntry(
                "San Pablo City Emergency Hotlines & Disaster Contacts",
                "🚨 **San Pablo City & Campus Emergency Hotlines**\n\n" +
                "• **National Emergency Hotline**: Dial **911**\n" +
                "• **San Pablo City CDRRMO (Disaster & Rescue)**: (049) 562-7634 / (049) 562-1111\n" +
                "• **San Pablo City Police Station**: (049) 562-6467 / 0998-598-5712\n" +
                "• **San Pablo Bureau of Fire Protection (BFP)**: (049) 562-7654 / 0915-602-2144\n" +
                "• **San Pablo City General Hospital**: (049) 562-0268\n" +
                "• **Philippine Red Cross - San Pablo Chapter**: (049) 562-4069\n" +
                "• **LSPU Campus Security Guardhouse**: Main Campus Entrance Post\n\n" +
                "📖 *Ref: LSPU Student Handbook 2021, Emergency Directory*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("emergency", "hotline", "saklolo", "pulis", "police", "fire", "bombero", "cdrrmo", "hospital", "red cross", "rescue"),
                Arrays.asList("emergency numbers", "hotline ng san pablo", "police hotline", "rescue number", "emergency hotlines", "emergency contact"),
                "Emergency Directory",
                "Campus Life"
        ));
    }

    /**
     * Fast, word-boundary aware search across encyclopedic entries.
     * Evaluates phrase matches, token overlap, and intent alignment in <2 milliseconds.
     */
    public static KnowledgeEntry findBestMatch(String query, LocalNanoAIEngine.Intent detectedIntent) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        String q = query.toLowerCase(Locale.ROOT).trim();
        String cleanQ = q.replaceAll("[^a-z0-9\\s]", " ");

        KnowledgeEntry bestEntry = null;
        int maxScore = 0;

        for (KnowledgeEntry entry : ENTRIES) {
            int score = 0;

            // 1. Exact phrase matches (High weight +10)
            for (String phrase : entry.keyPhrases) {
                String cleanPhrase = phrase.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
                if (q.contains(phrase) || cleanQ.contains(cleanPhrase)) {
                    score += 10;
                    break;
                }
            }

            // 2. Keyword hits (+2 each with word boundary check)
            for (String kw : entry.triggerKeywords) {
                if (matchesWord(cleanQ, kw)) {
                    score += 2;
                }
            }

            // 3. Intent alignment bonus (+3)
            if (entry.intent == detectedIntent && detectedIntent != LocalNanoAIEngine.Intent.UNKNOWN) {
                score += 3;
            }

            if (score > maxScore && score >= 4) {
                maxScore = score;
                bestEntry = entry;
            }
        }

        return bestEntry;
    }

    private static boolean matchesWord(String text, String word) {
        if (text == null || word == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        String w = word.toLowerCase(Locale.ROOT);
        if (w.length() <= 3) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(w) + "\\b");
            return p.matcher(t).find();
        }
        return t.contains(w);
    }

    /**
     * Returns total count of encyclopedic entries.
     */
    public static int getEntryCount() {
        return ENTRIES.size();
    }

    public static List<KnowledgeEntry> getAllEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }
}
