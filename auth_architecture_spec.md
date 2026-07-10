# DIAMONDS AUTHENTICATION & CREDENTIAL SECURITY
## Multi-Tenant Secure Employee Access Architecture

This document blueprints the secure multi-tenant authentication protocol implemented across the Diamonds mobile app terminals.

---

### 1. SECURITY PROTOCOL LANDSCAPE

```
[ Operator Input ] ---> ( Subdomain Lookup ) ---> [ Supabase Auth Instance ]
                                                        |
                                              ( Trigger OTP Handshake )
                                                        |
                                                        v
[ Biometric Secure Enclave ] <--- ( Encrypted Key ) <--- [ SMS / Email Code ]
```

---

### 2. CORE AUTHENTICATION STAGES

#### Stage A: Tenant Authentication (Company Lookup)
To protect structural separation in a multi-tenant PostgreSQL structure, users must specify their authorized corporate identifier:
1.  **Verification endpoint**: The client queries `/companies?subdomain=eq.{tenant_subdomain}`.
2.  **Handshake**: If authorized, the app locks the primary API headers to the specific company schema and caches the verified `company_id`.

#### Stage B: Passwordless OTP Flow
Passwordless access prevents credential harvesting and handles offline key rotatability:
1.  **Initiation**: Client submits user's secure corporate email address.
2.  **Supabase Auth Handshake**:
    ```js
    const { data, error } = await supabase.auth.signInWithOtp({
      email: 'operator@company.diamonds',
      options: {
        shouldCreateUser: false, // Strict: employees must be enrolled by admins
      }
    });
    ```
3.  **Client Entry**: A 6-character, high-contrast OTP numeric input field with automatic keyboard focus.
4.  **Token Handshake**: Submitting the OTP returns a standard JSON Web Token (JWT), loaded with user claims including `company_id` and employee `role`.

#### Stage C: Local Biometric Enrollment (FaceID / Fingerprint)
To secure active terminal viewports without repeating OTP requests, the app binds cryptographic keys to biometric hardware locks.

*   **Android (BiometricPrompt API)**:
    1.  Uses `BiometricManager` to verify support for `BIOMETRIC_STRONG`.
    2.  Upon first OTP success, a cryptographic signature key (`KeyPair`) is generated inside the hardware **Android Keystore System**.
    3.  The private key is bound to biometric authentication (`setUserAuthenticationRequired(true)`).
    4.  Subsequent logins require a physical hardware verification callback, which signs an authorization nonce to locally refresh security credentials.

*   **iOS (LocalAuthentication Framework)**:
    1.  Queries `LAContext` to evaluate `deviceOwnerAuthenticationWithBiometrics`.
    2.  Reads securely stored JWT directly from the **Keychain Services**, protected by the `kSecAttrAccessControl` attribute with `userPresence` validation.
