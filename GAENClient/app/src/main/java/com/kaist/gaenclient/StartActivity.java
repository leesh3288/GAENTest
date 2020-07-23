package com.kaist.gaenclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.kaist.gaenclient.databinding.ActivityDeviceIdBinding;


public class StartActivity extends Activity {

    private ActivityDeviceIdBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set listeners
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_device_id);
        mBinding.setActivity(this);
        mBinding.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.putExtra("deviceId",mBinding.editTextDeviceId.getText().toString());
                startActivity(intent);
            }
        });
    }
}
