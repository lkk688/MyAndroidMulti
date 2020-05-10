package sjsu.cmpe277.myandroidmulti.Question

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import sjsu.cmpe277.myandroidmulti.R
import sjsu.cmpe277.myandroidmulti.databinding.FragmentQuestionBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [QuestionFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [QuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuestionFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//    private var listener: OnFragmentInteractionListener? = null

    private lateinit var viewModel: QuestionViewModel
    //private lateinit var viewModelFactory: QuestionViewModelFactory
//    private var fever: Boolean = false
//    private var injured: Boolean = false

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
        //return inflater.inflate(R.layout.fragment_question, container, false)

        val binding = DataBindingUtil.inflate<FragmentQuestionBinding>(inflater,
            R.layout.fragment_question,container,false)

        binding.button2.setOnClickListener {view: View ->
            val checkedId = binding.questionRadioGroup.checkedRadioButtonId
            if (-1 != checkedId) {
                var answerIndex = 0
                when (checkedId) {
                    R.id.firstanswerradioButton -> answerIndex = 1
                    R.id.secondasnwerradioButton -> answerIndex = 2
                }
                if (answerIndex ==1)
                {
                    //val action = QuestionFragmentDirections.actionQuestionFragmentToAnswerOneFragment2()
                    //action.ri
                    Navigation.findNavController(view).navigate(R.id.action_questionFragment_to_answerOneFragment2)
                }else {
                    Navigation.findNavController(view).navigate(R.id.action_questionFragment_to_answerTwoFragment2)
                }
            }

        }

        Log.i("QuestionFragment","Called ViewModelProviders.of")
        //viewModelFactory = QuestionViewModelFactory(QuestionFragmentArgs.fromBundle(arguments!!).score)
        //viewModel = ViewModelProviders.of(this, viewModelFactory).get(QuestionViewModel::class.java)
        viewModel = ViewModelProviders.of(this).get(QuestionViewModel::class.java)

        binding.firstanswerradioButton.setOnClickListener { view: View ->
            onRadioButtonClicked(view)
        }
        binding.secondasnwerradioButton.setOnClickListener { view: View ->
            onRadioButtonClicked(view)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //val argscore = QuestionFragmentArgs.fromBundle(savedInstanceState!!).riskscore
        val argscore = QuestionFragmentArgs.fromBundle(arguments!!).riskscore
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.firstanswerradioButton ->
                    if (checked) {
//                        fever = true
//                        injured = false
                        viewModel.fever = true
                        viewModel.injured = false
                    }
                R.id.secondasnwerradioButton ->
                    if (checked) {
//                        fever = false
//                        injured = true
                        viewModel.fever = false
                        viewModel.injured = true
                    }
            }
        }
    }
}
