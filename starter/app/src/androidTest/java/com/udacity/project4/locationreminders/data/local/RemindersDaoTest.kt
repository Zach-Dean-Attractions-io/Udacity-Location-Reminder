package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 2.0)
    val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 11.0, 12.0)
    val reminder3 = ReminderDTO("Reminder3", "Description3", "Location3", 21.0, 22.0)

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertReminder_IsInsertedIntoDatabase() = runBlockingTest {

        // GIVEN - A Reminder

        // WHEN - It is inserted into the database
        database.reminderDao().saveReminder(reminder1)

        // THEN - It is inserted as expected
        val insertedReminder = database.reminderDao().getReminderById(reminder1.id)

        assertThat(insertedReminder).isNotNull()
        assertThat(insertedReminder?.id).isEqualTo(reminder1.id)
        assertThat(insertedReminder?.title).isEqualTo(reminder1.title)
        assertThat(insertedReminder?.description).isEqualTo(reminder1.description)
        assertThat(insertedReminder?.location).isEqualTo(reminder1.location)
        assertThat(insertedReminder?.latitude).isEqualTo(reminder1.latitude)
        assertThat(insertedReminder?.longitude).isEqualTo(reminder1.longitude)

    }

    @Test
    fun deleteAllReminders_ReturnsNoReminders() = runBlockingTest {
        // GIVEN - Reminders in the database
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // Verify they did in fact save
        assertThat(database.reminderDao().getReminders().size).isEqualTo(3)

        // WHEN - All Reminders Are Deleted
        database.reminderDao().deleteAllReminders()

        // THEN - No reminders are returned
        assertThat(database.reminderDao().getReminders()).isEmpty()
    }

    @Test
    fun loadingReminders_ReturnsReminders() = runBlockingTest {
        // GIVEN - Reminders in the database
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // WHEN - Reminders are loaded
        val reminders = database.reminderDao().getReminders()

        // THEN - Reminders are correctly retrieved
        assertThat(reminders).isNotEmpty()
        assertThat(reminders.size).isEqualTo(3)
        assertThat(reminders.map { reminder -> reminder.id }).contains(reminder1.id)
        assertThat(reminders.map { reminder -> reminder.id }).contains(reminder2.id)
        assertThat(reminders.map { reminder -> reminder.id }).contains(reminder3.id)
    }

    @Test
    fun unFoundReminder_ReturnsNoReminder() = runBlockingTest {
        // WHEN - A Reminder isn't found
        val reminder = database.reminderDao().getReminderById("You wont find me")

        // THEN - The reminder is null
        assertThat(reminder).isNull()
    }

}