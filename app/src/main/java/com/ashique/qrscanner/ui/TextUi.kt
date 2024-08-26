package com.ashique.qrscanner.ui

import android.app.DatePickerDialog
import android.text.InputType
import android.widget.EditText
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.custom.RadioButtons
import com.ashique.qrscanner.databinding.LayoutQrTextBinding
import com.ashique.qrscanner.helper.QrUiSetup
import com.ashique.qrscanner.utils.Extensions.animateLayout
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object TextUi {

    private var selectedAuthentication: QrData.Wifi.Authentication = QrData.Wifi.Authentication.OPEN

    var onBkashScan: ((String?) -> Unit)? = null
    fun textSetting(ui: LayoutQrTextBinding, onUpdate: (QrData) -> Unit) {

        fun updateCheckedState(checkedButton: RadioButtons, radioButtons: Sequence<RadioButtons>) {
            radioButtons.forEach { rb ->
                rb.isChecked = rb.id == checkedButton.id
            }
        }

        fun resetFieldsAndInputs() {
            val fieldsAndInputs = listOf(
                ui.field1 to ui.input1,
                ui.field2 to ui.input2,
                ui.field3 to ui.input3,
                ui.field4 to ui.input4,
                ui.field5 to ui.input5,
                ui.field6 to ui.input6,
                null to ui.input7,
                null to ui.input8
            )

            fieldsAndInputs.forEach { (field, input) ->
                field?.isVisible = false
                input.inputType = InputType.TYPE_NULL
                input.text?.clear()
            }

            ui.field2.endIconMode = TextInputLayout.END_ICON_NONE
            ui.selectorsLayout.isVisible = false
            ui.dateLayout.isVisible = false
            ui.encrypt.isVisible = false

            ui.btnLayout1.isVisible = true
            ui.btnLayout2.isVisible = true
            ui.btnLayout3.isVisible = true
        }


        with(ui) {
            val activity = ui.root.context as? QrGenerator
            rootLayout.animateLayout()
            inputLayout.animateLayout()

            // Mapping RadioButton IDs to corresponding DataType
            val typeMap = mapOf(
                R.id.url to QrUiSetup.DataType.URL,
                R.id.text to QrUiSetup.DataType.TEXT,
                R.id.email to QrUiSetup.DataType.EMAIL,
                R.id.phone to QrUiSetup.DataType.PHONE,
                R.id.sms to QrUiSetup.DataType.SMS,
                R.id.wifi to QrUiSetup.DataType.WIFI,
                R.id.map to QrUiSetup.DataType.GEO,
                R.id.contacts to QrUiSetup.DataType.CONTACT,
                R.id.bkash to QrUiSetup.DataType.BKASH,
                R.id.youtube to QrUiSetup.DataType.YOUTUBE,
                R.id.googleplay to QrUiSetup.DataType.GOOGLEPLAY,
                R.id.event to QrUiSetup.DataType.EVENT,
                R.id.bcard to QrUiSetup.DataType.BCARD
            )

            // Get all RadioButtons from the FlexboxLayout
            val radioButtons = flexboxLayout.children.filterIsInstance<RadioButtons>()

            radioButtons.forEach { radioButton ->
                radioButton.setOnClickListener {
                    updateCheckedState(radioButton, radioButtons)
                    resetFieldsAndInputs()

                    // Execute logic based on selected type
                    when (val selectedType = typeMap[radioButton.id]) {
                        QrUiSetup.DataType.TEXT -> {
                            field1.isVisible = true
                            field1.hint = "Text"
                            input1.inputType = InputType.TYPE_CLASS_TEXT
                            input1.doOnTextChanged { text, start, before, count ->
                                onUpdate(createQrData(selectedType, ui))
                            }
                        }

                        QrUiSetup.DataType.EMAIL -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            field3.isVisible = true
                            field4.isVisible = true
                            // Set hints and input types
                            val hints = listOf("Email", "Subject", "Body", "Copy To")
                            val inputTypes = listOf(
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                                InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT,
                                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT,
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            )

                            listOf(
                                field1,
                                field2,
                                field3,
                                field4
                            ).forEachIndexed { index, textInputLayout ->
                                textInputLayout.hint = hints[index]
                            }

                            listOf(input1, input2, input3, input4).forEachIndexed { index, input ->
                                input.inputType = inputTypes[index]
                                input.doOnTextChanged { _, _, _, _ ->
                                    onUpdate(
                                        createQrData(
                                            selectedType, ui
                                        )
                                    )
                                }
                            }
                        }


                        QrUiSetup.DataType.WIFI -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            selectorsLayout.isVisible = true
                            encrypt.isVisible = true

                            // Set hints and labels
                            val hints = listOf("Name(SSID)", "Password")

                            selectorsText.text = "Authentication Type"
                            option1.text = "WEP"
                            option2.text = "WPA"
                            option3.text = "Open"

                            option3.isChecked = true
                            field2.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

                            // Set input types
                            val inputTypes = listOf(
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            )

                            // Function to update QR data
                            fun updateQrData() = onUpdate(createQrData(selectedType, ui))

                            // RadioGroup listener
                            groupSelectors.setOnCheckedChangeListener { _, checkedId ->
                                selectedAuthentication = when (checkedId) {
                                    R.id.option1 -> QrData.Wifi.Authentication.WEP
                                    R.id.option2 -> QrData.Wifi.Authentication.WPA
                                    R.id.option3 -> QrData.Wifi.Authentication.OPEN
                                    else -> QrData.Wifi.Authentication.OPEN
                                }
                                updateQrData()
                            }

                            // Encrypt CheckBox listener
                            encrypt.setOnCheckedListener { updateQrData() }

                            listOf(
                                field1,
                                field2
                            ).forEachIndexed { index, textInputLayout ->
                                textInputLayout.hint = hints[index]
                            }
                            // TextInputEditText listeners
                            listOf(input1, input2).forEachIndexed { index, input ->
                                input.inputType = inputTypes[index]
                                input.doOnTextChanged { _, _, _, _ -> updateQrData() }
                            }
                        }


                        QrUiSetup.DataType.GEO -> {
                            field1.isVisible = true
                            field2.isVisible = true

                            // Set hints and input types
                            field1.hint = "Latitude"
                            field2.hint = "Longitude"

                            listOf(input1, input2).forEach { input ->
                                input.inputType =
                                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                                input.doOnTextChanged { _, _, _, _ ->
                                    onUpdate(createQrData(selectedType, ui))
                                }
                            }
                        }


                        QrUiSetup.DataType.CONTACT -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            field3.isVisible = true
                            field4.isVisible = true
                            field5.isVisible = true
                            field6.isVisible = true

                            val hints = listOf(
                                "Name",
                                "Email",
                                "Phone Number",
                                "Address",
                                "Company",
                                "Website"
                            )

                            val inputTypes = listOf(
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                                InputType.TYPE_CLASS_PHONE,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_TEXT_VARIATION_URI
                            )

                            listOf(
                                field1,
                                field2,
                                field3,
                                field4,
                                field5,
                                field6
                            ).forEachIndexed { index, textInputLayout ->
                                textInputLayout.hint = hints[index]
                            }

                            listOf(
                                input1,
                                input2,
                                input3,
                                input4,
                                input5,
                                input6
                            ).forEachIndexed { index, input ->
                                input.inputType = inputTypes[index]
                                input.doOnTextChanged { _, _, _, _ ->
                                    onUpdate(
                                        createQrData(
                                            selectedType,
                                            ui
                                        )
                                    )
                                }
                            }
                        }

                        QrUiSetup.DataType.SMS -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            encrypt.isVisible = true

                            field1.hint = "Phone Number"
                            field2.hint = "Message"
                            encrypt.text = "IS MMS ?"

                            input1.inputType = InputType.TYPE_CLASS_PHONE
                            input2.inputType = InputType.TYPE_CLASS_TEXT

                            listOf(input1, input2).forEach { input ->
                                input.doOnTextChanged { _, _, _, _ ->
                                    onUpdate(
                                        createQrData(
                                            selectedType,
                                            ui
                                        )
                                    )
                                }
                            }
                            encrypt.setOnCheckedListener {
                                onUpdate(createQrData(selectedType, ui))
                            }


                        }

                        QrUiSetup.DataType.BCARD -> {
                            val hints =
                                listOf("Name", "Email", "Phone Number", "Address", "Job", "Company")
                            val inputTypes = listOf(
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                                InputType.TYPE_CLASS_PHONE,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_TEXT_VARIATION_URI
                                )

                            listOf(
                                field1,
                                field2,
                                field3,
                                field4,
                                field5,
                                field6
                            ).forEachIndexed { index, textInputLayout ->
                                textInputLayout.isVisible = true
                                textInputLayout.hint = hints[index]
                            }

                            listOf(
                                input1,
                                input2,
                                input3,
                                input4,
                                input5,
                                input6
                            ).forEachIndexed { index, input ->
                                input.inputType = inputTypes[index]
                                input.doOnTextChanged { _, _, _, _ ->
                                    onUpdate(
                                        createQrData(
                                            selectedType,
                                            ui
                                        )
                                    )
                                }
                            }

                        }

                        QrUiSetup.DataType.EVENT -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            dateLayout.isVisible = true
                            btnLayout3.isVisible = false
                            val hints =
                                listOf("Title", "Organizer", "Select Start Date", "Select End Date")
                            val inputTypes = listOf(
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT,
                                InputType.TYPE_CLASS_TEXT

                            )


                            fun showDatePicker(editText: EditText) {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    root.context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedDate = "$dayOfMonth/${month + 1}/$year"
                                        editText.setText(selectedDate)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }

                            // Set up the click listeners for both buttons
                            btn1.setOnClickListener { showDatePicker(input7) }
                            btn2.setOnClickListener { showDatePicker(input8) }

                            listOf(
                                field1,
                                field2,
                                field7,
                                field8
                            ).forEachIndexed { index, textInputLayout ->
                                textInputLayout.hint = hints[index]
                            }

                            listOf(input1, input2, input7, input8).forEachIndexed { index, input ->
                                input.inputType = inputTypes[index]

                            }
                        }

                        null -> {

                        }

                        QrUiSetup.DataType.URL, QrUiSetup.DataType.YOUTUBE, QrUiSetup.DataType.GOOGLEPLAY -> {
                            field1.isVisible = true
                            field1.hint = "Url"
                            input1.inputType = InputType.TYPE_TEXT_VARIATION_URI
                            input1.doOnTextChanged { text, start, before, count ->
                                onUpdate(createQrData(selectedType, ui))
                            }
                        }

                        QrUiSetup.DataType.PHONE -> {
                            field1.isVisible = true
                            field2.isVisible = true
                            field1.hint = "Name"
                            field2.hint = "Phone Number"
                            input1.inputType = InputType.TYPE_CLASS_TEXT
                            input2.inputType = InputType.TYPE_CLASS_PHONE

                        }

                        QrUiSetup.DataType.BKASH -> {
                            dateLayout.isVisible = true
                            btnLayout1.isVisible = false
                            btnLayout2.isVisible = false
                            input9.inputType = InputType.TYPE_TEXT_VARIATION_URI
                            field9.hint = "Your bKash Api"



                            btn3.setOnClickListener {
                                activity?.importDrawable(isLogo = false, isBkash = true)
                            }

                            activity?.setOnBkashScanCallback { result ->
                                activity.lifecycleScope.launch(Dispatchers.Main) {
                                    input9.setText(result)
                                    onUpdate(createQrData(selectedType, ui))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createQrData(selectedType: QrUiSetup.DataType?, ui: LayoutQrTextBinding): QrData {
        with(ui) {
            return when (selectedType) {
                QrUiSetup.DataType.URL -> QrData.Url(input1.text.toString())
                QrUiSetup.DataType.TEXT -> QrData.Text(input1.text.toString())
                QrUiSetup.DataType.PHONE -> QrData.Phone(
                    name = input1.text.toString(),
                    phoneNumber = input2.text.toString()
                )

                QrUiSetup.DataType.SMS -> QrData.SMS(
                    phoneNumber = input1.text.toString(),
                    subject = input2.text.toString(),
                    isMMS = encrypt.isChecked
                )

                QrUiSetup.DataType.YOUTUBE -> QrData.YouTube(input1.text.toString())
                QrUiSetup.DataType.GOOGLEPLAY -> QrData.GooglePlay(input1.text.toString())
                QrUiSetup.DataType.BKASH -> {
                    QrData.Bkash(input7.text.toString())
                }

                QrUiSetup.DataType.CONTACT -> {
                    QrData.VCard(
                        name = input1.text.toString(),
                        email = input2.text.toString(),
                        phoneNumber = input3.text.toString(),
                        address = input4.text.toString(),
                        company = input5.text.toString(),
                        website = input6.text.toString()
                    )
                }

                QrUiSetup.DataType.EVENT -> {
                    QrData.Event(
                        uid = input1.text.toString(),
                        organizer = input2.text.toString(),
                        start = input7.text.toString(),
                        end = input8.text.toString()
                    )
                }

                QrUiSetup.DataType.GEO -> QrData.GeoPos(
                    lat = input1.text.toString().toFloatOrNull() ?: 0f,
                    lon = input2.text.toString().toFloatOrNull() ?: 0f
                )

                QrUiSetup.DataType.EMAIL -> QrData.Email(
                    email = input1.text.toString(),
                    subject = input2.text.toString(),
                    body = input3.text.toString(),
                    copyTo = input4.text.toString()
                )

                QrUiSetup.DataType.WIFI -> QrData.Wifi(
                    ssid = input1.text.toString(),
                    psk = input2.text.toString(),
                    hidden = encrypt.isChecked,
                    authentication = selectedAuthentication
                )

                QrUiSetup.DataType.BCARD -> QrData.BizCard(
                    firstName = input1.text.toString(),
                    email = input2.text.toString(),
                    phone = input3.text.toString(),
                    address = input4.text.toString(),
                    job = input5.text.toString(),
                    company = input6.text.toString()
                )


                else -> QrData.Text("") // Default case
            }
        }
    }

}