package com.example

import com.example.data.Card
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun check_no_duplicate_card_ids() {
    val cards = Card.PreloadedCards
    val ids = cards.map { it.id }
    val duplicates = ids.groupBy { it }.filter { it.value.size > 1 }.keys
    assertTrue("Duplicate card IDs found: $duplicates", duplicates.isEmpty())
  }
}
