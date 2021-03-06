package eu.socialsensor.sfc.streams.monitors;

import java.util.ArrayList;
import java.util.List;

import eu.socialsensor.framework.common.domain.Feed;
import eu.socialsensor.framework.streams.Stream;
import eu.socialsensor.framework.streams.StreamException;

/**
 * Class for handling a Stream Task that is responsible for retrieving
 * multimedia content for the corresponding stream
 * @author ailiakop
 * @email  ailiakop@iti.gr
 */
public class StreamFetchTask implements Runnable{
	
	Stream stream;
	
	int totalRetrievedItems = 0;
	
	boolean completed = false;
	
	List<Feed> feeds = new ArrayList<Feed>();
	
	public StreamFetchTask(Stream stream){
		this.stream = stream;
	}
	
	public StreamFetchTask(Stream stream,List<Feed> feeds){
		this.stream = stream;
		this.feeds.addAll(feeds);
	}
	
	/**
	 * Adds the input feeds for the wrappers to 
	 * retrieve relevant multimedia content
	 * @param feeds
	 */
	public void addFeeds(List<Feed> feeds){
		this.feeds.addAll(feeds);
	}
	
	/**
	 * Clear the input feeds
	 */
	public void clear(){
		feeds.clear();
	}
	
	/**
	 * Returns the total number of retrieved items
	 * for the associated stream
	 * @return
	 */
	public int getTotalRetrievedItems(){
		return totalRetrievedItems;
	}
	
	/**
	 * Returns true if the stream task is completed
	 * @return
	 */
	public boolean isCompleted(){
		return completed;
	}
	
	public void restartTask(){
		completed = false;
	}
	
	/**
	 * Retrieves images/videos using the feeds
	 * as input to the corresponding wrapper to the stream
	 */
	@Override
	public void run(){
		try {
			
			totalRetrievedItems = stream.poll(feeds);
			
		} catch (StreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		completed = true;
	}
}
