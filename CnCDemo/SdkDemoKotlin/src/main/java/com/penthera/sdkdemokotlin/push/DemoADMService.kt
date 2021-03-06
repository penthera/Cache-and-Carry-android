//  Copyright (c) 2017 Penthera Partners, LLC. All rights reserved.
//
// PENTHERA CONFIDENTIAL
//
// (c) 2013 Penthera Partners Inc. All Rights Reserved.
//
// NOTICE: This file is the property of Penthera Partners Inc.
// The concepts contained herein are proprietary to Penthera Partners Inc.
// and may be covered by U.S. and/or foreign patents and/or patent
// applications, and are protected by trade secret or copyright law.
// Distributing and/or reproducing this information is forbidden
// unless prior written permission is obtained from Penthera Partners Inc.
//
package com.penthera.sdkdemokotlin.push

import android.content.Intent
import android.util.Log
import com.penthera.virtuososdk.client.push.ADMService

/**
 * Demonstrates how to subclass an ADM service and intercept tokens and messages.
 * <BR></BR><BR></BR>
 * When subclassing ADMService remember to include the amazon-device-messaging jar file in the libs
 * folder of the project and to add provided files('libs/amazon-device-messaging-1.0.1.jar') to the
 * dependencies in build.gradle
 */
class DemoADMService : ADMService() {
    override fun onMessage(intent: Intent) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onMessage")
        super.onMessage(intent)
    }

    override fun onRegistrationError(s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onRegistrationError")
        super.onRegistrationError(s)
    }

    override fun onRegistered(s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onRegistered")
        super.onRegistered(s)
    }

    override fun onUnregistered(s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onUnregistered")
        super.onUnregistered(s)
    }

    companion object {
        private val TAG = DemoADMService::class.java.name
    }
}