package sjsu.cmpe277.myandroidmulti.Firebase

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.random.Random

class LoginViewModel : ViewModel() {
    //private val firebaseAuth = FirebaseAuth.getInstance()

//    val currentuser = FirebaseUserLiveData().map { user ->
//        if (user != null) {
//            user
//        } else {
//            user
//        }
//    }


    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            updateuserToServer()
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    private fun updateuserToServer() {
        // TODO: Implement this method to send token to your app server

    }

    fun getUserInfo(): String {
        val currentuser = FirebaseAuth.getInstance().currentUser//FirebaseUserLiveData().firebaseAuth.currentUser
        val name = currentuser?.displayName
        val email = currentuser?.email
        val photoUrl = currentuser?.photoUrl

        // Check if user's email is verified
        val emailVerified = currentuser?.isEmailVerified

        // The user's ID, unique to the Firebase project. Do NOT use this value to
        // authenticate with your backend server, if you have one. Use
        // FirebaseUser.getToken() instead.
        val uid = currentuser?.uid

        val userinfo = "Username: ${name} \n ${email} "

        Log.i(
            "MainActivity",
            "LoginViewModel User info, name: ${name}; email: ${email} ${emailVerified}; photoUrl: ${photoUrl}; uid:${uid} ."
        )
        return userinfo
    }

}