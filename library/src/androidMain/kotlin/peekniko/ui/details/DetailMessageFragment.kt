package peekniko.ui.details

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import peekniko.library.R

internal class DetailMessageFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        TextView(requireContext()).apply {
            text = getString(R.string.peekniko_call_not_found)
            gravity = Gravity.CENTER
        }
}
