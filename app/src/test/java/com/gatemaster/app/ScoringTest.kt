package com.gatemaster.app

import com.gatemaster.app.core.model.AnswerState
import com.gatemaster.app.core.model.Attempt
import com.gatemaster.app.core.model.MockTest
import com.gatemaster.app.core.model.NumericAnswer
import com.gatemaster.app.core.model.Option
import com.gatemaster.app.core.model.Question
import com.gatemaster.app.core.model.QuestionType
import com.gatemaster.app.core.model.ResultKind
import com.gatemaster.app.core.model.TestSection
import com.gatemaster.app.core.model.formatMarks
import com.gatemaster.app.core.model.gradeQuestion
import com.gatemaster.app.core.model.scoreAttempt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scoring rules, which the previous engine had none of.
 *
 * GATE's marking scheme is specific and easy to get subtly wrong: only
 * single-answer MCQs are penalised, the penalty is a third of the question's
 * marks, and MSQ awards no partial credit.
 */
class ScoringTest {

    private fun mcq(id: String, marks: Int, correct: String = "A") = Question(
        id = id,
        number = id.removePrefix("q").toInt(),
        type = QuestionType.MCQ,
        marks = marks,
        text = "Question $id",
        options = listOf(Option("A", "a"), Option("B", "b"), Option("C", "c"), Option("D", "d")),
        correctOptionIds = listOf(correct),
    )

    private fun msq(id: String, marks: Int, correct: List<String>) = Question(
        id = id,
        number = id.removePrefix("q").toInt(),
        type = QuestionType.MSQ,
        marks = marks,
        text = "Question $id",
        options = listOf(Option("A", "a"), Option("B", "b"), Option("C", "c"), Option("D", "d")),
        correctOptionIds = correct,
    )

    private fun nat(id: String, marks: Int, min: Double, max: Double) = Question(
        id = id,
        number = id.removePrefix("q").toInt(),
        type = QuestionType.NAT,
        marks = marks,
        text = "Question $id",
        numericAnswer = NumericAnswer(min, max),
    )

    private fun chose(question: Question, vararg options: String) =
        AnswerState(question.id, selectedOptionIds = options.toSet())

    private fun typed(question: Question, value: String) =
        AnswerState(question.id, numericInput = value)

    // -- MCQ -----------------------------------------------------------------

    @Test
    fun `correct one mark mcq awards one`() {
        val q = mcq("q1", 1)
        assertEquals(1.0, gradeQuestion(q, chose(q, "A")).marksAwarded, 1e-9)
    }

    @Test
    fun `wrong one mark mcq deducts one third`() {
        val q = mcq("q1", 1)
        val result = gradeQuestion(q, chose(q, "B"))
        assertEquals(ResultKind.INCORRECT, result.kind)
        assertEquals(-1.0 / 3, result.marksAwarded, 1e-9)
    }

    @Test
    fun `wrong two mark mcq deducts two thirds`() {
        val q = mcq("q1", 2)
        assertEquals(-2.0 / 3, gradeQuestion(q, chose(q, "C")).marksAwarded, 1e-9)
    }

    @Test
    fun `unattempted never deducts`() {
        val q = mcq("q1", 2)
        val result = gradeQuestion(q, AnswerState(q.id))
        assertEquals(ResultKind.UNATTEMPTED, result.kind)
        assertEquals(0.0, result.marksAwarded, 1e-9)
    }

    // -- MSQ -----------------------------------------------------------------

    @Test
    fun `msq needs the exact set and gives no partial credit`() {
        val q = msq("q1", 2, listOf("A", "C"))
        assertEquals(2.0, gradeQuestion(q, chose(q, "A", "C")).marksAwarded, 1e-9)
        // One of two right is still wrong.
        assertEquals(0.0, gradeQuestion(q, chose(q, "A")).marksAwarded, 1e-9)
        // An extra selection is wrong too.
        assertEquals(0.0, gradeQuestion(q, chose(q, "A", "B", "C")).marksAwarded, 1e-9)
    }

    @Test
    fun `msq never deducts`() {
        val q = msq("q1", 2, listOf("A", "C"))
        val result = gradeQuestion(q, chose(q, "B"))
        assertEquals(ResultKind.INCORRECT, result.kind)
        assertEquals(0.0, result.marksAwarded, 1e-9)
    }

    // -- NAT -----------------------------------------------------------------

    @Test
    fun `nat accepts anything inside the published range`() {
        val q = nat("q1", 2, 3.14, 3.16)
        assertEquals(2.0, gradeQuestion(q, typed(q, "3.15")).marksAwarded, 1e-9)
        assertEquals(2.0, gradeQuestion(q, typed(q, "3.14")).marksAwarded, 1e-9)
        assertEquals(2.0, gradeQuestion(q, typed(q, "3.16")).marksAwarded, 1e-9)
        assertEquals(0.0, gradeQuestion(q, typed(q, "3.17")).marksAwarded, 1e-9)
    }

    @Test
    fun `nat never deducts and survives junk input`() {
        val q = nat("q1", 1, 5.0, 5.0)
        val result = gradeQuestion(q, typed(q, "not a number"))
        assertEquals(ResultKind.INCORRECT, result.kind)
        assertEquals(0.0, result.marksAwarded, 1e-9)
    }

    @Test
    fun `nat negative values parse`() {
        val q = nat("q1", 1, -3.0, -2.0)
        assertEquals(1.0, gradeQuestion(q, typed(q, "-2.5")).marksAwarded, 1e-9)
    }

    // -- whole attempt -------------------------------------------------------

    private fun sampleTest() = MockTest(
        id = "t1",
        title = "Sample",
        durationMinutes = 60,
        sections = listOf(
            TestSection("ga", "General Aptitude", listOf("q1", "q2")),
            TestSection("cs", "Computer Science", listOf("q3", "q4")),
        ),
        questions = listOf(
            mcq("q1", 1),
            mcq("q2", 2),
            msq("q3", 2, listOf("A", "B")),
            nat("q4", 1, 10.0, 10.0),
        ),
    )

    @Test
    fun `scorecard totals and section split`() {
        val test = sampleTest()
        val attempt = Attempt(
            testId = test.id,
            startedAtEpochMs = 0,
            durationMinutes = 60,
            answers = mapOf(
                "q1" to AnswerState("q1", selectedOptionIds = setOf("A")),   // +1
                "q2" to AnswerState("q2", selectedOptionIds = setOf("D")),   // -2/3
                "q3" to AnswerState("q3", selectedOptionIds = setOf("A", "B")), // +2
                // q4 left blank                                             //  0
            ),
        )

        val card = scoreAttempt(test, attempt)

        assertEquals(1 + 2 - 2.0 / 3, card.score, 1e-9)
        assertEquals(6, card.maxMarks)
        assertEquals(2, card.correct)
        assertEquals(1, card.incorrect)
        assertEquals(1, card.unattempted)
        assertEquals(2.0 / 3, card.marksLost, 1e-9)
        assertEquals(2f / 3, card.accuracy, 1e-6f)

        val ga = card.sections.first { it.name == "General Aptitude" }
        assertEquals(1 - 2.0 / 3, ga.score, 1e-9)
        assertEquals(3, ga.maxMarks)
    }

    @Test
    fun `empty attempt scores zero rather than going negative`() {
        val test = sampleTest()
        val card = scoreAttempt(test, Attempt(test.id, 0, 60))
        assertEquals(0.0, card.score, 1e-9)
        assertEquals(0, card.attempted)
        assertEquals(0f, card.accuracy, 1e-6f)
    }

    @Test
    fun `every question is graded even when the map is sparse`() {
        // Regression: the old engine only ever looked at the question it was
        // currently on, so anything skipped simply vanished from the reckoning.
        val test = sampleTest()
        val card = scoreAttempt(test, Attempt(test.id, 0, 60))
        assertEquals(test.questionCount, card.results.size)
    }

    @Test
    fun `marks format the way GATE reports them`() {
        assertEquals("1", formatMarks(1.0))
        assertEquals("-0.33", formatMarks(-1.0 / 3))
        assertEquals("66.67", formatMarks(66.6666))
        assertEquals("0", formatMarks(0.0))
    }

    @Test
    fun `ordered questions follow section order`() {
        val test = sampleTest()
        assertEquals(listOf("q1", "q2", "q3", "q4"), test.orderedQuestions.map { it.id })
        assertTrue(test.sectionOf("q3")?.name == "Computer Science")
    }
}
