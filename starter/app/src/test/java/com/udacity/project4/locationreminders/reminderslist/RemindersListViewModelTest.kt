package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: ReminderDataSource

    val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 2.0)
    val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 11.0, 12.0)
    val reminder3 = ReminderDTO("Reminder3", "Description3", "Location3", 21.0, 22.0)

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @Before
    fun setupReminderViewModel() = runBlockingTest {
        fakeDataSource = FakeDataSource()

        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        fakeDataSource.saveReminder(reminder1)
        fakeDataSource.saveReminder(reminder2)
        fakeDataSource.saveReminder(reminder3)
    }

    @Test
    fun loadReminders_isLoading() {

        // GIVEN - Reminders in the database (Taken care for in @Before)

        // WHEN - Loading reminders from the database
        mainCoroutineRule.pauseDispatcher()

        remindersListViewModel.loadReminders()

        // THEN - The loading indicator is shown
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue()).isTrue()

        // WHEN - Reminders are loaded
        mainCoroutineRule.resumeDispatcher()

        // THEN - The loading indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue()).isFalse()

    }

    @Test
    fun loadReminders_RemindersRetrieved() {

        // GIVEN - Reminders in the database (Taken care for in @Before)

        // WHEN - Loading reminders from the database
        remindersListViewModel.loadReminders()

        // THEN - Reminders are retrieved
        val retrievedReminders = remindersListViewModel.remindersList.getOrAwaitValue()

        assertThat(retrievedReminders).isNotEmpty()

        assertThat(retrievedReminders.size).isEqualTo(3)

        assertThat(retrievedReminders.map { reminder -> reminder.id }).contains(reminder1.id)
        assertThat(retrievedReminders.map { reminder -> reminder.id }).contains(reminder2.id)
        assertThat(retrievedReminders.map { reminder -> reminder.id }).contains(reminder3.id)

        // Verify No Data Message isn't Shown
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue()).isFalse()
    }

    @Test
    fun errorRetrievingReminders_ShowsError() {

        // GIVEN - Reminders in the database (Taken care for in @Before)

        // WHEN - Loading reminders from the database results in an error
        (fakeDataSource as FakeDataSource).shouldReturnError = true

        remindersListViewModel.loadReminders()

        // THEN - The Snackbar message contains the error & No data is shown
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue()).isEqualTo("Get Reminders Test Error")
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue()).isTrue()

    }

}