package com.contramano.interno;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // El plugin se registra ANTES de super.onCreate
        registerPlugin(ContramanoPlugin.class);
        super.onCreate(savedInstanceState);
    }
}