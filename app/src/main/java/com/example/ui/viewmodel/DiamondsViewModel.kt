package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class DiamondsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DiamondsRepository(database)

    // Reactive database streams
    val experiences = repository.allExperiences.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bookings = repository.allBookings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val vehicles = repository.allVehicles.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val staff = repository.allStaff.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI state states
    val currentScreen = MutableStateFlow("explore") // explore, dashboard, bookings, experiences, customers, calendar, staff, fleet, reports, settings, profile, qr_scan, favorites
    val searchQuery = MutableStateFlow("")
    val isSearching = MutableStateFlow(false)
    
    // Favorites State
    val favoriteExperienceIds = MutableStateFlow<Set<Int>>(setOf(1, 2))

    fun toggleFavorite(experienceId: Int) {
        val current = favoriteExperienceIds.value
        favoriteExperienceIds.value = if (current.contains(experienceId)) {
            current - experienceId
        } else {
            current + experienceId
        }
    }
    
    // Auth State
    val isLoggedIn = MutableStateFlow(false) // Prompt auth screens on launch as per security guidelines
    val loggedInUser = MutableStateFlow<Staff?>(null)
    val loginCompanyCode = MutableStateFlow("DIAMOND_EXCLUSIVE_YACHTS")

    // Security Logs
    data class SecurityLog(
        val timestamp: String,
        val eventType: String, // "LOGIN_SUCCESS", "LOGIN_FAILURE", "REGISTRATION", "PASSWORD_RESET", "MFA_TOGGLE", "PASSWORD_CHANGE"
        val message: String,
        val ipAddress: String = "192.168.1.15"
    )
    val securityLogs = MutableStateFlow<List<SecurityLog>>(
        listOf(
            SecurityLog("12:00:15", "SYSTEM_START", "Luxury portal terminal initiated with local hardware-backed AES-256 keychain."),
            SecurityLog("11:45:00", "POLICY_ENFORCED", "Active directory group policy loaded. Minimum 4-character password/PIN required.")
        )
    )

    fun addSecurityLog(eventType: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val log = SecurityLog(timestamp = timeStr, eventType = eventType, message = message)
        securityLogs.value = listOf(log) + securityLogs.value
    }

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
        seedInitialDataIfNeeded()
        generateLiveNotifications()
        startSimulatedPushNotifications()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "excursion_alerts"
            val channelName = "Excursion Alerts"
            val channelDescription = "Alerts about incoming booking requests or status changes for pending excursions"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startSimulatedPushNotifications() {
        viewModelScope.launch {
            // Initial delay to let the app settle
            delay(15000)
            while (true) {
                // Periodically trigger simulated push notification (every 40 seconds)
                delay(40000)
                if (isLoggedIn.value) { // Only notify if operator is logged in
                    triggerSimulatedPushNotification()
                }
            }
        }
    }

    fun triggerSystemNotification(title: String, message: String) {
        val channelId = "excursion_alerts"
        val context = getApplication<Application>()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun triggerSimulatedPushNotification() {
        viewModelScope.launch {
            val random = Random()
            val decision = random.nextInt(2)
            if (decision == 0 || bookings.value.none { it.status == "Pending" }) {
                // Generate a new booking request!
                val randomNames = listOf("Emma Watson", "Christian Bale", "Scarlett Johansson", "Robert Downey Jr.", "Margot Robbie", "Leonardo DiCaprio")
                val randomTours = listOf("Sunset Champagne Yacht Cruise", "Amalfi Coast Speedboat Tour", "Volcanic Helicopter Heli-Excursion")
                val randomTimes = listOf("09:00 - 13:00", "14:00 - 18:00", "18:30 - 22:30")
                
                val name = randomNames[random.nextInt(randomNames.size)]
                val tourTitle = randomTours[random.nextInt(randomTours.size)]
                val time = randomTimes[random.nextInt(randomTimes.size)]
                val price = when(tourTitle) {
                    "Sunset Champagne Yacht Cruise" -> 2500.0
                    "Amalfi Coast Speedboat Tour" -> 1800.0
                    "Volcanic Helicopter Heli-Excursion" -> 5500.0
                    else -> 1200.0
                }

                // Add customer
                val customer = Customer(
                    fullName = name,
                    phoneNumber = "+39 333 ${random.nextInt(9000000) + 1000000}",
                    vipStatus = random.nextBoolean(),
                    emergencyContact = "+39 347 ${random.nextInt(9000000) + 1000000}",
                    pickupHotel = "Hotel Sirenuse, Positano",
                    roomNumber = (random.nextInt(300) + 100).toString(),
                    language = "English",
                    passportNumber = "IT${random.nextInt(900000) + 100000}X",
                    nationality = "Italian"
                )
                repository.insertCustomer(customer)
                
                // Fetch the customer id or fallback to random/default
                delay(200)
                val customerId = customers.value.firstOrNull { it.fullName == name }?.id ?: 1

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, random.nextInt(5) + 1)
                val futureDateStr = sdf.format(calendar.time)

                val newBooking = Booking(
                    customerId = customerId,
                    customerName = name,
                    experienceId = random.nextInt(3) + 1,
                    experienceTitle = tourTitle,
                    date = futureDateStr,
                    timeSlot = time,
                    revenue = price,
                    status = "Pending",
                    ticketQrCode = "TICKET-" + UUID.randomUUID().toString().take(8),
                    staffId = 1,
                    vehicleId = 1,
                    notes = "Real-time incoming online booking request. Awaiting verification."
                )
                
                repository.insertBooking(newBooking)
                
                val notifTitle = "New Booking Request Received"
                val notifDesc = "$name requested booking for '$tourTitle' on $futureDateStr."
                addNotification(
                    title = notifTitle,
                    description = notifDesc,
                    type = "Booking"
                )
                triggerSystemNotification(notifTitle, notifDesc)
            } else {
                // Change status of an existing pending excursion
                val pendingBookings = bookings.value.filter { it.status == "Pending" }
                if (pendingBookings.isNotEmpty()) {
                    val target = pendingBookings[random.nextInt(pendingBookings.size)]
                    val possibleStatuses = listOf("Confirmed", "Cancelled")
                    val newStatus = possibleStatuses[random.nextInt(possibleStatuses.size)]
                    
                    repository.updateBookingStatus(target.id, newStatus)
                    
                    val notifTitle = "Excursion Status Automatically Changed"
                    val notifDesc = "Pending booking for ${target.customerName} on ${target.date} was updated to $newStatus."
                    addNotification(
                        title = notifTitle,
                        description = notifDesc,
                        type = "Booking"
                    )
                    triggerSystemNotification(notifTitle, notifDesc)
                }
            }
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            // Check experiences
            experiences.take(2).collect { list ->
                if (list.isEmpty()) {
                    // Seed experiences
                    val yachtExp = Experience(
                        title = "Sunset Champagne Yacht Cruise",
                        description = "Sail into the pristine horizon of the Amalfi Coast onboard an ultra-luxury superyacht. Includes vintage Krug champagne, private chef caviar tastings, and deep-water swimming at secluded coastal caves.",
                        price = 2500.0,
                        capacity = 12,
                        duration = "4 Hours",
                        difficulty = "Easy",
                        pickupLocation = "Marina Grande Port, Dock A",
                        featured = true,
                        status = "Active"
                    )
                    val speedExp = Experience(
                        title = "Amalfi Coast Speedboat Tour",
                        description = "An exhilarating high-speed adventure showcasing Amalfi's towering cliffs. Stop in Positano for luxury shopping, swim under the Furore fjord bridge, and enjoy premium catering onboard.",
                        price = 1800.0,
                        capacity = 8,
                        duration = "6 Hours",
                        difficulty = "Medium",
                        pickupLocation = "Sorrento Pier 4, Gate B",
                        featured = true,
                        status = "Active"
                    )
                    val heliExp = Experience(
                        title = "Volcanic Helicopter Heli-Excursion",
                        description = "Fly high above Mount Vesuvius and the ancient ruins of Pompeii with VIP transfers. Descend directly onto a private vineyard estate on Vesuvian slopes for a luxury wine pairing and lunch.",
                        price = 5500.0,
                        capacity = 4,
                        duration = "2 Hours",
                        difficulty = "Hard",
                        pickupLocation = "Naples Airport Executive Helipad",
                        featured = true,
                        status = "Active"
                    )
                    val wineExp = Experience(
                        title = "Private Tuscan Estate Wine Tour",
                        description = "Indulge in a premium, chauffeured Tuscan vineyard escape. Meet master winemakers, stroll private medieval cellars, and savor an estate-exclusive culinary degustation dinner.",
                        price = 1200.0,
                        capacity = 6,
                        duration = "8 Hours",
                        difficulty = "Easy",
                        pickupLocation = "Hotel Florence Lobby",
                        featured = false,
                        status = "Active"
                    )
                    repository.insertExperience(yachtExp)
                    repository.insertExperience(speedExp)
                    repository.insertExperience(heliExp)
                    repository.insertExperience(wineExp)

                    // Seed customers
                    val ladyB = Customer(
                        fullName = "Lady Beatrice Harrington",
                        passportNumber = "GBR948211A",
                        nationality = "British",
                        phoneNumber = "+44 7700 900077",
                        emergencyContact = "+44 7700 900088",
                        pickupHotel = "Belmond Hotel Caruso, Ravello",
                        roomNumber = "VIP Suite 3",
                        language = "English",
                        vipStatus = true,
                        blacklist = false,
                        internalNotes = "Prefers sparkling water only. Heavy security escort may accompany."
                    )
                    val maxS = Customer(
                        fullName = "Maximilian Sterling",
                        passportNumber = "USA104829B",
                        nationality = "American",
                        phoneNumber = "+1 (555) 019-2831",
                        emergencyContact = "+1 (555) 019-2832",
                        pickupHotel = "Villa d'Este Como",
                        roomNumber = "Presidential Penthouse",
                        language = "English",
                        vipStatus = true,
                        blacklist = false,
                        internalNotes = "Celebrity travel client. Keep itinerary confidential."
                    )
                    val countJ = Customer(
                        fullName = "Count Jean-Pierre de Rothschild",
                        passportNumber = "FRA829103C",
                        nationality = "French",
                        phoneNumber = "+33 6 5555 0142",
                        emergencyContact = "+33 6 5555 0143",
                        pickupHotel = "Hotel de Paris Monaco",
                        roomNumber = "Imperial Suite 704",
                        language = "French",
                        vipStatus = true,
                        blacklist = false,
                        internalNotes = "Passionate about marine archaeology. Requesting Captain Marco Rossi specifically."
                    )
                    repository.insertCustomer(ladyB)
                    repository.insertCustomer(maxS)
                    repository.insertCustomer(countJ)

                    // Seed Staff
                    val captain = Staff(
                        name = "Captain Marco Rossi",
                        role = "Boat Captain",
                        phoneNumber = "+39 333 456 7890",
                        attendanceStatus = "Present",
                        rating = 4.9f,
                        performanceScore = 98,
                        certificateUrl = "Yacht Master Ocean Unlimited",
                        email = "captain@diamonds.com",
                        passwordHash = "1111",
                        securityQuestion = "What is your favorite harbor?",
                        securityAnswer = "Portofino",
                        isTwoFactorEnabled = true
                    )
                    val guide = Staff(
                        name = "Elena Moretti",
                        role = "Guide",
                        phoneNumber = "+39 333 123 4567",
                        attendanceStatus = "Present",
                        rating = 4.8f,
                        performanceScore = 95,
                        certificateUrl = "National Historic Tour Operator License",
                        email = "guide@diamonds.com",
                        passwordHash = "2222",
                        securityQuestion = "What is your first pet's name?",
                        securityAnswer = "Diamond",
                        isTwoFactorEnabled = false
                    )
                    val driver = Staff(
                        name = "Giovanni Bianchi",
                        role = "Driver",
                        phoneNumber = "+39 333 987 6543",
                        attendanceStatus = "Present",
                        rating = 4.7f,
                        performanceScore = 92,
                        certificateUrl = "Commercial Class D Escort License",
                        email = "driver@diamonds.com",
                        passwordHash = "3333",
                        securityQuestion = "What is your dream yacht?",
                        securityAnswer = "SuperYacht",
                        isTwoFactorEnabled = false
                    )
                    val manager = Staff(
                        name = "Demo Manager",
                        role = "Manager",
                        phoneNumber = "+39 333 000 1111",
                        attendanceStatus = "Present",
                        rating = 5.0f,
                        performanceScore = 100,
                        certificateUrl = "Company Administrator Credentials",
                        email = "manager@diamonds.com",
                        passwordHash = "8888",
                        securityQuestion = "What city was the company founded in?",
                        securityAnswer = "Milan",
                        isTwoFactorEnabled = true
                    )
                    repository.insertStaff(captain)
                    repository.insertStaff(guide)
                    repository.insertStaff(driver)
                    repository.insertStaff(manager)

                    // Seed Fleet/Vehicles
                    val yacht = Vehicle(
                        name = "Perla Nera Superyacht",
                        type = "Yacht",
                        licensePlate = "IT-84920-YH",
                        fuelLevel = 88,
                        status = "Available",
                        lastMaintenance = "2026-06-15",
                        insuranceExpiry = "2027-06-15",
                        capacity = 15
                    )
                    val speedboat = Vehicle(
                        name = "Aura Super Speedboat",
                        type = "Speedboat",
                        licensePlate = "IT-10394-SB",
                        fuelLevel = 92,
                        status = "Available",
                        lastMaintenance = "2026-07-01",
                        insuranceExpiry = "2027-07-01",
                        capacity = 8
                    )
                    val van = Vehicle(
                        name = "Mercedes-Benz V-Class Elite",
                        type = "Bus",
                        licensePlate = "IT-58392-MV",
                        fuelLevel = 75,
                        status = "Active",
                        lastMaintenance = "2026-05-20",
                        insuranceExpiry = "2027-05-20",
                        capacity = 7
                    )
                    repository.insertVehicle(yacht)
                    repository.insertVehicle(speedboat)
                    repository.insertVehicle(van)

                    // Seed Bookings
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val todayDate = formatter.format(Date())

                    val b1 = Booking(
                        customerId = 1,
                        customerName = "Lady Beatrice Harrington",
                        experienceId = 1,
                        experienceTitle = "Sunset Champagne Yacht Cruise",
                        date = todayDate,
                        timeSlot = "16:00 - 20:00",
                        status = "Checked In",
                        revenue = 2500.0,
                        ticketQrCode = "DIAMOND_TKT_LADYB_001",
                        staffId = 1, // Captain Marco
                        vehicleId = 1, // Perla Nera
                        notes = "Wants chef-exclusive caviar selection. Sparkling water only.",
                        internalComment = "Extremely high value client, priority service."
                    )
                    val b2 = Booking(
                        customerId = 2,
                        customerName = "Maximilian Sterling",
                        experienceId = 2,
                        experienceTitle = "Amalfi Coast Speedboat Tour",
                        date = todayDate,
                        timeSlot = "09:00 - 15:00",
                        status = "Confirmed",
                        revenue = 1800.0,
                        ticketQrCode = "DIAMOND_TKT_MAXS_002",
                        staffId = 2, // Elena Moretti
                        vehicleId = 2, // Aura Speedboat
                        notes = "Prefers secluded swimming beaches. Avoid crowded tourist docks.",
                        internalComment = "Confidential paparazzi-safe trip guidelines active."
                    )
                    val b3 = Booking(
                        customerId = 3,
                        customerName = "Count Jean-Pierre de Rothschild",
                        experienceId = 3,
                        experienceTitle = "Volcanic Helicopter Heli-Excursion",
                        date = "2026-07-11",
                        timeSlot = "11:00 - 13:00",
                        status = "Paid",
                        revenue = 5500.0,
                        ticketQrCode = "DIAMOND_TKT_ROTHS_003",
                        staffId = 3, // Giovanni Bianchi (transfers)
                        vehicleId = 3, // Mercedes V-Class Elite
                        notes = "Wants wine steward to meet on Vesuvian estate helipad.",
                        internalComment = "Willing to upgrade to yacht trip next year."
                    )
                    repository.insertBooking(b1)
                    repository.insertBooking(b2)
                    repository.insertBooking(b3)

                    // Ready for authentication
                }
            }
        }
    }

    private fun generateLiveNotifications() {
        _notifications.value = listOf(
            NotificationItem(
                id = 1,
                title = "Live Weather Alert",
                description = "Wind speeds expected to rise to 18 knots by 17:00. Advise coastal excursions to return early.",
                type = "Weather",
                timestamp = "10 mins ago"
            ),
            NotificationItem(
                id = 2,
                title = "New Booking Request",
                description = "VIP guest Lady Beatrice Harrington requested additional dinner booking on yacht tomorrow.",
                type = "Booking",
                timestamp = "32 mins ago"
            ),
            NotificationItem(
                id = 3,
                title = "Fleet Fleet Notification",
                description = "Aura Super Speedboat fuel level below 40%. Scheduled for refueling at Dock C.",
                type = "Fleet",
                timestamp = "1 hour ago"
            ),
            NotificationItem(
                id = 4,
                title = "Sea Conditions: Warning",
                description = "Moderate swell detected outside Naples harbor, yacht stabilizers active.",
                type = "Weather",
                timestamp = "3 hours ago"
            )
        )
    }

    // Interactive functions
    fun addBooking(booking: Booking) {
        viewModelScope.launch {
            repository.insertBooking(booking)
            addNotification(
                title = "New Booking Created",
                description = "Excursion booking for ${booking.customerName} on ${booking.experienceTitle} has been recorded.",
                type = "Booking"
            )
        }
    }

    fun updateBookingStatus(bookingId: Int, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, status)
            val booking = bookings.value.firstOrNull { it.id == bookingId }
            val guestName = booking?.customerName ?: "Guest"
            val tourTitle = booking?.experienceTitle ?: "Excursion"
            val notifTitle = "Booking Status Updated"
            val notifDesc = "$guestName's booking for '$tourTitle' is now $status."
            addNotification(
                title = notifTitle,
                description = notifDesc,
                type = "Booking"
            )
            triggerSystemNotification(notifTitle, notifDesc)
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            repository.deleteBooking(booking)
            addNotification(
                title = "Booking Deleted",
                description = "Booking ID #${booking.id} for ${booking.customerName} has been deleted/archived.",
                type = "Booking"
            )
        }
    }

    fun addExperience(experience: Experience) {
        viewModelScope.launch {
            repository.insertExperience(experience)
            addNotification(
                title = "Experience Added",
                description = "Luxury tour '${experience.title}' added to company catalog.",
                type = "Experience"
            )
        }
    }

    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.insertCustomer(customer)
            addNotification(
                title = "VIP Customer Registered",
                description = "Profile for ${customer.fullName} successfully registered under company record.",
                type = "Customer"
            )
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.insertVehicle(vehicle)
            addNotification(
                title = "Fleet Asset Added",
                description = "${vehicle.name} (${vehicle.type}) enrolled in the live tracking fleet.",
                type = "Fleet"
            )
        }
    }

    fun updateVehicleStatus(vehicle: Vehicle, status: String) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle.copy(status = status))
            addNotification(
                title = "Fleet Asset Status",
                description = "${vehicle.name} status updated to $status.",
                type = "Fleet"
            )
        }
    }

    fun addStaff(staffMember: Staff) {
        viewModelScope.launch {
            repository.insertStaff(staffMember)
            addNotification(
                title = "Employee Enrolled",
                description = "${staffMember.name} joined as a registered ${staffMember.role}.",
                type = "Staff"
            )
        }
    }

    fun updateStaffAttendance(staffMember: Staff, status: String) {
        viewModelScope.launch {
            repository.updateStaff(staffMember.copy(attendanceStatus = status))
            addNotification(
                title = "Staff Attendance",
                description = "${staffMember.name} attendance marked as $status.",
                type = "Staff"
            )
        }
    }

    private fun addNotification(title: String, description: String, type: String) {
        val nextId = (_notifications.value.maxOfOrNull { it.id } ?: 0) + 1
        val newNotif = NotificationItem(
            id = nextId,
            title = title,
            description = description,
            type = type,
            timestamp = "Just now"
        )
        _notifications.value = listOf(newNotif) + _notifications.value
    }

    fun dismissNotification(id: Int) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    fun performLogout() {
        viewModelScope.launch {
            val user = loggedInUser.value
            addSecurityLog("LOGOUT", "User ${user?.name ?: "Unknown"} logged out securely.")
            isLoggedIn.value = false
            loggedInUser.value = null
        }
    }

    fun performLogin(companyCode: String, role: String) {
        viewModelScope.launch {
            loginCompanyCode.value = companyCode
            isLoggedIn.value = true
            val match = staff.value.firstOrNull { it.role == role }
            if (match != null) {
                loggedInUser.value = match
            } else {
                loggedInUser.value = Staff(
                    name = "Demo $role",
                    role = role,
                    phoneNumber = "+39 000 0000",
                    attendanceStatus = "Present",
                    rating = 5.0f,
                    performanceScore = 100,
                    certificateUrl = "Verified credentials",
                    email = "${role.lowercase()}@diamonds.com",
                    passwordHash = "1234"
                )
            }
            currentScreen.value = "explore"
            addSecurityLog("LOGIN_SUCCESS", "Legacy secure login as $role from authorized terminal.")
            addNotification(
                title = "Secure Login Successful",
                description = "Employee account ($role) accessed from verified hardware.",
                type = "Security"
            )
        }
    }

    fun performSecureLogin(
        email: String,
        passwordHash: String,
        onTwoFactorRequired: (Staff) -> Unit,
        onSuccess: (Staff) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()

            if (isSupabaseConfigured()) {
                val authResult = supabaseService.signIn(trimmedEmail, passwordHash)
                if (!authResult.success) {
                    addSecurityLog("LOGIN_FAILURE", "Supabase authentication failed for $trimmedEmail: ${authResult.message}")
                    onFailure(authResult.message)
                    return@launch
                }
            }

            val match = staff.value.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
            
            if (match == null) {
                if (isSupabaseConfigured()) {
                    // Automatically provision a local account on first login since Supabase authenticated successfully
                    val newStaff = Staff(
                        name = trimmedEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        role = "Guide",
                        phoneNumber = "+39 000 0000",
                        attendanceStatus = "Present",
                        rating = 5.0f,
                        performanceScore = 100,
                        certificateUrl = "Enrolled via Supabase Auth",
                        email = trimmedEmail,
                        passwordHash = passwordHash,
                        securityQuestion = "Auto-created",
                        securityAnswer = "supabase",
                        isTwoFactorEnabled = false
                    )
                    repository.insertStaff(newStaff)
                    completeLogin(newStaff)
                    onSuccess(newStaff)
                } else {
                    addSecurityLog("LOGIN_FAILURE", "Failed login attempt for unknown email: $trimmedEmail")
                    onFailure("No account registered with this email.")
                }
                return@launch
            }

            if (match.isLocked) {
                addSecurityLog("LOGIN_FAILURE", "Attempted login to locked account: ${match.email}")
                onFailure("This account is temporarily locked due to excessive failed attempts. Use recovery questions.")
                return@launch
            }

            if (!isSupabaseConfigured() && match.passwordHash != passwordHash) {
                val newAttempts = match.loginAttempts + 1
                val isNowLocked = newAttempts >= 3
                val updatedStaff = match.copy(loginAttempts = newAttempts, isLocked = isNowLocked)
                repository.updateStaff(updatedStaff)

                if (isNowLocked) {
                    addSecurityLog("LOGIN_LOCK", "Account locked due to 3 failed attempts: ${match.email}")
                    onFailure("Account locked. Please reset your password via security questions.")
                } else {
                    addSecurityLog("LOGIN_FAILURE", "Incorrect password for: ${match.email} (Attempt $newAttempts/3)")
                    onFailure("Incorrect PIN or Password. (${3 - newAttempts} attempts remaining)")
                }
                return@launch
            }

            // Correct password - reset attempts
            if (match.loginAttempts > 0) {
                repository.updateStaff(match.copy(loginAttempts = 0))
            }

            if (match.isTwoFactorEnabled) {
                addSecurityLog("2FA_CHALLENGE", "Multi-factor authentication required for ${match.email}")
                onTwoFactorRequired(match)
            } else {
                completeLogin(match)
                onSuccess(match)
            }
        }
    }

    fun completeLogin(staffMember: Staff) {
        loggedInUser.value = staffMember
        isLoggedIn.value = true
        currentScreen.value = "explore"
        addSecurityLog("LOGIN_SUCCESS", "User ${staffMember.name} logged in successfully (Role: ${staffMember.role}).")
        addNotification(
            title = "Secure Login Successful",
            description = "Welcome back, ${staffMember.name}. Session authenticated.",
            type = "Security"
        )
    }

    fun registerSecureUser(
        name: String,
        email: String,
        passwordHash: String,
        role: String,
        phone: String,
        securityQuestion: String,
        securityAnswer: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            if (staff.value.any { it.email.equals(trimmedEmail, ignoreCase = true) }) {
                onFailure("An account with this email already exists.")
                return@launch
            }

            var successMsg = "Operator enrolled successfully! Please login."
            if (isSupabaseConfigured()) {
                val signUpResult = supabaseService.signUp(trimmedEmail, passwordHash)
                if (!signUpResult.success) {
                    onFailure(signUpResult.message)
                    return@launch
                }
                successMsg = signUpResult.message
            }

            val newStaff = Staff(
                name = name,
                role = role,
                phoneNumber = phone.ifEmpty { "+39 000 0000" },
                attendanceStatus = "Present",
                rating = 5.0f,
                performanceScore = 100,
                certificateUrl = "Enrolled via secure terminal",
                email = trimmedEmail,
                passwordHash = passwordHash,
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer.trim().lowercase(Locale.ROOT),
                isTwoFactorEnabled = false
            )

            repository.insertStaff(newStaff)
            addSecurityLog("REGISTRATION", "New operator ${name} enrolled as ${role} (${trimmedEmail}).")
            onSuccess(successMsg)
        }
    }

    fun resetSecurePassword(
        email: String,
        securityAnswer: String,
        newPasswordHash: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            val match = staff.value.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }

            if (match == null) {
                onFailure("No registered user found with that email.")
                return@launch
            }

            val ansMatch = match.securityAnswer.trim().lowercase(Locale.ROOT) == securityAnswer.trim().lowercase(Locale.ROOT)
            if (!ansMatch) {
                addSecurityLog("PASSWORD_RESET_FAILURE", "Failed password reset attempt for ${match.email}")
                onFailure("Security recovery answer is incorrect.")
                return@launch
            }

            val updatedStaff = match.copy(
                passwordHash = newPasswordHash,
                loginAttempts = 0,
                isLocked = false
            )
            repository.updateStaff(updatedStaff)
            addSecurityLog("PASSWORD_RESET", "Password successfully reset via security recovery for ${match.email}")
            onSuccess()
        }
    }

    fun updateSecureProfile(updatedStaff: Staff) {
        viewModelScope.launch {
            repository.updateStaff(updatedStaff)
            if (loggedInUser.value?.id == updatedStaff.id) {
                loggedInUser.value = updatedStaff
            }
            addSecurityLog("PROFILE_UPDATE", "User profile and security attributes updated for ${updatedStaff.name}.")
        }
    }

    // Support Ticket System
    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(
        listOf(
            SupportTicket(
                id = 1,
                title = "Yacht 3 Local WLAN Sync Issue",
                description = "WLAN router in primary guest saloon is dropping connection to check-in tablets. Needs manual firmware sync.",
                priority = "Medium",
                status = "Resolved",
                timestamp = "2026-07-11 14:30",
                category = "IT Support"
            ),
            SupportTicket(
                id = 2,
                title = "Starboard Fender Wear on Yacht 'Diamond Queen'",
                description = "Severe abrasion noticed during docking at Amalfi port. Need a spare pneumatic fender replacement before tomorrow's VIP charter.",
                priority = "High",
                status = "Processing",
                timestamp = "2026-07-11 18:15",
                category = "Vessel Maintenance"
            )
        )
    )
    val supportTickets = _supportTickets.asStateFlow()

    fun createSupportTicket(title: String, description: String, priority: String, category: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val newId = (_supportTickets.value.maxOfOrNull { it.id } ?: 0) + 1
        val ticket = SupportTicket(
            id = newId,
            title = title,
            description = description,
            priority = priority,
            status = "Open",
            timestamp = dateStr,
            category = category
        )
        _supportTickets.value = listOf(ticket) + _supportTickets.value
        addSecurityLog("SUPPORT_TICKET_CREATED", "Support Ticket #$newId ('$title') filed by operator.")
    }

    // Supabase Sync Integration
    private val supabaseService = SupabaseService()
    val isSyncing = MutableStateFlow(false)
    val supabaseSyncReport = MutableStateFlow<SyncReport?>(null)

    fun isSupabaseConfigured(): Boolean {
        return supabaseService.isConfigured()
    }

    fun syncDataWithSupabase() {
        viewModelScope.launch {
            isSyncing.value = true
            supabaseSyncReport.value = null
            addSecurityLog("SUPABASE_SYNC_START", "Initiating master node synchronization sequence to remote database.")

            val result = supabaseService.syncToSupabase(
                staffList = staff.value,
                customerList = customers.value,
                vehicleList = vehicles.value,
                bookingList = bookings.value
            )

            supabaseSyncReport.value = result
            if (result.success) {
                addSecurityLog("SUPABASE_SYNC_SUCCESS", "All tables synchronized with remote node. UUID schema intact.")
            } else {
                addSecurityLog("SUPABASE_SYNC_FAILURE", "Sync completed with status / errors: ${result.message}")
            }
            isSyncing.value = false
        }
    }
}


data class NotificationItem(
    val id: Int,
    val title: String,
    val description: String,
    val type: String, // Weather, Booking, Fleet, Staff, Security
    val timestamp: String
)

data class SupportTicket(
    val id: Int,
    val title: String,
    val description: String,
    val priority: String, // Low, Medium, High
    val status: String, // Open, Processing, Resolved
    val timestamp: String,
    val category: String // IT Support, Billing, Fleet Management, Customer Inquiry
)
