package com.example.data.firebase

import com.example.data.model.AdminQuestionDraft
import kotlinx.coroutines.CancellationException

/**
 * Optional one-tap Admin Curriculum Publisher: publishes real bilingual (Hindi + English)
 * Navodaya (JNVST), Sainik School, Simultala, and Olympiad tests, formula sheets, and an announcement
 * directly into Firestore using the exact validated test publishing workflow.
 */
object MbsStarterSeeder {

    suspend fun publishStarterCurriculum(repo: MbsRepository): Result<String> {
        return try {
            // 1. Featured Navodaya (JNVST) Full Mock Test
            repo.saveAndPublishTest(
                existingTestId = "navodaya_mock_4",
                title = "Navodaya • Mock 4",
                description = "कक्षा 6 प्रवेश परीक्षा (JNVST) — मानसिक योग्यता, अंकगणित और भाषा।",
                category = "Navodaya",
                testType = "FULL",
                subject = "All Subjects",
                chapter = "",
                durationMinutes = 45,
                totalMarks = 10.0,
                negativeMarking = 0.0,
                publishNow = true,
                featuredToday = true,
                assignmentMode = "ALL",
                assignedBatch = "",
                assignedStudentIds = emptyList(),
                questions = listOf(
                    AdminQuestionDraft(
                        subject = "Reasoning",
                        chapter = "Number Series",
                        questionText = "संख्या श्रृंखला में अगली संख्या क्या होगी?\n2, 4, 8, 16, ?",
                        optionA = "24",
                        optionB = "30",
                        optionC = "32",
                        optionD = "36",
                        correctOptionIndex = 2,
                        marks = 1.0,
                        explanation = "प्रत्येक संख्या पिछली संख्या की दोगुनी है: 16 × 2 = 32."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "4 अंकों की सबसे बड़ी संख्या और 3 अंकों की सबसे छोटी संख्या का अंतर क्या है?\n(Difference between largest 4-digit and smallest 3-digit number)",
                        optionA = "9899",
                        optionB = "9900",
                        optionC = "9999",
                        optionD = "8999",
                        correctOptionIndex = 0,
                        marks = 1.0,
                        explanation = "सबसे बड़ी 4 अंकों की संख्या = 9999, सबसे छोटी 3 अंकों की संख्या = 100। अंतर = 9999 - 100 = 9899."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Percentage",
                        questionText = "500 का 25% कितना होगा? (What is 25% of 500?)",
                        optionA = "100",
                        optionB = "125",
                        optionC = "150",
                        optionD = "175",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "500 × (25 / 100) = 5 × 25 = 125."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Fractions",
                        questionText = "3/4 + 5/8 का मान क्या है? (Value of 3/4 + 5/8)",
                        optionA = "8/12",
                        optionB = "11/8",
                        optionC = "9/8",
                        optionD = "13/8",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "3/4 = 6/8; इसलिए 6/8 + 5/8 = 11/8."
                    ),
                    AdminQuestionDraft(
                        subject = "Reasoning",
                        chapter = "Analogy",
                        questionText = "यदि 5 : 25 :: 9 : ? (Find the missing number)",
                        optionA = "72",
                        optionB = "81",
                        optionC = "90",
                        optionD = "64",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "5² = 25, उसी प्रकार 9² = 81."
                    ),
                    AdminQuestionDraft(
                        subject = "Hindi",
                        chapter = "व्याकरण",
                        questionText = "'सूर्य' का सही पर्यायवाची शब्द कौन सा है?",
                        optionA = "शशि",
                        optionB = "दिनकर",
                        optionC = "नीर",
                        optionD = "पवन",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "'दिनकर', 'भास्कर', 'रवि' सूर्य के पर्यायवाची हैं।"
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Profit and Loss",
                        questionText = "एक किताब ₹400 में खरीदकर ₹460 में बेची गई। लाभ प्रतिशत कितना है?",
                        optionA = "12%",
                        optionB = "15%",
                        optionC = "18%",
                        optionD = "20%",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "लाभ = 460 - 400 = ₹60। लाभ % = (60/400) × 100 = 15%."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Time and Work",
                        questionText = "A किसी काम को 12 दिन में और B उसी काम को 6 दिन में पूरा करता है। दोनों मिलकर कितने दिन में पूरा करेंगे?",
                        optionA = "3 दिन",
                        optionB = "4 दिन",
                        optionC = "5 दिन",
                        optionD = "6 दिन",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "दोनों का 1 दिन का काम = 1/12 + 1/6 = 3/12 = 1/4। अतः कुल समय = 4 दिन।"
                    ),
                    AdminQuestionDraft(
                        subject = "English",
                        chapter = "Grammar",
                        questionText = "Choose the correct synonym of 'BRAVE':",
                        optionA = "Timid",
                        optionB = "Courageous",
                        optionC = "Weak",
                        optionD = "Silent",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "'Courageous' means fearless and brave."
                    ),
                    AdminQuestionDraft(
                        subject = "EVS",
                        chapter = "Science",
                        questionText = "पौधे प्रकाश संश्लेषण (Photosynthesis) में कौन सी गैस लेते हैं?",
                        optionA = "Oxygen",
                        optionB = "Carbon Dioxide",
                        optionC = "Nitrogen",
                        optionD = "Hydrogen",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "पौधे प्रकाश संश्लेषण में कार्बन डाइऑक्साइड (CO₂) ग्रहण करते हैं और ऑक्सीजन छोड़ते हैं।"
                    )
                )
            ).getOrThrow()

            // 2. Navodaya Math Sectional Test (Number System)
            repo.saveAndPublishTest(
                existingTestId = "navodaya_math_number_system",
                title = "Math Test 1 • Number System",
                description = "संख्या पद्धति (Number System) के महत्वपूर्ण प्रश्नों का अभ्यास।",
                category = "Navodaya",
                testType = "SECTIONAL",
                subject = "Mathematics",
                chapter = "Number System",
                durationMinutes = 25,
                totalMarks = 5.0,
                negativeMarking = 0.0,
                publishNow = true,
                featuredToday = false,
                assignmentMode = "ALL",
                assignedBatch = "",
                assignedStudentIds = emptyList(),
                questions = listOf(
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "सबसे छोटी अभाज्य संख्या (Smallest Prime Number) कौन सी है?",
                        optionA = "0",
                        optionB = "1",
                        optionC = "2",
                        optionD = "3",
                        correctOptionIndex = 2,
                        marks = 1.0,
                        explanation = "2 सबसे छोटी और एकमात्र सम अभाज्य संख्या है।"
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "संख्या 54,782 में 7 का स्थानीय मान (Place Value) क्या है?",
                        optionA = "7",
                        optionB = "70",
                        optionC = "700",
                        optionD = "7000",
                        correctOptionIndex = 2,
                        marks = 1.0,
                        explanation = "7 सैकड़े के स्थान पर है, इसलिए स्थानीय मान = 7 × 100 = 700."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "प्रथम 10 प्राकृतिक संख्याओं का योग कितना होता है?",
                        optionA = "45",
                        optionB = "50",
                        optionC = "55",
                        optionD = "60",
                        correctOptionIndex = 2,
                        marks = 1.0,
                        explanation = "सूत्र n(n+1)/2 से: 10 × 11 / 2 = 55."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "निम्न में से कौन सी संख्या 9 से पूर्णतः विभाज्य है?",
                        optionA = "4321",
                        optionB = "5679",
                        optionC = "3452",
                        optionD = "2183",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "5 + 6 + 7 + 9 = 27, जो 9 से विभाज्य है।"
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "12 और 18 का ल.स.प. (LCM) क्या है?",
                        optionA = "24",
                        optionB = "36",
                        optionC = "48",
                        optionD = "72",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "12 = 2² × 3 तथा 18 = 2 × 3²। अतः LCM = 2² × 3² = 36."
                    )
                )
            ).getOrThrow()

            // 3. Sainik School Full Test
            repo.saveAndPublishTest(
                existingTestId = "sainik_aissee_mock_1",
                title = "Sainik School • AISSEE Mock 1",
                description = "All India Sainik Schools Entrance Exam (Class 6) Practice Paper.",
                category = "Sainik School",
                testType = "FULL",
                subject = "All Subjects",
                chapter = "",
                durationMinutes = 30,
                totalMarks = 10.0,
                negativeMarking = 0.0,
                publishNow = true,
                featuredToday = false,
                assignmentMode = "ALL",
                assignedBatch = "",
                assignedStudentIds = emptyList(),
                questions = listOf(
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Geometry",
                        questionText = "त्रिभुज के तीनों अंतः कोणों का योग कितना होता है? (Sum of angles of a triangle)",
                        optionA = "90°",
                        optionB = "180°",
                        optionC = "270°",
                        optionD = "360°",
                        correctOptionIndex = 1,
                        marks = 2.0,
                        explanation = "किसी भी त्रिभुज के तीनों कोणों का योग हमेशा 180° होता है।"
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Mensuration",
                        questionText = "12 cm लंबाई और 8 cm चौड़ाई वाले आयत का परिमाप (Perimeter) ज्ञात करें।",
                        optionA = "20 cm",
                        optionB = "40 cm",
                        optionC = "96 cm",
                        optionD = "32 cm",
                        correctOptionIndex = 1,
                        marks = 2.0,
                        explanation = "परिमाप = 2 × (लंबाई + चौड़ाई) = 2 × (12 + 8) = 40 cm."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Average",
                        questionText = "10, 20, 30, 40 और 50 का औसत (Average) क्या होगा?",
                        optionA = "25",
                        optionB = "30",
                        optionC = "35",
                        optionD = "40",
                        correctOptionIndex = 1,
                        marks = 2.0,
                        explanation = "औसत = (10 + 20 + 30 + 40 + 50) / 5 = 150 / 5 = 30."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Ratio and Proportion",
                        questionText = "यदि A : B = 2 : 3 और कुल राशि ₹500 है, तो A का हिस्सा कितना होगा?",
                        optionA = "₹150",
                        optionB = "₹200",
                        optionC = "₹250",
                        optionD = "₹300",
                        correctOptionIndex = 1,
                        marks = 2.0,
                        explanation = "A का हिस्सा = (2 / 5) × 500 = ₹200."
                    ),
                    AdminQuestionDraft(
                        subject = "EVS",
                        chapter = "General Knowledge",
                        questionText = "भारत का राष्ट्रीय खेल दिवस किसकी जयंती पर मनाया जाता है?",
                        optionA = "मेजर ध्यानचंद",
                        optionB = "मिल्खा सिंह",
                        optionC = "कपिल देव",
                        optionD = "सचिन तेंदुलकर",
                        correctOptionIndex = 0,
                        marks = 2.0,
                        explanation = "29 अगस्त को हॉकी के जादूगर मेजर ध्यानचंद की जयंती पर राष्ट्रीय खेल दिवस मनाया जाता है।"
                    )
                )
            ).getOrThrow()

            // 4. Simultala & Olympiad Tests
            repo.saveAndPublishTest(
                existingTestId = "simultala_mock_1",
                title = "Simultala • Prelims Mock 1",
                description = "सिमुलतला आवासीय विद्यालय प्रवेश परीक्षा अभ्यास सेट।",
                category = "Simultala",
                testType = "FULL",
                subject = "All Subjects",
                chapter = "",
                durationMinutes = 30,
                totalMarks = 4.0,
                negativeMarking = 0.0,
                publishNow = true,
                featuredToday = false,
                assignmentMode = "ALL",
                assignedBatch = "",
                assignedStudentIds = emptyList(),
                questions = listOf(
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Percentage",
                        questionText = "यदि किसी वस्तु का मूल्य ₹200 से बढ़कर ₹250 हो जाए, तो वृद्धि प्रतिशत क्या है?",
                        optionA = "20%",
                        optionB = "25%",
                        optionC = "30%",
                        optionD = "50%",
                        correctOptionIndex = 1,
                        marks = 1.0,
                        explanation = "वृद्धि = 50। वृद्धि % = (50 / 200) × 100 = 25%."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Mensuration",
                        questionText = "9 cm भुजा वाले वर्ग (Square) का क्षेत्रफल कितना होगा?",
                        optionA = "36 cm²",
                        optionB = "72 cm²",
                        optionC = "81 cm²",
                        optionD = "90 cm²",
                        correctOptionIndex = 2,
                        marks = 1.0,
                        explanation = "वर्ग का क्षेत्रफल = भुजा × भुजा = 9 × 9 = 81 cm²."
                    ),
                    AdminQuestionDraft(
                        subject = "Hindi",
                        chapter = "व्याकरण",
                        questionText = "'विद्यालय' शब्द का सही संधि-विच्छेद क्या है?",
                        optionA = "विद्या + आलय",
                        optionB = "विद्य + आलय",
                        optionC = "विद्या + लय",
                        optionD = "वि + द्यालय",
                        correctOptionIndex = 0,
                        marks = 1.0,
                        explanation = "विद्या + आलय = विद्यालय (दीर्घ स्वर संधि)।"
                    ),
                    AdminQuestionDraft(
                        subject = "Reasoning",
                        chapter = "Odd One Out",
                        questionText = "इनमें से सबसे भिन्न संख्या चुनिए: 16, 25, 36, 48",
                        optionA = "16",
                        optionB = "25",
                        optionC = "36",
                        optionD = "48",
                        correctOptionIndex = 3,
                        marks = 1.0,
                        explanation = "16 (4²), 25 (5²), और 36 (6²) पूर्ण वर्ग हैं, जबकि 48 पूर्ण वर्ग नहीं है।"
                    )
                )
            ).getOrThrow()

            repo.saveAndPublishTest(
                existingTestId = "olympiad_maths_challenge_1",
                title = "Olympiad • Maths Challenge 1",
                description = "High-order problem solving for Junior Mathematics Olympiad.",
                category = "Olympiad",
                testType = "SECTIONAL",
                subject = "Mathematics",
                chapter = "Geometry",
                durationMinutes = 20,
                totalMarks = 6.0,
                negativeMarking = 0.25,
                publishNow = true,
                featuredToday = false,
                assignmentMode = "ALL",
                assignedBatch = "",
                assignedStudentIds = emptyList(),
                questions = listOf(
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Number System",
                        questionText = "What is the unit digit of 7²⁰²⁶?",
                        optionA = "1",
                        optionB = "3",
                        optionC = "7",
                        optionD = "9",
                        correctOptionIndex = 3,
                        marks = 2.0,
                        explanation = "Powers of 7 repeat unit digits in cycle of 4: 7, 9, 3, 1. Since 2026 mod 4 = 2, unit digit is 9."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Geometry",
                        questionText = "How many diagonals does a regular hexagon (6 sides) have?",
                        optionA = "6",
                        optionB = "9",
                        optionC = "12",
                        optionD = "15",
                        correctOptionIndex = 1,
                        marks = 2.0,
                        explanation = "Diagonals = n(n - 3)/2 = 6 × 3 / 2 = 9."
                    ),
                    AdminQuestionDraft(
                        subject = "Mathematics",
                        chapter = "Profit and Loss",
                        questionText = "By selling an article for ₹720, a shopkeeper loses 10%. What is its Cost Price?",
                        optionA = "₹780",
                        optionB = "₹792",
                        optionC = "₹800",
                        optionD = "₹820",
                        correctOptionIndex = 2,
                        marks = 2.0,
                        explanation = "90% of CP = 720 => CP = 720 × 100 / 90 = ₹800."
                    )
                )
            ).getOrThrow()

            // 5. Study Notes & Formula Sheets
            repo.saveStudyNoteAdmin(
                title = "Number System Complete Notes",
                subject = "Mathematics",
                chapter = "Number System",
                materialType = "NOTES",
                category = "Navodaya",
                description = "Place value, prime numbers, divisibility rules & LCM/HCF shortcuts by Mithlesh Sir.",
                pdfUrl = "",
                contentMarkdown = """
                    # 1. संख्या पद्धति (Number System)
                    - प्राकृतिक संख्याएँ (Natural Numbers): 1, 2, 3, 4, ...
                    - पूर्ण संख्याएँ (Whole Numbers): 0, 1, 2, 3, ...
                    - सबसे छोटी अभाज्य संख्या (Smallest Prime Number) = 2 (एकमात्र सम अभाज्य संख्या)।
                    - 1 से 100 के बीच कुल 25 अभाज्य संख्याएँ होती हैं।

                    # 2. विभाज्यता के नियम (Divisibility Rules)
                    - 2 से विभाज्य: इकाई का अंक 0, 2, 4, 6 या 8 हो।
                    - 3 से विभाज्य: सभी अंकों का योग 3 से कटे।
                    - 9 से विभाज्य: सभी अंकों का योग 9 से कटे।
                    - 11 से विभाज्य: सम और विषम स्थानों के अंकों के योग का अंतर 0 या 11 का गुणज हो।

                    # 3. महत्वपूर्ण सूत्र (Key Shortcuts)
                    - प्रथम n प्राकृतिक संख्याओं का योग = n(n + 1) / 2
                    - प्रथम n विषम संख्याओं का योग = n²
                    - प्रथम n सम संख्याओं का योग = n(n + 1)
                    - पहली संख्या × दूसरी संख्या = LCM × HCF
                """.trimIndent(),
                fileSizeLabel = "1.4 MB"
            ).getOrThrow()

            repo.saveStudyNoteAdmin(
                title = "All-in-One Maths Formula Sheet",
                subject = "Mathematics",
                chapter = "Percentage",
                materialType = "FORMULA_SHEET",
                category = "Navodaya",
                description = "Quick revision sheet for Percentage, Profit & Loss, Average, Mensuration & Time-Work.",
                pdfUrl = "",
                contentMarkdown = """
                    # प्रतिशत (Percentage)
                    - X का Y% = (X × Y) / 100
                    - प्रतिशत वृद्धि = (वृद्धि / प्रारंभिक मान) × 100

                    # लाभ और हानि (Profit & Loss)
                    - लाभ = विक्रय मूल्य (SP) - क्रय मूल्य (CP)
                    - लाभ % = (लाभ / CP) × 100
                    - हानि % = (हानि / CP) × 100

                    # क्षेत्रमिति (Mensuration)
                    - आयत का क्षेत्रफल = लंबाई × चौड़ाई | परिमाप = 2(l + b)
                    - वर्ग का क्षेत्रफल = भुजा² | परिमाप = 4 × भुजा
                    - त्रिभुज का क्षेत्रफल = ½ × आधार × ऊँचाई
                    - वृत्त का क्षेत्रफल = πr² | परिधि = 2πr

                    # समय और कार्य (Time & Work)
                    - यदि A किसी कार्य को x दिन में व B y दिन में करे, तो दोनों मिलकर = (x × y) / (x + y) दिन।
                """.trimIndent(),
                fileSizeLabel = "1.1 MB"
            ).getOrThrow()

            repo.saveStudyNoteAdmin(
                title = "Navodaya JNVST Previous Year Paper",
                subject = "Reasoning",
                chapter = "Mental Ability",
                materialType = "PREVIOUS_YEAR",
                category = "Navodaya",
                description = "Solved Mental Ability & Arithmetic practice worksheet for Class 6 JNVST.",
                pdfUrl = "",
                contentMarkdown = """
                    # JNVST Class 6 Exam Pattern
                    - Mental Ability (मानसिक योग्यता): 40 Questions • 50 Marks
                    - Arithmetic (अंकगणित): 20 Questions • 25 Marks
                    - Language (भाषा परीक्षण): 20 Questions • 25 Marks
                    - कुल समय: 2 घंटे | कोई Negative Marking नहीं।

                    # Practice Questions
                    1. 3, 6, 12, 24, ?  -> उत्तर: 48 (प्रत्येक पद दोगुना हो रहा है)
                    2. 120 का 15% क्या होगा? -> उत्तर: 18
                    3. 1 से 20 तक अभाज्य संख्याओं की संख्या कितनी है? -> उत्तर: 8 (2, 3, 5, 7, 11, 13, 17, 19)
                """.trimIndent(),
                fileSizeLabel = "2.2 MB"
            ).getOrThrow()

            // 6. Announcement & Default Settings
            repo.sendAnnouncementAdmin(
                title = "Navodaya Mock 4 & Formula Sheets Live!",
                message = "सभी विद्यार्थियों के लिए Navodaya Mock 4, Sainik School, Simultala और Olympiad के फ्री मॉक टेस्ट और PDF नोट्स लाइव हो चुके हैं। रोज़ अभ्यास करें और अपनी रैंक सुधारें!",
                targetCategory = "ALL",
                targetBatch = ""
            ).getOrThrow()

            repo.updateAppSettingsAdmin(
                leaderboardEnabled = true,
                rankingMode = "PERCENTAGE",
                welcomeMessage = "सीखें गणित, बढ़ें सफलता की ओर"
            ).getOrThrow()

            Result.success("Published 5 bilingual mock tests, 3 PDF study notes, and 1 announcement!")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
