package sjsu.cmpe277.myandroidmulti.Question

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QuestionViewModel: ViewModel() {

    var fever: Boolean = false
    var injured: Boolean = false
    var phonenumber: String? = null
    //var nameinfo = yourname

    // The current question
    val question = MutableLiveData<String>()
    // The current risk score
    val riskscore = MutableLiveData<Int>()

    init {
        Log.i("QuestionViewModel", "QuestionViewModel created!")
        question.value = ""
        riskscore.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("QuestionViewModel","QuestionViewModel destroyed!")
    }

    fun updateRiskscore() {
        riskscore.value = (riskscore.value)?.plus(1)
    }

    private fun nextQuestion() {
        question.value = "test question"
    }

}