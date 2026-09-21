package com.example.cartescanner.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cartescanner.R
import com.example.cartescanner.data.ScanHistoryItem

class ScanHistoryAdapter : ListAdapter<ScanHistoryItem, ScanHistoryAdapter.ScanViewHolder>(ScanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_large, parent, false)
        return ScanViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNameLarge: TextView = itemView.findViewById(R.id.textNameLarge)
        private val textRoleOrgLarge: TextView = itemView.findViewById(R.id.textRoleOrgLarge)
        private val textPhoneLarge: TextView = itemView.findViewById(R.id.textPhoneLarge)
        private val textEmailLarge: TextView = itemView.findViewById(R.id.textEmailLarge)

        private var currentScanId: Long? = null

        init {
            itemView.setOnClickListener {
                currentScanId?.let { scanId ->
                    val intent = Intent(itemView.context, ResultActivity::class.java)
                    intent.putExtra("IMAGE_ID", scanId)
                    itemView.context.startActivity(intent)
                }
            }
        }

        fun bind(item: ScanHistoryItem) {
            currentScanId = item.scanId

            val fullName = listOfNotNull(item.name, item.surname).joinToString(" ").trim()
            textNameLarge.text = fullName.ifEmpty { "Nom inconnu" }

            textRoleOrgLarge.text = item.organisationName ?: "Organisation non identifiée"
            textRoleOrgLarge.visibility = if (item.organisationName.isNullOrBlank()) View.GONE else View.VISIBLE

            if (item.phone.isNullOrBlank()) {
                textPhoneLarge.visibility = View.GONE
            } else {
                textPhoneLarge.visibility = View.VISIBLE
                textPhoneLarge.text = item.phone
            }

            if (item.linkedin.isNullOrBlank()) {
                textEmailLarge.visibility = View.GONE
            } else {
                textEmailLarge.visibility = View.VISIBLE
                textEmailLarge.text = item.linkedin
            }
        }
    }

    class ScanDiffCallback : DiffUtil.ItemCallback<ScanHistoryItem>() {
        override fun areItemsTheSame(oldItem: ScanHistoryItem, newItem: ScanHistoryItem): Boolean {
            return oldItem.scanId == newItem.scanId
        }
        override fun areContentsTheSame(oldItem: ScanHistoryItem, newItem: ScanHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}