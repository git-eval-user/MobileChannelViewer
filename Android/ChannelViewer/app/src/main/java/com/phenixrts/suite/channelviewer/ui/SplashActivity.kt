/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelviewer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.phenixrts.suite.channelviewer.ChannelViewerApplication
import com.phenixrts.suite.channelviewer.R
import com.phenixrts.suite.channelviewer.common.enums.ExpressError
import com.phenixrts.suite.channelviewer.common.lazyViewModel
import com.phenixrts.suite.channelviewer.common.showErrorDialog
import com.phenixrts.suite.channelviewer.common.showSnackBar
import com.phenixrts.suite.channelviewer.databinding.ActivitySplashBinding
import com.phenixrts.suite.channelviewer.repositories.ChannelExpressRepository
import com.phenixrts.suite.channelviewer.ui.viewmodel.ChannelViewModel
import com.phenixrts.suite.phenixcommon.common.launchMain
import com.phenixrts.suite.phenixdeeplink.DeepLinkActivity
import com.phenixrts.suite.phenixdeeplink.DeepLinkStatus
import com.phenixrts.suite.phenixdeeplink.common.asConfigurationModel
import timber.log.Timber
import javax.inject.Inject

private const val TIMEOUT_DELAY = 10000L

class SplashActivity : DeepLinkActivity() {

    @Inject lateinit var channelExpress: ChannelExpressRepository
    private lateinit var binding: ActivitySplashBinding

    private val viewModel: ChannelViewModel by lazyViewModel({ application as ChannelViewerApplication }) {
        ChannelViewModel(channelExpress)
    }

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        launchMain {
            binding.root.showSnackBar(getString(R.string.err_network_problems))
        }
    }

    override val additionalConfiguration: HashMap<String, String>
        get() = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        ChannelViewerApplication.component.inject(this)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launchMain {
            channelExpress.onChannelExpressError.collect { error ->
                Timber.d("Room express failed")
                showErrorDialog(error)
            }
        }

        Timber.d("Splash activity created")
        super.onCreate(savedInstanceState)
    }

    override fun onDeepLinkQueried(status: DeepLinkStatus) {
        launchMain {
            when (status) {
                DeepLinkStatus.RELOAD -> showErrorDialog(ExpressError.CONFIGURATION_CHANGED_ERROR)
                DeepLinkStatus.READY -> showLandingScreen()
            }
        }
    }

    override fun isAlreadyInitialized(): Boolean = channelExpress.isRoomExpressInitialized()

    private suspend fun showLandingScreen() {
        var config = configuration.asConfigurationModel()

        if (config == null || config.edgeToken.isNullOrBlank()) {
            showErrorDialog(ExpressError.DEEP_LINK_ERROR)
            return
        }

        if (config.authToken.isNullOrBlank()) {
            config = config.copy(authToken = config.edgeToken)
        }

        Timber.d("Initializing ChannelExpress with configuration ${config}")

        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
        channelExpress.setupChannelExpress(config)
        channelExpress.waitForPCast()
        timeoutHandler.removeCallbacks(timeoutRunnable)

        Timber.d("Navigating to Landing Screen")
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
