package com.example

import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DiamondsViewModel
import com.example.ui.viewmodel.NotificationItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request runtime permission for notifications on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                DiamondsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiamondsApp(viewModel: DiamondsViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Modals & Bottom Sheets States
    var showAddBookingSheet by remember { mutableStateOf(false) }
    var showAddExperienceDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = {
            (fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
             scaleIn(initialScale = 1.02f, animationSpec = tween(500, easing = FastOutSlowInEasing))) togetherWith
            (fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
             scaleOut(targetScale = 0.98f, animationSpec = tween(400, easing = FastOutSlowInEasing)))
        },
        label = "login_transition",
        modifier = Modifier.fillMaxSize()
    ) { loggedIn ->
        if (!loggedIn) {
            LoginScreen(viewModel = viewModel)
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            var isAdminFabVisible by remember { mutableStateOf(true) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        if (delta < -12f) {
                            isAdminFabVisible = false
                        } else if (delta > 12f) {
                            isAdminFabVisible = true
                        }
                        return Offset.Zero
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "DIAMONDS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = GoldPremium
                                )
                                Text(
                                    text = "Live Excursion Hub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        },
                        actions = {
                            // Weather Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceGlass)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.WbSunny,
                                    contentDescription = "Sunny",
                                    tint = GoldPremium,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "24°C • Calm Sea",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Profile Round Circle Button
                            val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
                            val initials = loggedInUser?.name?.split(" ")?.map { it.take(1) }?.joinToString("")?.take(2)?.uppercase() ?: "U"
                            var showProfileMenu by remember { mutableStateOf(false) }

                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldPremium)
                                        .clickable {
                                            showProfileMenu = true
                                        }
                                        .padding(1.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(SlateDarkBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPremium,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showProfileMenu,
                                    onDismissRequest = { showProfileMenu = false },
                                    modifier = Modifier
                                        .background(SlateDarkBg)
                                        .border(1.dp, GoldPremium.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        text = "VIP PORTAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        letterSpacing = 1.sp
                                    )
                                    Divider(color = SurfaceGlassElevated, modifier = Modifier.padding(bottom = 4.dp))

                                    DropdownMenuItem(
                                        text = { Text("Account Profile", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = GoldPremium, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            viewModel.currentScreen.value = "profile"
                                            showProfileMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("App Preferences", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GoldPremium, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            viewModel.currentScreen.value = "settings"
                                            showProfileMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Privacy & Security", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = "Security", tint = GoldPremium, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            // Redirects to settings for convenience
                                            viewModel.currentScreen.value = "settings"
                                            showProfileMenu = false
                                        }
                                    )

                                    Divider(color = SurfaceGlassElevated, modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Secure Disconnect", color = StatusErrorRed, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = StatusErrorRed, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            viewModel.performLogout()
                                            showProfileMenu = false
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SlateDarkBg,
                            titleContentColor = TextPrimary
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = SurfaceGlass,
                        tonalElevation = 8.dp
                    ) {
                        val bottomItems = listOf(
                            Triple("explore", Icons.Default.Explore, "Explore"),
                            Triple("bookings", Icons.Default.AirplaneTicket, "Bookings"),
                            Triple("favorites", Icons.Default.Favorite, "Favorites"),
                            Triple("profile", Icons.Default.Person, "Profile")
                        )

                        bottomItems.forEach { (screenKey, icon, label) ->
                            NavigationBarItem(
                                selected = currentScreen == screenKey,
                                onClick = { viewModel.currentScreen.value = screenKey },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SlateDarkBg,
                                    selectedTextColor = GoldPremium,
                                    indicatorColor = GoldPremium,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                },
                floatingActionButton = {
                    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
                    val isAdmin = loggedInUser?.role?.equals("Manager", ignoreCase = true) == true || 
                                  loggedInUser?.role?.equals("Admin", ignoreCase = true) == true
                    val showAddFab = (isAdmin && when (currentScreen) {
                        "experiences", "customers", "fleet", "staff" -> true
                        else -> false
                    }) || currentScreen == "dashboard"
                    if (showAddFab) {
                        FloatingActionButton(
                            onClick = {
                                when (currentScreen) {
                                    "dashboard" -> showAddBookingSheet = true
                                    "experiences" -> showAddExperienceDialog = true
                                    "customers" -> showAddCustomerDialog = true
                                    "fleet" -> showAddVehicleDialog = true
                                    "staff" -> showAddStaffDialog = true
                                    else -> {}
                                }
                            },
                            containerColor = GoldPremium,
                            contentColor = SlateDarkBg,
                            shape = CircleShape,
                            modifier = Modifier.testTag("global_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(28.dp))
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                containerColor = SlateDarkBg
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            "explore" -> ExploreScreen(viewModel)
                            "favorites" -> FavoritesScreen(viewModel)
                            "dashboard" -> DashboardScreen(viewModel)
                            "bookings" -> BookingsScreen(viewModel)
                            "experiences" -> ExperiencesScreen(viewModel)
                            "customers" -> CustomersScreen(viewModel)
                            "calendar" -> CalendarScreen(viewModel)
                            "fleet" -> FleetScreen(viewModel)
                            "staff" -> StaffScreen(viewModel)
                            "qr_scan" -> QRScanScreen()
                            "reports" -> ReportsScreen(viewModel)
                            "settings" -> SettingsScreen(viewModel)
                            "profile" -> ProfileScreen(viewModel)
                        }
                    }
                }
            }

            DiamondsFloatingAdminMenu(
                viewModel = viewModel,
                isVisible = isAdminFabVisible
            )
        }
    }

    // Modal Sheet and Dialog Implementations
    if (showAddBookingSheet) {
        AddBookingSheet(
            viewModel = viewModel,
            onDismiss = { showAddBookingSheet = false }
        )
    }

    if (showAddExperienceDialog) {
        AddExperienceDialog(
            viewModel = viewModel,
            onDismiss = { showAddExperienceDialog = false }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            viewModel = viewModel,
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    if (showAddVehicleDialog) {
        AddVehicleDialog(
            viewModel = viewModel,
            onDismiss = { showAddVehicleDialog = false }
        )
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            viewModel = viewModel,
            onDismiss = { showAddStaffDialog = false }
        )
    }
}
}
}

// ==========================================
// 1. AUTHENTICATION & LOGIN SCREEN
// ==========================================
// ==========================================
// 1. AUTHENTICATION & LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current
    var screenMode by remember { mutableStateOf("login") } // login, register, forgot, mfa
    val activeTenant by viewModel.loginCompanyCode.collectAsStateWithLifecycle()
    var showTenantSelector by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Login fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }

    // Register fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("Manager") }
    var regPhone by remember { mutableStateOf("") }
    val securityQuestionsList = listOf(
        "What was the name of your first pet?",
        "What is your favorite harbor?",
        "What is your dream yacht?",
        "What city was the company founded in?",
        "What is the brand of your first watch?"
    )
    var regQuestionIndex by remember { mutableStateOf(0) }
    var regAnswer by remember { mutableStateOf("") }
    var regError by remember { mutableStateOf("") }

    // Reset Password fields
    var resetEmail by remember { mutableStateOf("") }
    var resetQuestion by remember { mutableStateOf("") }
    var resetAnswer by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetConfirmPassword by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf("") }
    var hasVerifiedAnswer by remember { mutableStateOf(false) }

    // 2FA state
    var mfaCode by remember { mutableStateOf("") }
    var mfaError by remember { mutableStateOf("") }
    var pendingUser2Fa by remember { mutableStateOf<Staff?>(null) }
    var showBiometricDemo by remember { mutableStateOf(false) }

    // Password strength check
    val isMinLength = regPassword.length >= 6
    val hasDigit = regPassword.any { it.isDigit() }
    val hasLetter = regPassword.any { it.isLetter() }
    val passwordStrength = if (regPassword.isEmpty()) 0f 
                          else (if (isMinLength) 0.34f else 0f) + (if (hasDigit) 0.33f else 0f) + (if (hasLetter) 0.33f else 0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
    ) {
        // Aesthetic background grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val gridWidth = 60.dp.toPx()
                    val gridHeight = 60.dp.toPx()
                    for (x in 0..size.width.toInt() step gridWidth.toInt()) {
                        drawLine(
                            color = GoldPremium.copy(alpha = 0.03f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..size.height.toInt() step gridHeight.toInt()) {
                        drawLine(
                            color = GoldPremium.copy(alpha = 0.03f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Animated luxury logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(GoldPremium.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                        val path = Path().apply {
                            val cx = size.width / 2
                            val cy = size.height / 2
                            val w = size.width * 0.7f
                            val h = size.height * 0.7f
                            moveTo(cx, cy - h / 2)
                            lineTo(cx + w / 2, cy)
                            lineTo(cx, cy + h / 2)
                            lineTo(cx - w / 2, cy)
                            close()
                        }
                        drawPath(path, color = GoldPremium, style = Stroke(width = 4f))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "Diamonds Logo",
                    tint = GoldPremium,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "D I A M O N D S",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GoldPremium,
                letterSpacing = 6.sp
            )

            Text(
                "SECURE PORTAL TERMINAL",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))



            // Tab switching indicators
            if (screenMode != "mfa" && screenMode != "forgot") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceGlass)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple("login", "SIGN IN", Icons.Default.Lock),
                        Triple("register", "REGISTER", Icons.Default.Person)
                    ).forEach { (mode, label, icon) ->
                        val isSelected = screenMode == mode
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldPremium else Color.Transparent)
                                .clickable { 
                                    screenMode = mode 
                                    loginError = ""
                                    regError = ""
                                    resetError = ""
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) SlateDarkBg else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SlateDarkBg else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SCREEN MODES WITH PREMIUM TRANSITIONS
            AnimatedContent(
                targetState = screenMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + 
                     scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing))) togetherWith
                    (fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + 
                     scaleOut(targetScale = 0.96f, animationSpec = tween(200, easing = FastOutSlowInEasing)))
                },
                label = "auth_screen_modes_transition",
                modifier = Modifier.fillMaxWidth()
            ) { targetMode ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (targetMode) {
                        "login" -> {
                            Text(
                                "OPERATOR SECURE SIGN-IN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (loginError.isNotEmpty()) {
                                Text(
                                    text = loginError,
                                    color = Color(0xFFE57373),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("operator@diamonds.com") },
                                placeholder = { Text("operator@diamonds.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = GoldPremium) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_email"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Secure Password / PIN") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = GoldPremium) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("login_password"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Forgot password? Use recovery questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clickable {
                                        screenMode = "forgot"
                                        hasVerifiedAnswer = false
                                        resetEmail = email
                                        resetError = ""
                                    }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (email.isEmpty() || password.isEmpty()) {
                                        loginError = "Please enter both email and password."
                                    } else {
                                        viewModel.performSecureLogin(
                                            email = email,
                                            passwordHash = password,
                                            onTwoFactorRequired = { staff ->
                                                pendingUser2Fa = staff
                                                screenMode = "mfa"
                                                mfaCode = ""
                                                mfaError = ""
                                            },
                                            onSuccess = {
                                                Toast.makeText(context, "Logged in as ${it.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                loginError = err
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_login_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("AUTHENTICATE SESSION", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Biometric alternative login
                            OutlinedButton(
                                onClick = { showBiometricDemo = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPremium),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint")
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("FACIAL / BIOMETRIC ID", fontWeight = FontWeight.Bold)
                            }
                        }

                        "register" -> {
                            Text(
                                "ENROLL NEW DIAMONDS OPERATOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (regError.isNotEmpty()) {
                                Text(
                                    text = regError,
                                    color = Color(0xFFE57373),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = GoldPremium) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_name"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("Corporate Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = GoldPremium) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_email"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("Secure Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = GoldPremium) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_password"),
                                singleLine = true
                            )

                            // Real-time password strength advisor
                            if (regPassword.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Password Strength:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = when {
                                                passwordStrength < 0.4f -> "Weak"
                                                passwordStrength < 0.7f -> "Medium"
                                                else -> "Strong (Excellent)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                passwordStrength < 0.4f -> Color(0xFFEF5350)
                                                passwordStrength < 0.7f -> Color(0xFFFFB74D)
                                                else -> Color(0xFF66BB6A)
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { passwordStrength },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.2.dp)),
                                        color = when {
                                            passwordStrength < 0.4f -> Color(0xFFEF5350)
                                            passwordStrength < 0.7f -> Color(0xFFFFB74D)
                                            else -> Color(0xFF66BB6A)
                                        },
                                        trackColor = SurfaceGlassElevated,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val checks = listOf(
                                            Pair(isMinLength, "6+ chars"),
                                            Pair(hasDigit, "1+ digit"),
                                            Pair(hasLetter, "1+ letter")
                                        )
                                        checks.forEach { (isOk, title) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = if (isOk) Color(0xFF66BB6A) else Color(0xFFEF5350),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(title, fontSize = 10.sp, color = if (isOk) TextPrimary else TextMuted)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = GoldPremium) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Role Picker
                            Text("Assigned Domain Role:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Manager", "Boat Captain", "Guide", "Driver").forEach { role ->
                                    val isSel = regRole == role
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isSel) GoldPremium else SurfaceGlassElevated, RoundedCornerShape(8.dp))
                                            .background(if (isSel) GoldPremium.copy(alpha = 0.15f) else SurfaceGlass)
                                            .clickable { regRole = role }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = role.replace("Boat ", ""),
                                            color = if (isSel) GoldPremium else TextSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text("Contact Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = GoldPremium) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_phone"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Security Question configuration
                            Text("Password Recovery Setup:", style = MaterialTheme.typography.bodySmall, color = GoldPremium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Used for emergency reset in case of locking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Mini Dropdown simulation for recovery question
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceGlass)
                                    .border(1.dp, SurfaceGlassElevated, RoundedCornerShape(8.dp))
                                    .clickable {
                                        regQuestionIndex = (regQuestionIndex + 1) % securityQuestionsList.size
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = securityQuestionsList[regQuestionIndex],
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select question", tint = GoldPremium)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = regAnswer,
                                onValueChange = { regAnswer = it },
                                label = { Text("Recovery Answer") },
                                leadingIcon = { Icon(Icons.Default.Help, contentDescription = "Answer", tint = GoldPremium) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("reg_answer"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val emailTrim = regEmail.trim()
                                    if (regName.isEmpty() || emailTrim.isEmpty() || regPassword.isEmpty() || regAnswer.isEmpty()) {
                                        regError = "Please fill in all mandatory enrollment fields."
                                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                                        regError = "Please enter a valid company email address."
                                    } else if (regPassword != regConfirmPassword) {
                                        regError = "Passwords do not match."
                                    } else if (passwordStrength < 0.6f) {
                                        regError = "Password too weak. Please include letters and numbers."
                                    } else {
                                        viewModel.registerSecureUser(
                                            name = regName,
                                            email = emailTrim,
                                            passwordHash = regPassword,
                                            role = regRole,
                                            phone = regPhone,
                                            securityQuestion = securityQuestionsList[regQuestionIndex],
                                            securityAnswer = regAnswer,
                                            onSuccess = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                screenMode = "login"
                                                email = emailTrim
                                                password = ""
                                            },
                                            onFailure = { err ->
                                                regError = err
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_register_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("ENROLL OPERATOR", fontWeight = FontWeight.Bold)
                            }
                        }

                        "forgot" -> {
                            Text(
                                "SECURE PASSWORD RECOVERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (resetError.isNotEmpty()) {
                                Text(
                                    text = resetError,
                                    color = Color(0xFFE57373),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (!hasVerifiedAnswer) {
                                // Stage 1: Verify identity
                                Text(
                                    "Provide your registered operator email to verify credentials and unlock security question.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                OutlinedTextField(
                                    value = resetEmail,
                                    onValueChange = { resetEmail = it },
                                    label = { Text("Operator Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = GoldPremium) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPremium,
                                        unfocusedBorderColor = SurfaceGlassElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = SurfaceGlass,
                                        unfocusedContainerColor = SurfaceGlass
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reset_email"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val trimmedEmail = resetEmail.trim()
                                        val match = viewModel.staff.value.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
                                        if (match != null) {
                                            resetQuestion = match.securityQuestion
                                            hasVerifiedAnswer = true
                                            resetError = ""
                                        } else {
                                            resetError = "No operator account found with that email address."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("LOAD RECOVERY PROTOCOL", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Stage 2: Recover
                                Text(
                                    "Recovery protocol loaded for $resetEmail.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF81C784),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("SECURITY QUESTION", style = MaterialTheme.typography.labelSmall, color = GoldPremium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(resetQuestion, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                }

                                OutlinedTextField(
                                    value = resetAnswer,
                                    onValueChange = { resetAnswer = it },
                                    label = { Text("Security Answer") },
                                    leadingIcon = { Icon(Icons.Default.Help, contentDescription = "Answer", tint = GoldPremium) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPremium,
                                        unfocusedBorderColor = SurfaceGlassElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = SurfaceGlass,
                                        unfocusedContainerColor = SurfaceGlass
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reset_answer"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = resetNewPassword,
                                    onValueChange = { resetNewPassword = it },
                                    label = { Text("New Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New Password", tint = GoldPremium) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPremium,
                                        unfocusedBorderColor = SurfaceGlassElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = SurfaceGlass,
                                        unfocusedContainerColor = SurfaceGlass
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reset_new_password"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = resetConfirmPassword,
                                    onValueChange = { resetConfirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm", tint = GoldPremium) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPremium,
                                        unfocusedBorderColor = SurfaceGlassElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = SurfaceGlass,
                                        unfocusedContainerColor = SurfaceGlass
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("reset_confirm_password"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        if (resetAnswer.isEmpty() || resetNewPassword.isEmpty()) {
                                            resetError = "Please complete all password recovery fields."
                                        } else if (resetNewPassword != resetConfirmPassword) {
                                            resetError = "Passwords do not match."
                                        } else if (resetNewPassword.length < 4) {
                                            resetError = "Recovery password must be at least 4 characters."
                                        } else {
                                            viewModel.resetSecurePassword(
                                                email = resetEmail,
                                                securityAnswer = resetAnswer,
                                                newPasswordHash = resetNewPassword,
                                                onSuccess = {
                                                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                    screenMode = "login"
                                                    email = resetEmail
                                                    password = ""
                                                },
                                                onFailure = { err ->
                                                    resetError = err
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("UPDATE ACCESS PASSWORD", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(
                                onClick = { screenMode = "login" }
                            ) {
                                Text("← Return to Sign In", color = GoldPremium, fontWeight = FontWeight.Bold)
                            }
                        }

                        "mfa" -> {
                            val user = pendingUser2Fa
                            Text(
                                "MULTI-FACTOR AUTHENTICATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPremium,
                                letterSpacing = 2.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "An encrypted dynamic token has been dispatched to ${user?.name ?: "your registered device"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulation token assist
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("SIMULATED HARDWARE TOKEN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🔑 OTP code: 123456", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GoldPremium, letterSpacing = 2.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (mfaError.isNotEmpty()) {
                                Text(
                                    text = mfaError,
                                    color = Color(0xFFE57373),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            OutlinedTextField(
                                value = mfaCode,
                                onValueChange = { mfaCode = it },
                                label = { Text("6-Digit MFA Verification Code") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = "OTP", tint = GoldPremium) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("mfa_code_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (mfaCode == "123456") {
                                        user?.let {
                                            viewModel.completeLogin(it)
                                            Toast.makeText(context, "Session Authenticated.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        mfaError = "Invalid verification code. Please check simulation helper."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("mfa_verify_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("CONFIRM SECURITY CODE", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(
                                onClick = { screenMode = "login" }
                            ) {
                                Text("Cancel & Return", color = GoldPremium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "Diamonds operator terminal. Fully offline-first encrypted.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showBiometricDemo) {
        var scanningPhase by remember { mutableStateOf(0) } // 0: Start, 1: Scanning, 2: Success
        LaunchedEffect(Unit) {
            scanningPhase = 1
            delay(1500)
            scanningPhase = 2
            delay(1000)
            showBiometricDemo = false
            val match = viewModel.staff.value.firstOrNull { it.role == "Boat Captain" } ?: viewModel.staff.value.firstOrNull()
            if (match != null) {
                viewModel.completeLogin(match)
                Toast.makeText(context, "Biometric matched: ${match.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Biometric authentication failed. No enrolled operator found.", Toast.LENGTH_LONG).show()
            }
        }

        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "BIOMETRIC PROTOCOL",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldPremium,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val infiniteTransition = rememberInfiniteTransition(label = "Biometric pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "PulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .drawBehind {
                                if (scanningPhase == 1) {
                                    drawCircle(
                                        color = GoldPremium.copy(alpha = 0.2f),
                                        radius = size.width / 2 * pulseScale
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (scanningPhase) {
                                2 -> Icons.Default.CheckCircle
                                else -> Icons.Default.Fingerprint
                            },
                            contentDescription = "Scanning",
                            tint = if (scanningPhase == 2) Color(0xFF66BB6A) else GoldPremium,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (scanningPhase) {
                            1 -> "VERIFYING BIOMETRICS..."
                            2 -> "IDENTITY CONFIRMED"
                            else -> "READY"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (scanningPhase == 2) Color(0xFF66BB6A) else TextPrimary
                    )
                    Text(
                        text = "Encrypted local key-exchange in progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (showTenantSelector) {
        var subdomainInput by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }
        var verificationError by remember { mutableStateOf("") }
        var verificationSuccess by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isVerifying) showTenantSelector = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "DNS Handshake",
                        tint = GoldPremium,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TENANT GATEWAY LOOKUP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPremium,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verify your corporate tenant subdomain to securely isolate data streams and bind database headers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (verificationError.isNotEmpty()) {
                        Text(
                            text = verificationError,
                            color = Color(0xFFE57373),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (verificationSuccess) {
                        Text(
                            text = "✓ SECURE SCHEMA LOCKED",
                            color = Color(0xFF81C784),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = subdomainInput,
                        onValueChange = { 
                            subdomainInput = it
                            verificationError = ""
                        },
                        label = { Text("Corporate Subdomain") },
                        placeholder = { Text("e.g. exclusive-yachts") },
                        enabled = !isVerifying && !verificationSuccess,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPremium,
                            unfocusedBorderColor = SurfaceGlassElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceGlass,
                            unfocusedContainerColor = SurfaceGlass
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available: exclusive-yachts, amalfi-charters, elite-transfers",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isVerifying) {
                        CircularProgressIndicator(color = GoldPremium, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Querying GET /companies?subdomain=eq...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showTenantSelector = false },
                                enabled = !isVerifying
                            ) {
                                Text("CANCEL", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val domain = subdomainInput.trim().lowercase()
                                    if (domain.isEmpty()) {
                                        verificationError = "Subdomain cannot be empty."
                                        return@Button
                                    }
                                    isVerifying = true
                                    coroutineScope.launch {
                                        delay(1500) // Simulated latency
                                        isVerifying = false
                                        when (domain) {
                                            "exclusive-yachts", "exclusive" -> {
                                                viewModel.loginCompanyCode.value = "DIAMOND_EXCLUSIVE_YACHTS"
                                                viewModel.addSecurityLog("TENANT_HANDSHAKE", "Verified subdomain 'exclusive-yachts'. Schema locked.")
                                                verificationSuccess = true
                                                delay(1000)
                                                showTenantSelector = false
                                            }
                                            "amalfi-charters", "amalfi" -> {
                                                viewModel.loginCompanyCode.value = "AMALFI_VIP_CHARTERS"
                                                viewModel.addSecurityLog("TENANT_HANDSHAKE", "Verified subdomain 'amalfi-charters'. Schema locked.")
                                                verificationSuccess = true
                                                delay(1000)
                                                showTenantSelector = false
                                            }
                                            "elite-transfers", "elite" -> {
                                                viewModel.loginCompanyCode.value = "ELITE_LUXURY_TRANSFERS"
                                                viewModel.addSecurityLog("TENANT_HANDSHAKE", "Verified subdomain 'elite-transfers'. Schema locked.")
                                                verificationSuccess = true
                                                delay(1000)
                                                showTenantSelector = false
                                            }
                                            else -> {
                                                verificationError = "No Diamonds network instance matches subdomain '$domain'."
                                                viewModel.addSecurityLog("TENANT_HANDSHAKE_FAILURE", "Failed domain lookup for '$domain'.")
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("VERIFY DOMAIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyLoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var companyCode by remember { mutableStateOf("DIAMOND_EXCLUSIVE_YACHTS") }
    var pinCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Manager") } // Manager, Captain, Guide, Driver
    var showBiometricDemo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
    ) {
        // Aesthetic background grid/shimmer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val gridWidth = 60.dp.toPx()
                    val gridHeight = 60.dp.toPx()
                    for (x in 0..size.width.toInt() step gridWidth.toInt()) {
                        drawLine(
                            color = GoldPremium.copy(alpha = 0.03f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..size.height.toInt() step gridHeight.toInt()) {
                        drawLine(
                            color = GoldPremium.copy(alpha = 0.03f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Animated luxury logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(GoldPremium.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                        val sizeMultiplier = 0.7f
                        val w = size.width * sizeMultiplier
                        val h = size.height * sizeMultiplier
                        val cx = size.width / 2
                        val cy = size.height / 2

                        val path = Path().apply {
                            moveTo(cx, cy - h / 2) // Top
                            lineTo(cx + w / 2, cy) // Right
                            lineTo(cx, cy + h / 2) // Bottom
                            lineTo(cx - w / 2, cy) // Left
                            close()
                        }
                        drawPath(path, color = GoldPremium, style = Stroke(width = 4f))

                        // Inner lines
                        val innerPath = Path().apply {
                            moveTo(cx, cy - h / 2)
                            lineTo(cx, cy + h / 2)
                            moveTo(cx - w / 2, cy)
                            lineTo(cx + w / 2, cy)
                        }
                        drawPath(innerPath, color = GoldPremium.copy(alpha = 0.4f), style = Stroke(width = 2f))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "Diamonds Logo",
                    tint = GoldPremium,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "D I A M O N D S",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GoldPremium,
                letterSpacing = 6.sp
            )

            Text(
                "PRIVATE EXCURSION ENTERPRISE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Role selection pill
            Text(
                "ACCOUNT ACCESS PROFILE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceGlass)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val roles = listOf("Manager", "Captain", "Guide", "Driver")
                roles.forEach { role ->
                    val isSelected = selectedRole == role
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldPremium else Color.Transparent)
                            .clickable { selectedRole = role }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            role,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SlateDarkBg else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inputs
            OutlinedTextField(
                value = companyCode,
                onValueChange = { companyCode = it },
                label = { Text("Company Domain ID") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = "Company", tint = GoldPremium) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPremium,
                    unfocusedBorderColor = SurfaceGlassElevated,
                    focusedLabelColor = GoldPremium,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pinCode,
                onValueChange = { pinCode = it },
                label = { Text("Secure Access PIN") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "PIN", tint = GoldPremium) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPremium,
                    unfocusedBorderColor = SurfaceGlassElevated,
                    focusedLabelColor = GoldPremium,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (companyCode.isNotEmpty() && pinCode.isNotEmpty()) {
                        onLoginSuccess(companyCode, selectedRole)
                    } else {
                        onLoginSuccess("DIAMOND_EXCLUSIVE_YACHTS", "Manager")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("VERIFY & ENTER HUB", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric Simulation Trigger
            OutlinedButton(
                onClick = { showBiometricDemo = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(1.dp, GoldPremium),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPremium),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint")
                Spacer(modifier = Modifier.width(12.dp))
                Text("FACIAL / BIOMETRIC ID", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "Diamonds operator terminal. Fully offline-first encrypted.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showBiometricDemo) {
        var scanningPhase by remember { mutableStateOf(0) } // 0: Start, 1: Scanning, 2: Success
        LaunchedEffect(Unit) {
            scanningPhase = 1
            delay(1500)
            scanningPhase = 2
            delay(1000)
            showBiometricDemo = false
            onLoginSuccess("DIAMOND_EXCLUSIVE_YACHTS", "Manager")
        }

        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "BIOMETRIC PROTOCOL",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldPremium,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val infiniteTransition = rememberInfiniteTransition(label = "Biometric pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "PulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .drawBehind {
                                if (scanningPhase == 1) {
                                    drawCircle(
                                        color = GoldPremium.copy(alpha = 0.2f),
                                        radius = size.width / 2 * pulseScale
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (scanningPhase) {
                                1 -> Icons.Default.Fingerprint
                                2 -> Icons.Default.CheckCircle
                                else -> Icons.Default.Face
                            },
                            contentDescription = "Scan Icon",
                            tint = if (scanningPhase == 2) StatusLiveGreen else GoldPremium,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = when (scanningPhase) {
                            1 -> "Analyzing Hardware Token..."
                            2 -> "Biometric ID Authenticated"
                            else -> "Initializing Biometric Scanner"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fulfilling hardware cryptographic handshake.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==========================================
// SHIMMER LOADING AND SWIPE HELPERS
// ==========================================
@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E2E2),
            Color(0xFFF3F3F3),
            Color(0xFFE2E2E2)
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun KpiCardShimmer(brush: Brush) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(80.dp, 12.dp).background(brush, RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.size(20.dp).background(brush, CircleShape))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.size(120.dp, 24.dp).background(brush, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.size(60.dp, 10.dp).background(brush, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun ExcursionCardShimmer(brush: Brush) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SurfaceGlassElevated),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(brush)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.size(200.dp, 18.dp).background(brush, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(280.dp, 12.dp).background(brush, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(70.dp, 20.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(70.dp, 20.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(70.dp, 20.dp).background(brush, RoundedCornerShape(8.dp)))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(brush, RoundedCornerShape(12.dp)))
            }
        }
    }
}

@Composable
fun BookingCardShimmer(brush: Brush) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(150.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(220.dp, 12.dp).background(brush, RoundedCornerShape(4.dp)))
            }
            Box(modifier = Modifier.size(80.dp, 28.dp).background(brush, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
fun BulletinShimmer(brush: Brush) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(brush, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(120.dp, 12.dp).background(brush, RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(40.dp, 10.dp).background(brush, RoundedCornerShape(2.dp)))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.size(240.dp, 10.dp).background(brush, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableBookingItem(
    booking: Booking,
    viewModel: DiamondsViewModel,
    context: android.content.Context,
    onItemClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (booking.status != "Confirmed") {
                        viewModel.updateBookingStatus(booking.id, "Confirmed")
                        Toast.makeText(context, "Confirmed booking for ${booking.customerName}", Toast.LENGTH_SHORT).show()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    viewModel.deleteBooking(booking)
                    Toast.makeText(context, "Deleted/Archived booking for ${booking.customerName}", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981).copy(alpha = 0.2f)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444).copy(alpha = 0.2f)
                else -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.CheckCircle
            }
            val iconColor = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                else -> Color.Gray
            }
            val label = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> "Confirm"
                SwipeToDismissBoxValue.EndToStart -> "Delete / Archive"
                else -> ""
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .border(
                        BorderStroke(1.dp, iconColor.copy(alpha = 0.5f)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(icon, contentDescription = null, tint = iconColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = iconColor)
                    }
                }
            }
        },
        content = {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceGlassElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick() }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            booking.customerName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            booking.experienceTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldPremium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("📅 ${booking.date}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("🕒 ${booking.timeSlot}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (booking.status) {
                                        "Confirmed" -> StatusPaidBlue.copy(alpha = 0.2f)
                                        "Checked In" -> StatusLiveGreen.copy(alpha = 0.2f)
                                        "Paid" -> StatusLiveGreen.copy(alpha = 0.2f)
                                        "Pending" -> StatusAlertAmber.copy(alpha = 0.2f)
                                        else -> StatusErrorRed.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                booking.status,
                                color = when (booking.status) {
                                    "Confirmed" -> StatusPaidBlue
                                    "Checked In" -> StatusLiveGreen
                                    "Paid" -> StatusLiveGreen
                                    "Pending" -> StatusAlertAmber
                                    else -> StatusErrorRed
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "€${String.format("%,.0f", booking.revenue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

// ==========================================
// 2. DASHBOARD SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val experiences by viewModel.experiences.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val totalRevenue = bookings.filter { it.status != "Cancelled" }.sumOf { it.revenue }
    val pendingBookings = bookings.filter { it.status == "Pending" }.size
    val activeFleet = vehicles.filter { it.status == "Available" || it.status == "Active" }.size
    val activeStaff = staff.filter { it.attendanceStatus == "Present" }.size

    // Search and Category states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var recentExcursionsSearchQuery by remember { mutableStateOf("") }
    var selectedBookingForDetails by remember { mutableStateOf<Booking?>(null) }

    // Quick Booking states
    var showQuickBookingDialog by remember { mutableStateOf(false) }
    var selectedExperienceForBooking by remember { mutableStateOf<Experience?>(null) }
    var guestName by remember { mutableStateOf("") }
    var selectedCustomerForBooking by remember { mutableStateOf<Customer?>(null) }
    var selectedDate by remember { mutableStateOf("2026-07-11") }
    var selectedTimeSlot by remember { mutableStateOf("18:00 - 22:00") }
    var assignedVehicleId by remember { mutableStateOf(1) }
    var assignedStaffId by remember { mutableStateOf(1) }
    var customPrice by remember { mutableStateOf("") }
    var specialRequests by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1200)
        isLoading = false
    }

    val calendarDates = listOf(
        "2026-07-08" to ("Wed" to "08"),
        "2026-07-09" to ("Thu" to "09"),
        "2026-07-10" to ("Fri" to "10"),
        "2026-07-11" to ("Sat" to "11"),
        "2026-07-12" to ("Sun" to "12"),
        "2026-07-13" to ("Mon" to "13"),
        "2026-07-14" to ("Tue" to "14"),
        "2026-07-15" to ("Wed" to "15"),
        "2026-07-16" to ("Thu" to "16"),
        "2026-07-17" to ("Fri" to "17")
    )
    var selectedDashboardDate by remember { mutableStateOf<String?>("2026-07-10") }

    // Dynamic experience categorizer helper
    fun getExperienceCategory(exp: Experience): String {
        val titleLower = exp.title.lowercase()
        val descLower = exp.description.lowercase()
        return when {
            titleLower.contains("speedboat") || titleLower.contains("speed") -> "Speedboats"
            titleLower.contains("yacht") || titleLower.contains("cruise") || titleLower.contains("boat") || titleLower.contains("sails") || descLower.contains("yacht") || descLower.contains("boat") -> "Boats"
            titleLower.contains("island") || titleLower.contains("capri") || titleLower.contains("ischia") || titleLower.contains("procida") || descLower.contains("island") -> "Islands"
            titleLower.contains("limo") || titleLower.contains("chauffeur") || titleLower.contains("transfer") || titleLower.contains("van") || titleLower.contains("car") || titleLower.contains("suv") || descLower.contains("chauffeured") || descLower.contains("transfer") -> "Limosuine"
            titleLower.contains("hotel") || titleLower.contains("resort") || titleLower.contains("estate") || titleLower.contains("vineyard") || titleLower.contains("villa") || titleLower.contains("stay") || descLower.contains("hotel") || descLower.contains("estate") || descLower.contains("vineyard") -> "Hotels"
            else -> "Hotels"
        }
    }

    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = {
            scope.launch {
                isLoading = true
                delay(1200)
                isLoading = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        // Welcome and Hero Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Welcome Back, Captain",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Diamond Operator Command Room",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Small avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GoldPremium)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(SurfaceGlass),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MR", fontWeight = FontWeight.Bold, color = GoldPremium, fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Compact horizontal date-picker/calendar component
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    "CHOOSE OPERATION DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // "All" option chip
                    item {
                        val isAllSelected = selectedDashboardDate == null
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAllSelected) GoldPremium else SurfaceGlass
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isAllSelected) GoldPremium else SurfaceGlassElevated
                            ),
                            modifier = Modifier
                                .testTag("all_dates_chip")
                                .clickable { selectedDashboardDate = null }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "ALL",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) Color.White else TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "DATES",
                                    color = if (isAllSelected) Color.White.copy(alpha = 0.8f) else TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    
                    // Specific calendar dates
                    items(calendarDates) { (dateStr, dateDetails) ->
                        val (dayName, dayNum) = dateDetails
                        val isSelected = selectedDashboardDate == dateStr
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldPremium else SurfaceGlass
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) GoldPremium else SurfaceGlassElevated
                            ),
                            modifier = Modifier
                                .testTag("date_chip_" + dateStr)
                                .clickable { selectedDashboardDate = dateStr }
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(55.dp)
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted,
                                    fontSize = 9.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generated premium visual banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceGlass)
            ) {
                // Background Image Loading with a nice gradient fallback
                AsyncImage(
                    model = "file:///app/src/main/res/drawable/img_luxury_yacht_1783707158887.jpg",
                    contentDescription = "Luxury Yacht",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay gradient for maximum luxury text contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, SlateDarkBg.copy(alpha = 0.85f)),
                                startY = 100f
                            )
                        )
                )

                // Banner Details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPremium)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "VIP FLIGHTS & SAILS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateDarkBg,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sea Swell Calm (0.4m)", color = GoldLight, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "98% Elite Fleet Ready Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            WeeklyRevenueChartCard(bookings = bookings)
        }

        // Analytics / KPI Stats Grid
        item {
            Text(
                "TERMINAL OVERVIEW STATS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                val brush = shimmerBrush()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) { KpiCardShimmer(brush) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) { KpiCardShimmer(brush) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) { KpiCardShimmer(brush) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) { KpiCardShimmer(brush) }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        KpiCard(
                            title = "Live Revenue",
                            value = "€${String.format("%,.0f", totalRevenue)}",
                            subtitle = "Cumulative total",
                            icon = Icons.Default.CurrencyExchange,
                            color = StatusPaidBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        KpiCard(
                            title = "Pending Action",
                            value = "$pendingBookings Bookings",
                            subtitle = "Needs validation",
                            icon = Icons.Default.PendingActions,
                            color = StatusAlertAmber
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        KpiCard(
                            title = "Active Fleet",
                            value = "$activeFleet / ${vehicles.size}",
                            subtitle = "Yachts, Speedboats, Vans",
                            icon = Icons.Default.DirectionsBoat,
                            color = GoldPremium
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        KpiCard(
                            title = "Active Crew",
                            value = "$activeStaff / ${staff.size}",
                            subtitle = "Present at stations",
                            icon = Icons.Default.Badge,
                            color = StatusLiveGreen
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Operational Bulletins / Notifications Center
        if (isLoading || notifications.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LIVE TELEMETRY BULLETINS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    if (!isLoading) {
                        TextButton(onClick = { viewModel.clearAllNotifications() }) {
                            Text("Clear", color = GoldPremium, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = GoldPremium,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "REAL-TIME PUSH TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Background alerts are active. You can force an incoming simulated booking or status change to verify the push notification channel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.triggerSimulatedPushNotification()
                                Toast.makeText(context, "Triggering simulated push alert...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TRIGGER SIMULATED PUSH ALERT", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoading) {
                items(2) {
                    BulletinShimmer(brush = shimmerBrush())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                items(notifications) { item ->
                    NotificationRow(item, onDismiss = { viewModel.dismissNotification(item.id) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ==========================================
        // RECENT EXCURSIONS LIST (WITH SWIPE GESTURES)
        // ==========================================
        item {
            Text(
                "RECENT EXCURSION BOOKINGS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp
            )
            Text(
                "Swipe right to Confirm • Swipe left to Delete",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = recentExcursionsSearchQuery,
                onValueChange = { recentExcursionsSearchQuery = it },
                placeholder = { Text("Search by guest or tour name...", color = TextSecondary.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Recent", tint = GoldPremium) },
                trailingIcon = {
                    if (recentExcursionsSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { recentExcursionsSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPremium,
                    unfocusedBorderColor = SurfaceGlassElevated,
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Filter bookings by selectedDashboardDate (if not null) and recentExcursionsSearchQuery
        val filteredDashboardBookings = bookings.filter { booking ->
            (selectedDashboardDate == null || booking.date == selectedDashboardDate) &&
            (recentExcursionsSearchQuery.isEmpty() || 
             booking.customerName.contains(recentExcursionsSearchQuery, ignoreCase = true) ||
             booking.experienceTitle.contains(recentExcursionsSearchQuery, ignoreCase = true))
        }

        if (isLoading) {
            items(2) {
                BookingCardShimmer(brush = shimmerBrush())
            }
        } else if (filteredDashboardBookings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(SurfaceGlass, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, SurfaceGlassElevated), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (recentExcursionsSearchQuery.isNotEmpty()) "No matching excursions found." else if (selectedDashboardDate != null) "No excursions scheduled for $selectedDashboardDate" else "No excursions recorded.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(filteredDashboardBookings, key = { it.id }) { booking ->
                SwipeableBookingItem(
                    booking = booking,
                    viewModel = viewModel,
                    context = context,
                    onItemClick = { selectedBookingForDetails = booking }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ==========================================
        // EXCURSION & BOAT TRIP BOOKING HUB
        // ==========================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "ELITE BOOKING TERMINAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 2.sp
                )
                Text(
                    "Instantly Book Yachts, Heli flights, and Private Estates",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Elegant Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search yacht tours, heli, wine estates...", color = TextSecondary.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldPremium) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPremium,
                        unfocusedBorderColor = SurfaceGlassElevated,
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful scrolling category chips
                val categories = listOf("All", "Boats", "Speedboats", "Islands", "Limosuine", "Hotels", "Featured")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val icon = when (cat) {
                            "All" -> "🌍"
                            "Boats" -> "⛵"
                            "Speedboats" -> "🚤"
                            "Islands" -> "🏝️"
                            "Limosuine" -> "🚗"
                            "Hotels" -> "🏨"
                            "Featured" -> "✨"
                            else -> "✨"
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldPremium else SurfaceGlass
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) GoldPremium else SurfaceGlassElevated
                            ),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Excursion List Filter Logic
        val filteredCatalogExps = experiences.filter { exp ->
            val matchesSearch = exp.title.contains(searchQuery, ignoreCase = true) ||
                    exp.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Featured" -> exp.featured
                else -> getExperienceCategory(exp) == selectedCategory
            }
            matchesSearch && matchesCategory
        }

        if (isLoading) {
            items(2) {
                ExcursionCardShimmer(brush = shimmerBrush())
            }
        } else if (filteredCatalogExps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(SurfaceGlass, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Cancel, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching luxury assets found", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(filteredCatalogExps) { exp ->
                val categoryName = getExperienceCategory(exp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, SurfaceGlassElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column {
                        // Visual backdrop gradient header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = when (categoryName) {
                                            "Boats" -> listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))
                                            "Speedboats" -> listOf(Color(0xFFCFFAFE), Color(0xFF99F6E4))
                                            "Islands" -> listOf(Color(0xFFDCFCE7), Color(0xFF86EFAC))
                                            "Limosuine" -> listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF))
                                            "Hotels" -> listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                                            else -> listOf(GoldLight, SurfaceGlassElevated)
                                        }
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            // Category Tag & Featured Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldPremium.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        categoryName,
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                if (exp.featured) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFB300))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Diamond, contentDescription = "Featured", tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "FEATURED",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Large icon mapping
                            Icon(
                                imageVector = when (categoryName) {
                                    "Boats" -> Icons.Default.DirectionsBoat
                                    "Speedboats" -> Icons.Default.DirectionsBoat
                                    "Islands" -> Icons.Default.Map
                                    "Limosuine" -> Icons.Default.DirectionsCar
                                    "Hotels" -> Icons.Default.Business
                                    else -> Icons.Default.Business
                                },
                                contentDescription = categoryName,
                                tint = GoldPremium.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center)
                            )

                            // Price Tag on bottom-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldPremium)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "€${String.format("%,.0f", exp.price)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Text content
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceGlassElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = "Duration", tint = TextSecondary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(exp.duration, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceGlassElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, contentDescription = "Capacity", tint = TextSecondary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Max ${exp.capacity} pax", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceGlassElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Diff: ${exp.difficulty}",
                                        color = when (exp.difficulty) {
                                            "Easy" -> StatusLiveGreen
                                            "Medium" -> StatusAlertAmber
                                            else -> StatusErrorRed
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Booking Action Button
                            Button(
                                onClick = {
                                    selectedExperienceForBooking = exp
                                    customPrice = exp.price.toInt().toString()
                                    // Reset fields
                                    guestName = ""
                                    selectedCustomerForBooking = null
                                    specialRequests = ""
                                    showQuickBookingDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AirplaneTicket, contentDescription = "Book", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Book Excursion Trip", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
    }

    // ==================================================
    // PREMIUM QUICK BOOKING DIALOG (DASHBOARD EXCLUSIVE)
    // ==================================================
    if (showQuickBookingDialog && selectedExperienceForBooking != null) {
        val exp = selectedExperienceForBooking!!
        Dialog(onDismissRequest = { showQuickBookingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ELITE MISSION BRIEFING",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldPremium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        IconButton(onClick = { showQuickBookingDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Excursion Details Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsBoat,
                                contentDescription = "Excursion",
                                tint = GoldPremium,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(exp.title, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text("Duration: ${exp.duration} • Max capacity: ${exp.capacity} pax", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. SELECT EXISTING VIP OR ENTER NEW
                    Text(
                        "1. CHOOSE VIP GUEST PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick VIP guest selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "New Guest" option chip
                        val isNewGuest = selectedCustomerForBooking == null
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNewGuest) GoldPremium.copy(alpha = 0.15f) else SurfaceGlassElevated.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isNewGuest) GoldPremium else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                selectedCustomerForBooking = null
                                guestName = ""
                            }
                        ) {
                            Text(
                                "➕ Add Custom Guest",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (isNewGuest) GoldPremium else TextPrimary,
                                fontSize = 12.sp
                            )
                        }

                        // Seedeed customers loop
                        customers.forEach { customer ->
                            val isSelectedCust = selectedCustomerForBooking?.id == customer.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelectedCust) GoldPremium.copy(alpha = 0.15f) else SurfaceGlassElevated.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelectedCust) GoldPremium else Color.Transparent
                                ),
                                modifier = Modifier.clickable {
                                    selectedCustomerForBooking = customer
                                    guestName = customer.fullName
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (customer.vipStatus) "👑" else "👤", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        customer.fullName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedCust) GoldPremium else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Guest name input text box
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = {
                            guestName = it
                            // If they change text directly, reset customer link unless matches exactly
                            if (selectedCustomerForBooking?.fullName != it) {
                                selectedCustomerForBooking = null
                            }
                        },
                        label = { Text("Guest Full Name") },
                        placeholder = { Text("e.g. Prince Albert of Monaco") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPremium,
                            unfocusedBorderColor = SurfaceGlassElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. TIMING & DATE CONFIGURATION
                    Text(
                        "2. DATE & TIME SELECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            label = { Text("Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPremium,
                                unfocusedBorderColor = SurfaceGlassElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = selectedTimeSlot,
                            onValueChange = { selectedTimeSlot = it },
                            label = { Text("Time Slot") },
                            placeholder = { Text("e.g. 18:00 - 22:00") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPremium,
                                unfocusedBorderColor = SurfaceGlassElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. VEHICLE / STAFF ASSIGNMENT
                    Text(
                        "3. ASSIGN CRUISE STAFF & FLEET CRAFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Assign Yacht / Vehicle Asset:", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        vehicles.forEach { v ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { assignedVehicleId = v.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = assignedVehicleId == v.id,
                                    onClick = { assignedVehicleId = v.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPremium)
                                )
                                Text(
                                    text = "${v.name} (${v.type}) • Cap ${v.capacity}",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Assign Captain / Guide:", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        staff.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { assignedStaffId = s.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = assignedStaffId == s.id,
                                    onClick = { assignedStaffId = s.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPremium)
                                )
                                Text(
                                    text = "${s.name} (${s.role})",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. PRICING & SPECIAL REQUESTS
                    Text(
                        "4. VALUE AGREED & DISCLOSURES",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Agreed Pricing (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPremium,
                            unfocusedBorderColor = SurfaceGlassElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = specialRequests,
                        onValueChange = { specialRequests = it },
                        label = { Text("Special Requests / Dietary Guidelines") },
                        placeholder = { Text("Caviar selection, extra security escort, only sparkling water, etc.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPremium,
                            unfocusedBorderColor = SurfaceGlassElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showQuickBookingDialog = false }) {
                            Text("Dismiss", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (guestName.trim().isEmpty()) {
                                    Toast.makeText(context, "Guest name is mandatory", Toast.LENGTH_SHORT).show()
                                } else {
                                    val finalPrice = customPrice.toDoubleOrNull() ?: exp.price
                                    viewModel.addBooking(
                                        Booking(
                                            customerId = selectedCustomerForBooking?.id ?: 99,
                                            customerName = guestName,
                                            experienceId = exp.id,
                                            experienceTitle = exp.title,
                                            date = selectedDate,
                                            timeSlot = selectedTimeSlot,
                                            status = "Confirmed",
                                            revenue = finalPrice,
                                            ticketQrCode = "DIAMONDS_QR_${System.currentTimeMillis()}",
                                            staffId = assignedStaffId,
                                            vehicleId = assignedVehicleId,
                                            notes = specialRequests,
                                            internalComment = "Registered instantly via the Dashboard Excursion Booking Hub."
                                        )
                                    )
                                    showQuickBookingDialog = false
                                    Toast.makeText(context, "VIP Excursion Trip Successfully Booked!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Booking", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selectedBookingForDetails?.let { booking ->
        val customer = customers.firstOrNull { it.id == booking.customerId }
        BookingDetailModal(
            booking = booking,
            customer = customer,
            onDismiss = { selectedBookingForDetails = null }
        )
    }
}

@Composable
fun KpiCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
fun NotificationRow(item: NotificationItem, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.type) {
                            "Weather" -> StatusAlertAmber
                            "Booking" -> StatusPaidBlue
                            "Fleet" -> GoldPremium
                            else -> StatusLiveGreen
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(item.timestamp, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ==========================================
// 3. BOOKING MANAGEMENT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(viewModel: DiamondsViewModel) {
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val staffList by viewModel.staff.collectAsStateWithLifecycle()
    val vehicleList by viewModel.vehicles.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    
    var selectedFilter by remember { mutableStateOf("All") } // All, Upcoming, Completed, Cancelled
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filters = listOf("All", "Upcoming", "Completed", "Cancelled")

    // Modals state
    var activeBoardingPassBooking by remember { mutableStateOf<Booking?>(null) }
    var activeChatBooking by remember { mutableStateOf<Booking?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
            .padding(16.dp)
    ) {
        // Luxury Header
        Text(
            "MY PRIVATE VOYAGES",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Elite Itinerary",
            style = MaterialTheme.typography.headlineMedium,
            color = GoldPremium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Manage your boutique reservations, access digital boarding passes, and contact private yacht/helicopter concierge support.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your cruises & excursions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPremium,
                unfocusedBorderColor = SurfaceGlassElevated,
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal filter list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GoldPremium else SurfaceGlass)
                        .border(1.dp, if (isSelected) GoldPremium else SurfaceGlassElevated, RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        filter,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter logic
        val filteredList = bookings.filter { booking ->
            val matchesQuery = booking.experienceTitle.contains(searchQuery, ignoreCase = true) ||
                               booking.customerName.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Upcoming" -> booking.status in listOf("Pending", "Confirmed", "Paid", "Checked In")
                "Completed" -> booking.status == "Completed"
                "Cancelled" -> booking.status == "Cancelled"
                else -> true
            }

            // In user-side mode, non-managers (e.g. Captain, Guide, Driver) only see their assigned bookings
            val isUserOnly = loggedInUser?.role?.equals("Manager", ignoreCase = true) == false && 
                             loggedInUser?.role?.equals("Admin", ignoreCase = true) == false
            val matchesUser = if (isUserOnly) {
                booking.staffId == (loggedInUser?.id ?: -1)
            } else {
                true // Administrators can see all bookings
            }
            
            matchesQuery && matchesFilter && matchesUser
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(GoldLight.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = GoldPremium,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your Itinerary is Clear",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You don't have any excursions booked in this category yet. Begin your exclusive Amalfi adventure today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.currentScreen.value = "explore" },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = "Explore", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explore Amalfi Cruises", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { booking ->
                    val assignedStaff = staffList.find { it.id == booking.staffId }
                    val assignedVehicle = vehicleList.find { it.id == booking.vehicleId }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SurfaceGlassElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_booking_card_${booking.id}")
                    ) {
                        Column {
                            // Banner block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                val cardImageUrl = when {
                                    booking.experienceTitle.contains("Yacht") -> "https://images.unsplash.com/photo-1567899378494-47b22a2ae96a?auto=format&fit=crop&q=80&w=600"
                                    booking.experienceTitle.contains("Speedboat") -> "https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&q=80&w=600"
                                    booking.experienceTitle.contains("Helicopter") -> "https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?auto=format&fit=crop&q=80&w=600"
                                    else -> "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=600"
                                }
                                AsyncImage(
                                    model = cardImageUrl,
                                    contentDescription = booking.experienceTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                startY = 100f
                                            )
                                        )
                                )
                                
                                // Left schedule badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = "Schedule", tint = GoldLight, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(booking.timeSlot, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Right status badge
                                val (statusText, statusBg, statusColor) = when (booking.status) {
                                    "Pending" -> Triple("Awaiting VIP Approval", StatusAlertAmber.copy(alpha = 0.9f), Color.White)
                                    "Confirmed" -> Triple("Confirmed Voyage", StatusPaidBlue, Color.White)
                                    "Paid" -> Triple("VIP Charter Certified", StatusLiveGreen, Color.White)
                                    "Checked In" -> Triple("Now Boarding", GoldPremium, Color.White)
                                    "Completed" -> Triple("Voyage Completed", Color.Gray, Color.White)
                                    else -> Triple("Cancelled", StatusErrorRed, Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(statusBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }

                                // Bottom left / right overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = booking.experienceTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "€${String.format("%,.0f", booking.revenue)}",
                                        color = GoldLight,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Details content block
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Vessel Info
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(GoldLight.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.DirectionsBoat, contentDescription = "Boat", tint = GoldPremium, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("VESSEL / CRAFT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(assignedVehicle?.name ?: "Premium Charter", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                    }

                                    // Captain / Crew Info
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(BronzeWarm.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = "Crew", tint = Color(0xFF1070B8), modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("VIP CAPTAIN / PILOT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(assignedStaff?.name ?: "Elite Host", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = SurfaceGlassElevated.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Date & Location Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Date: ${booking.date}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = "Pickup", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Marina Grande", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                if (booking.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GoldLight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = "Notes", tint = GoldPremium, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "VIP Concierge Request: \"${booking.notes}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // User action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { activeBoardingPassBooking = booking },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        Icon(Icons.Default.QrCode, contentDescription = "Ticket", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Boarding Pass", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { activeChatBooking = booking },
                                        border = BorderStroke(1.dp, GoldPremium),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPremium),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Icon(Icons.Default.Face, contentDescription = "Concierge", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Concierge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Digital Boarding Pass
    if (activeBoardingPassBooking != null) {
        val booking = activeBoardingPassBooking!!
        val assignedStaff = staffList.find { it.id == booking.staffId }
        val assignedVehicle = vehicleList.find { it.id == booking.vehicleId }

        Dialog(onDismissRequest = { activeBoardingPassBooking = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Logo / Brand Accent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DIAMONDS CO.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = GoldPremium
                        )
                        Box(
                            modifier = Modifier
                                .background(GoldLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "ELITE PASS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPremium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title of Experience
                    Text(
                        booking.experienceTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Luxury Voyage Reservation Pass",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stylized QR code with Sweeping laser
                    val infiniteTransition = rememberInfiniteTransition(label = "QR Scan Animation")
                    val laserPosition by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "LaserLine"
                    )

                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceGlassElevated, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        StylizedQrCode(
                            content = booking.ticketQrCode,
                            modifier = Modifier.size(160.dp)
                        )
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val y = size.height * laserPosition
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        booking.ticketQrCode,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dotted ticket punch out line
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    ) {
                        val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        drawLine(
                            color = SurfaceGlassElevated,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Passenger Details Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PASSENGER", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(booking.customerName.take(18), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DEPARTURE DATE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(booking.date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CRAFT / VESSEL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(assignedVehicle?.name ?: "Superyacht", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BOARDING TIME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(booking.timeSlot.split("-").first().trim(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ASSIGNED CAPTAIN", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(assignedStaff?.name ?: "Marco Rossi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PORT TERMINAL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text("Marina Grande", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPremium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Apple / Google Wallet integration
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Charter synced to Google Wallet & Apple Calendar!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, SurfaceGlassElevated),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { activeBoardingPassBooking = null },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Direct Concierge Private Chat
    if (activeChatBooking != null) {
        val booking = activeChatBooking!!
        var chatInputText by remember { mutableStateOf("") }
        var chatMessages by remember { mutableStateOf(listOf<Pair<String, String>>()) }

        // Initialize chat with prefilled greetings
        LaunchedEffect(booking.id) {
            chatMessages = listOf(
                "Concierge" to "Benvenuto, VIP Traveler! I am your dedicated private concierge for your upcoming '${booking.experienceTitle}' excursion on ${booking.date}.\n\nHow can I perfect your luxury yachting or heli-excursion itinerary today? (e.g. specialized champagnes, private driver transfers, custom catering requests)"
            )
        }

        Dialog(onDismissRequest = { activeChatBooking = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .padding(16.dp)
                ) {
                    // Chat header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(GoldPremium, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Face, contentDescription = "Concierge", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Private Concierge", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Online Assist", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(onClick = { activeChatBooking = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SurfaceGlassElevated)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Message lists
                    val listState = rememberLazyListState()
                    LaunchedEffect(chatMessages.size) {
                        listState.animateScrollToItem(chatMessages.size)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatMessages) { (sender, text) ->
                            val isUser = sender == "Guest"
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(if (isUser) GoldPremium else SurfaceGlassElevated.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                        .widthIn(max = 220.dp)
                                ) {
                                    Text(
                                        text = text,
                                        color = if (isUser) Color.White else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SurfaceGlassElevated)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat Input text field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Ask the Concierge...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPremium,
                                unfocusedBorderColor = SurfaceGlassElevated,
                                focusedContainerColor = SurfaceGlass,
                                unfocusedContainerColor = SurfaceGlass
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                if (chatInputText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val guestMsg = chatInputText
                                            chatMessages = chatMessages + ("Guest" to guestMsg)
                                            chatInputText = ""
                                            
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1200)
                                                val replies = listOf(
                                                    "An exquisite selection! I have updated your charter requests and passed these instructions directly to Captain Marco Rossi. We will ensure everything is on board and chilled.",
                                                    "Perfectly understood. I have logged this request with the vessel's steward. They will arrange premium catering to accommodate this request.",
                                                    "Absolutely! We will sync with our private chef on board to finalize this request. Your updated details are fully secured.",
                                                    "Of course! I have noted this preference in your VIP traveler card. The crew is fully briefed for your boarding tomorrow at Marina Grande Dock B."
                                                )
                                                chatMessages = chatMessages + ("Concierge" to replies.random())
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = "Send", tint = GoldPremium)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylizedQrCode(content: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val cellSize = sizePx / 15f
        
        // Draw background
        drawRect(color = Color.White)
        
        // Draw finder patterns (top-left, top-right, bottom-left)
        // Top-left
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(0f, 0f), size = Size(cellSize * 5, cellSize * 5))
        drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize), size = Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(cellSize * 2, cellSize * 2), size = Size(cellSize, cellSize))
        
        // Top-right
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(cellSize * 10, 0f), size = Size(cellSize * 5, cellSize * 5))
        drawRect(color = Color.White, topLeft = Offset(cellSize * 11, cellSize), size = Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(cellSize * 12, cellSize * 2), size = Size(cellSize, cellSize))
        
        // Bottom-left
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(0f, cellSize * 10), size = Size(cellSize * 5, cellSize * 5))
        drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize * 11), size = Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color(0xFF1C1B1F), topLeft = Offset(cellSize * 2, cellSize * 12), size = Size(cellSize, cellSize))
        
        // Seed deterministic pseudo-random noise
        val hash = content.hashCode()
        val random = java.util.Random(hash.toLong())
        
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                // Skip finder patterns
                if (row < 5 && col < 5) continue
                if (row < 5 && col >= 10) continue
                if (row >= 10 && col < 5) continue
                
                if (random.nextBoolean()) {
                    drawRect(
                        color = Color(0xFF1C1B1F),
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize + 0.5f, cellSize + 0.5f)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. EXPERIENCES CATALOG SCREEN
// ==========================================
@Composable
fun ExperiencesScreen(viewModel: DiamondsViewModel) {
    val experiences by viewModel.experiences.collectAsStateWithLifecycle()
    var searchKey by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "VIP LUXURY EXPERIENCE PORTFOLIO",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchKey,
            onValueChange = { searchKey = it },
            placeholder = { Text("Search catalog tours...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPremium,
                unfocusedBorderColor = SurfaceGlassElevated,
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        val filteredExps = experiences.filter {
            it.title.contains(searchKey, ignoreCase = true) || it.difficulty.contains(searchKey, ignoreCase = true)
        }

        if (filteredExps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Luxury Catalog Assets Found", color = TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredExps) { exp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GoldPremium.copy(alpha = 0.3f), SurfaceGlassElevated)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CardMembership,
                                    contentDescription = "Experience",
                                    tint = GoldPremium,
                                    modifier = Modifier.size(56.dp)
                                )

                                if (exp.featured) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldPremium)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("FEATURED VIP", color = SlateDarkBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SlateDarkBg.copy(alpha = 0.8f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("€${exp.price.toInt()}", color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(exp.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Badge(containerColor = SurfaceGlassElevated, contentColor = TextSecondary) {
                                            Text(exp.duration)
                                        }
                                        Badge(containerColor = SurfaceGlassElevated, contentColor = TextSecondary) {
                                            Text("Max cap: ${exp.capacity}")
                                        }
                                        Badge(containerColor = SurfaceGlassElevated, contentColor = TextSecondary) {
                                            Text(exp.difficulty)
                                        }
                                    }

                                    // Quick state duplicate action
                                    IconButton(
                                        onClick = {
                                            viewModel.addExperience(exp.copy(id = 0, title = "${exp.title} (Duplicate)"))
                                            Toast.makeText(context, "Asset duplicated successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = GoldPremium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. CUSTOMERS MANAGEMENT SCREEN
// ==========================================
@Composable
fun CustomersScreen(viewModel: DiamondsViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var searchVal by remember { mutableStateOf("") }
    var expandedCustId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "VIP HIGH-PROFILE GUEST DIRECTORY",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchVal,
            onValueChange = { searchVal = it },
            placeholder = { Text("Search guests name or hotel...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPremium,
                unfocusedBorderColor = SurfaceGlassElevated,
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        val filteredCust = customers.filter {
            it.fullName.contains(searchVal, ignoreCase = true) || it.pickupHotel.contains(searchVal, ignoreCase = true)
        }

        if (filteredCust.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No guests registered.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredCust) { customer ->
                    val isExpanded = expandedCustId == customer.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCustId = if (isExpanded) null else customer.id }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(GoldPremium)
                                            .padding(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(SurfaceGlass),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = "VIP", tint = GoldPremium, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(customer.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Nationality: ${customer.nationality}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                if (customer.vipStatus) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldPremium.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("VIP BLACK", color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Hotel: ${customer.pickupHotel} (Room: ${customer.roomNumber})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = SurfaceGlassElevated)
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Passport: ${customer.passportNumber}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Contact: ${customer.phoneNumber}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Emergency: ${customer.emergencyContact}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Preferred Language: ${customer.language}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Special Medical / Diet Requirements:", style = MaterialTheme.typography.labelSmall, color = GoldPremium)
                                Text(customer.internalNotes.ifEmpty { "No special requirements declared." }, style = MaterialTheme.typography.bodySmall, color = GoldLight)

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { Toast.makeText(context, "Initiating secure encrypted call...", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Secure Call", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { Toast.makeText(context, "Passport copy exported to secure secure cloud...", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Passport", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export ID", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SCHEDULES CALENDAR SCREEN
// ==========================================
@Composable
fun CalendarScreen(viewModel: DiamondsViewModel) {
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf("Today") } // Today, Tomorrow, Upcoming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "COMPANY MASTER OPERATION CALENDAR",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Days picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceGlass)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val days = listOf("Today", "Tomorrow", "Upcoming Schedules")
            days.forEach { day ->
                val isSelected = selectedDay == day
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldPremium else Color.Transparent)
                        .clickable { selectedDay = day }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) SlateDarkBg else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scheduled excursions listing
        val calendarBookings = when (selectedDay) {
            "Today" -> bookings.filter { it.status != "Cancelled" }
            "Tomorrow" -> bookings.filter { it.date != "2026-07-10" && it.status != "Cancelled" }
            else -> bookings
        }

        if (calendarBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No private missions scheduled.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(calendarBookings) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = GoldPremium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(booking.timeSlot, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceGlassElevated)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(booking.date, color = GoldLight, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(booking.experienceTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GoldPremium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Assigned Captain / Driver ID: #00${booking.staffId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Assigned Yacht / Speedboat ID: #00${booking.vehicleId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Client: ${booking.customerName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Badge(containerColor = GoldPremium.copy(alpha = 0.15f), contentColor = GoldPremium) {
                                    Text(booking.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. FLEET MANAGEMENT SCREEN
// ==========================================
@Composable
fun FleetScreen(viewModel: DiamondsViewModel) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "OPERATIONAL CRUISE FLEET MONITOR",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(vehicles) { vehicle ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsBoat, contentDescription = "Boat", tint = GoldPremium, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(vehicle.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("License ID: ${vehicle.licensePlate} • Cap: ${vehicle.capacity}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (vehicle.status) {
                                            "Available" -> StatusLiveGreen.copy(alpha = 0.2f)
                                            "Active" -> StatusPaidBlue.copy(alpha = 0.2f)
                                            else -> StatusAlertAmber.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    vehicle.status,
                                    color = when (vehicle.status) {
                                        "Available" -> StatusLiveGreen
                                        "Active" -> StatusPaidBlue
                                        else -> StatusAlertAmber
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fuel indicator progress
                        Text("Fuel Telemetry Level: ${vehicle.fuelLevel}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { vehicle.fuelLevel / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (vehicle.fuelLevel > 50) GoldPremium else StatusAlertAmber,
                            trackColor = SurfaceGlassElevated
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Last Maintenance: ${vehicle.lastMaintenance}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("Insurance Expiry: ${vehicle.insuranceExpiry}", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateVehicleStatus(vehicle, "Available")
                                    Toast.makeText(context, "${vehicle.name} Marked Available", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ready", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.updateVehicleStatus(vehicle, "Maintenance")
                                    Toast.makeText(context, "${vehicle.name} Scheduled For Repair", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = StatusAlertAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Service", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Contacting harbor master for safety logs...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = "GPS", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live GPS", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. STAFF & CREW SCREEN
// ==========================================
@Composable
fun StaffScreen(viewModel: DiamondsViewModel) {
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "CREW OPERATIONAL DISPATCH",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(staff) { member ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(GoldPremium)
                                        .padding(1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(SurfaceGlass),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(member.name.take(1), fontWeight = FontWeight.Bold, color = GoldPremium)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(member.role, style = MaterialTheme.typography.bodySmall, color = GoldLight)
                                }
                            }

                            // Attendance state
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (member.attendanceStatus) {
                                            "Present" -> StatusLiveGreen.copy(alpha = 0.2f)
                                            "Leave" -> StatusAlertAmber.copy(alpha = 0.2f)
                                            else -> StatusErrorRed.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    member.attendanceStatus,
                                    color = when (member.attendanceStatus) {
                                        "Present" -> StatusLiveGreen
                                        "Leave" -> StatusAlertAmber
                                        else -> StatusErrorRed
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rating: ⭐ ${member.rating}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Mission Score: ${member.performanceScore}/100", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        Text("Credentials: ${member.certificateUrl}", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateStaffAttendance(member, "Present")
                                    Toast.makeText(context, "${member.name} marked Present", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Present", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateStaffAttendance(member, "Leave")
                                    Toast.makeText(context, "${member.name} marked on approved Leave", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = StatusAlertAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Leave", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Initiating telemetry trace to crew mobile...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Crew", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. QR VALIDATOR SCANNER SCREEN
// ==========================================
@Composable
fun QRScanScreen() {
    var scannerState by remember { mutableStateOf("Ready") } // Ready, Scanning, Valid, Invalid
    var showScanLine by remember { mutableStateOf(true) }

    val scanAnimation = rememberInfiniteTransition(label = "Scan Line")
    val lineOffset by scanAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LineOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "VIP TICKET CRYPTO QR VALIDATOR",
            style = MaterialTheme.typography.labelSmall,
            color = GoldPremium,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Scanner Frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceGlass)
                .border(2.dp, GoldPremium, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (scannerState == "Scanning" || scannerState == "Ready") {
                // Diagonal corner lines representing scanner target
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .drawBehind {
                            val strokeW = 4f
                            val lineLength = 40f
                            // Top-Left Corner
                            drawLine(GoldPremium, Offset(0f, 0f), Offset(lineLength, 0f), strokeW)
                            drawLine(GoldPremium, Offset(0f, 0f), Offset(0f, lineLength), strokeW)

                            // Top-Right Corner
                            drawLine(GoldPremium, Offset(size.width, 0f), Offset(size.width - lineLength, 0f), strokeW)
                            drawLine(GoldPremium, Offset(size.width, 0f), Offset(size.width, lineLength), strokeW)

                            // Bottom-Left Corner
                            drawLine(GoldPremium, Offset(0f, size.height), Offset(lineLength, size.height), strokeW)
                            drawLine(GoldPremium, Offset(0f, size.height), Offset(0f, size.height - lineLength), strokeW)

                            // Bottom-Right Corner
                            drawLine(GoldPremium, Offset(size.width, size.height), Offset(size.width - lineLength, size.height), strokeW)
                            drawLine(GoldPremium, Offset(size.width, size.height), Offset(size.width, size.height - lineLength), strokeW)
                        }
                )

                // Simulated QR graphics inside
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code Placeholder",
                    tint = TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(180.dp)
                )

                if (scannerState == "Scanning") {
                    // Scanning laser light line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .offset(y = (-140 + 280 * lineOffset).dp)
                            .background(GoldPremium)
                    )
                }
            } else if (scannerState == "Valid") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = StatusLiveGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("TICKET VERIFIED", fontWeight = FontWeight.Bold, color = StatusLiveGreen, letterSpacing = 2.sp)
                    Text("Guest: Lady Beatrice Harrington", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("Excursion: Yacht Cruise", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Cancel, contentDescription = "Invalid", tint = StatusErrorRed, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CRYPTOGRAPHIC ERROR", fontWeight = FontWeight.Bold, color = StatusErrorRed, letterSpacing = 1.sp)
                    Text("Ticket expired or unrecorded.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnostic logs / state buttons
        Text(
            text = when (scannerState) {
                "Ready" -> "Align VIP ticket QR code within viewfinder."
                "Scanning" -> "Resolving cryptographic blockchain signature..."
                "Valid" -> "Check-in successful. Logged in central database."
                else -> "Crypto verification mismatch. Terminal halted."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    scannerState = "Scanning"
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg)
            ) {
                Text("Start Laser", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    scannerState = "Valid"
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = StatusLiveGreen)
            ) {
                Text("Simulate VIP Pass")
            }

            Button(
                onClick = {
                    scannerState = "Invalid"
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = StatusErrorRed)
            ) {
                Text("Simulate Invalid")
            }
        }

        if (scannerState != "Ready") {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { scannerState = "Ready" }) {
                Text("Reset Scanner Terminal", color = GoldPremium)
            }
        }
    }
}

// ==========================================
// 10. REPORTS & ANALYTICS SCREEN
// ==========================================
@Composable
fun ReportsScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "COMPANY METRIC REPORTS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Total Performance Chart card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EXCURSION SALES VELOCITY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                Text("Comparison against forecast models", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(24.dp))

                // Inline Bar graphs drawn with Compose Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val reportData = listOf(30, 55, 45, 90, 75, 98)
                    val reportLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")

                    reportData.forEachIndexed { idx, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(value / 100f)
                                    .width(28.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(GoldPremium, GoldPremium.copy(alpha = 0.4f))
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(reportLabels[idx], style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats rows
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("FINANCIAL AUDIT TELEMETRY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Operating Gross Profit", color = TextSecondary)
                    Text("€1,248,500.00", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Refund Rate (Cancelled Missions)", color = TextSecondary)
                    Text("0.4%", color = StatusLiveGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vessel Utilization Factor", color = TextSecondary)
                    Text("94.2%", color = GoldLight, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Guide Satisfaction Rating", color = TextSecondary)
                    Text("4.92 / 5.0", color = GoldPremium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Actions
        Button(
            onClick = {
                Toast.makeText(context, "Assembling PDF report on client-side...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
            Spacer(modifier = Modifier.width(8.dp))
            Text("COMPILE & EXPORT PDF REPORT", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(context, "Compiling Excel spreadsheet...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            border = BorderStroke(1.dp, GoldPremium),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPremium),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.TableChart, contentDescription = "Excel")
            Spacer(modifier = Modifier.width(8.dp))
            Text("GENERATE EXCEL LEDGER", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ==========================================
// 11. COMPANY SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current
    var biometricsEnabled by remember { mutableStateOf(true) }
    var offlineSyncEnabled by remember { mutableStateOf(true) }

    // Observe Supabase Sync State
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncReport by viewModel.supabaseSyncReport.collectAsStateWithLifecycle()
    val isSupabaseConfigured = viewModel.isSupabaseConfigured()

    // Architecture Hub States
    var selectedSpecSection by remember { mutableStateOf("none") } // "none", "supabase", "design", "react_native", "motion", "auth"

    // Motion Playgrounds States
    var motionTriggered by remember { mutableStateOf(false) }
    var activeSpringType by remember { mutableStateOf("tight") } // "tight", "heavy"
    val animOffset by animateDpAsState(
        targetValue = if (motionTriggered) 160.dp else 0.dp,
        animationSpec = if (activeSpringType == "tight") {
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
        },
        label = "MotionPlaygroundAnim"
    )

    // React Native Dark/Light simulation
    var rnSimulateDarkMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "OPERATOR TERMINAL CONFIGURATION",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SECURE ACCESS CONTROLS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fingerprint / Face ID Handshake", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Fast biometric credential verification", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = { biometricsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline Database Auto-Save", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Caches and queues database writes offline", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = offlineSyncEnabled,
                        onCheckedChange = { offlineSyncEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DATABASE BACKEND CONNECTION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                    
                    val badgeColor = if (isSupabaseConfigured) StatusLiveGreen else Color(0xFFFFB300)
                    val badgeText = if (isSupabaseConfigured) "ACTIVE NODE" else "NOT CONFIG"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Supabase DB Status", color = TextSecondary)
                    Text(
                        text = if (isSupabaseConfigured) "Synchronized (Realtime)" else "Disconnected / Local Only",
                        color = if (isSupabaseConfigured) StatusLiveGreen else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Supabase Target Host", color = TextSecondary)
                    Text(
                        text = if (isSupabaseConfigured) {
                            val host = com.example.BuildConfig.SUPABASE_URL.removePrefix("https://").removeSuffix("/")
                            if (host.length > 25) host.take(22) + "..." else host
                        } else "None configured",
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local Room Latency", color = TextSecondary)
                    Text("0.1 ms (Direct-Cache)", color = GoldLight)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSupabaseConfigured) {
                    Button(
                        onClick = { viewModel.syncDataWithSupabase() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SlateDarkBg, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNCHRONIZING SECURE TUNNEL...", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRIGGER BACKEND HARDWARE SYNC", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "INTEGRATION GUIDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "This terminal is ready to sync local data with your private Supabase backend. Follow these steps to complete the link:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val steps = listOf(
                                "1. Open the Secrets panel in AI Studio UI.",
                                "2. Add SUPABASE_URL with your project base URL.",
                                "3. Add SUPABASE_KEY with your anon/service key.",
                                "4. Copy & run /supabase_schema.sql on Supabase."
                            )
                            steps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                syncReport?.let { report ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.success) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (report.success) StatusLiveGreen.copy(alpha = 0.4f) else Color(0xFFE57373).copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (report.success) Icons.Default.CloudDone else Icons.Default.Warning,
                                    contentDescription = "Sync Report",
                                    tint = if (report.success) StatusLiveGreen else Color(0xFFE57373),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SYNC REPORT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.success) StatusLiveGreen else Color(0xFFE57373)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(report.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            
                            if (report.details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = SurfaceGlass)
                                Spacer(modifier = Modifier.height(6.dp))
                                report.details.forEach { detail ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${detail.tableName.uppercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (detail.success) "SUCCESS" else "FAILED",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (detail.success) StatusLiveGreen else Color(0xFFE57373),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // SYSTEM ARCHITECTURE EXPLORER (DIAMONDS HUB)
        // ==========================================
        Text(
            "SYSTEM ARCHITECTURE EXPLORER",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Grid of architecture files/specs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SPEC 1: SUPABASE PGSQL SCHEMA
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSpecSection == "supabase") GoldPremium.copy(alpha = 0.08f) else SurfaceGlass
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedSpecSection == "supabase") GoldPremium else SurfaceGlassElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSpecSection = if (selectedSpecSection == "supabase") "none" else "supabase"
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = "Supabase",
                                tint = GoldPremium,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Supabase PostgreSQL Schema", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Tables, relationships, keys & RLS security", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            if (selectedSpecSection == "supabase") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = selectedSpecSection == "supabase") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Database Entities Defined:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldPremium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val schemaItems = listOf(
                                "companies" to "Root Multi-tenant catalog. Houses subdomains and identifiers.",
                                "employees" to "References company. Holds role (operator, driver, crew, admin).",
                                "customers" to "VIP profiles with tiered flags (Platinum, Centurion, Royal Diamonds).",
                                "fleet" to "Excursion craft details (Yachts, Jets, Heli, SUVs) and rates.",
                                "bookings" to "Central transactional state. Connects guest, craft, staff and rates."
                            )
                            
                            schemaItems.forEach { (table, desc) ->
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("• $table: ", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Row Level Security (RLS) Policy Blueprint:", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceGlassElevated)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "CREATE POLICY \"Manage own bookings\"\n" +
                                    "  ON bookings FOR ALL\n" +
                                    "  USING (company_id = get_user_company_id());",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "Saved to workspace root as: /supabase_schema.sql",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SPEC 2: SLEEK INTERFACE DESIGN SPECS
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSpecSection == "design") GoldPremium.copy(alpha = 0.08f) else SurfaceGlass
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedSpecSection == "design") GoldPremium else SurfaceGlassElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSpecSection = if (selectedSpecSection == "design") "none" else "design"
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Design System",
                                tint = GoldPremium,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sleek Interface Specifications", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Color systems, spacing tokens & typography", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            if (selectedSpecSection == "design") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = selectedSpecSection == "design") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Active Color Tokens (Luxury Palette):", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            val designColors = listOf(
                                Triple("Canvas Background", Color(0xFFFDF8F6), "Warm Peach/Ivory background"),
                                Triple("Surface Card", Color(0xFFFFFFFF), "Pure White elevated bento cards"),
                                Triple("Primary Accent", Color(0xFF6750A4), "Luxury brand Royal Purple"),
                                Triple("Secondary Accent", Color(0xFFE8DEF8), "Soft warm Lavender containers"),
                                Triple("Tertiary Accent", Color(0xFFD1E1FF), "Sophisticated Ice Blue details"),
                                Triple("Text Primary", Color(0xFF1C1B1F), "Deep charcoal high-contrast copy")
                            )

                            designColors.forEach { (label, col, description) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(col)
                                            .border(1.dp, SurfaceGlassElevated, RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Spacing & Layout Rules:", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "• Strict 8dp structural grid increments.\n" +
                                "• Container Corner Radius: 28dp smooth M3 profiles.\n" +
                                "• Tactile targets guaranteed at minimum 48dp x 48dp boundary.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Saved to workspace root as: /design_system_spec.md",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SPEC 3: REACT NATIVE BASE LAYOUT
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSpecSection == "react_native") GoldPremium.copy(alpha = 0.08f) else SurfaceGlass
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedSpecSection == "react_native") GoldPremium else SurfaceGlassElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSpecSection = if (selectedSpecSection == "react_native") "none" else "react_native"
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhoneIphone,
                                contentDescription = "React Native",
                                tint = GoldPremium,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("React Native Layout Spec", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Base bottom bar container & dark mode toggle", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            if (selectedSpecSection == "react_native") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = selectedSpecSection == "react_native") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interactive Layout Preview", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dark Mode", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = rnSimulateDarkMode,
                                        onCheckedChange = { rnSimulateDarkMode = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Mock Simulator Frame
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (rnSimulateDarkMode) Color(0xFF1C1B1F) else Color(0xFFFDF8F6))
                                    .border(2.dp, if (rnSimulateDarkMode) Color(0xFF49454F) else Color(0xFFE7E0EC), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    // App header inside simulation
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "DIAMONDS OPERATOR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rnSimulateDarkMode) Color(0xFFCAC4D0) else Color(0xFF625B71)
                                            )
                                            Text(
                                                "Blue Horizon Excursions",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rnSimulateDarkMode) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (rnSimulateDarkMode) Color(0xFFD0BCFF) else Color(0xFF6750A4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("JD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Simulated Revenue Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (rnSimulateDarkMode) Color(0xFF2B2930) else Color(0xFFFFFFFF))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                "ACTIVE EXCURSIONS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rnSimulateDarkMode) Color(0xFF938F99) else Color(0xFF625B71)
                                            )
                                            Text(
                                                "8 Live luxury tours in progress",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (rnSimulateDarkMode) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Bottom Navigation Bar simulation
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (rnSimulateDarkMode) Color(0xFF2B2930) else Color(0xFFFFFFFF))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        listOf("🏠 Dash", "📅 Bookings", "🛥️ Fleet", "⚙️ Config").forEachIndexed { index, title ->
                                            val active = index == 0
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (active) {
                                                                if (rnSimulateDarkMode) Color(0xFF4F378B) else Color(0xFFE8DEF8)
                                                            } else Color.Transparent
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        title.split(" ")[0],
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Text(
                                                    title.split(" ")[1],
                                                    fontSize = 8.sp,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (active) {
                                                        if (rnSimulateDarkMode) Color(0xFFD0BCFF) else Color(0xFF6750A4)
                                                    } else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Saved to workspace root as: /react_native_layout.tsx",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SPEC 4: MOTION DESIGN SYSTEM
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSpecSection == "motion") GoldPremium.copy(alpha = 0.08f) else SurfaceGlass
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedSpecSection == "motion") GoldPremium else SurfaceGlassElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSpecSection = if (selectedSpecSection == "motion") "none" else "motion"
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Animation,
                                contentDescription = "Motion Design",
                                tint = GoldPremium,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Kinetic Motion Design Spec", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("60 FPS physics, spring tension & micro-flows", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            if (selectedSpecSection == "motion") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = selectedSpecSection == "motion") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Live Spring Physics Playground", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Select an animation curve style and trigger the sphere transit to visualize real-time 60 FPS motion feedback.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        activeSpringType = "tight"
                                        motionTriggered = !motionTriggered
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeSpringType == "tight") GoldPremium else SurfaceGlassElevated,
                                        contentColor = if (activeSpringType == "tight") Color.White else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Tight Spring", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        activeSpringType = "heavy"
                                        motionTriggered = !motionTriggered
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeSpringType == "heavy") GoldPremium else SurfaceGlassElevated,
                                        contentColor = if (activeSpringType == "heavy") Color.White else TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Heavy Fluid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Motion playground area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceGlassElevated)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = animOffset)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GoldPremium)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Theoretical Physics Constants Defined:", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "• Tight Spring (Switches, Micro-feedback):\n  Stiffness/Tension: 220, Damping/Friction: 25, Mass: 0.8\n" +
                                "• Heavy Fluid Spring (Sheets, Cards):\n  Stiffness/Tension: 140, Damping/Friction: 18, Mass: 1.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Saved to workspace root as: /motion_design_system.md",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SPEC 5: SECURE AUTHENTICATION FLOW
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedSpecSection == "auth") GoldPremium.copy(alpha = 0.08f) else SurfaceGlass
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedSpecSection == "auth") GoldPremium else SurfaceGlassElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedSpecSection = if (selectedSpecSection == "auth") "none" else "auth"
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Security Specs",
                                tint = GoldPremium,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Secure Multi-Tenant Auth Flow", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Company subdomains, OTP & Biometric enclave", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            if (selectedSpecSection == "auth") Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = selectedSpecSection == "auth") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceGlassElevated)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Step-by-Step Security Protocol:", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            val steps = listOf(
                                "1. Company Subdomain Mapping" to "The operator inputs their company ID. The client sends a lookup request to verify the tenant is active and fetch configuration metrics.",
                                "2. Passwordless OTP Verification" to "An encrypted, single-use One Time Password (OTP) is dispatched via Supabase Auth services to the authorized employee's device.",
                                "3. Hardware Keystore Binding" to "Once OTP succeeds, a public/private cryptographic KeyPair is bound inside the secure hardware Android Keystore / iOS Secure Enclave.",
                                "4. Biometric Handshake Bypass" to "Subsequent logins bypass SMS/OTP. The terminal requests hardware biometric approval to instantly sign a secure auth nonce."
                            )

                            steps.forEach { (title, description) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = GoldPremium)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Saved to workspace root as: /auth_architecture_spec.md",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DIAMONDS TERMINAL PROTOCOL", style = MaterialTheme.typography.labelSmall, color = GoldPremium, letterSpacing = 1.sp)
                Text("Version 2.4.9 (Build 1081)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Licensed strictly to private operating excursion entities. Any reproduction of credentials or telemetry logs is strictly audited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// 12. EMPLOYEE PROFILE SCREEN (WITH INTEGRATED SYSTEM SETTINGS)
// ==========================================
@Composable
fun ProfileScreen(viewModel: DiamondsViewModel) {
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val u = loggedInUser ?: return

    var editablePhone by remember(u.id) { mutableStateOf(u.phoneNumber) }

    var showPasswordChange by remember { mutableStateOf(false) }
    var changeOldPass by remember { mutableStateOf("") }
    var changeNewPass by remember { mutableStateOf("") }
    var changeAnswer by remember { mutableStateOf("") }
    var changeError by remember { mutableStateOf("") }

    // Sub-Tabs for operator-only layout
    var activeSubTab by remember { mutableStateOf("information") } // "information", "security"

    // Dialog States for Quick Actions
    var showQuickSettingsDialog by remember { mutableStateOf(false) }
    var showAboutUsDialog by remember { mutableStateOf(false) }
    var showContactTicketsDialog by remember { mutableStateOf(false) }

    // Settings States
    var settingsBiometric by remember { mutableStateOf(true) }
    var settingsNotifications by remember { mutableStateOf(true) }
    var settingsOfflineSync by remember { mutableStateOf(true) }
    var settingsHighPerformance by remember { mutableStateOf(true) }

    // Ticket States
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    var ticketTitle by remember { mutableStateOf("") }
    var ticketDescription by remember { mutableStateOf("") }
    var ticketCategory by remember { mutableStateOf("IT Support") }
    var ticketPriority by remember { mutableStateOf("Medium") }
    var isCreatingTicket by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Visual Header Banner with Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&q=80&w=800",
                contentDescription = "Amalfi Coast Premium Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Premium Dark Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, SlateDarkBg),
                            startY = 50f
                        )
                    )
            )

            // Overlaid Avatar at the bottom center of banner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 30.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(GoldPremium)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SlateDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = u.name.split(" ").map { it.take(1) }.joinToString("").take(2).uppercase(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPremium,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Identity Text Labels
        Text(
            text = u.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GoldPremium.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${u.role.uppercase()} • ACTIVE OPERATOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPremium,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Horizontal Sub-Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceGlass)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                "information" to "Information",
                "security" to "Security"
            )
            tabs.forEach { (tabKey, label) ->
                val isSelected = activeSubTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) GoldPremium.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { activeSubTab = tabKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) GoldPremium else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Sub-Tab Content Switcher
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            when (activeSubTab) {
                "information" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Analytics / Statistics Micro-Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val staffBookingsCount = bookings.count { it.staffId == u.id }

                            // Card 1: Completed Trips
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ASSIGNED TRIPS", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$staffBookingsCount Trips", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            // Card 2: Rating
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("RATING", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("⭐ ${u.rating}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                                }
                            }

                            // Card 3: Performance Score
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${u.performanceScore}% Score", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        // Detailed Personal Information Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                            border = BorderStroke(1.dp, SurfaceGlassElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "OPERATOR CREDENTIALS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPremium,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Full Name", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text(u.name, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceGlassElevated.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Username", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text(u.email.substringBefore("@"), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceGlassElevated.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Corporate Email", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text(u.email, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceGlassElevated.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Registry Code", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text("DS-OPERATOR-0${u.id}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceGlassElevated.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Enterprise Binding", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text("Diamonds Elite Yachting Group", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SurfaceGlassElevated.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Registry Status", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    val isLocked = u.isLocked
                                    val statusColor = if (isLocked) Color(0xFFE57373) else StatusLiveGreen
                                    val statusText = if (isLocked) "Locked (Audit Required)" else "Active (On-Duty - ${u.attendanceStatus})"
                                    Text(
                                        text = statusText.uppercase(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Editable Contact Sheet
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                            border = BorderStroke(1.dp, SurfaceGlassElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "UPDATE OPERATOR METADATA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPremium,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editablePhone,
                                    onValueChange = { editablePhone = it },
                                    label = { Text("Contact Number") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPremium,
                                        unfocusedBorderColor = SurfaceGlassElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = SurfaceGlassElevated,
                                        unfocusedContainerColor = SurfaceGlassElevated
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val updated = u.copy(
                                            phoneNumber = editablePhone.trim()
                                        )
                                        viewModel.updateSecureProfile(updated)
                                        Toast.makeText(context, "Operator profile cached securely.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("SAVE PROFILE DETAILS", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "security" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                            border = BorderStroke(1.dp, SurfaceGlassElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("OPERATOR SECURITY SUITE", style = MaterialTheme.typography.labelSmall, color = GoldPremium, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Multi-Factor Authentication", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Require PIN verification on session start", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                    Switch(
                                        checked = u.isTwoFactorEnabled,
                                        onCheckedChange = { isEnabled ->
                                            val updated = u.copy(isTwoFactorEnabled = isEnabled)
                                            viewModel.updateSecureProfile(updated)
                                            viewModel.addSecurityLog("2FA_TOGGLE", "MFA toggled to ${if (isEnabled) "ENABLED" else "DISABLED"}.")
                                            Toast.makeText(context, "MFA status updated.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GoldPremium,
                                            checkedTrackColor = GoldPremium.copy(alpha = 0.4f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = SurfaceGlassElevated
                                        )
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGlassElevated)

                                if (!showPasswordChange) {
                                    OutlinedButton(
                                        onClick = { showPasswordChange = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPremium)
                                    ) {
                                        Text("CHANGE OPERATOR PASSWORD")
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Update Session Credentials", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

                                        if (changeError.isNotEmpty()) {
                                            Text(changeError, color = Color(0xFFE57373), style = MaterialTheme.typography.bodySmall)
                                        }

                                        OutlinedTextField(
                                            value = changeOldPass,
                                            onValueChange = { changeOldPass = it },
                                            label = { Text("Verify Current Password") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = changeNewPass,
                                            onValueChange = { changeNewPass = it },
                                            label = { Text("New Secure Password (PIN)") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Text("Security Question Verification:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated)) {
                                            Text(u.securityQuestion, modifier = Modifier.padding(12.dp), color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                        }

                                        OutlinedTextField(
                                            value = changeAnswer,
                                            onValueChange = { changeAnswer = it },
                                            label = { Text("Your Recovery Answer") },
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    showPasswordChange = false
                                                    changeOldPass = ""
                                                    changeNewPass = ""
                                                    changeAnswer = ""
                                                    changeError = ""
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel")
                                            }

                                            Button(
                                                onClick = {
                                                    if (changeOldPass != u.passwordHash) {
                                                        changeError = "Current password verification failed."
                                                    } else if (changeAnswer.trim().lowercase(Locale.ROOT) != u.securityAnswer.trim().lowercase(Locale.ROOT)) {
                                                        changeError = "Security recovery answer is incorrect."
                                                    } else if (changeNewPass.length < 4) {
                                                        changeError = "New password must be at least 4 characters."
                                                    } else {
                                                        val updated = u.copy(passwordHash = changeNewPass)
                                                        viewModel.updateSecureProfile(updated)
                                                        viewModel.addSecurityLog("PASSWORD_CHANGE", "Password successfully changed in settings.")
                                                        Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                        showPasswordChange = false
                                                        changeOldPass = ""
                                                        changeNewPass = ""
                                                        changeAnswer = ""
                                                        changeError = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                                modifier = Modifier.weight(1.5f)
                                            ) {
                                                Text("Save Credentials")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                            border = BorderStroke(1.dp, SurfaceGlassElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SECURITY AUDIT TRAIL", style = MaterialTheme.typography.labelSmall, color = GoldPremium, letterSpacing = 1.sp)
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPremium.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                securityLogs.take(4).forEach { log ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "[${log.eventType}]",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.eventType.contains("SUCCESS")) Color(0xFF81C784) else GoldPremium,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = log.timestamp,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Node IP: ${log.ipAddress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = SurfaceGlassElevated.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Always display logout button nicely styled at the bottom
        Button(
            onClick = {
                viewModel.performLogout()
                Toast.makeText(context, "Session Terminated Securely.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(" Log out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick action buttons panel (Replacing the terminal protocol footer card)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "QUICK PORTAL ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPremium,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Settings button
                    Button(
                        onClick = { showQuickSettingsDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GoldPremium, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // About Us button
                    Button(
                        onClick = { showAboutUsDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "About Us", tint = GoldPremium, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("About Us", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Support Tickets button
                    Button(
                        onClick = { showContactTicketsDialog = true },
                        modifier = Modifier.weight(1.2f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = "Tickets", tint = GoldPremium, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tickets Sys", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Settings Dialog
    if (showQuickSettingsDialog) {
        Dialog(onDismissRequest = { showQuickSettingsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OPERATOR PREFERENCES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                        IconButton(onClick = { showQuickSettingsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Biometrics Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Security Handshake", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Unlock terminal using fingerprints or face unlock.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settingsBiometric,
                            onCheckedChange = { settingsBiometric = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGlass.copy(alpha = 0.5f))

                    // Push Notifications Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Realtime Dispatch Alerts", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Receive high-priority vessel schedule updates.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settingsNotifications,
                            onCheckedChange = { settingsNotifications = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGlass.copy(alpha = 0.5f))

                    // Offline Sync Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline Transaction Queuing", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Queue writes locally when out at deep sea.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settingsOfflineSync,
                            onCheckedChange = { settingsOfflineSync = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGlass.copy(alpha = 0.5f))

                    // High Performance Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Acceleration", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Enable high-density spring physics rendering.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settingsHighPerformance,
                            onCheckedChange = { settingsHighPerformance = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPremium, checkedTrackColor = GoldPremium.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showQuickSettingsDialog = false
                            Toast.makeText(context, "Preferences cached to secure system.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Configurations", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // About Us Dialog
    if (showAboutUsDialog) {
        Dialog(onDismissRequest = { showAboutUsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ABOUT DIAMONDS ELITE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                        IconButton(onClick = { showAboutUsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Diamonds Elite Yachting Group is the premier private marine excursion operator along the Amalfi Coast and Sorrento peninsula.",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Our mission is to marry ultra-luxury maritime hospitality with bleeding-edge fleet security and live telemetry. Every vessel, guide, and route is certified under Diamonds Strict Protocol standards to ensure safety, extreme discretion, and bespoke experiences.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("FLEET STATS", fontWeight = FontWeight.Bold, color = GoldPremium, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    val stats = listOf(
                        "Active Fleet Count" to "12 Custom Mega Yachts & Speedboats",
                        "Certified Captains" to "8 Master Mariners",
                        "Port of Registry" to "Amalfi Harbor (Molo Pennello)",
                        "Secure Protocol" to "AES-256 Multi-Node Local Ledger"
                    )

                    stats.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAboutUsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Acknowledge", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Contact Us (Ticket System) Dialog
    if (showContactTicketsDialog) {
        Dialog(onDismissRequest = { 
            showContactTicketsDialog = false
            isCreatingTicket = false
            ticketTitle = ""
            ticketDescription = ""
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.3f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCreatingTicket) "FILE NEW SUPPORT TICKET" else "OPERATOR TICKET SYSTEM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldPremium
                        )
                        IconButton(onClick = { 
                            showContactTicketsDialog = false
                            isCreatingTicket = false
                            ticketTitle = ""
                            ticketDescription = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCreatingTicket) {
                        // File a new ticket view
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = ticketTitle,
                                onValueChange = { ticketTitle = it },
                                label = { Text("Short Subject") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = ticketDescription,
                                onValueChange = { ticketDescription = it },
                                label = { Text("Detailed Description") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )

                            // Category selector
                            Text("Category:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val categories = listOf("IT Support", "Vessel", "Dispatch", "Logistics")
                                categories.forEach { cat ->
                                    val isSel = ticketCategory == cat
                                    Button(
                                        onClick = { ticketCategory = cat },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) GoldPremium else SurfaceGlass,
                                            contentColor = if (isSel) SlateDarkBg else TextPrimary
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Priority selector
                            Text("Priority Level:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val priorities = listOf("Low", "Medium", "High")
                                priorities.forEach { prio ->
                                    val isSel = ticketPriority == prio
                                    val prioColor = when(prio) {
                                        "High" -> Color(0xFFE57373)
                                        "Medium" -> GoldLight
                                        else -> Color(0xFF81C784)
                                    }
                                    Button(
                                        onClick = { ticketPriority = prio },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) prioColor else SurfaceGlass,
                                            contentColor = if (isSel) SlateDarkBg else TextPrimary
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(prio, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isCreatingTicket = false },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back to Tickets")
                                }

                                Button(
                                    onClick = {
                                        if (ticketTitle.isBlank() || ticketDescription.isBlank()) {
                                            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.createSupportTicket(
                                                title = ticketTitle.trim(),
                                                description = ticketDescription.trim(),
                                                priority = ticketPriority,
                                                category = ticketCategory
                                            )
                                            Toast.makeText(context, "Ticket successfully submitted. Direct Node sync complete.", Toast.LENGTH_SHORT).show()
                                            isCreatingTicket = false
                                            ticketTitle = ""
                                            ticketDescription = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("Submit Ticket")
                                }
                            }
                        }
                    } else {
                        // Tickets list view
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Operational Tickets", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                TextButton(
                                    onClick = { isCreatingTicket = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Ticket", tint = GoldPremium, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("File New", color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                            ) {
                                if (supportTickets.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No active tickets found.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(supportTickets) { ticket ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = ticket.category.uppercase(),
                                                            color = GoldPremium,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Status badge
                                                            val statusColor = when (ticket.status) {
                                                                "Resolved" -> Color(0xFF81C784)
                                                                "Processing" -> GoldLight
                                                                else -> Color(0xFFE57373)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(statusColor.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = ticket.status.uppercase(),
                                                                    color = statusColor,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }

                                                            // Priority badge
                                                            val prioColor = when (ticket.priority) {
                                                                "High" -> Color(0xFFE57373)
                                                                "Medium" -> GoldLight
                                                                else -> Color(0xFF81C784)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(prioColor.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = ticket.priority.uppercase(),
                                                                    color = prioColor,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Text(
                                                        text = ticket.title,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Text(
                                                        text = ticket.description,
                                                        color = TextSecondary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Ticket ID: #${ticket.id}",
                                                            color = TextMuted,
                                                            fontSize = 9.sp,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        Text(
                                                            text = ticket.timestamp,
                                                            color = TextMuted,
                                                            fontSize = 9.sp,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showContactTicketsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlassElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close Portal Panel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 13. SHEET & DIALOG BUILDERS (BOTTOM SHEETS)
// ==========================================
@Composable
fun AddBookingSheet(viewModel: DiamondsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var guestName by remember { mutableStateOf("") }
    var selectedExpId by remember { mutableStateOf(1) }
    var selectedVehicleId by remember { mutableStateOf(1) }
    var selectedStaffId by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var priceValue by remember { mutableStateOf("2500") }

    val experiences by viewModel.experiences.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val staff by viewModel.staff.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "REGISTER EXCURSION MISSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPremium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    label = { Text("Guest Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPremium,
                        unfocusedBorderColor = SurfaceGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Luxury Excursion Catalog Asset", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                experiences.forEach { exp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedExpId = exp.id
                                priceValue = exp.price.toInt().toString()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedExpId == exp.id,
                            onClick = {
                                selectedExpId = exp.id
                                priceValue = exp.price.toInt().toString()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldPremium)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(exp.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceValue,
                    onValueChange = { priceValue = it },
                    label = { Text("Custom Agreed Price (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPremium,
                        unfocusedBorderColor = SurfaceGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Special Requests / Dietary Guidelines") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPremium,
                        unfocusedBorderColor = SurfaceGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (guestName.isNotEmpty()) {
                                val selectedExp = experiences.firstOrNull { it.id == selectedExpId }
                                viewModel.addBooking(
                                    Booking(
                                        customerId = 100,
                                        customerName = guestName,
                                        experienceId = selectedExpId,
                                        experienceTitle = selectedExp?.title ?: "Yacht Cruise",
                                        date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                        timeSlot = "10:00 - 14:00",
                                        status = "Pending",
                                        revenue = priceValue.toDoubleOrNull() ?: 2500.0,
                                        ticketQrCode = "DIAMOND_QR_${System.currentTimeMillis()}",
                                        staffId = selectedStaffId,
                                        vehicleId = selectedVehicleId,
                                        notes = notes,
                                        internalComment = "Created via terminal admin hub."
                                    )
                                )
                                onDismiss()
                                Toast.makeText(context, "VIP Excursion Reserved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter Guest Name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify & Reserve", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddExperienceDialog(viewModel: DiamondsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("ENROLL NEW EXCURSION MODEL", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Excursion Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Excursion Description Catalog") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (e.g. 4 Hours)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && price.isNotEmpty()) {
                                viewModel.addExperience(
                                    Experience(
                                        title = title,
                                        description = desc,
                                        price = price.toDoubleOrNull() ?: 1000.0,
                                        capacity = capacity.toIntOrNull() ?: 10,
                                        duration = duration.ifEmpty { "4 Hours" },
                                        difficulty = "Medium",
                                        pickupLocation = "Marina Grande Port",
                                        featured = false
                                    )
                                )
                                onDismiss()
                                Toast.makeText(context, "Excursion successfully added to catalogue.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(viewModel: DiamondsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var hotel by remember { mutableStateOf("") }
    var passport by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("VIP GUEST PROFILE ENROLLMENT", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Guest Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = { Text("Nationality") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = hotel,
                    onValueChange = { hotel = it },
                    label = { Text("Pickup Luxury Resort") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passport,
                    onValueChange = { passport = it },
                    label = { Text("Passport Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isNotEmpty()) {
                                viewModel.addCustomer(
                                    Customer(
                                        fullName = fullName,
                                        passportNumber = passport.ifEmpty { "N/A" },
                                        nationality = nationality.ifEmpty { "Unknown" },
                                        phoneNumber = "+39 123 4567",
                                        emergencyContact = "+39 987 6543",
                                        pickupHotel = hotel.ifEmpty { "Grand Hotel" },
                                        roomNumber = "N/A",
                                        vipStatus = true
                                    )
                                )
                                onDismiss()
                                Toast.makeText(context, "VIP Guest Registered", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Register", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddVehicleDialog(viewModel: DiamondsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Yacht") }
    var license by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("12") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("ENROLL FLEET VEHICLE / YACHT", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle / Yacht Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = license,
                    onValueChange = { license = it },
                    label = { Text("License Plate / Marine Register ID") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text("Vessel Capacity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.addVehicle(
                                    Vehicle(
                                        name = name,
                                        type = type,
                                        licensePlate = license.ifEmpty { "REG-001" },
                                        fuelLevel = 100,
                                        status = "Available",
                                        lastMaintenance = "2026-07-10",
                                        insuranceExpiry = "2027-07-10",
                                        capacity = capacity.toIntOrNull() ?: 12
                                    )
                                )
                                onDismiss()
                                Toast.makeText(context, "Fleet Asset Enrolled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enroll", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddStaffDialog(viewModel: DiamondsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Guide") }
    var phone by remember { mutableStateOf("") }
    var cert by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("ENROLL CREW / STAFF MEMEBER", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (e.g. Boat Captain, Guide, Driver)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cert,
                    onValueChange = { cert = it },
                    label = { Text("Professional Credentials / License") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium, unfocusedBorderColor = SurfaceGlass, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.addStaff(
                                    Staff(
                                        name = name,
                                        role = role,
                                        phoneNumber = phone.ifEmpty { "+39 000 0000" },
                                        attendanceStatus = "Present",
                                        rating = 5.0f,
                                        performanceScore = 100,
                                        certificateUrl = cert.ifEmpty { "Certified Class License" }
                                    )
                                )
                                onDismiss()
                                Toast.makeText(context, "Crew Enrolled in system.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Crew", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================================================
// DIAMONDS V2 - LUXURY TRAVEL SCREENS & COMPONENTS
// ==================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val experiences by viewModel.experiences.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val favoriteExperienceIds by viewModel.favoriteExperienceIds.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedExpForDetail by remember { mutableStateOf<Experience?>(null) }

    val categories = listOf("All", "Boats", "Speedboats", "Islands", "Limosuine", "Hotels")

    // Destinations mock data
    val mockDestinations = listOf(
        "Capri Island" to "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=400",
        "Amalfi Coast" to "https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?auto=format&fit=crop&q=80&w=400",
        "Positano Pier" to "https://images.unsplash.com/photo-1533900298318-6b8da08a523e?auto=format&fit=crop&q=80&w=400",
        "Sorrento Port" to "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=400"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Travel Destinations Imagery List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "WORLD-FAMOUS DESTINATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(mockDestinations) { (destName, imageUrl) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(130.dp)
                            .height(180.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = destName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                        )
                                    )
                            )
                            Text(
                                text = destName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Selection
        item {
            Column {
                Text(
                    "CHOOSE TRAVEL EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldPremium else SurfaceGlass
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) GoldPremium else SurfaceGlassElevated
                            ),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedCategory = category
                            }
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Popular Experiences Horizontal Carousel
        item {
            Column {
                Text(
                    "POPULAR BOUTIQUE EXPERIENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                val popularExperiences = experiences.filter { it.featured }
                if (popularExperiences.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(SurfaceGlass, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No popular experiences currently available", color = TextSecondary)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(popularExperiences) { exp ->
                            val cardImageUrl = when {
                                exp.title.contains("Yacht") -> "https://images.unsplash.com/photo-1567899378494-47b22a2ae96a?auto=format&fit=crop&q=80&w=600"
                                exp.title.contains("Speedboat") -> "https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&q=80&w=600"
                                exp.title.contains("Helicopter") -> "https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?auto=format&fit=crop&q=80&w=600"
                                else -> "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=600"
                            }
                            
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(180.dp)
                                    .clickable { selectedExpForDetail = exp }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = cardImageUrl,
                                        contentDescription = exp.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Gradient Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                                    startY = 100f
                                                )
                                            )
                                    )
                                    // Popular badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldPremium)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "POPULAR",
                                            color = SlateDarkBg,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    
                                    // Price badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SlateDarkBg.copy(alpha = 0.75f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "€${exp.price.toInt()}",
                                            color = GoldPremium,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Info
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = exp.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "⏱ ${exp.duration}",
                                                color = GoldLight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "👤 Max ${exp.capacity}",
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cruise travel updates card removed as requested

        // Coastal Exploration Interactive Map Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceGlassElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = "Map", tint = GoldPremium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "COASTAL EXPLORATION CHART",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        Text("5 Ports Active", style = MaterialTheme.typography.bodySmall, color = StatusLiveGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Elegant representation of local ports
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BronzeWarm.copy(alpha = 0.3f))
                    ) {
                        // Drawing a fancy path resembling a coastline
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.4f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.2f,
                                    size.width * 0.5f, size.height * 0.8f,
                                    size.width, size.height * 0.5f
                                )
                            }
                            drawPath(
                                path = path,
                                color = GoldPremium.copy(alpha = 0.4f),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            // Draw some glowing dock pins
                            drawCircle(color = GoldPremium, radius = 6.dp.toPx(), center = Offset(size.width * 0.1f, size.height * 0.38f))
                            drawCircle(color = GoldPremium, radius = 6.dp.toPx(), center = Offset(size.width * 0.35f, size.height * 0.45f))
                            drawCircle(color = GoldPremium, radius = 6.dp.toPx(), center = Offset(size.width * 0.65f, size.height * 0.65f))
                            drawCircle(color = GoldPremium, radius = 6.dp.toPx(), center = Offset(size.width * 0.88f, size.height * 0.52f))
                        }
                        
                        // Text Labels floating on ports
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                            Text("Sorrento Pier 4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 100.dp, top = 20.dp)) {
                            Text("Positano Pier", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                            Text("Capri Island Marina", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Text("Amalfi Port", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Featured Excursions Section
        item {
            Text(
                "LUXURY VOYAGES & EXPERIENCES",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Filtering experiences based on search query and category selector
        val filteredExperiences = experiences.filter { exp ->
            val matchesSearch = exp.title.lowercase().contains(searchQuery.lowercase()) ||
                               exp.description.lowercase().contains(searchQuery.lowercase())
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Boats" -> exp.title.lowercase().contains("yacht") || exp.title.lowercase().contains("cruise") || exp.title.lowercase().contains("boat") || exp.title.lowercase().contains("sails") || exp.title.lowercase().contains("vessel")
                "Speedboats" -> exp.title.lowercase().contains("speedboat") || exp.title.lowercase().contains("speed")
                "Islands" -> exp.title.lowercase().contains("island") || exp.title.lowercase().contains("capri") || exp.title.lowercase().contains("ischia") || exp.title.lowercase().contains("procida") || exp.description.lowercase().contains("island")
                "Limosuine" -> exp.title.lowercase().contains("limo") || exp.title.lowercase().contains("chauffeur") || exp.title.lowercase().contains("transfer") || exp.title.lowercase().contains("van") || exp.title.lowercase().contains("car") || exp.title.lowercase().contains("suv") || exp.description.lowercase().contains("chauffeured") || exp.description.lowercase().contains("transfer")
                "Hotels" -> exp.title.lowercase().contains("hotel") || exp.title.lowercase().contains("resort") || exp.title.lowercase().contains("estate") || exp.title.lowercase().contains("vineyard") || exp.title.lowercase().contains("villa") || exp.title.lowercase().contains("stay") || exp.description.lowercase().contains("hotel") || exp.description.lowercase().contains("estate") || exp.description.lowercase().contains("vineyard")
                else -> true
            }
            matchesSearch && matchesCategory
        }

        if (filteredExperiences.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(SurfaceGlass, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No boutique experiences match your search.", color = TextSecondary)
                }
            }
        } else {
            items(filteredExperiences) { exp ->
                ExploreExperienceCard(
                    exp = exp,
                    isFavorite = favoriteExperienceIds.contains(exp.id),
                    onToggleFavorite = { viewModel.toggleFavorite(exp.id) },
                    onSelect = { selectedExpForDetail = exp }
                )
            }
        }



        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (selectedExpForDetail != null) {
        ExcursionDetailSheet(
            exp = selectedExpForDetail!!,
            viewModel = viewModel,
            isFavorite = favoriteExperienceIds.contains(selectedExpForDetail!!.id),
            onToggleFavorite = { viewModel.toggleFavorite(selectedExpForDetail!!.id) },
            onDismiss = { selectedExpForDetail = null }
        )
    }
}

@Composable
fun ExploreExperienceCard(
    exp: Experience,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val imageUrl = when {
        exp.title.contains("Yacht") -> "https://images.unsplash.com/photo-1567899378494-47b22a2ae96a?auto=format&fit=crop&q=80&w=600"
        exp.title.contains("Speedboat") -> "https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&q=80&w=600"
        exp.title.contains("Helicopter") -> "https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?auto=format&fit=crop&q=80&w=600"
        else -> "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=600"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SurfaceGlassElevated),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("experience_card_${exp.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = exp.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Backdrop gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                )

                // Top Floating Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = exp.duration,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Heart Favorite button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Price Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(GoldPremium, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "€${String.format("%,.0f", exp.price)}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = exp.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exp.description,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SurfaceGlassElevated)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = "Capacity",
                            tint = GoldPremium,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Up to ${exp.capacity} Guests",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = StatusAlertAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9 (Elite Rating)",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: DiamondsViewModel) {
    val experiences by viewModel.experiences.collectAsStateWithLifecycle()
    val favoriteExperienceIds by viewModel.favoriteExperienceIds.collectAsStateWithLifecycle()
    var selectedExpForDetail by remember { mutableStateOf<Experience?>(null) }

    val favoritedList = experiences.filter { favoriteExperienceIds.contains(it.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "YOUR SAVED VOYAGES",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Exclusive boutique itineraries you marked as favorites",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (favoritedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "No Favorites",
                            tint = SurfaceGlassElevated,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Your Heart's Desires Await",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Explore and tap the heart icon on your favorite excursions to save them here.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(favoritedList) { exp ->
                ExploreExperienceCard(
                    exp = exp,
                    isFavorite = true,
                    onToggleFavorite = { viewModel.toggleFavorite(exp.id) },
                    onSelect = { selectedExpForDetail = exp }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (selectedExpForDetail != null) {
        ExcursionDetailSheet(
            exp = selectedExpForDetail!!,
            viewModel = viewModel,
            isFavorite = true,
            onToggleFavorite = { viewModel.toggleFavorite(selectedExpForDetail!!.id) },
            onDismiss = { selectedExpForDetail = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcursionDetailSheet(
    exp: Experience,
    viewModel: DiamondsViewModel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showBookingForm by remember { mutableStateOf(false) }

    // Form inputs
    var guestName by remember { mutableStateOf("") }
    var travelDate by remember { mutableStateOf("2026-07-11") }
    var specialRequests by remember { mutableStateOf("") }

    val imageUrl = when {
        exp.title.contains("Yacht") -> "https://images.unsplash.com/photo-1567899378494-47b22a2ae96a?auto=format&fit=crop&q=80&w=600"
        exp.title.contains("Speedboat") -> "https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&q=80&w=600"
        exp.title.contains("Helicopter") -> "https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?auto=format&fit=crop&q=80&w=600"
        else -> "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&q=80&w=600"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SurfaceGlassElevated),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = exp.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else TextMuted
                            )
                        }
                    }

                    Text(
                        text = exp.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "€${String.format("%,.0f", exp.price)} / tour",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPremium
                        )
                        Box(
                            modifier = Modifier
                                .background(GoldLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(exp.duration, color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ABOUT THE EXCURSION", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exp.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = SurfaceGlassElevated)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Features Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CAPACITY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("${exp.capacity} Guests Max", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(exp.difficulty, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PICKUP TERMINAL LOCATION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(exp.pickupLocation, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!showBookingForm) {
                        Button(
                            onClick = { showBookingForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Begin Exclusive Reservation", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkBg, RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, SurfaceGlassElevated), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("EXCLUSIVE BOOKING DETAILS", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = guestName,
                                onValueChange = { guestName = it },
                                label = { Text("Lead Guest Name") },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = travelDate,
                                onValueChange = { travelDate = it },
                                label = { Text("Departure Date (YYYY-MM-DD)") },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = specialRequests,
                                onValueChange = { specialRequests = it },
                                label = { Text("Special VIP Concierge Requests") },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPremium,
                                    unfocusedBorderColor = SurfaceGlassElevated,
                                    focusedContainerColor = SurfaceGlass,
                                    unfocusedContainerColor = SurfaceGlass
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showBookingForm = false }) {
                                    Text("Back", color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (guestName.isNotEmpty()) {
                                            viewModel.addBooking(
                                                Booking(
                                                    customerId = 1,
                                                    customerName = guestName,
                                                    experienceId = exp.id,
                                                    experienceTitle = exp.title,
                                                    date = travelDate,
                                                    timeSlot = "10:00 - 14:00",
                                                    status = "Pending",
                                                    revenue = exp.price,
                                                    ticketQrCode = "QR_EXCLUSIV_${System.currentTimeMillis()}",
                                                    vehicleId = 1,
                                                    staffId = 1,
                                                    notes = specialRequests,
                                                    internalComment = "Registered via Elite Travel App"
                                                )
                                            )
                                            Toast.makeText(context, "VIP Booking Created Successfully!", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Please enter Guest Name", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Confirm Booking", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AdminToolItem(
    val key: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiamondsFloatingAdminMenu(
    viewModel: DiamondsViewModel,
    isVisible: Boolean
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val isAdmin = loggedInUser?.role?.equals("Manager", ignoreCase = true) == true || 
                  loggedInUser?.role?.equals("Admin", ignoreCase = true) == true

    if (!isAdmin) return

    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    // Breathing pulse infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val fabVisibleScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "fab_visible"
    )

    if (fabVisibleScale <= 0.01f) {
        if (isExpanded) {
            isExpanded = false
        }
    }

    // Expandable bottom sheet administrative control panel
    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { isExpanded = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0C0E11), // Deep luxury slate black
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = GoldPremium.copy(alpha = 0.4f)
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Radial golden glow background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldPremium.copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width / 2, size.height * 0.15f),
                                    radius = size.width * 0.8f
                                )
                            )
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = GoldPremium,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DIAMONDS OPERATIONS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPremium,
                                    letterSpacing = 2.sp
                                )
                            }
                            Text(
                                text = "Luxury Fleet & Operations Management Suite",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isExpanded = false
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                                .border(BorderStroke(1.dp, SurfaceGlassElevated), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Panel", tint = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Role Details Info Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SurfaceGlassElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldPremium)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(SlateDarkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = "Admin", tint = GoldPremium, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = loggedInUser?.name ?: "Demo Administrator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "SECURED SYSTEM ACCESS • ROLE: ${loggedInUser?.role?.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldLight,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ADMINISTRATIVE CONTROL PANELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val adminTools = listOf(
                        AdminToolItem("dashboard", Icons.Default.Dashboard, "CRM & Operations", "Real-time KPIs, operational logs & analytical metrics"),
                        AdminToolItem("bookings", Icons.Default.AirplaneTicket, "Bookings Database", "Review, filter and register luxury bookings"),
                        AdminToolItem("experiences", Icons.Default.CardMembership, "Experiences Catalog", "Configure details and launch new tour packages"),
                        AdminToolItem("customers", Icons.Default.People, "VIP Guest Directory", "Direct profiles, hotels and emergency contact database"),
                        AdminToolItem("calendar", Icons.Default.CalendarToday, "Schedules Calendar", "Live departure schedules and captain schedules"),
                        AdminToolItem("fleet", Icons.Default.DirectionsBoat, "Fleet & Yachts", "Yacht health tracking, fuel and maintenance logs"),
                        AdminToolItem("staff", Icons.Default.Badge, "Staff & Crew", "Manage guide attendance, scores and certificates"),
                        AdminToolItem("qr_scan", Icons.Default.QrCodeScanner, "QR Ticket Validator", "Direct verification of client digital booking tickets"),
                        AdminToolItem("reports", Icons.Default.Analytics, "Finance & Revenue", "Financial reports, excursion revenue and trends"),
                        AdminToolItem("settings", Icons.Default.Settings, "Company Settings", "Change active business profile parameters")
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(adminTools) { tool ->
                            val isSelected = currentScreen == tool.key
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) GoldPremium.copy(alpha = 0.12f) else SurfaceGlass
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) GoldPremium else SurfaceGlassElevated
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.currentScreen.value = tool.key
                                        isExpanded = false
                                    }
                                    .testTag("admin_menu_item_${tool.key}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) GoldPremium else SurfaceGlassElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tool.icon,
                                                contentDescription = tool.title,
                                                tint = if (isSelected) SlateDarkBg else GoldPremium,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldPremium)
                                            )
                                        }
                                    }
                                    
                                    Column {
                                        Text(
                                            text = tool.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) GoldPremium else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tool.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Secure exit session
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.performLogout()
                            isExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, StatusErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = StatusErrorRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOGOUT SECURE SESSION", color = StatusErrorRed, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }

    // Floating container Sit Comfortably above the Bottom navigation bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 85.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Animated Breathing FAB
        if (fabVisibleScale > 0.01f) {
            val scale = (if (isExpanded) 1f else pulseScale) * fabVisibleScale
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .testTag("diamonds_admin_fab")
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .drawBehind {
                        val shadowAlpha = if (isExpanded) 0.5f else 0.25f + 0.1f * pulseScale
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldPremium.copy(alpha = shadowAlpha), Color.Transparent),
                                center = center,
                                radius = size.minDimension * 0.75f
                            ),
                            radius = size.minDimension * 0.75f
                        )
                    }
                    .clip(CircleShape)
                    .background(GoldPremium)
                    .border(BorderStroke(2.dp, GoldLight), CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = !isExpanded
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "Diamonds Menu",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun BookingDetailModal(
    booking: Booking,
    customer: Customer?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateDarkBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GoldPremium)
                Text(
                    text = "BOOKING SPECIFICATIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldPremium,
                    letterSpacing = 1.5.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EXCURSION SECTION
                Column {
                    Text(
                        text = "EXCURSION & SCHEDULE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        border = BorderStroke(1.dp, SurfaceGlassElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = booking.experienceTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(booking.date, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(booking.timeSlot, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // GUEST CONTACT SECTION
                Column {
                    Text(
                        text = "GUEST CONTACT & INFO",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        border = BorderStroke(1.dp, SurfaceGlassElevated)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = booking.customerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (customer?.vipStatus == true) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GoldPremium.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("VIP GUEST", color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = SurfaceGlassElevated, thickness = 1.dp)
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Phone: ${customer?.phoneNumber ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Emergency, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency: ${customer?.emergencyContact ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pickup: ${customer?.pickupHotel ?: "N/A"} (Room: ${customer?.roomNumber ?: "N/A"})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Language: ${customer?.language ?: "English"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Passport: ${customer?.passportNumber ?: "N/A"} (${customer?.nationality ?: "N/A"})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }

                // TRANSACTION & NOTES
                Column {
                    Text(
                        text = "FINANCIALS & TRANSACTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        border = BorderStroke(1.dp, SurfaceGlassElevated)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Revenue:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text("€${String.format("%,.2f", booking.revenue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GoldPremium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(booking.status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = when(booking.status) {
                                    "Confirmed", "Paid" -> StatusLiveGreen
                                    "Pending" -> StatusAlertAmber
                                    else -> StatusErrorRed
                                })
                            }
                            if (booking.notes.isNotEmpty()) {
                                HorizontalDivider(color = SurfaceGlassElevated, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Operator Notes:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(booking.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = SlateDarkBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun WeeklyRevenueChartCard(bookings: List<Booking>) {
    val last7Days = listOf(
        "2026-07-08" to "Wed",
        "2026-07-09" to "Thu",
        "2026-07-10" to "Fri",
        "2026-07-11" to "Sat",
        "2026-07-12" to "Sun",
        "2026-07-13" to "Mon",
        "2026-07-14" to "Tue"
    )

    // Compute revenue for each of the 7 days
    val revenues = last7Days.map { (dateStr, _) ->
        bookings.filter { it.date == dateStr && it.status != "Cancelled" }.sumOf { it.revenue }.toFloat()
    }

    val totalWeekRevenue = revenues.sum()
    val maxRevenue = (revenues.maxOrNull() ?: 100f).coerceAtLeast(100f)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SurfaceGlassElevated),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY REVENUE TREND",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "€${String.format("%,.0f", totalWeekRevenue)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldPremium
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusLiveGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+12.4% vs last week",
                        color = StatusLiveGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp, start = 8.dp, end = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (revenues.size - 1)

                    // Draw helper horizontal grid lines (3 lines)
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = height * i / gridLines
                        drawLine(
                            color = SurfaceGlassElevated.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Create connection points for the path
                    val points = revenues.mapIndexed { index, revenue ->
                        val x = index * spacing
                        // Invert y because (0,0) is top-left
                        val normalizedY = if (maxRevenue > 0) revenue / maxRevenue else 0f
                        val y = height - (normalizedY * height * 0.85f) // offset slightly from top
                        Offset(x, y)
                    }

                    // Draw background gradient under the curve
                    if (points.isNotEmpty()) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { point ->
                                lineTo(point.x, point.y)
                            }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GoldPremium.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )
                    }

                    // Draw the primary trend line (smoothed line)
                    if (points.isNotEmpty()) {
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                // Simple bezier connection for smooth rendering
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val controlPoint1 = Offset(p0.x + spacing / 2, p0.y)
                                val controlPoint2 = Offset(p1.x - spacing / 2, p1.y)
                                cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p1.x, p1.y
                                )
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = GoldPremium,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                miter = Stroke.DefaultMiter
                            )
                        )

                        // Draw golden dots and outer halo on each peak
                        points.forEachIndexed { index, point ->
                            drawCircle(
                                color = SlateDarkBg,
                                radius = 5.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = GoldPremium,
                                radius = 3.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }

                // X Axis Day labels at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    last7Days.forEach { (_, dayName) ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
