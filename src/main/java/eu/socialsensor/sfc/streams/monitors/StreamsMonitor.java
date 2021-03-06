package eu.socialsensor.sfc.streams.monitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import eu.socialsensor.framework.common.domain.Feed;
import eu.socialsensor.framework.streams.Stream;

/**
 * Class for monitoring the streams that correspond to each social network
 * (Twitter, Youtube,Flickr,Instagram,Tumblr,Facebook,GooglePlus)
 * @author ailiakop
 * @email  ailiakop@iti.gr
 */
public class StreamsMonitor {
	private static final long DEFAULT_REQUEST_TIME = 3600000 ; //1 hour in milliseconds 
	
	public final Logger logger = Logger.getLogger(StreamsMonitor.class);

	ExecutorService executor;
	
	Map<String,Stream> streams = new HashMap<String,Stream>();
	Map<String,List<Feed>> feedsPerStream = new HashMap<String,List<Feed>>();
	Map<String,Long> requestTimePerStream = new HashMap<String,Long>();
	Map<String,Long> runningTimePerStream = new HashMap<String,Long>();
	Map<String,StreamFetchTask> streamsFetchTasks = new HashMap<String,StreamFetchTask>();
	
	boolean isFinished = false;
	
	int totalRetrievedItems = 0;
	
	private ReInitializer reInitializer = new ReInitializer();
	
	public StreamsMonitor(int numberOfStreams){
		executor = Executors.newFixedThreadPool(numberOfStreams);
	}
	
	public int getTotalRetrievedItems(){
		return totalRetrievedItems;
	}
	
	public int getNumberOfStreamFetchTasks(){
		return streamsFetchTasks.size();
	}
	
	/**
	 * Adds the streams to the monitor
	 * @param streams
	 */
	public void addStreams(Map<String,Stream> streams){
		for(String streamId : streams.keySet()){
			addStream(streamId,streams.get(streamId));
		}
	}

	public void addStream(String streamId,Stream stream){
		this.streams.put(streamId, stream);
		this.requestTimePerStream.put(streamId, DEFAULT_REQUEST_TIME);
	}
	/**
	 * Adds a stream to the monitor
	 * @param stream
	 */
	public void addStream(String streamId,Stream stream,List<Feed> feeds){
		this.streams.put(streamId, stream);
		this.feedsPerStream.put(streamId, feeds);
		this.requestTimePerStream.put(streamId, DEFAULT_REQUEST_TIME);
	}
	
	public void setStreamRequestTime(String streamId,Long requestTime){
		this.requestTimePerStream.put(streamId, requestTime);
	}
	
	public void startStream(String streamId){
		if(!streams.containsKey(streamId)){
			logger.error("Stream "+streamId+" needs to be added to the monitor first");
			return;
		}
		
		StreamFetchTask streamTask = new StreamFetchTask(streams.get(streamId),feedsPerStream.get(streamId));
		streamsFetchTasks.put(streamId, streamTask);
		executor.execute(streamTask);
		runningTimePerStream.put(streamId, System.currentTimeMillis());
		
		logger.info("Start stream task : "+streamId+" with "+feedsPerStream.get(streamId).size()+" feeds");
	}

	/**
	 * Starts the retrieval process for each stream separately 
	 * as a different thread
	 * @param 
	 */
	public void startAllStreamsAtOnce(){
		
		for(Map.Entry<String, Stream> entry : streams.entrySet()){
			
			startStream(entry.getKey());
		}
		
		reInitializer.start();
	}
	
	public void startReInitializer(){
		reInitializer.start();
	}
	
	/**
	 * Starts the retrieval process for each stream separately 
	 * as a different thread with the same input feeds
	 * @param feeds
	 */
	public void startAllStreamsAtOnceWithStandarFeeds(List<Feed> feeds){
		totalRetrievedItems = 0;
		
		for(Map.Entry<String, Stream> entry : streams.entrySet()){
			
			StreamFetchTask streamTask = new StreamFetchTask(entry.getValue(),feeds);
			streamsFetchTasks.put(entry.getKey(), streamTask);
			executor.execute(streamTask);
			runningTimePerStream.put(entry.getKey(), System.currentTimeMillis());
			
			System.out.println("Start stream task : "+entry.getKey()+" with "+feeds.size()+" feeds");
		}
	}
	
	private class ReInitializer extends Thread{
		private Map<String,Long> reformedRunningTimes = new HashMap<String,Long>();
		
		public ReInitializer(){
			logger.info("ReInitializer Thread instantiated");
		}
		
		public void run(){
			logger.info("ReInitializer Thread started");
			while(!isFinished){
				long currentTime = System.currentTimeMillis();
				
				for(String streamId : runningTimePerStream.keySet()){
					if((currentTime - runningTimePerStream.get(streamId)) >= requestTimePerStream.get(streamId)){
						if(streamsFetchTasks.get(streamId).completed){
							streamsFetchTasks.get(streamId).restartTask();
							executor.execute(streamsFetchTasks.get(streamId));
							reformedRunningTimes.put(streamId, System.currentTimeMillis());
						}
					}
				}
				
				for(String streamId : reformedRunningTimes.keySet()){
					logger.info("Reinitializing Stream : "+streamId);
					runningTimePerStream.put(streamId, reformedRunningTimes.get(streamId));
				}
				
				reformedRunningTimes.clear();
			}
		}
		
	}
	
	/*public void reinitializePolling(){
		while(!areAllStreamFinished()){
			System.out.println("Stream Monitor - Wait for all streams to finish - normally should not happen");
		}
		
		for(StreamFetchTask streamTask : streamsFetchTasks){
			if(!streamTask.isSubscriber){
				streamTask.restartTask();
				executor.execute(streamTask);
			}
		}
		
		System.out.println("Streams Monitor reinitialized");
	}*/
	
	
	/**
	 * Checks if all streams are finished retrieving items
	 * and if yes sets the stream monitor as finished
	 * @return
	 */
	public boolean areAllStreamFinished(){
		int allStreamsDone = 0;
		int allRunningStreams;
		
		List<StreamFetchTask> finishedTasks = new ArrayList<StreamFetchTask>();
		
		allRunningStreams = streamsFetchTasks.size();
		
		while(allStreamsDone < allRunningStreams)
			for(StreamFetchTask streamTask : streamsFetchTasks.values()){
				if(streamTask.completed && !finishedTasks.contains(streamTask)){
					totalRetrievedItems += streamTask.getTotalRetrievedItems();
					finishedTasks.add(streamTask);
					allStreamsDone++;
				}	
			}
		
		return true;
	}
	
	/**
	 * Stops the monitor - waits for all streams to shutdown
	 */
	public void stop(){
		isFinished = true;
		
		executor.shutdown();
		
        while (!executor.isTerminated()) {
        	System.out.println("Waiting for StreamsMonitor to shutdown");
        }
        
        System.out.println("Streams Monitor stopped");
	}
}
