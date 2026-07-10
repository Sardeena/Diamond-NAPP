package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiences")
data class Experience(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val price: Double,
    val capacity: Int,
    val duration: String,
    val difficulty: String, // Easy, Medium, Hard
    val pickupLocation: String,
    val imageUrl: String = "",
    val featured: Boolean = false,
    val status: String = "Active" // Active, Inactive, Archived
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val passportNumber: String,
    val nationality: String,
    val phoneNumber: String,
    val emergencyContact: String,
    val pickupHotel: String,
    val roomNumber: String,
    val language: String = "English",
    val vipStatus: Boolean = false,
    val blacklist: Boolean = false,
    val internalNotes: String = ""
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val experienceId: Int,
    val experienceTitle: String,
    val date: String, // YYYY-MM-DD
    val timeSlot: String, // e.g. "09:00 - 14:00"
    val status: String, // Pending, Confirmed, Paid, Checked In, Completed, Cancelled
    val revenue: Double,
    val ticketQrCode: String,
    val staffId: Int, // Driver/Guide/Captain assigned
    val vehicleId: Int, // Boat/Yacht/Bus assigned
    val notes: String = "",
    val internalComment: String = ""
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Yacht, Speedboat, Catamaran, Bus, Jeep, Car
    val licensePlate: String,
    val fuelLevel: Int, // 0 - 100
    val status: String, // Available, Active, Maintenance, Repair
    val lastMaintenance: String,
    val insuranceExpiry: String,
    val capacity: Int
)

@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // Guide, Driver, Boat Captain, Manager
    val phoneNumber: String,
    val attendanceStatus: String, // Present, Absent, Leave
    val rating: Float,
    val performanceScore: Int, // 0-100
    val certificateUrl: String = "",
    val email: String = "",
    val passwordHash: String = "1234",
    val securityQuestion: String = "What is the name of your first pet?",
    val securityAnswer: String = "Diamond",
    val isTwoFactorEnabled: Boolean = false,
    val loginAttempts: Int = 0,
    val isLocked: Boolean = false
)
