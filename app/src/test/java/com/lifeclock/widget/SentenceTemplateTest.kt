package com.lifeclock.widget

import com.lifeclock.widget.ui.settings.BirthField
import com.lifeclock.widget.ui.settings.TemplatePart
import com.lifeclock.widget.ui.settings.splitTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceTemplateTest {
    private val text = TemplatePart::Text
    private val slot = TemplatePart::Slot

    @Test
    fun englishSentenceSplitsIntoTextAndSlotsInOrder() {
        assertEquals(
            listOf(text("I was born on"), slot(BirthField.Date), text("at"), slot(BirthField.Time), text("in"), slot(BirthField.City)),
            splitTemplate("I was born on {date} at {time} in {city}"),
        )
    }

    @Test
    fun russianSentenceKeepsItsOwnWordOrderAndPunctuation() {
        assertEquals(
            listOf(text("Я родился(-ась)"), slot(BirthField.Date), text("в"), slot(BirthField.Time), text(", город:"), slot(BirthField.City)),
            splitTemplate("Я родился(-ась) {date} в {time}, город: {city}"),
        )
    }

    @Test
    fun slotsMayComeInAnyOrderAndTextMayBeAbsent() {
        assertEquals(
            listOf(slot(BirthField.City), slot(BirthField.Date)),
            splitTemplate("{city}{date}"),
        )
        assertEquals(listOf(text("no slots")), splitTemplate("no slots"))
    }
}
