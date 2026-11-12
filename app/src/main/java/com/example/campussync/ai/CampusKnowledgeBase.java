package com.example.campussync.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CampusSense Offline Encyclopedic Knowledge Base
 * 
 * Provides extensive, 100% on-device student life guidance for Laguna State Polytechnic University
 * San Pablo City Campus (LSPU-SPCC).
 * 
 * Covers:
 * 1. Academic Grading System, Latin Honors & Dean's List
 * 2. College Programs & Course Offerings (CCS, CTE, CHMT, CBA, CIT, CAS)
 * 3. Scholarship Guidelines & Grants (TDP, CHED, UniFAST, TES, Iskolar ng Laguna, OSAS)
 * 4. Campus Directory, Office Locations & Operating Hours
 * 5. Student Handbook Rules, Uniform Policy, Wash Days & ID Regulations
 * 6. Weather Suspension Protocols, Typhoon Signals & Disaster Guidelines
 * 7. Student Organizations & Councils (SSC, OCTA, COESC)
 * 8. Emergency Contacts & San Pablo City Hotlines
 * 9. Standard Student Procedures (COR, TOR, INC Resolution, Shifting, Clearance)
 * 10. University Administration & Student Portal
 */
public class CampusKnowledgeBase {

    public static class KnowledgeEntry {
        public final String title;
        public final String content;
        public final List<String> triggerKeywords;
        public final List<String> keyPhrases;
        public final LocalNanoAIEngine.Intent intent;

        public KnowledgeEntry(String title, String content, LocalNanoAIEngine.Intent intent,
                              List<String> triggerKeywords, List<String> keyPhrases) {
            this.title = title;
            this.content = content;
            this.intent = intent;
            this.triggerKeywords = triggerKeywords;
            this.keyPhrases = keyPhrases;
        }
    }

    private static final List<KnowledgeEntry> ENTRIES = new ArrayList<>();

    static {
        // ========================================================
        // 1. ACADEMIC GRADING SYSTEM, HONORS & INC RESOLUTION
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "LSPU Grading System & Evaluation Scale",
                "📊 **LSPU Official Grading System Scale**\n\n" +
                "• **1.00** (99–100%) - Excellent / Superior\n" +
                "• **1.25** (96–98%) - Highly Outstanding\n" +
                "• **1.50** (93–95%) - Very Good\n" +
                "• **1.75** (90–92%) - Good\n" +
                "• **2.00** (87–89%) - Very Satisfactory\n" +
                "• **2.25** (84–86%) - Satisfactory\n" +
                "• **2.50** (81–83%) - Fair\n" +
                "• **2.75** (78–80%) - Passing\n" +
                "• **3.00** (75–77%) - Passing (Lowest passing mark)\n" +
                "• **5.00** (Below 75%) - Failed\n" +
                "• **INC** - Incomplete (Due to missed final exam or major requirement)\n" +
                "• **DRP** - Officially Dropped",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("grading", "grades", "scale", "passing", "singko", "tres", "inc", "drp", "marka", "score", "grade"),
                Arrays.asList("grading system", "passing grade", "anong passing", "grades scale", "ano ang tres", "ano ang singko", "lowest passing", "ano passing grade", "passing grade sa lspu")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Incomplete (INC) Grade Resolution Procedure",
                "📝 **How to Resolve an Incomplete (INC) Grade**\n\n" +
                "1. **Prescriptive Period**: An `INC` grade must be resolved within **one (1) academic year** from the semester it was incurred.\n" +
                "2. **Completion Form**: Obtain an official Completion Form from the Registrar's Office.\n" +
                "3. **Submission**: Complete the lacking requirement (special exam, project, or portfolio) with your course instructor.\n" +
                "4. **Instructor Endorsement**: Have the instructor sign and input the final numerical grade on the form.\n" +
                "5. **Registrar Filing**: Submit the signed form to the Office of the Registrar to update your official transcript.\n\n" +
                "⚠️ *Note: Failure to complete within 1 academic year automatically turns the INC into a permanent 5.00 (Failed).* ",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("inc", "incomplete", "completion", "resolution", "kulang", "ayos"),
                Arrays.asList("paano maayos ang inc", "how to resolve inc", "incomplete grade", "ano gagawin sa inc", "pano ayusin ang inc", "resolving inc", "paano ayusin ang inc")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Dean's List & Academic Honors Criteria",
                "🎖️ **LSPU Dean's List & Honor Student Criteria**\n\n" +
                "To qualify for the Dean's Honor Roll at the end of each semester:\n" +
                "• **General Weighted Average (GWA)**: Must be **1.75 or better**.\n" +
                "• **Grade Ceiling**: No individual grade lower than **2.00** in any enrolled academic course.\n" +
                "• **Academic Load**: Must carry the full regular semestral load as prescribed in the curriculum (no underloading).\n" +
                "• **Disciplinary Standing**: Must have no disciplinary sanction or record of misconduct.\n\n" +
                "📜 **Latin Honors at Graduation**:\n" +
                "• **Summa Cum Laude**: GWA of 1.00 to 1.20 (No grade below 1.75)\n" +
                "• **Magna Cum Laude**: GWA of 1.21 to 1.45 (No grade below 2.00)\n" +
                "• **Cum Laude**: GWA of 1.46 to 1.75 (No grade below 2.25)",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("deans", "dean's", "honors", "latin", "cum laude", "summa", "magna", "honor roll"),
                Arrays.asList("deans list requirements", "dean's list requirements", "how to be deans list", "how to be dean's list", "latin honors criteria", "gwa for cum laude", "deans list", "dean's list", "magkano gwa sa deans list")
        ));

        // ========================================================
        // 2. COLLEGE PROGRAMS & DEGREE OFFERINGS (LSPU-SPCC)
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "LSPU San Pablo Academic Programs & Colleges",
                "🏛️ **LSPU San Pablo City Campus Academic Programs**\n\n" +
                "• **College of Computer Studies (CCS)**:\n" +
                "  - BS in Computer Science (BSCS)\n" +
                "  - BS in Information Technology (BSIT)\n\n" +
                "• **College of Teacher Education (CTE)**:\n" +
                "  - Bachelor of Secondary Education (BSED: English, Math, Science, Social Studies)\n" +
                "  - Bachelor of Elementary Education (BEED)\n\n" +
                "• **College of Hospitality Management & Tourism (CHMT)**:\n" +
                "  - BS in Hospitality Management (BSHM)\n" +
                "  - BS in Tourism Management (BSTM)\n\n" +
                "• **College of Business Administration (CBA)**:\n" +
                "  - BS in Business Administration (Financial Mgt, Marketing Mgt, HR Mgt)\n\n" +
                "• **College of Industrial Technology (CIT)**:\n" +
                "  - Automotive, Electronics, Electrical, and Mechanical Technology\n\n" +
                "• **College of Arts & Sciences (CAS)**:\n" +
                "  - AB in English Language Studies, BS in Psychology",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("course", "courses", "program", "programs", "degree", "curriculum", "major", "colleges", "college", "ccs", "cte", "chmt", "cba", "cit"),
                Arrays.asList("anong mga kurso", "course offerings", "programs offered", "list of courses", "available courses", "courses offered in ccs", "degree programs", "anong kurso meron")
        ));

        // ========================================================
        // 3. SCHOLARSHIPS & FINANCIAL ASSISTANCE (OSAS)
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "CHED Tulong Dunong Program (TDP) Guidelines",
                "🎓 **CHED Tulong Dunong Program (TDP-TES)**\n\n" +
                "• **Grant Amount**: ₱7,500 per semester (₱15,000 per academic year) for eligible beneficiaries.\n" +
                "• **Managing Office**: Handled on campus by **OSAS (Office of Student Affairs and Services)**.\n" +
                "• **Mandatory Validation Documents**:\n" +
                "  1. Duly accomplished TDP Application / Annex Form\n" +
                "  2. Certificate of Registration (COR) for the current semester\n" +
                "  3. Official Grade Slip / True Copy of Grades from previous semester (GWA without failing marks)\n" +
                "  4. Certificate of Indigency issued by Barangay / Proof of Low Family Income\n" +
                "  5. Valid Student ID (front and back photocopy)\n" +
                "• **Submission Point**: OSAS Building, LSPU-SPCC. Watch official OSAS announcements for cutoff dates.",
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                Arrays.asList("tdp", "ched", "tulong dunong", "stipend", "allowance", "validation", "payout", "qualifiers"),
                Arrays.asList("tdp requirements", "tdp scholarship", "tdp scholarship requirements", "kailan release ng tdp", "tulong dunong requirements", "paano mag validate ng tdp", "dokumento sa tdp")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "UniFAST Free Higher Education (RA 10931)",
                "🆓 **UniFAST Free Higher Education Act (RA 10931)**\n\n" +
                "• **Coverage**: Covers 100% of tuition, miscellaneous, and mandatory school fees for qualified Filipino undergraduate students in State Universities.\n" +
                "• **Who is Eligible**: All regular and irregular undergraduate students without an existing bachelor's degree who finish their degree within the prescribed program duration + 1 year grace period.\n" +
                "• **Disqualification Conditions**:\n" +
                "  - Exceeding the maximum residency period\n" +
                "  - Already holds a bachelor's degree or higher\n" +
                "  - Failure to meet university retention and admission standards",
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                Arrays.asList("unifast", "free tuition", "ra 10931", "matrikula", "libre", "miscellaneous"),
                Arrays.asList("free tuition policy", "libre ba tuition", "unifast guidelines", "coverage ng free tuition", "may tuition fee ba")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Local Government (LGU & Provincial) Scholarships",
                "🏛️ **Provincial & City Government Educational Assistance**\n\n" +
                "• **Iskolar ng Laguna (Provincial Capitol)**:\n" +
                "  - Managed by Provincial Government of Laguna for bonafide provincial residents.\n" +
                "  - Requirements: Voter's Certificate (student/parents), COR, Grade Slip, Barangay Indigency.\n\n" +
                "• **San Pablo City LGU Educational Assistance**:\n" +
                "  - Sponsored by the City Mayor's Office & City Council of San Pablo.\n" +
                "  - Submit credentials directly to the City Hall Public Assistance / Scholarship Desk.\n\n" +
                "• **Student Assistantship (SA) Program**:\n" +
                "  - Work-study opportunity in campus departments (Library, Laboratory, Offices) with monthly allowance.",
                LocalNanoAIEngine.Intent.SCHOLARSHIP,
                Arrays.asList("iskolar", "laguna", "san pablo", "lgu", "provincial", "assistantship", "sa", "working student"),
                Arrays.asList("iskolar ng laguna", "san pablo scholarship", "working student program", "student assistant", "educational assistance")
        ));

        // ========================================================
        // 4. CAMPUS DIRECTORY, OFFICES & OPERATING HOURS
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "LSPU San Pablo Campus Directory & Key Offices",
                "🏢 **LSPU-SPCC Office Directory & Locations**\n\n" +
                "• **Administration Building**:\n" +
                "  - **Registrar's Office**: Ground Floor (Records, TOR, COR, Enrollment evaluation)\n" +
                "  - **Cashier & Accounting**: Ground Floor (Fee settlements, receipts, clearance)\n" +
                "  - **Campus Director's Office**: 2nd Floor\n" +
                "  - **HR & Administrative Services**: 2nd Floor\n\n" +
                "• **Student Services Building**:\n" +
                "  - **OSAS (Student Affairs & Scholarships)**: 1st Floor\n" +
                "  - **Guidance & Counseling Office**: 2nd Floor (Good Moral, mental health, counseling)\n" +
                "  - **Supreme Student Council (SSC) Headquarters**: 2nd Floor\n\n" +
                "• **Health Services Unit (Campus Clinic)**: Adjacent to Physical Plant Office\n" +
                "• **Campus Library**: Academic Building 2, 2nd & 3rd Floors\n" +
                "• **MIS / ICT Center**: Admin Building Annex (Student portal & Wi-Fi support)\n\n" +
                "⏰ **Operating Hours**: Monday to Friday, 8:00 AM – 5:00 PM (Closed weekends & holidays).",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("directory", "saan", "location", "building", "opisina", "office", "registrar", "cashier", "accounting", "guidance"),
                Arrays.asList("saan ang opisina", "campus directory", "saan ang registrar", "saan ang accounting", "location ng mga office", "registrar location", "saan makikita ang registrar")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Registrar Office Hours & Services",
                "🏛️ **LSPU-SPCC Registrar Office Hours & Services**\n\n" +
                "• **Operating Hours**: Monday to Friday, 8:00 AM – 5:00 PM (Closed on weekends and holidays).\n" +
                "• **Location**: Ground Floor, Administration Building.\n" +
                "• **Services Provided**:\n" +
                "  - Transcript of Records (TOR) & Honorable Dismissal\n" +
                "  - Certificate of Registration (COR) dry-sealing and validation\n" +
                "  - Grade evaluation, adding/dropping of subjects, shifting permits\n" +
                "  - Graduation clearances and diploma verification.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("registrar", "office hours", "oras", "sarado", "bukas", "schedule"),
                Arrays.asList("registrar office hours", "oras ng registrar", "anong oras bukas ang registrar", "kailan bukas ang registrar", "schedule ng registrar", "office hours registrar")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "OSAS (Office of Student Affairs & Services)",
                "🏢 **Office of Student Affairs and Services (OSAS)**\n\n" +
                "• **Location**: 1st Floor, Student Services Building, LSPU-SPCC.\n" +
                "• **Hours**: Monday to Friday, 8:00 AM – 5:00 PM.\n" +
                "• **Core Responsibilities**:\n" +
                "  - Scholarship document submission and validation (TDP, CHED, UniFAST)\n" +
                "  - Student discipline and student grievance handling\n" +
                "  - Student organization accreditation and activity permits\n" +
                "  - Insurance and student welfare assistance.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("osas", "student affairs", "services building"),
                Arrays.asList("saan ang osas", "osas location", "ano ang osas", "office ng scholarship", "saan magpapavalidate", "osas building")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Health Services Clinic & Emergency Care",
                "🏥 **Campus Health Services Clinic**\n\n" +
                "• **Location**: Adjacent to Physical Plant Office, Main Campus.\n" +
                "• **Operating Hours**: Monday to Friday, 8:00 AM – 5:00 PM.\n" +
                "• **Available Services**:\n" +
                "  - Emergency first aid treatment\n" +
                "  - Basic health checkups (Blood pressure, temperature, triage)\n" +
                "  - Over-the-counter medicine for headache, stomach ache, fever\n" +
                "  - Medical and dental clearance certification for OJT and intramurals\n" +
                "• **Emergency Note**: For severe incidents, the clinic coordinates immediate transport to the nearest hospital via university emergency vehicle.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("clinic", "health", "first aid", "masakit", "gamot", "dentist", "medical", "clearance", "doktor", "emergency", "nurse"),
                Arrays.asList("saan ang clinic", "masakit ulo ko", "kumuha ng medical clearance", "clinic services", "may gamot ba sa clinic", "health clinic", "clinic location")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "LSPU San Pablo University Library",
                "📚 **LSPU-SPCC University Library**\n\n" +
                "• **Location**: Academic Building 2, 2nd and 3rd Floors.\n" +
                "• **Operating Hours**: Monday to Friday, 8:00 AM – 5:00 PM (No noon break).\n" +
                "• **Entry Requirements**: Present your validated Student ID or printed semestral Certificate of Registration (COR).\n" +
                "• **Sections & Facilities**:\n" +
                "  - Circulation & Reserve Section (Textbooks and reference borrowing)\n" +
                "  - Periodicals & Theses Archive Section (Undergraduate research studies)\n" +
                "  - Electronic Learning Center (Internet research kiosks)\n" +
                "  - Quiet study zones and reading halls.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("library", "aklatan", "libro", "book", "thesis", "study"),
                Arrays.asList("library hours", "saan ang library", "oras ng library", "hiram ng libro", "kailan bukas ang library", "library requirements", "university library")
        ));

        // ========================================================
        // 5. STUDENT HANDBOOK RULES, DRESS CODE & ID POLICY
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "Uniform Policy, Wash Days & Dress Code",
                "👔 **LSPU Official Uniform & Dress Code Guidelines**\n\n" +
                "• **Uniform Days**: Monday, Tuesday, Thursday, and Friday.\n" +
                "  - Prescribed college uniform with official nameplate, school ID, and closed footwear.\n\n" +
                "• **Wash / Civilian Days**: Every **Wednesday** and **Saturday**.\n" +
                "  - Smart casual attire (jeans, polo shirts, decent blouses/t-shirts).\n\n" +
                "• **Prohibited Attire (Strictly enforced at the gate)**:\n" +
                "  ❌ Slippers, flip-flops, rubber beach sandals\n" +
                "  ❌ Sleeveless tops, tank tops, cropped shirts, plunging necklines\n" +
                "  ❌ Short shorts or micro-mini skirts\n" +
                "  ❌ Heavily ripped or distressed jeans\n" +
                "  ❌ Attire with offensive, obscene, or defamatory prints\n\n" +
                "💡 *Note: Validated school ID must be worn prominently at all times, including wash days.*",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("uniform", "dress code", "civilian", "wash day", "suot", "damit", "shorts", "tsinelas", "slippers"),
                Arrays.asList("uniform wash day", "kailan wash day", "dress code rules", "pwede ba mag civilian", "anong suot sa wednesday", "bawal ba shorts", "bawal ba tsinelas", "civilian day", "uniform policy", "anong suot bukas")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Student ID Regulations & Lost ID Replacement",
                "🪪 **Student ID Regulations & Lost ID Replacement**\n\n" +
                "• **\"No ID, No Entry\" Policy**: The official university ID with lanyard must be worn around the neck at all times upon entering campus premises.\n\n" +
                "• **What to do if your ID is Lost**:\n" +
                "  1. **Affidavit of Loss**: Secure a notarized Affidavit of Loss from a notary public or PAO.\n" +
                "  2. **OSAS Clearance**: Present the affidavit to OSAS / Campus Security to secure an ID Replacement Slip.\n" +
                "  3. **Payment**: Pay the ID replacement fee at the University Cashier (Admin Ground Floor).\n" +
                "  4. **ID Re-issuance**: Submit the receipt and form to the MIS/ICT ID Production Unit.\n" +
                "  5. **Claiming**: Claim your replacement ID upon scheduled release notification.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("id", "lost id", "nawala", "lanyard", "affidavit", "no id no entry", "replacement"),
                Arrays.asList("nawala ang id ko", "nawala id ko", "lost id", "how to replace lost id", "no id no entry rule", "paano kumuha ng bagong id", "id replacement procedure", "nawala ang id")
        ));

        // ========================================================
        // 6. WEATHER, DISASTER & CLASS SUSPENSION PROTOCOLS
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "Class Suspension Protocols & Typhoon Signals",
                "🌧️ **Official Class Suspension Matrix (DepEd/CHED/EO 66)**\n\n" +
                "• **PAGASA Typhoon Signal No. 1**: Pre-school to Senior High suspended. Tertiary/College classes subject to LGU / University President declaration.\n" +
                "• **PAGASA Typhoon Signal No. 2, 3, 4, 5**: **AUTOMATIC SUSPENSION OF ALL LEVELS**, including College/Tertiary and graduate school. No campus transactions.\n" +
                "• **Heavy Rainfall Alerts (PAGASA / CDRRMO)**:\n" +
                "  - **Orange Warning**: Threat of flood; classes may be shifted to online/asynchronous.\n" +
                "  - **Red Warning**: Torrential rainfall; automatic suspension of in-person classes.\n" +
                "• **Extreme Heat Index (42°C and above)**: Shift to asynchronous or modular online modality.\n\n" +
                "🏛️ **Recognized Authorities Declaring Suspensions**:\n" +
                "1. LSPU University President\n" +
                "2. Governor of Laguna\n" +
                "3. Mayor of San Pablo City (LGU San Pablo)",
                LocalNanoAIEngine.Intent.SUSPENSION,
                Arrays.asList("signal", "typhoon", "bagyo", "pagasa", "baha", "ulan", "flood", "heat index", "protocol"),
                Arrays.asList("may pasok ba kapag signal 1", "ano ang suspension protocol", "automatic suspension signal", "sino nag dedeklara ng suspension", "signal 1 may pasok ba", "suspension protocol")
        ));

        // ========================================================
        // 7. STUDENT PROCEDURES & HOW-TOS (COR, TOR, CLEARANCE)
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "Certificate of Registration (COR) & Enrollment Verification",
                "📄 **How to Obtain Your Certificate of Registration (COR)**\n\n" +
                "1. **Online Portal**: Log in to your student account at `https://lspu.edu.ph/portal`.\n" +
                "2. **Enrollment Tab**: Navigate to *Registration / Enrolled Subjects*.\n" +
                "3. **Print Official Copy**: Print your semestral COR on clean letter-sized bond paper.\n" +
                "4. **Validation Stamp**: For scholarship or CHED validation, bring the printed COR to the Registrar's Office for dry seal / official validation stamp.",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("cor", "registration", "certificate of registration", "enrolled", "print cor"),
                Arrays.asList("paano kumuha ng cor", "where to get cor", "how to print cor", "certificate of registration copy", "print certificate of registration", "makakuha ng cor")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Transcript of Records (TOR) & Honorable Dismissal",
                "📜 **Requesting Transcript of Records (TOR) & Honorable Dismissal**\n\n" +
                "1. **Clearance**: Secure a University Clearance Form from the Registrar's Office.\n" +
                "2. **Department Signatures**: Obtain clearance signatures from College Dean, Library, Clinic, Cashier, and OSAS.\n" +
                "3. **Payment**: Pay the TOR / Honorable Dismissal processing fee at the Cashier.\n" +
                "4. **Filing**: Submit the accomplished clearance and official receipt to the Registrar's Office Records Window.\n" +
                "5. **Processing Period**: Regular processing takes 7 to 15 working days.",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("tor", "transcript", "records", "honorable dismissal", "clearance", "bayad", "request tor"),
                Arrays.asList("paano kumuha ng tor", "transcript of records request", "honorable dismissal process", "requesting tor", "kumuha ng transcript")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Certificate of Good Moral Character Request",
                "🕊️ **Requesting Certificate of Good Moral Character**\n\n" +
                "• **Issuing Office**: Guidance and Counseling Center (2nd Floor, Student Services Building).\n" +
                "• **Procedure**:\n" +
                "  1. Secure a Good Moral Request Slip from the Guidance Office.\n" +
                "  2. Pay the document fee (₱50) at the University Cashier (Admin Building Ground Floor).\n" +
                "  3. Present the official receipt and your student ID to the Guidance Counselor.\n" +
                "  4. The certificate is typically released within 1 to 2 business days after records verification.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("good moral", "moral", "guidance", "character"),
                Arrays.asList("paano kumuha ng good moral", "where to get good moral", "good moral requirements", "magkano good moral", "good moral request", "kumuha ng good moral")
        ));

        // ========================================================
        // 8. UNIVERSITY LEADERSHIP & STUDENT PORTAL
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "LSPU Administration & Key University Officials",
                "🏛️ **LSPU University Leadership & Administration**\n\n" +
                "• **University President**: **Dr. Mario R. Briones** (Office of the University President)\n" +
                "• **SPCC Campus Leadership**: Guided by the Campus Director and Academic Deans\n" +
                "• **Student Government**: Federated Supreme Student Council (FSSC) & Supreme Student Council (SSC SPCC)\n" +
                "• **Deans Offices**: Located in their respective college buildings (CCS in Computer Studies Bldg, CTE in Education Bldg, etc.).",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("president", "presidente", "official", "officials", "chancellor", "briones", "namumuno"),
                Arrays.asList("who is the president", "sino ang presidente", "university president", "sino namumuno sa lspu", "president of lspu", "sino ang pangulo")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "LSPU Student Portal & MIS/ICT Support",
                "🌐 **LSPU Student Portal & Online Connectivity**\n\n" +
                "• **Student Portal URL**: `https://lspu.edu.ph/portal`\n" +
                "• **Portal Services**: View verified semestral grades, download Certificate of Registration (COR), and check student account balance.\n" +
                "• **Account & Password Reset**: Visit the MIS/ICT Unit (Administration Building Annex) or submit a help ticket with your student ID.\n" +
                "• **Campus Wi-Fi**: Available in designated academic zones; obtain student credentials at the MIS Unit.",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("portal", "wifi", "internet", "password", "grades online", "ict", "mis"),
                Arrays.asList("student portal", "paano buksan ang portal", "portal link", "forgot password portal", "campus wifi", "online portal")
        ));

        ENTRIES.add(new KnowledgeEntry(
                "Adding/Dropping, Subject Enrolment & Shifting Procedure",
                "📋 **Adding, Dropping & Course Shifting Procedures**\n\n" +
                "• **Adding/Dropping Period**: Typically held during the first two (2) weeks of the semester.\n" +
                "• **Procedure**:\n" +
                "  1. Secure an Adding/Dropping Form from the Registrar's Office.\n" +
                "  2. Secure written approval from the Department Chairperson and College Dean.\n" +
                "  3. Submit the accomplished form to the Registrar for system schedule updating.\n" +
                "• **Shifting Programs**: Requires clearance from current college, passing of interview/assessment in target college, and minimum GWA evaluation.",
                LocalNanoAIEngine.Intent.ACADEMICS,
                Arrays.asList("adding", "dropping", "shifting", "shift", "drop subject", "add subject"),
                Arrays.asList("paano mag drop", "paano mag shift", "adding and dropping", "shifting requirements", "drop ng subject")
        ));

        // ========================================================
        // 9. EMERGENCY CONTACTS & SAN PABLO CITY HOTLINES
        // ========================================================
        ENTRIES.add(new KnowledgeEntry(
                "San Pablo City Emergency Hotlines & Disaster Contacts",
                "🚨 **San Pablo City & Campus Emergency Hotlines**\n\n" +
                "• **National Emergency Hotline**: Dial **911**\n" +
                "• **San Pablo City CDRRMO (Disaster & Rescue)**: (049) 562-7634 / (049) 562-1111\n" +
                "• **San Pablo City Police Station**: (049) 562-6467 / 0998-598-5712\n" +
                "• **San Pablo Bureau of Fire Protection (BFP)**: (049) 562-7654 / 0915-602-2144\n" +
                "• **San Pablo City General Hospital**: (049) 562-0268\n" +
                "• **Philippine Red Cross - San Pablo Chapter**: (049) 562-4069\n" +
                "• **LSPU Campus Security Guardhouse**: Main Campus Entrance Post",
                LocalNanoAIEngine.Intent.CAMPUS_INFO,
                Arrays.asList("emergency", "hotline", "saklolo", "pulis", "police", "fire", "bombero", "cdrrmo", "hospital", "red cross", "rescue"),
                Arrays.asList("emergency numbers", "hotline ng san pablo", "police hotline", "rescue number", "emergency hotlines", "emergency contact")
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

            // 3. Intent alignment bonus (+2)
            if (entry.intent == detectedIntent && detectedIntent != LocalNanoAIEngine.Intent.UNKNOWN) {
                score += 2;
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
}
