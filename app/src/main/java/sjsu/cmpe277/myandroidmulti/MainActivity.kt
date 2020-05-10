package sjsu.cmpe277.myandroidmulti

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.findNavController //imported from androidx.navigation:navigation-ui-ktx
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.nav_header.view.*
import sjsu.cmpe277.myandroidmulti.Firebase.LoginViewModel
import sjsu.cmpe277.myandroidmulti.databinding.ActivityMainBinding


const val KEY_Data = "inputname_key"

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var headerView: View

    // Get a reference to the ViewModel scoped to this Fragment
    private val loginviewModel by viewModels<LoginViewModel>()

    companion object {
        const val TAG = "MainActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

//        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
//        navController = Navigation.findNavController(this, R.id.myNavHostFragment)
//        NavigationUI.setupActionBarWithNavController(this,navController)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        drawerLayout = binding.drawerLayout

        //navController = findNavController(R.id.myNavHostFragment)

        navController = Navigation.findNavController(this, R.id.myNavHostFragment)
        NavigationUI.setupActionBarWithNavController(this,navController,drawerLayout)


        NavigationUI.setupWithNavController(binding.navView, navController)

        Log.i("MainActivity", "onCreate Called")

        //Access the header view of the navigation drawer, show account information and signin/logout button
        headerView = binding.navView.getHeaderView(0)
        //headerView.navheadertextView.text = "No Account"
        observeAuthenticationState()//change the header view data and status based on authentication state
        headerView.loginbutton.setOnClickListener{launchSignInFlow()}

        if (savedInstanceState != null) {
            var restoredstr = savedInstanceState.getString(KEY_Data, "").toString()
            Log.i("MainActivity", restoredstr)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("MainActivity", "onStart Called")
    }

//    override fun onSupportNavigateUp(): Boolean {
//        navController = Navigation.findNavController(this, R.id.myNavHostFragment)
//        return navController.navigateUp(navController)
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        Log.i("MainActivity", "onSaveInstanceState Called")

        outState.putString(KEY_Data, "test save name")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.i("MainActivity", "onRestoreInstanceState Called")
        if (savedInstanceState != null) {
            var restoredstr = savedInstanceState.getString(KEY_Data, "").toString()
            Log.i("MainActivity", "onRestoreInstanceState not null")
        }
    }

    //SignIn function
    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            ).build(), SIGN_IN_RESULT_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                Log.i(
                    TAG,
                    "Successfully signed in user " +
                            "${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is a logged in user: (1) show a logout button and (2) display their name.
     * If there is no logged in user: show a login button
     */
    private fun observeAuthenticationState() {

        loginviewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    headerView.navheadertextView.text = loginviewModel.getUserInfo()
                    headerView.loginbutton.text = "SignOut"
                    headerView.loginbutton.setOnClickListener{
                        AuthUI.getInstance().signOut(this).addOnCompleteListener {
                            Log.i(TAG, "Sign out successful")
                        }
                    }
                }
                else -> {
                    headerView.navheadertextView.text = "No Account"
                    headerView.loginbutton.text = "SignIn"
                    headerView.loginbutton.setOnClickListener{
                        launchSignInFlow()
                    }
                }
            }
        })
    }

}
