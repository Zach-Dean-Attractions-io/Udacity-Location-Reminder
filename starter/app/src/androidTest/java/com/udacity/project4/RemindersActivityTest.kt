package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var firebaseAuth: FirebaseAuth

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        //IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun deregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        //IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        //Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword("zach@test.com", "Test123456")

        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
            single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun savedReminder_IsDisplayed() = runBlocking {

        val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 2.0)

        repository.saveReminder(reminder1)

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withText(reminder1.title)).check(matches(isDisplayed()))

        activityScenario.close()
    }


    @Test
    fun addReminder() = runBlocking {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify No Data is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        // Click To Add Reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click Save To Check Error Message Is Displayed
        onView(withId(R.id.saveReminder)).perform(click())

        delay(1000)

        onView(withText(appContext.getString(R.string.err_enter_title))).check(matches(isDisplayed()))

        delay(3000)

        // Enter Reminder Title & Description
        onView(withId(R.id.reminderTitle)).perform(replaceText("Reminder1"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Description1"))
        closeSoftKeyboard()

        // Click Save To Check Error Message Is Displayed
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(appContext.getString(R.string.err_select_location))).check(matches(isDisplayed()))

        delay(3000)

        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.map_fragment)).perform(longClick())

        onView(withId(R.id.save_button)).perform(click())

        // Enter Reminder Title & Description again
        onView(withId(R.id.reminderTitle)).perform(replaceText("Reminder1"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Description1"))
        closeSoftKeyboard()

        onView(withId(R.id.saveReminder)).perform(click())

        delay(1000)

        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(getActivity(activityScenario)!!.window.decorView))))
            .check(matches(isDisplayed()))

        delay(3000)

        onView(withText("Reminder1")).check(matches(isDisplayed()))
        onView(withText("Description1")).check(matches(isDisplayed()))

        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }




}

/**

delay(1000)

// Click to Login
onView(withId(R.id.login_button)).perform(click())

delay(1000)

// Click Email Button (ID's found from layout inspector)
onView(withId(R.id.email_button)).perform(click())
// Enter Email
onView(withId(R.id.email)).perform(replaceText("zach@test.com"))
// Click Next
onView(withId(R.id.button_next)).perform(click())
// Enter Password
onView(withId(R.id.password)).perform(replaceText("Test123456"))

// Seems to need a delay to work properly


// Sign In
onView(withId(R.id.button_done)).perform(click())**/