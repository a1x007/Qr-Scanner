package com.ashique.qrscanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ashique.qrscanner.utils.Extensions.setOnBackPressedAction
import com.ashique.qrscanner.R

class ResultFragment : Fragment() {

    companion object {
        private const val ARG_RESULT = "result"

        fun newInstance(result: String): ResultFragment {
            val fragment = ResultFragment()
            val args = Bundle()
            args.putString(ARG_RESULT, result)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_result, container, false)
        val resultTextView: TextView = view.findViewById(R.id.result_text_view)
        val result = arguments?.getString(ARG_RESULT)
        resultTextView.text = result
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set back press action using extension function
        this.setOnBackPressedAction {
            // For example, pop back stack if you have fragments in the back stack
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                // Otherwise, handle as needed
                (requireActivity() as AppCompatActivity).setOnBackPressedAction{
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        }

    }
}
