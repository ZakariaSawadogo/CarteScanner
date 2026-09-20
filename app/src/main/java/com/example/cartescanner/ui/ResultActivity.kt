package com.example.cartescanner.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cartescanner.R
import com.example.cartescanner.data.AppDatabase
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

    private lateinit var aiLoadingIndicator: ProgressBar
    private lateinit var aiStatusText: TextView

    private var currentImageId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        currentImageId = intent.getLongExtra("IMAGE_ID", -1L)
        if (currentImageId == -1L) {
            Toast.makeText(this, "Erreur: Aucune donnée trouvée", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()

        val dao = AppDatabase.getDatabase(this).appDao()

        lifecycleScope.launch {
            dao.observeImageById(currentImageId).collect { image ->
                if (image != null) {
                    if (image.processCompleted) {
                        aiLoadingIndicator.visibility = View.GONE
                        aiStatusText.text = "Données enrichies par l'IA"
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
            val intent = android.content.Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(android.provider.ContactsContract.Intents.Insert.NAME, "${editName.text} ${editSurname.text}".trim())
                putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, editOrg.text.toString())
                putExtra(android.provider.ContactsContract.Intents.Insert.JOB_TITLE, editPosition.text.toString())
                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, editPhone.text.toString())
                putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, editEmail.text.toString())
                putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, editLocation.text.toString())

                // Concaténer tous les réseaux sociaux dans les notes du contact
                val notes = buildString {
                    if (editLinkedin.text.isNotBlank()) append("LinkedIn: ${editLinkedin.text}\n")
                    if (editTwitter.text.isNotBlank()) append("Twitter: ${editTwitter.text}\n")
                    if (editFacebook.text.isNotBlank()) append("Facebook: ${editFacebook.text}")
                }
                putExtra(android.provider.ContactsContract.Intents.Insert.NOTES, notes)
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

        aiLoadingIndicator = findViewById(R.id.aiLoadingIndicator)
        aiStatusText = findViewById(R.id.aiStatusText)
    }

    private suspend fun loadFormData(imageId: Long, dao: com.example.cartescanner.data.AppDao) {
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
    }

    private fun saveChangesToDatabase(dao: com.example.cartescanner.data.AppDao) {
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
                        facebook = editFacebook.text.toString().trim()
                    ))
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResultActivity, getString(R.string.toast_changes_saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}