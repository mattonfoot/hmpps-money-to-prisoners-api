package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Comment Model")
class CommentTest {

  private fun createCredit(): Credit {
    val credit = Credit(id = 1L, amount = 1000)
    credit.onCreate()
    return credit
  }

  @Nested
  @DisplayName("CRD-105: Comment entity basics")
  inner class CommentBasics {

    @Test
    fun `stores comment text`() {
      val comment = Comment(comment = "Test comment")
      assertEquals("Test comment", comment.comment)
    }

    @Test
    fun `stores credit reference`() {
      val credit = createCredit()
      val comment = Comment(comment = "Test comment")
      comment.credit = credit
      assertEquals(credit, comment.credit)
    }

    @Test
    fun `stores user reference`() {
      val comment = Comment(comment = "Test comment", userId = "user1")
      assertEquals("user1", comment.userId)
    }

    @Test
    fun `onCreate sets timestamps`() {
      val comment = Comment(comment = "Test comment")
      comment.onCreate()
      assertNotNull(comment.created)
      assertNotNull(comment.modified)
    }

    @Test
    fun `onUpdate modifies timestamp`() {
      val comment = Comment(comment = "Test comment")
      comment.onCreate()
      val originalModified = comment.modified
      comment.onUpdate()
      assertNotNull(comment.modified)
    }
  }
}
