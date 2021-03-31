package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentFindUserBinding


class FindUserFragment : Fragment() {

    private lateinit var binding: FragmentFindUserBinding
    private lateinit var navController: NavController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_find_user, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchRoomsFragment(view)
    }

    /**Oculta el toolbar en este fragment al ponerlo en onResume y onStop**/
    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    private fun launchRoomsFragment(view: View) {
        val backRooms = binding.buttonBack

        navController = Navigation.findNavController(view)
        backRooms.setOnClickListener {
            navController.navigate(R.id.roomsFragment)
        }
    }
}