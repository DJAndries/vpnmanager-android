package ca.andries.vpnmanager

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import ca.andries.vpnmanager.ProfileConfigActivity.Companion.PROFILE_KEY
import ca.andries.vpnmanager.databinding.ActivityMainBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionWrangler: PermissionWrangler
    private val profileViewModel: ProfileViewModel by viewModels()

    private val profileConfigLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            profileViewModel.saveProfile(Json.decodeFromString(result.data?.getStringExtra(PROFILE_KEY)!!), null)
            MainService.reloadFromActivity(this)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        requestNextPermission(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { _ ->
            profileConfigLauncher.launch(Intent(this, ProfileConfigActivity::class.java))
        }

        permissionWrangler = PermissionWrangler {
            startService(Intent(this, MainService::class.java))
        }
        permissionWrangler.startPermissionCheck(this, requestPermissionLauncher)

//        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        cm.registerNetworkCallback(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network);
//                Log.d(javaClass.name, "ahahaghahahahaha")
//                Toast.makeText(applicationContext, "Test", Toast.LENGTH_LONG).show()
//            }
//        })
//        val filter = IntentFilter()
//        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
//        registerReceiver(NetworkChangeRecevier(), filter)
//
//        val job = JobInfo.Builder(0, ComponentName(this, NetworkChangeService::class.java))
//            .setMinimumLatency(1000)
//            .setOverrideDeadline(3000)
//            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//            .setPersisted(true)
//            .build()
//
//        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler;
//        scheduler.schedule(job)

        val wifiNet = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo.ssid
        val wifiNetBssid = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo.bssid
        Log.d(javaClass.name, "Avail! wifi: $wifiNet $wifiNetBssid")
    }

    private fun requestNextPermission(isGranted: Boolean) {
        permissionWrangler.requestNextPermission(this, requestPermissionLauncher, isGranted)
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.load()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}