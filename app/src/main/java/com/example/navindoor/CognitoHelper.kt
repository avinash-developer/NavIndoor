package com.example.navindoor

import android.content.Context
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.regions.Regions

object CognitoHelper {
    private lateinit var cognitoUserPool: CognitoUserPool

    fun initialize(context: Context) {
        cognitoUserPool = CognitoUserPool(
            context,
            "us-east-1_Wr28jXjWd",
            "53qb92l01h3tfpn3jnm586abs0",
            "1pdrrfjm0oham0ljhhgcem9c8jk5314mq45jc05c9iqackkfo7k8",
            Regions.US_EAST_1
        )
    }

    fun register(
        username: String,
        password: String,
        attributes: Map<String, String>,
        callback: SignUpHandler
    ) {
        val userAttributes = CognitoUserAttributes()
        for ((key, value) in attributes) {
            userAttributes.addAttribute(key, value)
        }
        cognitoUserPool.signUpInBackground(username, password, userAttributes, null, callback)
    }

    fun authenticate(email: String, password: String, callback: AuthenticationHandler) {
        val user = cognitoUserPool.getUser(email)
        user.getSessionInBackground(callback)
    }


}
