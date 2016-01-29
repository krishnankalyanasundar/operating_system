/*
* Krishnan Kalyanasundar
* N17957143 | kk3059
*
* CS 6233 - Section B
* HW 3 - Memory Management
*/


import java.io.*;
import java.util.*;
import java.nio.file.*;


public class csepaging {

	static String fileLoc;


	public static void main (String args[]) {

		System.out.println("\n\n########################################################");
		System.out.println("################## CSE PAGING - START ##################");
		System.out.println("########################################################\n\n");

		fileLoc = args[0];

		pffCall();

		vswsCall();
	
		System.out.println("\n\n########################################################");
		System.out.println("################### CSE PAGING - END ###################");
		System.out.println("########################################################\n\n\n");

	
	} // end of main


	public static void pffCall()
	{
		Properties pffResult = new Properties();	

		// run pff first time to find a resonable F value
		pffResult = pff(0);
		int pageFaults_pff = Integer.parseInt(pffResult.getProperty("pageFaults"));
		int avgWindowSize_pff = Integer.parseInt(pffResult.getProperty("resonableFValue"));
		int maxWindowSize_pff = Integer.parseInt(pffResult.getProperty("maxWindowSize"));

		//run pff with defined window size
		pffResult = pff(avgWindowSize_pff);
		pageFaults_pff = Integer.parseInt(pffResult.getProperty("pageFaults"));
		avgWindowSize_pff = Integer.parseInt(pffResult.getProperty("resonableFValue"));
		maxWindowSize_pff = Integer.parseInt(pffResult.getProperty("maxWindowSize"));

		System.out.println("PFF :");
		System.out.println("Number of page faults = " + pageFaults_pff);
		System.out.println("Resonable value for F = " + avgWindowSize_pff);
		System.out.println("Maximum window size = " + maxWindowSize_pff);
	}


	public static void vswsCall()
	{

		int mValue, lValue, qValue;
		mValue = lValue = qValue = 1; // initial assumption on M, L & Q

		Properties vswsResult = new Properties();
		vswsResult = vsws(mValue, lValue, qValue);

		int pageFaults_vsws = Integer.parseInt(vswsResult.getProperty("pageFaults"));
		int avgWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("avgWindowSize"));
		int maxWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("maxWindowSize"));

		/*
		* The parameter values (M, L, Q) are to be selected in such a way that there is a proper balance between 
		* memory overflow and memmory underflow conditions. The average window size can be assumed to be equal to the 
		* average sampling interval in between M and L. The occurance of Qth page fault just after elapsing M time
		* leads to better sampling results.
		*/

		int max = pageFaults_vsws;
		int diff = avgWindowSize_vsws;

		int sum = 0;
		int pfsum = 0;

		int tempWindowSize = avgWindowSize_vsws;

		for(int i=1; i<=avgWindowSize_vsws; i++)
		{
			for(int j=1; j<=avgWindowSize_vsws;  j++)
			{
				if(i<j)
				{			
					mValue = i;
					lValue = j;
					qValue = i;

					vswsResult = vsws(mValue, lValue, qValue);

					pageFaults_vsws = Integer.parseInt(vswsResult.getProperty("pageFaults"));
					avgWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("avgWindowSize"));
					maxWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("maxWindowSize"));

					sum = sum + (pageFaults_vsws * avgWindowSize_vsws);
					pfsum = pfsum + pageFaults_vsws;

				}


			}

		}


		diff = sum / pfsum;
		
		lValue = tempWindowSize;
		mValue = qValue = lValue - diff;
		if(mValue<0)
		{
			mValue = qValue = 1;
		}
			

		vswsResult = vsws(mValue, lValue, qValue);

		pageFaults_vsws = Integer.parseInt(vswsResult.getProperty("pageFaults"));
		avgWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("avgWindowSize"));
		maxWindowSize_vsws = Integer.parseInt(vswsResult.getProperty("maxWindowSize"));

		System.out.println("\n\nVSWS :");
		System.out.println("Value of M = " + mValue);
		System.out.println("Value of L = " + lValue);
		System.out.println("Value of Q = " + qValue);
		System.out.println("Number of page faults = " + pageFaults_vsws);
		System.out.println("Average window size = " + avgWindowSize_vsws);
		System.out.println("Maximum window size = " + maxWindowSize_vsws);


	}







	public static Properties pff ( int workingSetReasonableSize )
	{	
		
		int curIndex = -1;
		int index = -1;

		int workingSetSize = workingSetReasonableSize;
		int maxWorkingSetSize = 0;

		int pff, lastPFTime, curPFTime, diffPFTime;
		pff = lastPFTime = curPFTime = diffPFTime = 0;

		int pageNumber = -1;
		int useBit = 0;

		int sumWindowSize = 0;
		int maxWindowSize = 0;

		int pffDiffTotal= 0;
		int resonableFValue = 0;

	 	HashMap<Integer,Integer> pat_pff = new HashMap<Integer,Integer>();

		try
		{


			for (String readData : Files.readAllLines(Paths.get(fileLoc))) 
			{
				
				index++;
				
				if(index == 0) // first line of the file
				{
					maxWorkingSetSize = Integer.parseInt(readData);
				}
				else
				{
					pageNumber = Integer.parseInt(readData);
					useBit = 1;

					if(workingSetSize == 0) // first time when pff has never been run
						{
							workingSetSize = maxWorkingSetSize;
						}


					if(pat_pff.containsKey(pageNumber)) // No page fault. Setting useBit to 1 for pageNumber
					{
						useBit = 1;
						pat_pff.put(pageNumber,useBit);
					}
					else // Page fault occured
					{
						lastPFTime = curPFTime;
						curPFTime = index;
						
						pff++;
						
						diffPFTime = curPFTime - lastPFTime;
						
						pffDiffTotal = pffDiffTotal + diffPFTime;
						
						
						if(pat_pff.size() > workingSetSize) // remove pages in memory with useBit 0
						{
							Iterator <Map.Entry<Integer,Integer>> iter = pat_pff.entrySet().iterator();
							while (iter.hasNext()) {
							    Map.Entry<Integer,Integer> entry = iter.next();
							    if(entry.getValue() == 0){
							        iter.remove();
							    }
							}

						}
						else if(diffPFTime > pff) // clearing all useBit 1 to 0
						{
							for ( Integer pgNum : pat_pff.keySet() ) {    
							    useBit = 0;
							    pat_pff.put(pgNum,useBit);
							}

						}
						
						// add faulting page to working set
						useBit = 1;
						pat_pff.put(pageNumber,useBit);

						sumWindowSize = sumWindowSize + pat_pff.size();
						if(maxWindowSize < pat_pff.size())
						{
							maxWindowSize = pat_pff.size();
						}
						
					}



				}



			}




		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}


		resonableFValue = pffDiffTotal / pff ;
		
		Properties pffResult = new Properties();
		pffResult.setProperty("pageFaults",pff+"");
		pffResult.setProperty("resonableFValue",resonableFValue+"");
		pffResult.setProperty("maxWindowSize",maxWindowSize+"");


		return pffResult;


	}

	public static Properties vsws (int mValue, int lValue, int qValue)
	{
		int pageFaults = 0;

		int curIndex = -1;
		int index = -1;
		int workingSetSize = 0;

		int sumWindowSize = 0;
		int maxWindowSize = 0;

		int pageNumber = -1;
		int useBit = 0;

		HashMap<Integer,Integer> pat_vsws = new HashMap<Integer,Integer>();


		try
		{

			for (String readData : Files.readAllLines(Paths.get(fileLoc))) 
			{
			
				index++;
			
				if(index == 0) // first line of the file
				{
					workingSetSize = Integer.parseInt(readData);
				}
				else
				{

					pageNumber = Integer.parseInt(readData);
					useBit = 1;

					if(pat_vsws.containsKey(pageNumber)) // No page fault. Set useBit to 1.
					{
						useBit = 1;
						pat_vsws.put(pageNumber,useBit);
					}
					else // Page fault occured
					{
						pageFaults++;
						
						curIndex = index % lValue;
						if (curIndex == 0)
						{
							curIndex = lValue;
						}
						

						if(curIndex < mValue) // Wait to clear the bits
						{
							useBit = 1;
							pat_vsws.put(pageNumber,useBit);
						}

						else if(curIndex >= mValue && curIndex < lValue) // Use Q as a deciding factor
						{
							if(pageFaults < qValue) // wait to clear the bits
							{
								useBit = 1;
								pat_vsws.put(pageNumber,useBit);
							}
							else // clear the bits
							{
								for ( Integer pgNum : pat_vsws.keySet() ) {    
								    useBit = 0;
								    pat_vsws.put(pgNum,useBit);
								}

								useBit = 1;
								pat_vsws.put(pageNumber,useBit);

							}

						}

						else if(curIndex >= lValue) // removing pages in memory with useBit 0
						{
							Iterator <Map.Entry<Integer,Integer>> iter = pat_vsws.entrySet().iterator();
							while (iter.hasNext()) {
							    Map.Entry<Integer,Integer> entry = iter.next();
							    if(entry.getValue() == 0){
							        iter.remove();
							    }
							}

							useBit = 1;
							pat_vsws.put(pageNumber,useBit);
						}




					}

					sumWindowSize = sumWindowSize + pat_vsws.size();
					if(maxWindowSize < pat_vsws.size())
					{
						maxWindowSize = pat_vsws.size();
					}
						
				}


			}

		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		int avgWindowSize = sumWindowSize/index;
		
		Properties vswsResult = new Properties();
		vswsResult.setProperty("pageFaults",pageFaults+"");
		vswsResult.setProperty("avgWindowSize",avgWindowSize+"");
		vswsResult.setProperty("maxWindowSize",maxWindowSize+"");


		return vswsResult;


	}



} // end of class


