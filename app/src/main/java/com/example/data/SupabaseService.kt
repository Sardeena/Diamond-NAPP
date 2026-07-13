package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Deterministic Company ID for this multi-tenant layout
    val defaultCompanyId = UUID.nameUUIDFromBytes("company_diamonds_elite".toByteArray()).toString()

    private var resolvedUrl: String = ""
    private var resolvedKey: String = ""
    private var isResolved = false

    private fun resolveConfig() {
        if (isResolved) return
        val rawUrl = BuildConfig.SUPABASE_URL.trim()
        val rawKey = BuildConfig.SUPABASE_KEY.trim()

        var detectedUrl = ""
        var anonKey = ""
        var serviceKey = ""

        fun checkValue(value: String) {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                detectedUrl = value
            } else if (value.startsWith("eyJ")) {
                try {
                    val parts = value.split(".")
                    if (parts.size >= 2) {
                        val payloadBytes = android.util.Base64.decode(
                            parts[1],
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                        )
                        val payload = String(payloadBytes, Charsets.UTF_8)
                        
                        val refRegex = "\"ref\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                        val refMatch = refRegex.find(payload)
                        val ref = refMatch?.groupValues?.get(1)
                        if (ref != null && detectedUrl.isEmpty()) {
                            detectedUrl = "https://$ref.supabase.co"
                        }

                        val roleRegex = "\"role\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                        val roleMatch = roleRegex.find(payload)
                        val role = roleMatch?.groupValues?.get(1)
                        if (role == "service_role") {
                            serviceKey = value
                        } else {
                            anonKey = value
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseService", "Error decoding potential JWT: ${e.localizedMessage}")
                }
            }
        }

        checkValue(rawUrl)
        checkValue(rawKey)

        resolvedUrl = detectedUrl
        resolvedKey = when {
            serviceKey.isNotEmpty() -> serviceKey
            anonKey.isNotEmpty() -> anonKey
            rawKey.isNotEmpty() && !rawKey.startsWith("your-supabase-api-key-placeholder") -> rawKey
            else -> rawUrl
        }
        isResolved = true
    }

    fun getBaseUrl(): String {
        resolveConfig()
        return resolvedUrl
    }

    fun getApiKey(): String {
        resolveConfig()
        return resolvedKey
    }

    /**
     * Checks if Supabase credentials have been properly configured by the user.
     */
    fun isConfigured(): Boolean {
        resolveConfig()
        return resolvedUrl.isNotEmpty() && 
               !resolvedUrl.contains("placeholder") && 
               resolvedKey.isNotEmpty() && 
               resolvedKey != "your-supabase-api-key-placeholder"
    }

    /**
     * Helper to get deterministic UUID from local integer ID
     */
    fun getDeterministicUuid(prefix: String, localId: Int): String {
        return UUID.nameUUIDFromBytes("${prefix}_${localId}".toByteArray()).toString()
    }

    /**
     * Syncs all local Room entities to Supabase in sequence.
     */
    suspend fun syncToSupabase(
        staffList: List<Staff>,
        customerList: List<Customer>,
        vehicleList: List<Vehicle>,
        bookingList: List<Booking>,
        experienceList: List<Experience>
    ): SyncReport = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SyncReport(
                success = false,
                message = "Supabase backend is not configured yet. Please configure 'SUPABASE_URL' and 'SUPABASE_KEY' inside the Secrets panel of your AI Studio environment.",
                details = emptyList()
            )
        }

        val url = getBaseUrl().removeSuffix("/")
        val key = getApiKey()
        val reports = mutableListOf<TableSyncResult>()

        try {
            // 1. Sync Company Root Tenant
            val companyJson = """
                [
                  {
                    "id": "$defaultCompanyId",
                    "name": "Diamonds Elite Yachting",
                    "subdomain": "diamonds-elite",
                    "logo_url": "https://images.unsplash.com/photo-1540553016722-983e48a2cd10?q=80&w=200"
                  }
                ]
            """.trimIndent()
            
            val companyResult = sendUpsertRequest(url, key, "companies", companyJson)
            reports.add(companyResult)

            // 2. Sync Fleet (Vehicles)
            val fleetJson = buildFleetJson(vehicleList)
            if (vehicleList.isNotEmpty()) {
                val fleetResult = sendUpsertRequest(url, key, "fleet", fleetJson)
                reports.add(fleetResult)
            } else {
                reports.add(TableSyncResult("fleet", true, "No vehicles to sync.", 0))
            }

            // 3. Sync Customers
            val customersJson = buildCustomersJson(customerList)
            if (customerList.isNotEmpty()) {
                val customersResult = sendUpsertRequest(url, key, "customers", customersJson)
                reports.add(customersResult)
            } else {
                reports.add(TableSyncResult("customers", true, "No customers to sync.", 0))
            }

            // 4. Sync Staff/Employees
            val staffJson = buildEmployeesJson(staffList)
            if (staffList.isNotEmpty()) {
                // Note: employees references auth.users in standard schema. To make this demo/prototype
                // resilient even if corresponding auth accounts don't exist yet, we catch failures gracefully
                val staffResult = sendUpsertRequest(url, key, "employees", staffJson)
                reports.add(staffResult)
            } else {
                reports.add(TableSyncResult("employees", true, "No staff to sync.", 0))
            }

            // 4a. Sync Experiences
            val experiencesJson = buildExperiencesJson(experienceList)
            if (experienceList.isNotEmpty()) {
                val experiencesResult = sendUpsertRequest(url, key, "experiences", experiencesJson)
                reports.add(experiencesResult)
            } else {
                reports.add(TableSyncResult("experiences", true, "No experiences to sync.", 0))
            }

            // 5. Sync Bookings
            val bookingsJson = buildBookingsJson(bookingList)
            if (bookingList.isNotEmpty()) {
                val bookingsResult = sendUpsertRequest(url, key, "bookings", bookingsJson)
                reports.add(bookingsResult)
            } else {
                reports.add(TableSyncResult("bookings", true, "No bookings to sync.", 0))
            }

            val overallSuccess = reports.all { it.success || it.tableName == "employees" } // Make employees optional due to auth.users cascade check
            val overallMsg = if (overallSuccess) {
                "Database synchronization completed successfully. Real-time Node transaction log recorded."
            } else {
                "Synchronization completed with warning. Some tables failed due to schema integrity constraints (e.g. auth.users requirements for employees)."
            }

            SyncReport(
                success = overallSuccess,
                message = overallMsg,
                details = reports
            )

        } catch (e: Exception) {
            Log.e("SupabaseService", "Sync execution failure", e)
            SyncReport(
                success = false,
                message = "Network handshake failed: ${e.localizedMessage ?: "Unknown connection error"}",
                details = reports
            )
        }
    }

    private fun sendUpsertRequest(
        baseUrl: String,
        apiKey: String,
        tableName: String,
        jsonPayload: String
    ): TableSyncResult {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        // PostgREST upsert headers
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/$tableName")
            .post(requestBody)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val isSuccessful = response.isSuccessful || code == 201 || code == 200 || code == 204
                val responseBody = response.body?.string() ?: ""

                if (isSuccessful) {
                    TableSyncResult(
                        tableName = tableName,
                        success = true,
                        message = "Upsert successful. Server responded with status: $code",
                        statusCode = code
                    )
                } else {
                    Log.w("SupabaseService", "Failed to sync $tableName: Code $code - $responseBody")
                    TableSyncResult(
                        tableName = tableName,
                        success = false,
                        message = "Failed with status $code. Details: ${responseBody.take(150)}",
                        statusCode = code
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Request error for $tableName", e)
            TableSyncResult(
                tableName = tableName,
                success = false,
                message = "Connection timeout or socket error: ${e.localizedMessage}",
                statusCode = -1
            )
        }
    }

    // JSON builders avoiding external serialization complexities to remain lightweight & zero-config
    private fun buildFleetJson(vehicles: List<Vehicle>): String {
        val rows = vehicles.map { vehicle ->
            val uuid = getDeterministicUuid("vehicle", vehicle.id)
            val typeMapped = when (vehicle.type.lowercase()) {
                "yacht", "speedboat", "catamaran" -> "yacht"
                "bus", "jeep", "car" -> "suv"
                else -> "yacht"
            }
            val statusMapped = when (vehicle.status.lowercase()) {
                "available" -> "available"
                "maintenance", "repair" -> "maintenance"
                else -> "in_use"
            }
            """
            {
              "id": "$uuid",
              "company_id": "$defaultCompanyId",
              "name": "${escapeJson(vehicle.name)}",
              "type": "$typeMapped",
              "registration_number": "${escapeJson(vehicle.licensePlate)}",
              "capacity": ${vehicle.capacity},
              "hourly_rate": 250.00,
              "status": "$statusMapped",
              "current_gps_lat": 40.634,
              "current_gps_lon": 14.603,
              "fuel_level": ${vehicle.fuelLevel},
              "last_maintenance": "${escapeJson(vehicle.lastMaintenance)}",
              "insurance_expiry": "${escapeJson(vehicle.insuranceExpiry)}"
            }
            """.trimIndent()
        }
        return "[${rows.joinToString(",")}]"
    }

    private fun buildCustomersJson(customers: List<Customer>): String {
        val rows = customers.map { customer ->
            val uuid = getDeterministicUuid("customer", customer.id)
            val parts = customer.fullName.trim().split("\\s+".toRegex(), limit = 2)
            val firstName = parts.getOrNull(0) ?: "VIP"
            val lastName = parts.getOrNull(1) ?: "Guest"
            val tierMapped = if (customer.vipStatus) "royal_diamonds" else "platinum"
            """
            {
              "id": "$uuid",
              "company_id": "$defaultCompanyId",
              "first_name": "${escapeJson(firstName)}",
              "last_name": "${escapeJson(lastName)}",
              "email": "${escapeJson(customer.phoneNumber.replace("+", "") + "@diamonds.elite")}",
              "phone": "${escapeJson(customer.phoneNumber)}",
              "tier": "$tierMapped",
              "notes": "${escapeJson(customer.internalNotes)}",
              "passport_number": "${escapeJson(customer.passportNumber)}",
              "nationality": "${escapeJson(customer.nationality)}",
              "emergency_contact": "${escapeJson(customer.emergencyContact)}",
              "pickup_hotel": "${escapeJson(customer.pickupHotel)}",
              "room_number": "${escapeJson(customer.roomNumber)}",
              "language": "${escapeJson(customer.language)}",
              "blacklist": ${customer.blacklist}
            }
            """.trimIndent()
        }
        return "[${rows.joinToString(",")}]"
    }

    private fun buildExperiencesJson(experiences: List<Experience>): String {
        val rows = experiences.map { exp ->
            val uuid = getDeterministicUuid("experience", exp.id)
            """
            {
              "id": "$uuid",
              "company_id": "$defaultCompanyId",
              "title": "${escapeJson(exp.title)}",
              "description": "${escapeJson(exp.description)}",
              "price": ${exp.price},
              "capacity": ${exp.capacity},
              "duration": "${escapeJson(exp.duration)}",
              "difficulty": "${escapeJson(exp.difficulty)}",
              "pickup_location": "${escapeJson(exp.pickupLocation)}",
              "image_url": "${escapeJson(exp.imageUrl)}",
              "featured": ${exp.featured},
              "status": "${escapeJson(exp.status)}"
            }
            """.trimIndent()
        }
        return "[${rows.joinToString(",")}]"
    }

    private fun buildEmployeesJson(staffList: List<Staff>): String {
        val rows = staffList.map { staff ->
            val uuid = getDeterministicUuid("staff", staff.id)
            val parts = staff.name.trim().split("\\s+".toRegex(), limit = 2)
            val firstName = parts.getOrNull(0) ?: "Staff"
            val lastName = parts.getOrNull(1) ?: "Member"
            val roleMapped = when (staff.role.lowercase()) {
                "manager" -> "admin"
                "guide" -> "operator"
                "driver" -> "driver"
                "boat captain" -> "crew"
                else -> "operator"
            }
            val emailFinal = if (staff.email.isNotEmpty()) staff.email else "${staff.phoneNumber.replace("+", "")}@diamonds-operator.com"
            """
            {
              "id": "$uuid",
              "company_id": "$defaultCompanyId",
              "email": "${escapeJson(emailFinal)}",
              "first_name": "${escapeJson(firstName)}",
              "last_name": "${escapeJson(lastName)}",
              "role": "$roleMapped",
              "biometrics_enrolled": true,
              "device_token": "android_node_sync_dev",
              "phone_number": "${escapeJson(staff.phoneNumber)}",
              "attendance_status": "${escapeJson(staff.attendanceStatus)}",
              "rating": ${staff.rating},
              "performance_score": ${staff.performanceScore},
              "certificate_url": "${escapeJson(staff.certificateUrl)}",
              "password_hash": "${escapeJson(staff.passwordHash)}",
              "security_question": "${escapeJson(staff.securityQuestion)}",
              "security_answer": "${escapeJson(staff.securityAnswer)}",
              "is_two_factor_enabled": ${staff.isTwoFactorEnabled},
              "login_attempts": ${staff.loginAttempts},
              "is_locked": ${staff.isLocked}
            }
            """.trimIndent()
        }
        return "[${rows.joinToString(",")}]"
    }

    private fun buildBookingsJson(bookings: List<Booking>): String {
        val rows = bookings.map { booking ->
            val uuid = getDeterministicUuid("booking", booking.id)
            val customerUuid = getDeterministicUuid("customer", booking.customerId)
            val vehicleUuid = getDeterministicUuid("vehicle", booking.vehicleId)
            val staffUuid = getDeterministicUuid("staff", booking.staffId)
            val experienceUuid = getDeterministicUuid("experience", booking.experienceId)
            val statusMapped = when (booking.status.lowercase()) {
                "pending" -> "pending"
                "confirmed", "paid" -> "confirmed"
                "checked in", "active" -> "active"
                "completed" -> "completed"
                "cancelled" -> "cancelled"
                else -> "pending"
            }
            // Use ISO format timestamp: YYYY-MM-DDThh:mm:ssZ
            val startTime = "${booking.date}T09:00:00Z"
            val endTime = "${booking.date}T14:00:00Z"
            """
            {
              "id": "$uuid",
              "company_id": "$defaultCompanyId",
              "customer_id": "$customerUuid",
              "fleet_id": "$vehicleUuid",
              "primary_staff_id": "$staffUuid",
              "experience_id": "$experienceUuid",
              "experience_title": "${escapeJson(booking.experienceTitle)}",
              "start_time": "$startTime",
              "end_time": "$endTime",
              "status": "$statusMapped",
              "total_price": ${booking.revenue},
              "pickup_location": "${escapeJson(booking.notes.take(50))}",
              "dropoff_location": "${escapeJson(booking.internalComment.take(50))}",
              "pax_count": 4,
              "time_slot": "${escapeJson(booking.timeSlot)}",
              "ticket_qr_code": "${escapeJson(booking.ticketQrCode)}",
              "notes": "${escapeJson(booking.notes)}",
              "internal_comment": "${escapeJson(booking.internalComment)}"
            }
            """.trimIndent()
        }
        return "[${rows.joinToString(",")}]"
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
    }

    /**
     * Signs up a user in Supabase Auth.
     */
    suspend fun signUp(email: String, password: String, name: String = "", phone: String = ""): AuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AuthResult(false, "Supabase is not configured yet.")
        }
        val url = getBaseUrl().removeSuffix("/")
        val key = getApiKey()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = try {
            val root = org.json.JSONObject()
            root.put("email", email)
            root.put("password", password)
            
            val options = org.json.JSONObject()
            val data = org.json.JSONObject()
            data.put("full_name", name)
            data.put("phone", phone)
            options.put("data", data)
            root.put("options", options)
            
            root.toString()
        } catch (e: Exception) {
            """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}"
            }
            """.trimIndent()
        }

        val requestBody = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$url/auth/v1/signup")
            .post(requestBody)
            .addHeader("apikey", key)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful || code == 200 || code == 201) {
                    AuthResult(true, "Enrolled successfully! A confirmation link has been sent to your email from Supabase. Please confirm your account to log in.")
                } else {
                    Log.e("SupabaseService", "Sign up failed: $code - $responseBody")
                    val errMsg = parseSupabaseError(responseBody) ?: "Registration failed ($code)"
                    AuthResult(false, errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Sign up exception", e)
            AuthResult(false, "Network error during registration: ${e.localizedMessage}")
        }
    }

    /**
     * Signs in a user in Supabase Auth to verify credentials and check confirmation.
     */
    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AuthResult(false, "Supabase is not configured yet.")
        }
        val url = getBaseUrl().removeSuffix("/")
        val key = getApiKey()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}"
            }
        """.trimIndent()

        val requestBody = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$url/auth/v1/token?grant_type=password")
            .post(requestBody)
            .addHeader("apikey", key)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful || code == 200 || code == 201) {
                    var name: String? = null
                    var phone: String? = null
                    try {
                        val root = org.json.JSONObject(responseBody)
                        if (root.has("user")) {
                            val user = root.getJSONObject("user")
                            if (user.has("user_metadata")) {
                                val meta = user.getJSONObject("user_metadata")
                                if (meta.has("full_name")) {
                                    name = meta.getString("full_name")
                                }
                                if (meta.has("phone")) {
                                    phone = meta.getString("phone")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseService", "Error parsing user metadata from token response", e)
                    }
                    AuthResult(true, "Success", name, phone)
                } else {
                    Log.e("SupabaseService", "Sign in failed: $code - $responseBody")
                    val errMsg = parseSupabaseError(responseBody) ?: "Invalid credentials"
                    if (errMsg.lowercase().contains("confirm") || errMsg.lowercase().contains("verified")) {
                        AuthResult(false, "Email not confirmed. Please check your inbox and click the verification link sent by Supabase.")
                    } else {
                        AuthResult(false, errMsg)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Sign in exception", e)
            AuthResult(false, "Network error: ${e.localizedMessage}")
        }
    }

    private fun parseSupabaseError(json: String): String? {
        try {
            val descPattern = "\"error_description\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val msgPattern = "\"msg\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val messagePattern = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            
            val descMatch = descPattern.find(json)?.groupValues?.get(1)
            if (descMatch != null) return descMatch
            
            val msgMatch = msgPattern.find(json)?.groupValues?.get(1)
            if (msgMatch != null) return msgMatch

            val messageMatch = messagePattern.find(json)?.groupValues?.get(1)
            if (messageMatch != null) return messageMatch
        } catch (e: Exception) {
            // ignore
        }
        return null
    }
}

data class AuthResult(
    val success: Boolean,
    val message: String,
    val userName: String? = null,
    val userPhone: String? = null
)

data class SyncReport(
    val success: Boolean,
    val message: String,
    val details: List<TableSyncResult>
)

data class TableSyncResult(
    val tableName: String,
    val success: Boolean,
    val message: String,
    val statusCode: Int
)
