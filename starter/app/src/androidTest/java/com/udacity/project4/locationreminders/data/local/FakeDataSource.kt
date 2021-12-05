package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Get Reminders Test Error")
        }
        reminders?.let {
            return Result.Success(ArrayList(reminders))
        }
        return Result.Error("No Reminder List Found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if(shouldReturnError) {
            return Result.Error("Get Reminder Test Error")
        }
        reminders?.let {
            var relevantReminder = it.filter { it.id == id }

            return when(relevantReminder.size) {
                0 -> Result.Error("No Reminders With Specified ID")
                1 -> Result.Success(relevantReminder[0])
                else -> Result.Error("Multiple Reminders With The Same ID")
            }
        }
        return Result.Error("No Data")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}