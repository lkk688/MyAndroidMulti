package sjsu.cmpe277.myandroidmulti.Title

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import sjsu.cmpe277.myandroidmulti.Question.QuestionFragmentDirections
import sjsu.cmpe277.myandroidmulti.R
import sjsu.cmpe277.myandroidmulti.databinding.FragmentTitleBinding
import kotlin.math.round

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

const val KEY_INPUTNAME = "inputname_key"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TitleFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [TitleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TitleFragment : Fragment() {

    private lateinit var binding: FragmentTitleBinding

    private lateinit var viewModel: TitleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
        }

        if (savedInstanceState != null) {
            var restoredstr = savedInstanceState.getString(KEY_INPUTNAME, "").toString()
            Log.i("TitleFragment", restoredstr)
        }
        Log.i("TitleFragment", "onCreate called")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_title, container, false)

        binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater,
            R.layout.fragment_title,container,false)

        viewModel = ViewModelProviders.of(this).get(TitleViewModel::class.java)

        viewModel.riskscore.observe(viewLifecycleOwner, Observer { newRiskscore ->
            binding.textViewriskscore.text = newRiskscore.toString()
        })

        binding.button.setOnClickListener { view: View ->
            //viewModel.yourname = binding.inputName.text.toString()
            viewModel.yourname.value = binding.inputName.text.toString()

            val action = TitleFragmentDirections.actionTitleFragmentToQuestionFragment(riskscore = viewModel.riskscore.value!!)
            view.findNavController().navigate(action)

            //view.findNavController().navigate(R.id.action_titleFragment_to_questionFragment)
            //Navigation.findNavController(view).navigate(R.id.action_titleFragment_to_questionFragment)

        }

        binding.floatingActionButton.setOnClickListener { view ->
            Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show()

        }

        binding.buttonChange.setOnClickListener{
            viewModel.updateRiskscore()
        }

        //Test send notification
        viewModel.createChannel(getString(R.string.cmpe277_notification_channel_id),
            getString(R.string.cmpe277_notification_channel_name))
        binding.switch1.setOnClickListener {
            viewModel.sendNotification()
        }

        //Firebase
        viewModel.fetchTokens()
        //Test FCM send notification
        viewModel.createChannel(getString(R.string.cmpe277_fcm_notification_channel_id),
            getString(R.string.cmpe277_fcm_notification_channel_name))
        viewModel.subscribeTopic()

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(view!!))
                ||super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        Log.i("TitleFragment", "onSaveInstanceState Called")

        //outState.putString(KEY_INPUTNAME, binding.inputName.text.toString())
        outState.putString(KEY_INPUTNAME, "test title fragment data")
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.i("TitleFragment", "onActivityCreated called")
    }
    override fun onStart() {
        super.onStart()
        Log.i("TitleFragment", "onStart called")
    }
    override fun onResume() {
        super.onResume()
        Log.i("TitleFragment", "onResume called")
    }
    override fun onPause() {
        super.onPause()
        Log.i("TitleFragment", "onPause called")
    }
    override fun onStop() {
        super.onStop()
        Log.i("TitleFragment", "onStop called")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("TitleFragment", "onDestroyView called")
    }
    override fun onDetach() {
        super.onDetach()
        Log.i("TitleFragment", "onDetach called")
    }


}
