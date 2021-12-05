package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: ReminderDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupReminderViewModel() {
        fakeDataSource = FakeDataSource()

        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        val reminder1 = ReminderDataItem("Reminder1", "Description1", "Location1", 1.0, 2.0)
        val reminder2 = ReminderDataItem("Reminder2", "Description2", "Location2", 11.0, 12.0)
        val reminder3 = ReminderDataItem("Reminder3", "Description3", "Location3", 21.0, 22.0)

        saveReminderViewModel.saveReminder(reminder1)
        saveReminderViewModel.saveReminder(reminder2)
        saveReminderViewModel.saveReminder(reminder3)

    }

    @Test
    fun addNewReminder_IsSavedToLocalDataSource() = runBlockingTest {

        // GIVEN - A new reminder
        val reminder4 = ReminderDataItem("Reminder4", "Description4", "Location4", 31.0, 32.0)

        // WHEN - I save it to the database
        saveReminderViewModel.saveReminder(reminder4)

        // THEN - It saves to the database
        val reminderResult = fakeDataSource.getReminder(reminder4.id)

        // Verify Reminder Details
        assertThat(reminderResult).isInstanceOf(Result.Success::class.java)

        if(reminderResult is Result.Success) {
            assertThat(reminderResult.data.id).isEqualTo(reminder4.id)
            assertThat(reminderResult.data.description).isEqualTo(reminder4.description)
            assertThat(reminderResult.data.location).isEqualTo(reminder4.location)
            assertThat(reminderResult.data.latitude).isEqualTo(reminder4.latitude)
            assertThat(reminderResult.data.longitude).isEqualTo(reminder4.longitude)
        }

        // Verify Toast Message & Loading Status
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue()).isEqualTo("Reminder Saved !")
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue()).isFalse()

    }

}