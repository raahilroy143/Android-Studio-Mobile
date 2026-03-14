package com.mrrvk.assistant.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mrrvk.assistant.MrRvkApp
import com.mrrvk.assistant.R
import com.mrrvk.assistant.service.AssistantService
import com.mrrvk.assistant.util.ResponseGenerator

class MainActivity : AppCompatActivity(), AssistantService.AssistantListener {

    private lateinit var hologramView: HologramView
    private lateinit var statusIndicator: View
    private lateinit var statusLabel: TextView
    private lateinit var responseText: TextView
    private lateinit var partialText: TextView
    private lateinit var commandInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var conversationContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var welcomeText: TextView

    private var service: AssistantService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val assistantBinder = binder as AssistantService.AssistantBinder
            service = assistantBinder.getService()
            service?.setListener(this@MainActivity)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service?.setListener(null)
            service = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if setup is complete
        if (!MrRvkApp.isSetupComplete(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        startAssistantService()
    }

    private fun initViews() {
        hologramView = findViewById(R.id.hologramView)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusLabel = findViewById(R.id.statusLabel)
        responseText = findViewById(R.id.responseText)
        partialText = findViewById(R.id.partialText)
        commandInput = findViewById(R.id.commandInput)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        conversationContainer = findViewById(R.id.conversationContainer)
        scrollView = findViewById(R.id.scrollView)
        welcomeText = findViewById(R.id.welcomeText)

        val userName = MrRvkApp.getUserName(this)
        welcomeText.text = "Welcome, $userName"

        hologramView.startAnimations()
        hologramView.activate()
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val text = commandInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendCommand(text)
                commandInput.text.clear()
                hideKeyboard()
            }
        }

        btnMic.setOnClickListener {
            // Toggle manual mic listening
            service?.startWakeWordListening()
            hologramView.setListening(true)
            statusLabel.text = "Listening..."
            addConversationBubble("🎤 Listening...", false)
        }

        commandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val text = commandInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    sendCommand(text)
                    commandInput.text.clear()
                    hideKeyboard()
                }
                true
            } else false
        }
    }

    private fun sendCommand(text: String) {
        addConversationBubble(text, true)
        service?.executeDirectCommand(text)
    }

    private fun addConversationBubble(text: String, isUser: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 14f
            setPadding(32, 20, 32, 20)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
                bottomMargin = 4
                if (isUser) {
                    marginStart = 80
                    gravity = android.view.Gravity.END
                } else {
                    marginEnd = 80
                    gravity = android.view.Gravity.START
                }
            }
            layoutParams = params

            if (isUser) {
                setBackgroundResource(R.drawable.bubble_user)
                setTextColor(0xFFFFFFFF.toInt())
            } else {
                setBackgroundResource(R.drawable.bubble_assistant)
                setTextColor(0xFF00E5FF.toInt())
            }
        }

        conversationContainer.addView(bubble)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }

        // Limit conversation history
        if (conversationContainer.childCount > 50) {
            conversationContainer.removeViewAt(0)
        }
    }

    private fun startAssistantService() {
        AssistantService.start(this)
        bindService(
            Intent(this, AssistantService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(commandInput.windowToken, 0)
    }

    // AssistantListener callbacks
    override fun onWakeWordDetected() {
        runOnUiThread {
            hologramView.activate()
            hologramView.setListening(true)
            statusLabel.text = "Wake word detected!"
            statusIndicator.setBackgroundResource(R.drawable.status_active)
        }
    }

    override fun onListeningStarted() {
        runOnUiThread {
            hologramView.setListening(true)
            statusLabel.text = "Listening for command..."
            partialText.visibility = View.VISIBLE
            partialText.text = ""
        }
    }

    override fun onListeningStopped() {
        runOnUiThread {
            hologramView.setListening(false)
            partialText.visibility = View.GONE
        }
    }

    override fun onCommandRecognized(text: String) {
        runOnUiThread {
            statusLabel.text = "Processing..."
            hologramView.setDisplayText(text)
            addConversationBubble(text, true)
        }
    }

    override fun onSpeaking(text: String) {
        runOnUiThread {
            statusLabel.text = "Speaking..."
            responseText.text = text
            hologramView.setDisplayText(text)
            addConversationBubble(text, false)
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusLabel.text = "Error: $message"
        }
    }

    override fun onPartialResult(text: String) {
        runOnUiThread {
            partialText.text = text
        }
    }

    override fun onResume() {
        super.onResume()
        hologramView.startAnimations()
        if (isBound) {
            service?.setListener(this)
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't stop animations - service continues in background
    }

    override fun onDestroy() {
        if (isBound) {
            service?.setListener(null)
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
        // Service continues running in background
    }
}
