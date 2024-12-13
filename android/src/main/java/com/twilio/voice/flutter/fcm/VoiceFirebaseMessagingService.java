package com.twilio.voice.flutter.fcm;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import com.twilio.voice.flutter.IncomingCallNotificationService;
import com.twilio.voice.flutter.Utils.TwilioConstants;

public class VoiceFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";
    public static final String ACTION_TOKEN = "com.twilio.voice.flutter.NEW_FCM_TOKEN";
    public static final String EXTRA_TOKEN = "token";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        Intent tokenIntent = new Intent(ACTION_TOKEN);
        tokenIntent.putExtra(EXTRA_TOKEN, token);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(tokenIntent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Received FCM message: " + remoteMessage.getData());

        if (remoteMessage.getData().isEmpty()) {
            Log.w(TAG, "Received empty data payload");
            return;
        }

        try {
            boolean validMessage = Voice.handleMessage(this, remoteMessage.getData(), new MessageListener() {
                @Override
                public void onCallInvite(@NonNull CallInvite callInvite) {
                    handleIncomingCall(callInvite);
                }

                @Override
                public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite,
                        @Nullable CallException callException) {
                    handleCancelledCall(cancelledCallInvite);
                }
            });

            if (!validMessage) {
                Log.e(TAG, "Received invalid Twilio Voice payload");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling FCM message", e);
        }
    }

    private void handleIncomingCall(@NonNull CallInvite callInvite) {
        Log.d(TAG, "Received incoming call invite: " + callInvite.getCallSid());
        Intent intent = new Intent(this, IncomingCallNotificationService.class)
                .setAction(TwilioConstants.ACTION_INCOMING_CALL)
                .putExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE, callInvite);
        startService(intent);
    }

    private void handleCancelledCall(@NonNull CancelledCallInvite cancelledCallInvite) {
        Log.d(TAG, "Received cancelled call invite: " + cancelledCallInvite.getCallSid());
        Intent intent = new Intent(this, IncomingCallNotificationService.class)
                .setAction(TwilioConstants.ACTION_CANCEL_CALL)
                .putExtra(TwilioConstants.EXTRA_CANCELLED_CALL_INVITE, cancelledCallInvite);
        startService(intent);
    }
}