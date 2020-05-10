package sjsu.cmpe277.myandroidmulti

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import sjsu.cmpe277.myandroidmulti.databinding.FragmentAnswerOneBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AnswerOneFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AnswerOneFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AnswerOneFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_answer_one, container, false)
        val binding = DataBindingUtil.inflate<FragmentAnswerOneBinding>(inflater,
            R.layout.fragment_answer_one,container,false)

        binding.buttonNext.setOnClickListener { view: View ->
            Navigation.findNavController(view).navigate(R.id.action_answerOneFragment_to_questionFragment)
        }

        return binding.root
    }
}
