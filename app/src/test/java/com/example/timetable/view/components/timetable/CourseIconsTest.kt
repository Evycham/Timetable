package com.example.timetable.view.components.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseIconsTest {
    @Test
    fun defaultIconsAreStableAndVaried() {
        val titles = listOf("Mathe", "Informatik", "Physik", "Englisch")
        val initial = titles.associateWith { CourseIcons.getIconNameForTitle(it, null) }

        assertTrue(initial.values.toSet().size > 1)
        titles.reversed().forEach { title ->
            assertEquals(initial[title], CourseIcons.getIconNameForTitle(title, null))
            assertSame(CourseIcons.getIcon(initial[title]), CourseIcons.getIconForTitle(title, null))
        }
    }

    @Test
    fun savedIconsOverrideDefaultsAndUnknownIconsUseTheDefault() {
        assertEquals("Calculate", CourseIcons.getIconNameForTitle("Mathe", "Calculate"))
        assertSame(CourseIcons.getIcon("Calculate"), CourseIcons.getIconForTitle("Mathe", "Calculate"))
        assertEquals(
            CourseIcons.getIconNameForTitle("Mathe", null),
            CourseIcons.getIconNameForTitle("Mathe", "removed-icon")
        )
    }

    @Test
    fun defaultIconsIgnoreIconOrderAndSurviveAddingOrRemovingOtherIcons() {
        val titles = List(200) { "Modul $it" }
        val allIcons = CourseIcons.iconsMap.keys
        val expected = titles.associateWith { CourseIcons.defaultIconName(it, allIcons) }

        // Breite Icon-Verteilung.
        assertTrue(expected.values.toSet().size >= 25)

        // Unabhängig von der Icon-Reihenfolge.
        assertEquals(expected, titles.associateWith { CourseIcons.defaultIconName(it, allIcons.reversed()) })

        // Entfernen betrifft nur Titel mit diesem Icon.
        val removed = expected.getValue(titles.first())
        val remaining = allIcons - removed
        titles.forEach { title ->
            val actual = CourseIcons.defaultIconName(title, remaining)
            if (expected[title] == removed) assertTrue(actual in remaining)
            else assertEquals(expected[title], actual)
        }

        // Wechsel nur zum neu hinzugefügten Icon.
        val added = "NewIcon"
        val expanded = allIcons + added
        val actual = titles.associateWith { CourseIcons.defaultIconName(it, expanded) }
        assertTrue(added in actual.values)
        titles.forEach { title ->
            assertTrue(actual[title] == expected[title] || actual[title] == added)
        }
        assertEquals(actual, titles.associateWith { CourseIcons.defaultIconName(it, expanded.reversed()) })
    }

    @Test
    fun equalIconHashesDoNotMakeSelectionDependOnOrder() {
        val candidates = listOf("Aa", "BB")
        assertEquals(candidates[0].hashCode(), candidates[1].hashCode())

        assertEquals(
            CourseIcons.defaultIconName("Mathe", candidates),
            CourseIcons.defaultIconName("Mathe", candidates.reversed())
        )
    }

    @Test
    fun emptyAndMinimumHashTitlesResolveSafely() {
        val minimumHashTitle = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, minimumHashTitle.hashCode())
        listOf("", minimumHashTitle).forEach { title ->
            assertTrue(CourseIcons.getIconNameForTitle(title, null) in CourseIcons.iconsMap)
        }
    }
}
