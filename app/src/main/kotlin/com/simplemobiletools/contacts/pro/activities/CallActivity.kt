package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        updateTextColors(call_holder)
        initButtons()
    }

    private fun initButtons() {
        call_decline.setOnClickListener { }
        call_accept.setOnClickListener { }

        call_toggle_microphone.setOnClickListener { }
        call_toggle_speaker.setOnClickListener { }
        call_dialpad.setOnClickListener { }
    }
}
