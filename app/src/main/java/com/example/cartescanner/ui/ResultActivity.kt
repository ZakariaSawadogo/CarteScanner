package com.example.cartescanner.ui

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cartescanner.R
import com.example.cartescanner.data.AppDao
import com.example.cartescanner.data.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editSurname: EditText
    private lateinit var editOrg: EditText
    private lateinit var editPosition: EditText
    private lateinit var editPhone: EditText
    private lateinit var editEmail: EditText
    private lateinit var editLocation: EditText
    private lateinit var editLinkedin: EditText
    private lateinit var editTwitter: EditText
    private lateinit var editFacebook: EditText
    private lateinit var editInstagram: EditText

    private lateinit var aiLoadingIndicator: ProgressBar
    private lateinit var aiStatusText: TextView

    private var currentImageId: Long = -1L
    private lateinit var dao: AppDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        dao = AppDatabase.getDatabase(this).appDao()

        currentImageId = intent.getLongExtra("IMAGE_ID", -1L)
        if (currentImageId == -1L) {
            Toast.makeText(this, getString(R.string.error_no_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()

        val btnDeleteCard = findViewById<ImageView>(R.id.btnDeleteCard)
        btnDeleteCard.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        lifecycleScope.launch {
            dao.observeImageById(currentImageId).collect { image ->
                if (image != null) {
                    if (image.processCompleted) {
                        aiLoadingIndicator.visibility = View.GONE
                        aiStatusText.text = getString(R.string.result_ai_done)
                        aiStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else {
                        aiLoadingIndicator.visibility = View.VISIBLE
                        aiStatusText.text = getString(R.string.result_ai_loading)
                    }

                    loadFormData(currentImageId, dao)
                }
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveChangesToDatabase(dao)
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, "${editName.text} ${editSurname.text}".trim())
                putExtra(ContactsContract.Intents.Insert.COMPANY, editOrg.text.toString())
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, editPosition.text.toString())
                putExtra(ContactsContract.Intents.Insert.PHONE, editPhone.text.toString())
                putExtra(ContactsContract.Intents.Insert.EMAIL, editEmail.text.toString())
                putExtra(ContactsContract.Intents.Insert.POSTAL, editLocation.text.toString())

                val notes = buildString {
                    if (editLinkedin.text.isNotBlank()) append("LinkedIn: ${editLinkedin.text}\n")
                    if (editTwitter.text.isNotBlank()) append("Twitter: ${editTwitter.text}\n")
                    if (editFacebook.text.isNotBlank()) append("Facebook: ${editFacebook.text}\n")
                    if (editInstagram.text.isNotBlank()) append("Instagram: ${editInstagram.text}")
                }
                putExtra(ContactsContract.Intents.Insert.NOTES, notes)
            }
            startActivity(intent)
        }
    }

    private fun initViews() {
        editName = findViewById(R.id.editName)
        editSurname = findViewById(R.id.editSurname)
        editOrg = findViewById(R.id.editOrg)
        editPosition = findViewById(R.id.editPosition)
        editPhone = findViewById(R.id.editPhone)
        editEmail = findViewById(R.id.editEmail)
        editLocation = findViewById(R.id.editLocation)
        editLinkedin = findViewById(R.id.editLinkedin)
        editTwitter = findViewById(R.id.editTwitter)
        editFacebook = findViewById(R.id.editFacebook)
        editInstagram = findViewById(R.id.editInstagram)

        aiLoadingIndicator = findViewById(R.id.aiLoadingIndicator)
        aiStatusText = findViewById(R.id.aiStatusText)
    }

    private suspend fun loadFormData(imageId: Long, dao: AppDao) {
        val scan = dao.getScanByImageId(imageId) ?: return
        val person = scan.personId?.let { dao.getPersonById(it) }
        val org = scan.organisationId?.let { dao.getOrganisationById(it) }
        val contactId = person?.contactId ?: org?.contactId
        val contact = contactId?.let { dao.getContactById(it) }

        if (!editName.hasFocus()) editName.setText(person?.name ?: "")
        if (!editSurname.hasFocus()) editSurname.setText(person?.surname ?: "")
        if (!editPosition.hasFocus()) editPosition.setText(person?.position ?: "")
        if (!editOrg.hasFocus()) editOrg.setText(org?.organisationName ?: "")

        if (!editPhone.hasFocus()) editPhone.setText(contact?.tel ?: "")
        if (!editEmail.hasFocus()) editEmail.setText(contact?.email ?: "")
        if (!editLocation.hasFocus()) editLocation.setText(contact?.location ?: "")
        if (!editLinkedin.hasFocus()) editLinkedin.setText(contact?.linkedin ?: "")
        if (!editTwitter.hasFocus()) editTwitter.setText(contact?.twitter ?: "")
        if (!editFacebook.hasFocus()) editFacebook.setText(contact?.facebook ?: "")
        if (!editInstagram.hasFocus()) editInstagram.setText(contact?.instagram ?: "")
    }

    private fun saveChangesToDatabase(dao: AppDao) {
        lifecycleScope.launch(Dispatchers.IO) {
            val scan = dao.getScanByImageId(currentImageId) ?: return@launch

            scan.personId?.let { pid ->
                dao.getPersonById(pid)?.let { person ->
                    dao.updatePerson(person.copy(
                        name = editName.text.toString().trim(),
                        surname = editSurname.text.toString().trim(),
                        position = editPosition.text.toString().trim()
                    ))
                }
            }

            scan.organisationId?.let { oid ->
                dao.getOrganisationById(oid)?.let { org ->
                    dao.updateOrganisation(org.copy(
                        organisationName = editOrg.text.toString().trim()
                    ))
                }
            }

            val contactId = scan.personId?.let { dao.getPersonById(it)?.contactId }
                ?: scan.organisationId?.let { dao.getOrganisationById(it)?.contactId }

            contactId?.let { cid ->
                dao.getContactById(cid)?.let { contact ->
                    dao.updateContact(contact.copy(
                        tel = editPhone.text.toString().trim(),
                        email = editEmail.text.toString().trim(),
                        location = editLocation.text.toString().trim(),
                        linkedin = editLinkedin.text.toString().trim(),
                        twitter = editTwitter.text.toString().trim(),
                        facebook = editFacebook.text.toString().trim(),
                        instagram = editInstagram.text.toString().trim()
                    ))
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResultActivity, getString(R.string.toast_changes_saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_card_title))
            .setMessage(getString(R.string.delete_card_message))
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deleteCurrentCard()
            }
            .show()
    }

    private fun deleteCurrentCard() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scan = dao.getScanByImageId(currentImageId)
            val contactId = scan?.personId?.let { dao.getPersonById(it)?.contactId }
                ?: scan?.organisationId?.let { dao.getOrganisationById(it)?.contactId }

            // Suppression en cascade: Image puis Contact
            dao.deleteImageById(currentImageId)
            contactId?.let { dao.deleteContactById(it) }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResultActivity, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}