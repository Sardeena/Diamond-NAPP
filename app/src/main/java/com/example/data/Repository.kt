package com.example.data

import kotlinx.coroutines.flow.Flow

class DiamondsRepository(private val database: AppDatabase) {

    // Experiences
    val allExperiences: Flow<List<Experience>> = database.experienceDao().getAllExperiences()
    suspend fun insertExperience(experience: Experience) = database.experienceDao().insertExperience(experience)
    suspend fun updateExperience(experience: Experience) = database.experienceDao().updateExperience(experience)
    suspend fun deleteExperience(experience: Experience) = database.experienceDao().deleteExperience(experience)

    // Customers
    val allCustomers: Flow<List<Customer>> = database.customerDao().getAllCustomers()
    suspend fun insertCustomer(customer: Customer) = database.customerDao().insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = database.customerDao().updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = database.customerDao().deleteCustomer(customer)

    // Bookings
    val allBookings: Flow<List<Booking>> = database.bookingDao().getAllBookings()
    suspend fun insertBooking(booking: Booking) = database.bookingDao().insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = database.bookingDao().updateBooking(booking)
    suspend fun updateBookingStatus(bookingId: Int, status: String) = database.bookingDao().updateBookingStatus(bookingId, status)
    suspend fun deleteBooking(booking: Booking) = database.bookingDao().deleteBooking(booking)

    // Fleet (Vehicles)
    val allVehicles: Flow<List<Vehicle>> = database.vehicleDao().getAllVehicles()
    suspend fun insertVehicle(vehicle: Vehicle) = database.vehicleDao().insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = database.vehicleDao().updateVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = database.vehicleDao().deleteVehicle(vehicle)

    // Staff
    val allStaff: Flow<List<Staff>> = database.staffDao().getAllStaff()
    suspend fun insertStaff(staff: Staff) = database.staffDao().insertStaff(staff)
    suspend fun updateStaff(staff: Staff) = database.staffDao().updateStaff(staff)
    suspend fun deleteStaff(staff: Staff) = database.staffDao().deleteStaff(staff)
}
