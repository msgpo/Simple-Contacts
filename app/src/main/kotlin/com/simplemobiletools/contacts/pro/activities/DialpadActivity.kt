package com.simplemobiletools.contacts.pro.activities

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony.Sms.Intents.SECRET_CODE_ACTION
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.dialogs.CallConfirmationDialog
import com.simplemobiletools.contacts.pro.extensions.*
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.helpers.KEY_PHONE
import com.simplemobiletools.contacts.pro.helpers.LOCATION_DIALPAD
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.SpeedDial
import kotlinx.android.synthetic.main.activity_dialpad.*
import kotlinx.android.synthetic.main.activity_dialpad.dialpad_holder
import kotlinx.android.synthetic.main.dialpad.*

class DialpadActivity : SimpleActivity() {
    private var contacts = ArrayList<Contact>()
    private var speedDialValues = ArrayList<SpeedDial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialpad)

        if (checkAppSideloading()) {
            return
        }

        speedDialValues = config.getSpeedDialValues()

        dialpad_0_holder.setOnClickListener { dialpadPressed('0', it) }
        dialpad_1.setOnClickListener { dialpadPressed('1', it) }
        dialpad_2.setOnClickListener { dialpadPressed('2', it) }
        dialpad_3.setOnClickListener { dialpadPressed('3', it) }
        dialpad_4.setOnClickListener { dialpadPressed('4', it) }
        dialpad_5.setOnClickListener { dialpadPressed('5', it) }
        dialpad_6.setOnClickListener { dialpadPressed('6', it) }
        dialpad_7.setOnClickListener { dialpadPressed('7', it) }
        dialpad_8.setOnClickListener { dialpadPressed('8', it) }
        dialpad_9.setOnClickListener { dialpadPressed('9', it) }

        dialpad_1.setOnLongClickListener { speedDial(1); true }
        dialpad_2.setOnLongClickListener { speedDial(2); true }
        dialpad_3.setOnLongClickListener { speedDial(3); true }
        dialpad_4.setOnLongClickListener { speedDial(4); true }
        dialpad_5.setOnLongClickListener { speedDial(5); true }
        dialpad_6.setOnLongClickListener { speedDial(6); true }
        dialpad_7.setOnLongClickListener { speedDial(7); true }
        dialpad_8.setOnLongClickListener { speedDial(8); true }
        dialpad_9.setOnLongClickListener { speedDial(9); true }

        dialpad_0_holder.setOnLongClickListener { dialpadPressed('+', null); true }
        dialpad_asterisk.setOnClickListener { dialpadPressed('*', it) }
        dialpad_hashtag.setOnClickListener { dialpadPressed('#', it) }
        dialpad_clear_char.setOnClickListener { clearChar(it) }
        dialpad_clear_char.setOnLongClickListener { clearInput(); true }
        dialpad_call_button.setOnClickListener { initCall() }
        dialpad_input.onTextChangeListener { dialpadValueChanged(it) }
        ContactsHelper(this).getContacts { gotContacts(it) }
        disableKeyboardPopping()

        val callIcon = resources.getColoredDrawableWithColor(R.drawable.ic_phone_vector, if (isBlackAndWhiteTheme()) Color.BLACK else config.primaryColor.getContrastColor())
        dialpad_call_button.setImageDrawable(callIcon)
        dialpad_call_button.background.applyColorFilter(getAdjustedPrimaryColor())

        val showLetters = config.showDialpadLetters
        arrayOf(dialpad_2_letters, dialpad_3_letters, dialpad_4_letters, dialpad_5_letters, dialpad_6_letters, dialpad_7_letters, dialpad_8_letters, dialpad_9_letters).forEach {
            it.beVisibleIf(showLetters)
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_holder)
        dialpad_clear_char.applyColorFilter(config.textColor)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dialpad, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_number_to_contact -> addNumberToContact()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun checkDialIntent(): Boolean {
        return if (intent.action == Intent.ACTION_DIAL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            val number = Uri.decode(intent.dataString).substringAfter("tel:")
            dialpad_input.setText(number)
            dialpad_input.setSelection(number.length)
            true
        } else {
            false
        }
    }

    private fun addNumberToContact() {
        Intent().apply {
            action = Intent.ACTION_INSERT_OR_EDIT
            type = "vnd.android.cursor.item/contact"
            putExtra(KEY_PHONE, dialpad_input.value)
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun dialpadPressed(char: Char, view: View?) {
        dialpad_input.addCharacter(char)
        view?.performHapticFeedback()
    }

    private fun clearChar(view: View) {
        dialpad_input.dispatchKeyEvent(dialpad_input.getKeyEvent(KeyEvent.KEYCODE_DEL))
        view.performHapticFeedback()
    }

    private fun clearInput() {
        dialpad_input.setText("")
    }

    private fun disableKeyboardPopping() {
        dialpad_input.showSoftInputOnFocus = false
    }

    private fun gotContacts(newContacts: ArrayList<Contact>) {
        contacts = newContacts
        if (!checkDialIntent() && dialpad_input.value.isEmpty()) {
            dialpadValueChanged("")
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun dialpadValueChanged(text: String) {
        val len = text.length
        if (len > 8 && text.startsWith("*#*#") && text.endsWith("#*#*")) {
            val secretCode = text.substring(4, text.length - 4)
            if (isOreoPlus()) {
                if (isDefaultDialer()) {
                    getSystemService(TelephonyManager::class.java)?.sendDialerSpecialCode(secretCode)
                } else {
                    launchSetDefaultDialerIntent()
                }
            } else {
                val intent = Intent(SECRET_CODE_ACTION, Uri.parse("android_secret_code://$secretCode"))
                sendBroadcast(intent)
            }
            return
        }

        val showLetters = config.showDialpadLetters
        (dialpad_list.adapter as? ContactsAdapter)?.finishActMode()
        val filtered = contacts.filter {
            val convertedName = PhoneNumberUtils.convertKeypadLettersToDigits(it.getNameToDisplay().normalizeString())
            val company = PhoneNumberUtils.convertKeypadLettersToDigits(it.getFullCompany().normalizeString())
            it.doesContainPhoneNumber(text, showLetters) || (showLetters && (convertedName.contains(text, true) || company.contains(text, true)))
        }.sortedWith(compareBy {
            if (showLetters) {
                !it.doesContainPhoneNumber(text, showLetters)
            } else {
                true
            }
        }).toMutableList() as ArrayList<Contact>

        ContactsAdapter(this, filtered, null, LOCATION_DIALPAD, null, dialpad_list, dialpad_fastscroller, text) {
            callContact(it as Contact)
        }.apply {
            dialpad_list.adapter = this
        }

        dialpad_fastscroller.setScrollToY(0)
        dialpad_fastscroller.setViews(dialpad_list) {
            val item = (dialpad_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
            dialpad_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            dialpadValueChanged(dialpad_input.value)
        }
    }

    private fun initCall(number: String = dialpad_input.value) {
        if (number.isNotEmpty()) {
            if (config.showCallConfirmation) {
                CallConfirmationDialog(this, number) {
                    startCallIntent(number)
                }
            } else {
                startCallIntent(number)
            }
        }
    }

    private fun speedDial(id: Int) {
        if (dialpad_input.value.isEmpty()) {
            val speedDial = speedDialValues.firstOrNull { it.id == id }
            if (speedDial?.isValid() == true) {
                initCall(speedDial.number)
            }
        }
    }
}
