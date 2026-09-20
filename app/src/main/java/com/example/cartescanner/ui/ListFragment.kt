package com.example.cartescanner.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cartescanner.R
import com.example.cartescanner.data.AppDatabase
import kotlinx.coroutines.launch

class ListFragment : Fragment(R.layout.fragment_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = ScanHistoryAdapter()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val dao = AppDatabase.getDatabase(requireContext()).appDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllScansFlow().collect { scans ->
                adapter.submitList(scans)
            }
        }
    }
}