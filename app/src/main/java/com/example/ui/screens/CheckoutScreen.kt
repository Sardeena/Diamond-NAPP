package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Booking
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import com.example.ui.viewmodel.DiamondsViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(viewModel: DiamondsViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pendingBooking by viewModel.pendingCheckoutBooking.collectAsStateWithLifecycle()

    if (pendingBooking == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Empty Checkout",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Excursion in Checkout",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.currentScreen.value = "explore" },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Explore Excursions", color = Color.White)
                }
            }
        }
        return
    }

    val booking = pendingBooking!!
    val basePrice = booking.revenue

    // Promo Code States
    var promoCode by remember { mutableStateOf("") }
    var appliedDiscountPercent by remember { mutableStateOf(0) }
    var promoError by remember { mutableStateOf("") }
    var promoSuccess by remember { mutableStateOf("") }

    // Billing & Card States
    var fullName by remember { mutableStateOf(booking.customerName) }
    var email by remember { mutableStateOf("client@diamonds.com") }
    var phone by remember { mutableStateOf("+39 342 9011") }
    
    var selectedPaymentTab by remember { mutableStateOf(0) } // 0: Card, 1: Yacht Account, 2: Bitcoin
    
    // Credit Card Fields
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    
    // Crypto Wallet / Yacht ID Field
    var yachtClubId by remember { mutableStateOf("") }
    var walletAddress by remember { mutableStateOf("") }

    // Stepper State (0: Billing & Review, 1: Secure Payment & Auth, 2: Completion Receipt)
    var currentStep by remember { mutableStateOf(0) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var secureOtp by remember { mutableStateOf("") }
    var secureOtpError by remember { mutableStateOf("") }
    
    // Computed Values
    val discount = basePrice * appliedDiscountPercent / 100
    val luxurySurcharge = basePrice * 0.08 // 8% VIP Concierge Service Fee
    val taxes = (basePrice - discount + luxurySurcharge) * 0.12 // 12% Local Yacht Tax
    val finalTotal = basePrice - discount + luxurySurcharge + taxes
    
    val decFormat = DecimalFormat("€#,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "VIP SECURE CHECKOUT", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ) 
                },
                navigationIcon = {
                    if (currentStep < 2) {
                        IconButton(onClick = {
                            if (currentStep > 0) {
                                currentStep--
                            } else {
                                viewModel.currentScreen.value = "explore"
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceGlass)
            )
        },
        containerColor = SlateDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // STEP PROGRESS INDICATOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(label = "Review", isActive = currentStep >= 0, isCompleted = currentStep > 0)
                StepLine(isCompleted = currentStep > 0)
                StepIndicator(label = "Payment", isActive = currentStep >= 1, isCompleted = currentStep > 1)
                StepLine(isCompleted = currentStep > 1)
                StepIndicator(label = "Success", isActive = currentStep >= 2, isCompleted = currentStep > 2)
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "CheckoutStepTransition"
            ) { step ->
                when (step) {
                    0 -> {
                        // STEP 0: EXCURSION REVIEW & BILLING
                        Column {
                            // Booking Summary Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(GoldLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsBoat,
                                                contentDescription = "Boat",
                                                tint = GoldPremium,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = booking.experienceTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Scheduled: ${booking.date}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = SurfaceGlassElevated.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Invoice breakdown
                                    PriceRow(label = "VIP Pass Rate", value = decFormat.format(basePrice))
                                    PriceRow(label = "8% Concierge Service Fee", value = decFormat.format(luxurySurcharge))
                                    if (appliedDiscountPercent > 0) {
                                        PriceRow(
                                            label = "Promo Discount ($appliedDiscountPercent%)", 
                                            value = "-${decFormat.format(discount)}", 
                                            color = StatusLiveGreen
                                        )
                                    }
                                    PriceRow(label = "12% Sovereign VAT / Taxes", value = decFormat.format(taxes))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = SurfaceGlassElevated)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Guaranteed Total", 
                                            fontWeight = FontWeight.ExtraBold, 
                                            color = TextPrimary,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            decFormat.format(finalTotal), 
                                            fontWeight = FontWeight.ExtraBold, 
                                            color = GoldPremium,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Promo Code Section
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "EXCLUSIVE PROMO CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = promoCode,
                                            onValueChange = { 
                                                promoCode = it
                                                promoError = ""
                                                promoSuccess = ""
                                            },
                                            placeholder = { Text("Enter Code (e.g. VIP20, ELITE10)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GoldPremium,
                                                unfocusedBorderColor = SurfaceGlassElevated,
                                                focusedContainerColor = SlateDarkBg,
                                                unfocusedContainerColor = SlateDarkBg
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Button(
                                            onClick = {
                                                val uc = promoCode.trim().uppercase()
                                                if (uc == "VIP20") {
                                                    appliedDiscountPercent = 20
                                                    promoSuccess = "VIP20 applied! Enjoy 20% elite discount."
                                                    promoError = ""
                                                } else if (uc == "ELITE10") {
                                                    appliedDiscountPercent = 10
                                                    promoSuccess = "ELITE10 applied! Enjoy 10% premium discount."
                                                    promoError = ""
                                                } else if (uc.isEmpty()) {
                                                    promoError = "Please input a code."
                                                } else {
                                                    promoError = "Invalid prestige promotion code."
                                                    appliedDiscountPercent = 0
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Apply", color = Color.White)
                                        }
                                    }
                                    if (promoError.isNotEmpty()) {
                                        Text(promoError, color = StatusErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                    }
                                    if (promoSuccess.isNotEmpty()) {
                                        Text(promoSuccess, color = StatusLiveGreen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Lead Guest Billing Details
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "BILLING & REGISTRATION CONTACT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("Lead Guest Full Name") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPremium,
                                            unfocusedBorderColor = SurfaceGlassElevated
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Billing Corporate Email") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPremium,
                                            unfocusedBorderColor = SurfaceGlassElevated
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Billing Contact Phone") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPremium,
                                            unfocusedBorderColor = SurfaceGlassElevated
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                                        Toast.makeText(context, "Please fill in all contact details", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentStep = 1
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("checkout_step_1_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Proceed to Secure Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                                }
                            }
                        }
                    }
                    1 -> {
                        // STEP 1: PAYMENT CHANNELS & SECURITY OTP
                        Column {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "SELECT LUXURY PAYMENT METHOD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Custom payment tabs
                                    TabRow(
                                        selectedTabIndex = selectedPaymentTab,
                                        containerColor = SlateDarkBg,
                                        contentColor = GoldPremium,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    ) {
                                        Tab(
                                            selected = selectedPaymentTab == 0,
                                            onClick = { selectedPaymentTab = 0 },
                                            text = { Text("Credit Card", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        )
                                        Tab(
                                            selected = selectedPaymentTab == 1,
                                            onClick = { selectedPaymentTab = 1 },
                                            text = { Text("Yacht Account", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        )
                                        Tab(
                                            selected = selectedPaymentTab == 2,
                                            onClick = { selectedPaymentTab = 2 },
                                            text = { Text("Prestige Crypto", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    when (selectedPaymentTab) {
                                        0 -> {
                                            // Credit Card Form
                                            OutlinedTextField(
                                                value = cardNumber,
                                                onValueChange = { cardNumber = it.take(16) },
                                                label = { Text("16-Digit Premium Card Number") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Card", tint = GoldPremium) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedTextField(
                                                    value = cardExpiry,
                                                    onValueChange = { cardExpiry = it.take(5) },
                                                    label = { Text("Expiry (MM/YY)") },
                                                    placeholder = { Text("12/28") },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                OutlinedTextField(
                                                    value = cardCvv,
                                                    onValueChange = { cardCvv = it.take(4) },
                                                    label = { Text("CVV/CVN") },
                                                    placeholder = { Text("902") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                        1 -> {
                                            // Yacht Club Account
                                            OutlinedTextField(
                                                value = yachtClubId,
                                                onValueChange = { yachtClubId = it },
                                                label = { Text("Prestige Yacht Club Member ID") },
                                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Yacht Account", tint = GoldPremium) },
                                                placeholder = { Text("YCHT-8902-VIP") },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Payment will be securely charged to your corporate ledger account or yacht berth room folio.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        2 -> {
                                            // Crypto Wallet
                                            OutlinedTextField(
                                                value = walletAddress,
                                                onValueChange = { walletAddress = it },
                                                label = { Text("Prestige Crypto / Web3 Wallet Address") },
                                                leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = "Wallet", tint = GoldPremium) },
                                                placeholder = { Text("0x71C765... elite signature") },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Secure transaction protocol supporting BTC, ETH, USDC or luxury yacht credits.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3D SECURE OTP SIMULATION
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = "Shield", tint = StatusLiveGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "3D SECURE ENCRYPTED AUTHENTICATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusLiveGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "For high-value luxury transactions, a one-time verification passcode is required. Enter the premium confirmation code sent to your registered device (Use '1234' for simulator bypass).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = secureOtp,
                                        onValueChange = { 
                                            secureOtp = it.take(4)
                                            secureOtpError = ""
                                        },
                                        label = { Text("One-Time Passcode (OTP)") },
                                        placeholder = { Text("e.g. 1234") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPremium,
                                            unfocusedBorderColor = SurfaceGlassElevated
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    if (secureOtpError.isNotEmpty()) {
                                        Text(
                                            secureOtpError, 
                                            color = StatusErrorRed, 
                                            style = MaterialTheme.typography.bodySmall, 
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isProcessingPayment) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GoldPremium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Encrypting high-value ledger & executing payment...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val isCardInvalid = selectedPaymentTab == 0 && (cardNumber.length < 16 || cardExpiry.isEmpty() || cardCvv.isEmpty())
                                        val isYachtInvalid = selectedPaymentTab == 1 && yachtClubId.isEmpty()
                                        val isCryptoInvalid = selectedPaymentTab == 2 && walletAddress.isEmpty()

                                        if (isCardInvalid) {
                                            Toast.makeText(context, "Please complete Card fields", Toast.LENGTH_SHORT).show()
                                        } else if (isYachtInvalid) {
                                            Toast.makeText(context, "Please enter Yacht Member ID", Toast.LENGTH_SHORT).show()
                                        } else if (isCryptoInvalid) {
                                            Toast.makeText(context, "Please enter Wallet Address", Toast.LENGTH_SHORT).show()
                                        } else if (secureOtp != "1234" && secureOtp != "0000") {
                                            secureOtpError = "Invalid verification code. Enter '1234' to bypass."
                                        } else {
                                            // Process Payment Simulation
                                            isProcessingPayment = true
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1800) // Artificial luxury delay
                                                isProcessingPayment = false
                                                
                                                // Create official Booking
                                                val finalBooking = booking.copy(
                                                    customerName = fullName,
                                                    notes = if (booking.notes.isNotEmpty()) "${booking.notes} | Paid via ${if(selectedPaymentTab==0) "Card" else if(selectedPaymentTab==1) "Berth Folio" else "Web3"}" else "Paid via ${if(selectedPaymentTab==0) "Card" else if(selectedPaymentTab==1) "Berth Folio" else "Web3"}",
                                                    revenue = finalTotal,
                                                    status = "Confirmed",
                                                    internalComment = "Checkout Authorized and Settled via Secure Payment Gateway"
                                                )
                                                
                                                viewModel.addBooking(finalBooking)
                                                viewModel.syncDataWithSupabase()
                                                
                                                currentStep = 2
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("checkout_pay_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Authorize & Pay ${decFormat.format(finalTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: COMPLETION RECEIPT SCREEN
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(40.dp))
                                    .background(StatusLiveGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Paid Successful",
                                    tint = StatusLiveGreen,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "TRANSACTION SETTLED",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusLiveGreen,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                text = "Thank you for booking with Diamonds Elite Yachting Group",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Ticket Style Bill Receipt Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                border = BorderStroke(1.dp, SurfaceGlassElevated),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "OFFICIAL TICKET RECEIPT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = SurfaceGlassElevated)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    ReceiptItem(label = "Excursion", value = booking.experienceTitle)
                                    ReceiptItem(label = "Lead Guest", value = fullName)
                                    ReceiptItem(label = "Departure Date", value = booking.date)
                                    ReceiptItem(label = "Status", value = "Confirmed & Settled")
                                    ReceiptItem(
                                        label = "Secure Ledger Ref", 
                                        value = "DS-TX-${System.currentTimeMillis().toString().takeLast(6)}"
                                    )
                                    ReceiptItem(label = "Amount Charged", value = decFormat.format(finalTotal), isBoldVal = true)

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = SurfaceGlassElevated.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Simulating ticket QR Code
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SlateDarkBg)
                                                .border(BorderStroke(1.dp, SurfaceGlassElevated), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Mimic QR pattern inside Box
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .drawBehind {
                                                        val pSize = this.size.width / 6
                                                        // Top Left anchor
                                                        drawRect(Color.Black, size = Size(pSize*2, pSize*2))
                                                        drawRect(Color.White, topLeft = Offset(pSize*0.5f, pSize*0.5f), size = Size(pSize, pSize))
                                                        
                                                        // Top Right anchor
                                                        drawRect(Color.Black, topLeft = Offset(this.size.width - pSize*2, 0f), size = Size(pSize*2, pSize*2))
                                                        drawRect(Color.White, topLeft = Offset(this.size.width - pSize*1.5f, pSize*0.5f), size = Size(pSize, pSize))

                                                        // Bottom Left anchor
                                                        drawRect(Color.Black, topLeft = Offset(0f, this.size.height - pSize*2), size = Size(pSize*2, pSize*2))
                                                        drawRect(Color.White, topLeft = Offset(pSize*0.5f, this.size.height - pSize*1.5f), size = Size(pSize, pSize))

                                                        // Some random luxury purple and slate QR bits
                                                        for(i in 1..8) {
                                                            val rx = (2..5).random() * pSize
                                                            val ry = (2..5).random() * pSize
                                                            drawRect(if (i%2==0) Color(0xFF6750A4) else Color.Black, topLeft = Offset(rx, ry), size = Size(pSize*0.7f, pSize*0.7f))
                                                        }
                                                    }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Secure Excursion Boarding Pass QR",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.pendingCheckoutBooking.value = null
                                    viewModel.currentScreen.value = "bookings"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("checkout_finish_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View Enrolled Bookings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(label: String, isActive: Boolean, isCompleted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isCompleted) StatusLiveGreen 
                    else if (isActive) GoldPremium 
                    else SurfaceGlassElevated
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = when(label) {
                        "Review" -> "1"
                        "Payment" -> "2"
                        else -> "3"
                    },
                    color = if (isActive) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold,
            color = if (isActive) GoldPremium else TextMuted
        )
    }
}

@Composable
fun RowScope.StepLine(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .padding(horizontal = 8.dp)
            .background(if (isCompleted) StatusLiveGreen else SurfaceGlassElevated)
    )
}

@Composable
fun PriceRow(label: String, value: String, color: Color = TextSecondary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReceiptItem(label: String, value: String, isBoldVal: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium, 
            color = if (isBoldVal) GoldPremium else TextPrimary,
            fontWeight = if (isBoldVal || label == "Status") FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
