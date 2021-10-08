package com.rainyseason.cj.common.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.buildModels
import com.rainyseason.cj.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHomeBinding.bind(view)
        GlideApp.with(binding.background)
            .load(R.drawable.main_background)
            .centerCrop()
            .into(binding.background)

        val models = buildModels {
            listOf(
                R.drawable.step_1,
                R.drawable.step_2,
                R.drawable.step_3,
            ).forEach {
                demoView {
                    id(it)
                    imageRes(it)
                }
            }
        }

        binding.carousel.layoutManager = LinearLayoutManager(
            requireContext(),
            RecyclerView.HORIZONTAL,
            false
        )
        val snapHelper = PagerSnapHelper()

        binding.carousel.setModels(models)
        binding.carousel.apply {
            clearOnScrollListeners()
            onFlingListener = null
        }
        snapHelper.attachToRecyclerView(binding.carousel)

        binding.indicator.attachToRecyclerView(binding.carousel, snapHelper)
        binding.addWidgetButton.setOnClickListener {
            requireActivity().finish()
            Toast.makeText(
                context,
                requireContext().getString(R.string.drag_and_drop_to_add_widget),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
