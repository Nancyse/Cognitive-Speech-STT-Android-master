/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 * //
 * Project Oxford: http://ProjectOxford.ai
 * //
 * ProjectOxford SDK GitHub:
 * https://github.com/Microsoft/ProjectOxford-ClientSDK
 * //
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 * //
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * //
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * //
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.CognitiveServicesExample;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.microsoft.bing.speech.Conversation;
import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.Confidence;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents
{
    int m_waitSeconds = 0;   //等待的秒数
    DataRecognitionClient dataClient = null;  //使用文件认知客户端
    MicrophoneRecognitionClient micClient = null;  //使用麦克风认知服务客户端
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;  //响应状态
    EditText _logText;
    RadioGroup _radioGroup;
    Button _buttonSelectMode;
    Button _startButton;

    String resultString=null;  //识别结果

    public enum FinalResponseStatus { NotReceived, OK, Timeout }  //最后的响应状态

    /**
     * Gets the primary subscription key
     */
    public String getPrimaryKey() {  //获取主要的key
        return this.getString(R.string.primaryKey);
    }

    /**
     * Gets the LUIS application identifier.
     * @return The LUIS application identifier.
     */
    private String getLuisAppId() {   //获取luisAPPID
        return this.getString(R.string.luisAppID);
    }

    /**
     * Gets the LUIS subscription identifier.
     * @return The LUIS subscription identifier.
     */
    private String getLuisSubscriptionID() {  //获取luis的标识符
        return this.getString(R.string.luisSubscriptionID);
    }

    /**
     * Gets a value indicating whether or not to use the microphone.
     * @return true if [use microphone]; otherwise, false.
     */
    private Boolean getUseMicrophone() {   //获取是否需要麦克风的指示
        int id = this._radioGroup.getCheckedRadioButtonId();
        return id == R.id.micIntentRadioButton ||
                id == R.id.micDictationRadioButton ||
                id == (R.id.micRadioButton - 1);
    }

    /**
     * Gets a value indicating whether LUIS results are desired.  
     * @return true if LUIS results are to be returned otherwise, false.
     */
    private Boolean getWantIntent() {  //获取是否需要luis的指示
        int id = this._radioGroup.getCheckedRadioButtonId();
        return id == R.id.dataShortIntentRadioButton ||
                id == R.id.micIntentRadioButton;
    }

    /**
     * Gets the current speech recognition mode.  
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {  //获取当前语音认知的模式
        int id = this._radioGroup.getCheckedRadioButtonId();
        if (id == R.id.micDictationRadioButton ||
                id == R.id.dataLongRadioButton) {
            return SpeechRecognitionMode.LongDictation;
        }

        return SpeechRecognitionMode.ShortPhrase;
    }

    /**
     * Gets the default locale.  
     * @return The default locale.
     */
    private String getDefaultLocale() {  //获取语言
        return "zh-cn";
    }

    /**
     * Gets the short wave file path.  
     * @return The short wave file.
     */
    private String getShortWaveFile() {  //获取短录音
        return "whatstheweatherlike.wav";
    }

    /**
     * Gets the long wave file path.  
     * @return The long wave file.
     */
    private String getLongWaveFile() {  //获取长录音
        return "batman.wav";
    }

    /**
     * Gets the Cognitive Service Authentication Uri.
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {   //获取认知服务的uri
        return this.getString(R.string.authenticationUri);
    }

    /*
    *获得最后的识别结果
    * @return 识别的结果
    * */
    public String getResult(){
        if( resultString ==null ){
            return "请输入语音";
        }
        else{
            return this.resultString;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {  //启动主页面
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this._logText = (EditText) findViewById(R.id.editText1);
        this._radioGroup = (RadioGroup)findViewById(R.id.groupMode);
        this._buttonSelectMode = (Button)findViewById(R.id.buttonSelectMode);
        this._startButton = (Button) findViewById(R.id.button1);

        if (getString(R.string.primaryKey).startsWith("Please")) {  //如果没有primary key的话
			/*弹出输入订阅key的对话框*/
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        // setup the buttons 
        final MainActivity This = this;
        this._startButton.setOnClickListener(new OnClickListener() {  //设置“start”按钮，
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click(arg0);
            }
        });

        this._buttonSelectMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.ShowMenu(This._radioGroup.getVisibility() == View.INVISIBLE);
            }
        });

        this._radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                This.RadioButton_Click(rGroup, checkedId);
            }
        });

        this.ShowMenu(true);
    }

    private void ShowMenu(boolean show) {  //显示菜单
        if (show) {
            this._radioGroup.setVisibility(View.VISIBLE);   //显示按钮
            this._logText.setVisibility(View.INVISIBLE);  //不显示文本
        } else {
            this._radioGroup.setVisibility(View.INVISIBLE);
            this._logText.setText("");
            this._logText.setVisibility(View.VISIBLE);
        }
    }
    /**
     * Handles the Click event of the _startButton control.  
     */
    private void StartButton_Click(View arg0) {  //“start”按钮的点击事件
        this._startButton.setEnabled(false);
        this._radioGroup.setEnabled(false);

        this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;  //获取语音模式对应的等待秒数

        this.ShowMenu(false);

        this.LogRecognitionStart();  //初始化认知服务对象

        if (this.getUseMicrophone()) {  //判断是否需要麦克风
            if (this.micClient == null) {  //判断使用麦克风认知服务是否为空 
                if (this.getWantIntent()) {  //判断是否需要luis的服务

                    //this.WriteLine("--- Start microphone dictation with Intent detection ----");
					
					//实例化麦克风传递数据的客户端对象
                    this.micClient =
                            SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                                    this,
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimaryKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else
                {
                    this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                            this,
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey());
                }

                this.micClient.setAuthenticationUri(this.getAuthenticationUri());  //是指麦克风语音服务的认证uri
            }
			//调用语音认知服务
            this.micClient.startMicAndRecognition();  //启用麦克风和语音识别服务
        }
        else  //如果不使用麦克风
        {
            if (null == this.dataClient) {
                if (this.getWantIntent()) {
					
					//实例传送文件数据的客户端对象
                    this.dataClient =
                            SpeechRecognitionServiceFactory.createDataClientWithIntent(
                                    this,
                                    this.getDefaultLocale(),
                                    this,
                                    this.getPrimaryKey(),
                                    this.getLuisAppId(),
                                    this.getLuisSubscriptionID());
                }
                else {
                    this.dataClient = SpeechRecognitionServiceFactory.createDataClient(
                            this,
                            this.getMode(),
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey());
                }
				
				//设置认证的uri
                this.dataClient.setAuthenticationUri(this.getAuthenticationUri());
            }

            this.SendAudioHelper((this.getMode() == SpeechRecognitionMode.ShortPhrase) ? this.getShortWaveFile() : this.getLongWaveFile());
        }
    }

    /**
     * Logs the recognition start.  
     */
    private void LogRecognitionStart() {  //初始化认知服务的参数
        String recoSource;
        if (this.getUseMicrophone()) { //判断是否选择麦克风
            recoSource = "microphone";
        } else if (this.getMode() == SpeechRecognitionMode.ShortPhrase) {  //判断是否选择短录音
            recoSource = "short wav file";
        } else {   //判断是否选择长录音
            recoSource = "long wav file";
        }

        this.WriteLine("\n--- Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language ----\n\n");
    }

    private void SendAudioHelper(String filename) {    //发送语音
        RecognitionTask doDataReco = new RecognitionTask(this.dataClient, this.getMode(), filename);  //执行文件记录
        try
        {
            doDataReco.execute().get(m_waitSeconds, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
        }
    }

    public void onFinalResponseReceived(final RecognitionResult response) {   //当获得响应结果
        //isFinalDicationMessage: 是否是长语音的最终的文本
        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone() && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDicationMessage) {
            this._startButton.setEnabled(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {  //不是长语音的最终的文本
            this.WriteLine("********* Final n-BEST Results *********");

            for (int i = 0; i < response.Results.length; i++) {
                /*
                this.WriteLine("[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                        " Text=\"" + response.Results[i].DisplayText + "\"");
                */
                if (response.Results[i].Confidence== Confidence.High){
                    this.WriteLine(response.Results[i].DisplayText);
                    this.resultString=response.Results[i].DisplayText;
                    break;
                }

            }
            this.WriteLine();
        }
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {  //当获得luis的响应结果时
        this.WriteLine("--- Intent received by onIntentReceived() ---");
        this.WriteLine(payload);
        this.WriteLine();
    }

    public void onPartialResponseReceived(final String response) {  //当获得部分响应结果时
        if(  getMode()==SpeechRecognitionMode.LongDictation ){  //如果是长语音
            this.WriteLine("--- Partial result received by onPartialResponseReceived() ---");
            this.WriteLine(response);
            this.WriteLine();
        }
    }

    public void onError(final int errorCode, final String response) {  //当出错时
        this._startButton.setEnabled(true);
        this.WriteLine("--- Error received by onError() ---");
        this.WriteLine("Error code: " + SpeechClientStatus.fromInt(errorCode) + " " + errorCode);
        this.WriteLine("Error text: " + response);
        this.WriteLine();
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {  //当麦克风的状态改变时
        this.WriteLine("--- Microphone status change received by onAudioEvent() ---");
       // this.WriteLine("********* Microphone status: " + recording + " *********");
        if (recording) {
            this.WriteLine("********* Microphone status: " + recording + " *********");
            this.WriteLine("Please start speaking.");
        }
        else {
            this.WriteLine("Stop Recording.");
        }

        WriteLine();
        if (!recording) {
            this.micClient.endMicAndRecognition();
            this._startButton.setEnabled(true);
        }
    }

    /**
     * Writes the line.
     */
    private void WriteLine() {  //一条空白线
        this.WriteLine("");
    }

    /**
     * Writes the line.
     * @param text The line to write.
     */
    private void WriteLine(String text) {   //画线
        this._logText.append(text + "\n");
    }

    /**
     * Handles the Click event of the RadioButton control.
     * @param rGroup The radio grouping.
     * @param checkedId The checkedId.
     */
    private void RadioButton_Click(RadioGroup rGroup, int checkedId) {  //  单选按钮的选项
        // Reset everything
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
            try {
                this.micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.micClient = null;
        }

        if (this.dataClient != null) {
            try {
                this.dataClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.dataClient = null;
        }

        this.ShowMenu(false);
        this._startButton.setEnabled(true);
    }

    /*
     * Speech recognition with data (for example from a file or audio source).  
     * The data is broken up into buffers and each buffer is sent to the Speech Recognition Service.
     * No modification is done to the buffers, so the user can apply their
     * own VAD (Voice Activation Detection) or Silence Detection
     * 
     * @param dataClient
     * @param recoMode
     * @param filename
     */
    private class RecognitionTask extends AsyncTask<Void, Void, Void> {    //认知服务
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any 
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe 
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                InputStream fileStream = getAssets().open(filename);
                int bytesRead = 0;
                byte[] buffer = new byte[1024];  //缓存窗口

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service. 
                        dataClient.sendAudio(buffer, bytesRead);   //发送语音
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            finally {
                dataClient.endAudio();  //结束发送语音
            }

            return null;
        }
    }
}
