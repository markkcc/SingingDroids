package cc.markk.androidtestapp;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.badlogic.gdx.audio.analysis.FFT;

public class MainActivity extends Activity {
	public static final String EXTRA_MESSAGE = "cc.markk.AndroidTestApp.MESSAGE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	// This function will be invoked when the button is clicked
	public void singMessage(View view) {
		EditText editText = (EditText) findViewById(R.id.EditText01);
		String txtstr = editText.getText().toString();
		if(txtstr.equals(""))
			txtstr = "0";
		String binary = StoB(txtstr);
		int count = 0;
		String temp = "";
		int Freq = 0;
		while(count < binary.length()) {
			temp = binary.substring(count, count+8);
			Freq = BtoF(temp);
			for(int i = 0; i < bufferSize; i++){
				double angle = i / ((float)sampleRate/Freq) * 2.0 * Math.PI;
				audiodata[i] = (byte)(Math.sin(angle) * 127.0);
			}
			for (int i = 0; i < sampleRate / 100.0 && i < audiodata.length / 2; i++) {
				audiodata[i] = (byte)(audiodata[i] * i / (sampleRate / 100.0));
	            audiodata[audiodata.length - 1 - i] = (byte)(audiodata[audiodata.length - 1 - i] 
	                    * i / (sampleRate / 100.0));
	        }
			sound.play();
			sound.write(audiodata, 0, bufferSize);
			count+=8;
		}
		
	}
	
	public String StoB(String s) {
		String binary = "";
		byte[] bytes = s.getBytes();
		for(int i = 0; i < bytes.length; i++) {
			int val = bytes[i];
			for(int k = 0; k < 8; k++) {
				binary += ((val & 128) == 0? 0 : 1);
				val<<=1;
			}
		}
		return binary;
	}// String to Binary
	
	public int BtoF(String S) {
		int multi = Integer.parseInt(S,2);
		//int Freq = 100 + multi * 3; //EQUATION
		int Freq = multi*30;
		
		return Freq;
	}//Binary to Frequency 
	
	//sing a single tone, basically for testing.
	public void singString(View view) {
		EditText editText = (EditText) findViewById(R.id.EditText01);
		String txtstr = editText.getText().toString();
		if(txtstr.equals(""))
			txtstr = "0";
		int freq = Integer.parseInt(txtstr);
		for(int i = 0; i < bufferSize; i++){
			double angle = i / ((float)sampleRate/freq) * 2.0 * Math.PI;
			audiodata[i] = (byte)(Math.sin(angle) * 127.0);
		}
		for (int i = 0; i < sampleRate / 100.0 && i < audiodata.length / 2; i++) {
			audiodata[i] = (byte)(audiodata[i] * i / (sampleRate / 100.0));
            audiodata[audiodata.length - 1 - i] = (byte)(audiodata[audiodata.length - 1 - i] 
                    * i / (sampleRate / 100.0));
        }
		sound.play();
		sound.write(audiodata, 0, bufferSize);
	}
	
	//THE GLOBALS!
	int msecs = 250;
	int sampleRate = 44100; //in Hz. Divide by 1, 2 or 4 for optimal quality (to obtain one of: 44100, 22050 or 11025).
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
	int bufferSize = (int)sampleRate * msecs / 1000;
	int recBufferSize = (bufferSize)*20;
	byte[] audiodata = new byte[bufferSize];
	byte[] recData = new byte[recBufferSize];
	float[] recordedData;
	int fftSize = 4096*4; //must be a power of 2
	float[] fftTempArray = new float[fftSize];
	float[] fftSpectrum;
	
	AudioTrack sound = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioEncoding, 4096, AudioTrack.MODE_STREAM);
	
	public void playSound(View view) {
		//play the frequency in the message box using the AudioTrack class
		sound.play();
		
		for(int hz = 0; hz <= 4500; hz += 20) {
			for(int i = 0; i < bufferSize; i++){
				double angle = i / ((float)sampleRate/hz) * 2.0 * Math.PI;
				audiodata[i] = (byte)(Math.sin(angle) * 127.0);
			}
			
			//loop below is to make it smoother
			for (int i = 0; i < sampleRate / 100.0 && i < audiodata.length / 2; i++) {
				audiodata[i] = (byte)(audiodata[i] * i / (sampleRate / 100.0));
	            audiodata[audiodata.length - 1 - i] = (byte)(audiodata[audiodata.length - 1 - i] 
	                    * i / (sampleRate / 100.0));
	        }
			
			sound.write(audiodata, 0, bufferSize);
		}
	}
	
	public void recSound(View view) {
		AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, audioEncoding, recBufferSize);
		try{
			audioRecord.startRecording();
			audioRecord.read(recData, 0, recBufferSize);
			audioRecord.stop();
			playRecSound(view);
			
		} catch( Throwable t) {
			Log.e("RECORD", t.toString());
		}
	}
	
	public void playRecSound(View view) {
		//if you want to play the sound clip recorded:
		//sound.play();
		//sound.write(recData, 0, recBufferSize);
		
		//transfer to an array of float for valid arguments
		float[] oldData = new float[recData.length];
		for(int i = 0; i < recData.length; i++) {
			oldData[i] = (float)recData[i];
		}
		recordedData = oldData;
		
		//compute the fft
		float[] freqs = new float[recBufferSize/bufferSize];
		for(int i = 0; i < freqs.length; i++)
			freqs[i] = fft(i*bufferSize, i*bufferSize+bufferSize-1);
		
		int bits = 8;
		
		//String frequenciesConcat = "";
		//String temp = "";
		int tempFreq = 0;
		String finalText = "";
		
		for(int i = 0; i < freqs.length; i++) {
			
			//tempFreq = (int)(freqs[i] - 100)/3; //EQUATION
			int temp = (int)freqs[i];
			int iter = 32;
			
			while(temp > iter*30)
				iter++;
			
			int diff1 = temp-iter*30;
			int diff2 = temp-(iter-1)*30;
			if(diff1 <= diff2)
				tempFreq = iter;
			else
				tempFreq = iter-1;
			
			System.out.println("freqs["+i+"]: " + freqs[i] + ", tempFreq: " + tempFreq);
			
			//old code in case more than 8 bits:
			/*
			temp = Integer.toBinaryString(tempFreq);
			int stringLength = temp.length();
			
			if(stringLength==0)
				continue;
			
			//System.out.println("temp before loop: " + temp+"\nDecimal: "+tempFreq+"\nFrequency: "+freqs[i]);
			if(stringLength > bits) {
				temp = temp.substring(0, bits);
			}
			
			if(stringLength < bits){
				for(int j = 0; j < (bits - stringLength); j++)
					temp = "0" + temp;
			}
			frequenciesConcat += temp;*/
			finalText += (char)tempFreq;
		}
		
		/*int count = 0;
		int value = 0;
		while(count < frequenciesConcat.length()) {
			String character = frequenciesConcat.substring(count, count + bits);
			value = Integer.parseInt(character, 2);
			finalText += (char)value;
			count += bits;
		}*/
		
		EditText editText = (EditText) findViewById(R.id.EditText01);
		editText.setText(finalText);
	}
	
	
	private float fft(int start, int end) {
		
		for (int i = start; i < end; i++) {

			if(i - start < fftTempArray.length)
				fftTempArray[i - start] = recordedData[i];
		}
		
		//old power of 2 was: 8192
		FFT fft = new FFT(fftSize, sampleRate); //changed to become power of 2
		fft.forward(fftTempArray);
		fftSpectrum = fft.getSpectrum();
		
		float maxFreq = 0;
		float tempFreq = 0;
		int indexOfMaxMag = 0;
		
		for(int i = 0; i < 5000; i++) {
			tempFreq = fft.getFreq(i);
			if(tempFreq > maxFreq){
				maxFreq = tempFreq;
				indexOfMaxMag = i;
			}
		}
		return indexOfMaxMag;

	}
	
	@Override
	protected void onPause() {
		if(sound != null) {
			if(sound.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
				sound.pause();
			}
		}
		super.onPause();
	}
}