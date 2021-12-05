package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 2.0)
    val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 11.0, 12.0)
    val reminder3 = ReminderDTO("Reminder3", "Description3", "Location3", 21.0, 22.0)

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun initialise() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        remindersLocalRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDown() {
        database.close()
    }

    @Test
    fun getAllReminders_ReturnsReminders() = mainCoroutineRule.runBlockingTest {
        // GIVEN - Reminders in the database
        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.saveReminder(reminder2)
        remindersLocalRepository.saveReminder(reminder3)

        // WHEN - Reminders are retrieved
        val remindersResult = remindersLocalRepository.getReminders()

        // THEN - It is successful and correct reminders retrieved
        assertThat(remindersResult).isInstanceOf(Result.Success::class.java)

        if(remindersResult is Result.Success) {
            assertThat(remindersResult.data.size).isEqualTo(3)
            assertThat(remindersResult.data.map {reminder -> reminder.id}).contains(reminder1.id)
            assertThat(remindersResult.data.map {reminder -> reminder.id}).contains(reminder2.id)
            assertThat(remindersResult.data.map {reminder -> reminder.id}).contains(reminder3.id)
        }

    }

    @Test
    fun specifiedIdDoesntExist_ReturnError() = mainCoroutineRule.runBlockingTest {
        // WHEN - A Reminder is retrieved that doesnt exist
        val remindersResult = remindersLocalRepository.getReminder("You wont find me")

        assertThat(remindersResult).isInstanceOf(Result.Error::class.java)

        if(remindersResult is Result.Error) {
            assertThat(remindersResult.message).isEqualTo("Reminder not found!")
        }
    }

}