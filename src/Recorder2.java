package com.samir_ahmed.Iris;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javaFlacEncoder.FLAC_FileEncoder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Recorder2 {

	/**
	 * Microphone is a nested Class for executing the recording
	 */

	public class Microphone extends Thread
	{
		private TargetDataLine			m_line;
		private AudioFileFormat.Type	m_targetType;
		private AudioInputStream		m_audioInputStream;
		private File					m_outputFile;

		public Microphone(TargetDataLine line,
				AudioFileFormat.Type targetType,
				File file)
		{
			m_line = line;
			m_audioInputStream = new AudioInputStream(line);
			m_targetType = targetType;
			m_outputFile = file;
		}

		//start recording 
		public void start()
		{
			m_line.start();
			super.start();
		}

		//stop recording
		public void stopRecording()
		{
			m_line.stop();
			m_line.close();
		}

		public void run()
		{
			m_line.start();
			try
			{
				AudioSystem.write(
						m_audioInputStream,
						m_targetType,
						m_outputFile);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private String strFilename;
	private File outputFile;
	private File croppedWaveFile;
	private AudioFormat audioFormat;
	private TargetDataLine primaryLine;
	private TargetDataLine	secondaryLine;
	private AudioFileFormat.Type targetType;
	private Microphone	mic1;
	private double maxRMS;
	private double staticAverage;
	private double signalAverage;
	private long speechDetectionTime;
	private boolean speechDetected;

	private final double NOISE_FACTOR= 4.00;
	private final long BYTES_PER_MILLISECOND = 16*4;
	private final long WAVE_HEADER = 44;
	private final double SILENCE_FACTOR= 0.20;
	private final long NOSPEECH_TIMEOUT = 10000;
	private final long LONGSPEECH_TIMEOUT = 10000;
	private final ExecutorService exs;
	private final int SAMPLE_RATE= 40;
	private final long SMOOTHENING_BUFFER = 200;
	private final int AUTOSTOP_RATE=40;


	/** Constructor, only requires the execution service for threading*/
	public Recorder2(ExecutorService ExServ){

		// Assign Executor Service to exs
		this.exs = ExServ; 
		this.speechDetected = false;
		this.strFilename = "infile.wav";
		this.outputFile = new File(strFilename);

		/*	Create Audioformat
			Frame Rate 16Khz from the Sample Rate of 16Khz
			2 Channels and 16 bit per channel 
		 */
		this.audioFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				16000.0F, 16, 2, 4, 16000.0F, false);

		/*Get a 2 datalines from the Computer microphone with given Audio Format*/
		DataLine.Info	info = new DataLine.Info(TargetDataLine.class, audioFormat);
		this.primaryLine = null;

		DataLine.Info	info2 = new DataLine.Info(TargetDataLine.class, audioFormat);
		this.secondaryLine = null;

		/* Try to open a data line with defined AudioFormat*/
		try{
			primaryLine = (TargetDataLine) AudioSystem.getLine(info);
			primaryLine.open(audioFormat);
			secondaryLine = (TargetDataLine) AudioSystem.getLine(info2);
			secondaryLine.open(audioFormat);
		}
		catch (LineUnavailableException luEx){
			System.out.println("unable to get a recording line");
			luEx.printStackTrace();
			System.exit(1);
		}

		/*Save data as format WAVE*/
		this.targetType = AudioFileFormat.Type.WAVE;


		/*Create a Recorder2 to Get Input Data on mic1*/
		this.mic1 = new Microphone(
				primaryLine,
				targetType,
				outputFile);

	}

	/** isActive(); calling this method will block execution until speech is heard or the microphone has timed out
	 *  will return true once voice activity is measured otherwise false*/
	public boolean isActive() throws InterruptedException, IOException{

		/* Get the bytes per second */
		int bytesPerSec = audioFormat.getSampleSizeInBits() * (int) audioFormat.getSampleRate();

		/* Set the sampling frequency [assuming samples at this rate ...]*/
		int sampleDataBufferSize = bytesPerSec / this.SAMPLE_RATE;

		/* Create a byte array of appropriate sample size*/
		byte[] sampleDataByteArray = new byte[sampleDataBufferSize];

		/* Start the microphone and the secondaryLine for sampling*/
		//exs.submit(mic1);
		mic1.start();
		secondaryLine.start();

		/* Intializing for scope*/
		int sampleCount=0;
		long startTime = System.currentTimeMillis();
		LinkedList<Double> rmsDeque = new LinkedList<Double>();
		double total =0;

		/* Indicate that recording has started*/
		System.out.println("Listening");

		/* While we are silent, i.e no speech detected, we remain in this loop*/
		while (isSilent(sampleCount++,startTime)){

			/* Blocking Read Assignment : will fill sampleDataByteArray */
			secondaryLine.read(sampleDataByteArray, 0, sampleDataBufferSize);
			
			/* Convert the array to floating point values between zero and one*/
			float[] fArray = Recorder2.getFloatArray(sampleDataByteArray);

			/* Determine the root mean square*/
			Double rms = Recorder2.getRMS(fArray);

			/* Push the rms to the back of the deque*/
			rmsDeque.push(rms); 

			/* If we have a sample size of five, 
			 * we dequeue the last value and average the new one in 
			 * Otherwise we just start to sum the total for staticAverage */

			if(rmsDeque.size()>5){
				maxRMS = getMaxRMS(rmsDeque); 
				total+= rms;
				staticAverage = total/sampleCount;
				rmsDeque.removeLast();
			}
			else{
				total += rms;
			}

			/* For User Feedback */
			System.out.println(maxRMS/staticAverage);
		}

		/* Determine the time at which Speech was detect (necessary for cropping later) */
		this.speechDetectionTime = getSpeechDetectionTime(System.currentTimeMillis(),startTime);

		/* If we have timed-out waiting for speech we close the lines,
		 * return false for isActive 
		 * otherwise true
		 *  */
		if ((System.currentTimeMillis()-startTime) >= NOSPEECH_TIMEOUT){
			mic1.stopRecording();
			secondaryLine.flush();
			secondaryLine.close();
			this.speechDetected = false;
		}
		else{
			this.speechDetected = true;
		}

		return this.speechDetected;
	}

	/** Public facing method, to be called right after isActive().
	 *  This method will call private helper methods to
	 *  Auto-stop the recording once the sound level has died down
	 *  Crop the recording to reduce filesize
	 *  Convert the recording to flac
	 *  
	 *  */
	public void record(){
		autoStop();
		crop();
		convert();		
	}

	/** Crop: Will Take the previous file, discard any period of inactivity format the remainder as .WAV*/
	private void crop(){
		try{

			/* Load up the previous file*/
			FileInputStream istream = new FileInputStream(this.outputFile);		

			/*Calculate the number of bytes to be cropped*/
			long cropLength = WAVE_HEADER+((long)speechDetectionTime*BYTES_PER_MILLISECOND);

			/* Initialize the new cropped.wav file */
			this.croppedWaveFile = new File("cropped.wav");

			/* Create a wave input stream from the file input stream */
			AudioInputStream waveStream = new AudioInputStream(istream,audioFormat,this.outputFile.length());

			/*Discard all the bytes until where we marked activity */
			waveStream.skip(cropLength);

			/*Write it to file in wave format*/
			AudioSystem.write(waveStream, 
					AudioFileFormat.Type.WAVE, 
					this.croppedWaveFile);

			/*Close the wavefile and file stream*/
			waveStream.close();
			istream.close();
		}
		catch(IOException IOe){
			IOe.printStackTrace();
		}
	}

	/** Using JFLACENCODER, convert files to flac. */
	private void convert(){
		FLAC_FileEncoder encoder1 = new FLAC_FileEncoder();
		File infile = this.croppedWaveFile;
		File outfile = new File("recording.flac");
		//encoder1.useThreads(false);
		encoder1.encode(infile, outfile);
	}

	/** Very Similary to isSilent, except this method will record until the maxRMS falls below a given threshold*/
	private void autoStop(){

		/* Similar variables as before !See isActive() Method!*/
		int bytesPerSec = audioFormat.getSampleSizeInBits() * (int) audioFormat.getSampleRate();
		int sampleDataBufferSize = bytesPerSec / this.AUTOSTOP_RATE;
		int sampleCount=0;
		long startTime = System.currentTimeMillis();
		double total =0;
		byte[] sampleDataByteArray = new byte[sampleDataBufferSize];
		LinkedList<Double> rmsDeque = new LinkedList<Double>();
		System.out.println("Recording");

		secondaryLine.start();

		/*While still speaking, keep recording*/
		while (isSpeech(startTime,sampleCount++)){

			/*Blocking read assignment to the buffer*/
			secondaryLine.read(sampleDataByteArray, 0, sampleDataBufferSize);

			/* Generate floating point values between 0 and 1*/
			float[] fArray = Recorder2.getFloatArray(sampleDataByteArray);

			/* Calculate RMS */
			Double rms = Recorder2.getRMS(fArray);

			/* Push to Deque and calculate an average while recieving signal and maxRMS */
			rmsDeque.push(rms); 
			if(rmsDeque.size()>5){
				maxRMS = getMaxRMS(rmsDeque);
				total+=rms;
				signalAverage = total/sampleCount;
				rmsDeque.removeLast();
			}
			else{
				total += rms;
			}

			/*For User FeedBack*/
			System.out.println(maxRMS/staticAverage +"      " + maxRMS/signalAverage);
		}

		//Iris.printTime();
		System.out.println("Processing...");

		/*Stop Recording, flush lines and close*/
		mic1.stopRecording();
		secondaryLine.flush();
		secondaryLine.close();
	}

	/** Test for speech or if Timed Out. Return true if still speaking, otherwise returns false*/
	private boolean isSpeech(long startTime, int count){

		/*If at least 5 samples have been taken, and we have not TIMEDOUT
		 * check if the MaxRMS is Below the Original Static Average
		 * check if the MaxRMS is A factor of 10 below the Signal Average
		 * if so return false, otherwise true and break the loop
		 * */

		if (count>5){
			if ((System.currentTimeMillis()-startTime) < LONGSPEECH_TIMEOUT && 
					(this.maxRMS > this.staticAverage*(NOISE_FACTOR/2)) && 
					(maxRMS > this.signalAverage*SILENCE_FACTOR) )
			{
				return true;
			}
			else{
				return false;
			}
		}
		return true;		
	}

	/** Take in the long time when activity was first noticed and the microphones start time
	 *  Determine the amount of time to be cropped from the beginning */
	private int getSpeechDetectionTime(long activity,long start){
		long cropTime = activity - start;

		/* In the even that the cropTime is below 200 ms
		 * we return 0 because we use the 200 ms for a buffer
		 * to ensure more efficient speech recognition
		 * Otherwise we return upto 200 ms before we flagged any activity
		 * */

		if (cropTime< SMOOTHENING_BUFFER){
			return 0;
		}

		return (int) (cropTime - SMOOTHENING_BUFFER);
	}

	/** isSilent(), returns false if the microphone activity increases a factor of 4 above static noise level*/
	private boolean isSilent(int count, long startTime){

		/* If we have sampled enough data
		 * and not timed out
		 * we check if are a factor of 4 above the background static noise
		 * If so we return false to break the sampling loop
		 * Otherwise we return true to keep looping till activity is measured or we time out
		 * */

		if (count>5){
			if ((System.currentTimeMillis()-startTime) < NOSPEECH_TIMEOUT && 
					(this.maxRMS < this.staticAverage*this.NOISE_FACTOR))
			{
				return true;
			}
			else{
				return false;
			}
		}
		return true;
	}

	/** Iterator through the linkedlist and determine the maximum*/
	private static double getMaxRMS(LinkedList<Double> rmsDeque){

		/* Assume first is max, iterator through and replace max with maximum RMS value of the last five*/
		double max = rmsDeque.getFirst();
		for (double dd: rmsDeque){
			if(dd>max){
				max=dd;
			}
		}
		return max;
	}

	/** Calculate the Root Mean Square and return it as double*/
	private static double getRMS(float [] fArray){

		double total=0.0;

		/*Iterate through the array and square and sum all the terms*/
		for (float sh : fArray){
			total+=(sh*sh);
		}

		/*Divide by the length of array and square root and return RMS*/
		double rms = Math.pow(total/(fArray.length),0.5d);
		return rms;
	}

	/** Convert the byte array to a readable float format, of values between zero and one*/
	private static float[] getFloatArray(byte [] audioDataByteArray){

		/* Convert from 16 bit byte array to 32 bit float
		 * Create buffer from the byte array as a short
		 * */
		ShortBuffer sBuffer = ByteBuffer.wrap(audioDataByteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] sArray = new short[sBuffer.capacity()];
		sBuffer.get(sArray);	

		/* Generate a float array with floats made from each short*/
		float[] fArray = new float[sArray.length];
		for (int ii = 0; ii < sArray.length; ii++) {
			fArray[ii] = ((float)sArray[ii])/0x8000;
		}

		return fArray;
	}

	/*Tester function: main*/


	public static void main(String[] args) throws InterruptedException, IOException {
		try{
			ExecutorService exs = Executors.newFixedThreadPool(20);
			Recorder2 rr = new Recorder2(exs);
			if (rr.isActive()){
				rr.record();
			}
			else{
			}
			exs.shutdownNow();
		}
		catch(Exception ee){
			ee.printStackTrace();
		}
	}

}
