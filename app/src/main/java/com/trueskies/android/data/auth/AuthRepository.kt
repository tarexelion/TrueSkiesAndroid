package com.trueskies.android.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.trueskies.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?
)

sealed class AuthState {
    data object NotAuthenticated : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: Auth
) {
    private val credentialManager = CredentialManager.create(context)

    val authState: Flow<AuthState> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                AuthState.Authenticated(user.toAuthUser())
            }
            is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            is SessionStatus.Initializing -> AuthState.Loading
            else -> AuthState.NotAuthenticated
        }
    }

    val currentUser: AuthUser?
        get() = auth.currentUserOrNull()?.toAuthUser()

    val isSignedIn: Boolean
        get() = auth.currentUserOrNull() != null

    /**
     * Sign in with Google using Credential Manager + Supabase IDToken flow.
     * Must be called from an Activity context.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> {
        return try {
            // Build the Google ID token request
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Show the Google account picker
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdToken.idToken

            // Exchange the Google ID token with Supabase
            auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }

            val user = auth.currentUserOrNull()?.toAuthUser()
                ?: return Result.failure(Exception("Sign-in succeeded but no user returned"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {
            // Ignore sign-out errors
        }
    }

    private fun UserInfo?.toAuthUser(): AuthUser {
        val metadata = this?.userMetadata
        return AuthUser(
            id = this?.id ?: "",
            email = this?.email,
            displayName = metadata?.get("full_name")?.toString()?.removeSurrounding("\"")
                ?: metadata?.get("name")?.toString()?.removeSurrounding("\""),
            avatarUrl = metadata?.get("avatar_url")?.toString()?.removeSurrounding("\"")
                ?: metadata?.get("picture")?.toString()?.removeSurrounding("\"")
        )
    }
}
