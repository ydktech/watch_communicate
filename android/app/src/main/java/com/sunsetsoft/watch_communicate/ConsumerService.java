/*
 * Copyright (c) 2016 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
 * the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer. 
 *     * Redistributions in binary form must reproduce the above copyright notice, 
 *       this list of conditions and the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution. 
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or 
 *       promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.sunsetsoft.watch_communicate;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAMessage;
import com.samsung.android.sdk.accessory.SAPeerAgent;

import java.io.IOException;

public class ConsumerService extends SAAgentV2 {
    private static final String TAG = "HelloMessage(C)";

    private Handler mHandler = new Handler();
    private SAMessage mMessage = null;
    private SAPeerAgent mSAPeerAgent = null;
    private Toast mToast;
    private Context mContext;

    public ConsumerService(Context context) {
        super(TAG, context);

        mContext = context;
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(mContext);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e)) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
        }

        mMessage = new SAMessage(this) {

            @Override
            protected void onSent(SAPeerAgent peerAgent, int id) {

                Log.d(TAG, "onSent(), id: " + id + ", ToAgent: " + peerAgent.getPeerId());
                String val = "" + id + " SUCCESS ";
                displayToast("ACK Received: " + val, Toast.LENGTH_SHORT);
            }

            @Override
            protected void onError(SAPeerAgent peerAgent, int id, int errorCode) {

                Log.d(TAG, "onError(), id: " + id + ", ToAgent: " + peerAgent.getPeerId() + ", errorCode: " + errorCode);
                String result = null;
                switch (errorCode) {
                    case ERROR_PEER_AGENT_UNREACHABLE:
                        result = " FAILURE" + "[ " + errorCode + " ] : PEER_AGENT_UNREACHABLE ";
                        break;
                    case ERROR_PEER_AGENT_NO_RESPONSE:
                        result = " FAILURE" + "[ " + errorCode + " ] : PEER_AGENT_NO_RESPONSE ";
                        break;
                    case ERROR_PEER_AGENT_NOT_SUPPORTED:
                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_PEER_AGENT_NOT_SUPPORTED ";
                        break;
                    case ERROR_PEER_SERVICE_NOT_SUPPORTED:
                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_PEER_SERVICE_NOT_SUPPORTED ";
                        break;
                    case ERROR_SERVICE_NOT_SUPPORTED:
                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_SERVICE_NOT_SUPPORTED ";
                        break;
                    case ERROR_UNKNOWN:
                        result = " FAILURE" + "[ " + errorCode + " ] : UNKNOWN ";
                        break;
                }
                String val = "" + id + result;
                displayToast("NAK Received: " + val, Toast.LENGTH_SHORT);
                MainActivity.updateButtonState(false);
            }

            @Override
            protected void onReceive(SAPeerAgent peerAgent, byte[] message) {
                String dataVal = new String(message);
                addMessage("Received: ", dataVal);
                MainActivity.updateButtonState(false);
            }
        };
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if ((result == PEER_AGENT_FOUND) && (peerAgents != null)) {
            updateTextView("PEERAGENT_FOUND");
            displayToast("PEERAGENT_FOUND", Toast.LENGTH_LONG);
            for(SAPeerAgent peerAgent:peerAgents) {
                mSAPeerAgent = peerAgent;
            }
            return;
        } else if (result == FINDPEER_DEVICE_NOT_CONNECTED) {
            displayToast("FINDPEER_DEVICE_NOT_CONNECTED", Toast.LENGTH_LONG);
        } else if (result == FINDPEER_SERVICE_NOT_FOUND) {
            displayToast("FINDPEER_SERVICE_NOT_FOUND", Toast.LENGTH_LONG);
        }
        updateTextView("PEERAGENT_NOT_FOUND");
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (peers != null) {
                    if (status == PEER_AGENT_AVAILABLE) {
                        displayToast("PEER_AGENT_AVAILABLE", Toast.LENGTH_LONG);
                    } else {
                        displayToast("PEER_AGENT_UNAVAILABLE", Toast.LENGTH_LONG);
                    }
                }
            }
        });
    }

    public void findPeers() {
        findPeerAgents();
    }

    int sendData(String message) {
        int tid;

        if(mSAPeerAgent == null) {
            displayToast("Try to find PeerAgent!", Toast.LENGTH_SHORT);
            return -1;
        }
        if (mMessage != null) {
            try {
                tid = mMessage.send(mSAPeerAgent, message.getBytes());
                addMessage("Sent: ", message);
                return tid;
            } catch (IOException e) {
                e.printStackTrace();
                displayToast(e.getMessage(), Toast.LENGTH_SHORT);
                return -1;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                displayToast(e.getMessage(), Toast.LENGTH_SHORT);
                return -1;
            }
        }
        return -1;
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }

    void clearToast() {
        if(mToast != null) {
            mToast.cancel();
        }
    }

    private void displayToast(int strId, int duration) {
        if(mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mContext, mContext.getResources().getString(strId), duration);
        mToast.show();
    }

    private void displayToast(String str, int duration) {
        if(mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mContext, str, duration);
        mToast.show();
    }

    private void updateTextView(final String str) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.updateTextView(str);
            }
        });
    }

    private void addMessage(final String prefix, final String data) {
        final String strToUI = prefix.concat(data);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.addMessage(strToUI);
            }
        });
    }
}
