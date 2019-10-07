package com.example.theliers

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import com.example.theliers.bluetooth.BluetoothHandler
import com.example.theliers.bluetooth.MyBluetoothService
import com.squareup.seismic.ShakeDetector
import kotlinx.android.synthetic.main.activity_play.*
import java.io.File

class PlayActivity : AppCompatActivity(), ShakeDetector.Listener, AdapterView.OnItemSelectedListener {

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(inputMessage: Message) {
            println("does this receive?")
            if (inputMessage.what == 1) {
                val receivedString = inputMessage.obj as String
                val order = receivedString.split(gibberish)
                println("decode ms")
                if(order.size == 3) {
                    println(order)
                    when (order[0]) {
                        "initGame" -> {
                            gameMaster.opponentValue = order[1].toInt()
                            gameMaster.decideTurn()
                        }

                        "bid" -> {
                            getOpponentGuessHistory(order[1].toInt(), order[2].toInt())
                            takeTurn()
                        }

                        "call" -> {
                            gameMaster.returnCall()
                            gameMaster.decideWin(order[1])
                        }

                        "returncall" -> {
                            gameMaster.decideWin(order[1])
                        }
                    }
                }
                println("it works somewhat")
            }
        }
    }

    private val gibberish = "--!@#$%^)+_-&*--"
    private val bluetoothService = BluetoothHandler.startBluetoothComm(mHandler)
    private val gameMaster = GameMaster(bluetoothService, this)

    //array list of int for spinner
    var totalList = listOf(1,2,3,4,5,6,7,8,9,10)
    var dicelist = listOf(1,2,3,4,5,6)

    //int to remember last opponent bid
    var totalBid = 0
    var typeBid = 0

    //string to remember players choice
    var opponentBid = ""
    var yourBid = ""

    var shakeTurn = 0
    private var playableState = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        txt_result.text = getString(R.string.shake_start)
        setUpSpinner()

        dice_no6.setImageResource(R.drawable.ic_dice_)
        dice_no7.setImageResource(R.drawable.ic_dice_)
        dice_no8.setImageResource(R.drawable.ic_dice_)
        dice_no9.setImageResource(R.drawable.ic_dice_)
        dice_no10.setImageResource(R.drawable.ic_dice_)
        displayBid("")

        if (playableState){
            val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val shakeDetector = ShakeDetector(this)
            shakeDetector.start(sensorManager)
        }

        btn_quit.setOnClickListener {
            finish()
        }

        btn_call.setOnClickListener {
            gameMaster.call()
        }

        btn_bid.setOnClickListener {
            bid()
        }

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

    }

    // listen to shake sensors
    override fun hearShake() {
        if (playableState) {
            if (shakeTurn==0) {
                gameMaster.initGame()
                shakeTurn++
                playableState = false
            } else {
                //val display =  gameMaster.enemyNumberOfDice + 1
                //enemyRoll.text = display.toString()
                gameMaster.startNextRound()
                playableState = false
            }
        }
        //txtView.setText(shakeTurn.toString()+ran)
    }

    //reset playable
    fun resetPlayable() {
        playableState = true
    }

    //init gameplay after shake
    fun gameInit(playOder: String) {
        when(playOder) {
            "first" -> {
                displayInfo("You go first")
                gameMaster.rollDice()
                takeFirstTurn()
                //startGameButton.isEnabled = false
            }

            "second" -> {
                displayInfo("You go second")
                gameMaster.rollDice() //roll from 0 to 4 aka 5 dice
                passTurn()
                //startGameButton.isEnabled = false
            }
        }
    }



    //display player dice


    // save to external storage
    fun saveToExternalStorage(diceArray: List<Int>) {
        if( diceArray.isNotEmpty() && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val inputText = diceArray.toString() + "\n"
            val filePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(filePath, "History.txt")
            file.appendText(inputText)
        } else {
            Toast.makeText(this, "No dice saved", Toast.LENGTH_LONG).show()
        }
    }

    //take your turn
    fun takeFirstTurn() {
        displayInfo("Your go first")
        btn_bid.isEnabled = true
        totalSpinner.isEnabled = true
        typeSpinner.isEnabled = true
    }

    //take your turn
    fun takeTurn() {
        displayInfo("Your go")
        btn_bid.isEnabled = true
        btn_call.isEnabled = true
        totalSpinner.isEnabled = true
        typeSpinner.isEnabled = true
    }

    //pass your turn
    fun passTurn(){
        displayInfo("Opponent turn")
        btn_bid.isEnabled = false
        btn_call.isEnabled = false
        totalSpinner.isEnabled = false
        typeSpinner.isEnabled = false
    }

    //get opponent roll
    fun getOpponentGuessHistory(total: Int, dice: Int) {
        opponentBid = "$total x $dice dice\n"
        displayBid("Opponent bids $opponentBid")
        totalBid = total
        typeBid = dice
        gameMaster.setCurrentBid(total,dice)
        println("------ current bid ------")
        println(gameMaster.currentBid)
        //setSpinnerChoice(total,dice)
        takeTurn()
    }

    // check illegal
    private fun checkIllegal(total: Int, dice: Int): Boolean {
        if(total < totalBid || dice < typeBid) return true
        if(total == totalBid && dice == typeBid) return true
        return false
    }

    //bid
    private fun bid() {
        val total = totalSpinner.selectedItem.toString()
        val type = typeSpinner.selectedItem.toString()

        if(checkIllegal(total.toInt(), type.toInt())) {
            Toast.makeText(this, "Illegal move", Toast.LENGTH_SHORT).show()
        } else {

            bluetoothService.sendInfo("bid$gibberish$total$gibberish$type")
            yourBid = total + " x " + type + "dice"+"\n"
            displayBid("You bids $yourBid")
            gameMaster.setCurrentBid(total.toInt(), type.toInt())
            passTurn()
        }

    }

    //set choice
    fun setSpinnerChoice(total: Int, dice: Int) {
        totalList = if (total < 9) {
            (total+1..10).toList()
        } else {
            listOf(10)
        }
        println("total list value")
        println(totalList)

        dicelist = if (dice < 5) {
            (dice+1..6).toList()
        } else {
            listOf(6)
        }
        println("total list value")
        println(totalList)
        totalSpinner.adapter
    }

    //set up spinner
    private fun setUpSpinner() {
        //set up total spinner
        totalSpinner.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,totalList)

        //set type spinner
        typeSpinner.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,dicelist)
    }

    //display enemy roll
    fun displayEnemyRoll(diceArray: List<String>){
        when(diceArray.size) {
            0 -> {
                setDiceImage(dice_no6,7)
                setDiceImage(dice_no7,7)
                setDiceImage(dice_no8,7)
                setDiceImage(dice_no9,7)
                setDiceImage(dice_no10,7)
            }

            1 -> {
                setDiceImage(dice_no6,diceArray[0].toInt())
                setDiceImage(dice_no7,7)
                setDiceImage(dice_no8,7)
                setDiceImage(dice_no9,7)
                setDiceImage(dice_no10,7)
            }
            2 -> {
                setDiceImage(dice_no6,diceArray[0].toInt())
                setDiceImage(dice_no7,diceArray[1].toInt())
                setDiceImage(dice_no8,7)
                setDiceImage(dice_no9,7)
                setDiceImage(dice_no10,7)
            }
            3 -> {
                setDiceImage(dice_no6,diceArray[0].toInt())
                setDiceImage(dice_no7,diceArray[1].toInt())
                setDiceImage(dice_no8,diceArray[2].toInt())
                setDiceImage(dice_no9,7)
                setDiceImage(dice_no10,7)
            }
            4 -> {
                setDiceImage(dice_no6,diceArray[0].toInt())
                setDiceImage(dice_no7,diceArray[1].toInt())
                setDiceImage(dice_no8,diceArray[2].toInt())
                setDiceImage(dice_no9,diceArray[3].toInt())
                setDiceImage(dice_no10,7)
            }
            5 -> {
                setDiceImage(dice_no6,diceArray[0].toInt())
                setDiceImage(dice_no7,diceArray[1].toInt())
                setDiceImage(dice_no8,diceArray[2].toInt())
                setDiceImage(dice_no9,diceArray[3].toInt())
                setDiceImage(dice_no10,diceArray[4].toInt())
            }
        }
    }

    //clear bid history
    fun clearGuessHistory(opponentDice: String) {
        val enemyDice = opponentDice.toInt()+1
        displayBid("Enemy has $enemyDice")
        totalBid = 0
        typeBid = 0
        opponentBid = ""
        yourBid = ""
    }

    //display info
    fun displayInfo(text: String) {
        txt_result.text = text
    }

    //display info
    fun displayBid(text: String) {
        txt_bid_info.text = text
    }

    //display win lost
    fun displayWin(win: String) {
        txt_result.text = "You $win last round."
    }

    //go to result
    fun goToResult(win: String){
        if(win == "win") {
            startActivity(Intent(this, winActivity::class.java))
        } else {
            startActivity(Intent(this, LoseActivity::class.java))
        }
    }

    fun displayDice(diceArray: List<Int>) {
        when(diceArray.size) {
            0 -> {
                setDiceImage(dice_no1,7)
                setDiceImage(dice_no2,7)
                setDiceImage(dice_no3,7)
                setDiceImage(dice_no4,7)
                setDiceImage(dice_no5,7)
            }

            1 -> {
                setDiceImage(dice_no1,diceArray[0])
                setDiceImage(dice_no2,7)
                setDiceImage(dice_no3,7)
                setDiceImage(dice_no4,7)
                setDiceImage(dice_no5,7)
            }
            2 -> {
                setDiceImage(dice_no1,diceArray[0])
                setDiceImage(dice_no2,diceArray[1])
                setDiceImage(dice_no3,7)
                setDiceImage(dice_no4,7)
                setDiceImage(dice_no5,7)
            }
            3 -> {
                setDiceImage(dice_no1,diceArray[0])
                setDiceImage(dice_no2,diceArray[1])
                setDiceImage(dice_no3,diceArray[2])
                setDiceImage(dice_no4,7)
                setDiceImage(dice_no5,7)
            }
            4 -> {
                setDiceImage(dice_no1,diceArray[0])
                setDiceImage(dice_no2,diceArray[1])
                setDiceImage(dice_no3,diceArray[2])
                setDiceImage(dice_no4,diceArray[3])
                setDiceImage(dice_no5,7)
            }
            5 -> {
                setDiceImage(dice_no1,diceArray[0])
                setDiceImage(dice_no2,diceArray[1])
                setDiceImage(dice_no3,diceArray[2])
                setDiceImage(dice_no4,diceArray[3])
                setDiceImage(dice_no5,diceArray[4])
            }
        }
        saveToExternalStorage(diceArray)
    }

    private fun setDiceImage(imageView: ImageView, number: Int){
        when (number) {
            1 -> imageView.setImageResource(R.drawable.ic_dice_1)
            2 -> imageView.setImageResource(R.drawable.ic_dice_2)
            3 -> imageView.setImageResource(R.drawable.ic_dice_3)
            4 -> imageView.setImageResource(R.drawable.ic_dice_4)
            5 -> imageView.setImageResource(R.drawable.ic_dice_5)
            6 -> imageView.setImageResource(R.drawable.ic_dice_6)
            else -> imageView.setImageResource(R.drawable.blackdice_background)
        }
    }

    private inner class GameMaster(val bluetoothService: MyBluetoothService, val playActivity: PlayActivity) {
        // Game Master class is to help control the flow of the game
        var numberOfDice = 4
        var enemyNumberOfDice = 4
        val yourRollList = mutableListOf<Int>()
        //first roll value
        var yourValue = -2
        var opponentValue = -2

        var currentBidder: String? = null
        var currentBid = arrayListOf(0,0)
        lateinit var lastRoundResult: String

        //init game by choosing a random value to decide who goes first
        fun initGame() {
            yourValue = (1..6).random()
            bluetoothService.sendInfo("initGame$gibberish$yourValue$gibberish"+0)
            decideTurn()
        }

        // decide between two players who will go first
        fun decideTurn() {
            if(yourValue > 0 && opponentValue > 0) {
                when {
                    yourValue > opponentValue -> {
                        println("you go first")
                        playActivity.gameInit("first")
                        currentBidder = "you"
                    }
                    yourValue < opponentValue -> {
                        println("you go second")
                        playActivity.gameInit("second")
                        currentBidder = "enemy"
                    }
                    else -> reRoll()
                }
            }
        }

        //get current bid
        fun setCurrentBid(total: Int, type: Int) {
            currentBid.clear()
            currentBid = arrayListOf(total, type)
        }

        // reRoll dice if two players roll the same value
        fun reRoll() {
            opponentValue = -2
            initGame()
            decideTurn()
        }

        // roll dice for game start
        fun rollDice(){
            yourRollList.clear()
            for (x in 0..numberOfDice) {
                yourRollList += (1..6).random()
            }
            playActivity.displayDice(yourRollList)
        }

        // challenge opponent bid
        fun call() {
            var sentInfo = "call$gibberish"
            yourRollList.forEach{
                sentInfo+= "$it-"
            }
            currentBidder = "enemy"
            sentInfo += "-!@#$%^)+_-&*--"
            playActivity.displayInfo("You challenged enemy bid")
            bluetoothService.sendInfo(sentInfo)
        }

        // return opponent call
        fun returnCall(){
            var sentInfo = "returncall$gibberish"
            yourRollList.forEach{
                sentInfo+= "$it-"
            }
            currentBidder = "you"
            playActivity.displayInfo("Enemy challenged your bid")
            sentInfo += "-!@#$%^)+_-&*--"
            bluetoothService.sendInfo(sentInfo)
        }

        // decide winnder of round
        fun decideWin(opponentRoll: String) {

            val oppRollList = opponentRoll.split("-")
            playActivity.displayEnemyRoll(oppRollList)
            val joinedList = oppRollList.toMutableList()
            yourRollList.forEach{
                joinedList.add(it.toString())
            }
            println(joinedList)
            val count = joinedList.count{it == currentBid[1].toString()}
            println("-----------------------")
            println(currentBidder)
            println(count)
            println(currentBid[0])
            println(currentBid[1])
            playActivity.displayBid("Bid is ${currentBid[0]} x ${currentBid[1]} dice. There are $count x ${currentBid[1]} dice.")
            if(currentBidder == "you") {
                lastRoundResult = if( count == currentBid[0] || count > currentBid[0]) {
                    println(1)
                    println("you win this round")
                    Toast.makeText(this@PlayActivity,"You win this \nStarting next round",Toast.LENGTH_LONG).show()
                    "win"
                } else {
                    println(2)
                    println("you lose this round")
                    Toast.makeText(this@PlayActivity,"You lose this round\nStarting next round",Toast.LENGTH_LONG).show()
                    "lost"
                }
            } else {
                lastRoundResult = if( count == currentBid[0] || count > currentBid[0]) {
                    println(3)
                    println("you lose this round")
                    Toast.makeText(this@PlayActivity,"You lost this \nStarting next round",Toast.LENGTH_LONG).show()
                    "lost"
                } else {
                    println(4)
                    println("you win this round")
                    Toast.makeText(this@PlayActivity,"You won this round\nStarting next round",Toast.LENGTH_LONG).show()
                    "win"
                }
            }
            playActivity.displayWin(lastRoundResult)
            playActivity.passTurn()
            playActivity.resetPlayable()
            //startNextRound()
            //playActivity.startGameButton.isEnabled = true
        }

        //start next round
        fun startNextRound() {
            currentBid.clear()
            currentBid = arrayListOf(0,0)

            if(lastRoundResult == "win") {
                enemyNumberOfDice--
                playActivity.clearGuessHistory(enemyNumberOfDice.toString())
                val enemyDisp = mutableListOf<String>()

                playActivity.displayEnemyRoll(enemyDisp)

                if(enemyNumberOfDice < 0) {
                    bluetoothService.stopConnect()
                    playActivity.goToResult("win")
                } else {
                    rollDice()
                    playActivity.passTurn()
                }

            } else {
                numberOfDice--
                playActivity.clearGuessHistory(enemyNumberOfDice.toString())

                if(numberOfDice < 0) {
                    bluetoothService.stopConnect()
                    playActivity.goToResult("lost")
                } else {
                    rollDice()
                    playActivity.takeFirstTurn()
                }
            }
        }
    }
}
