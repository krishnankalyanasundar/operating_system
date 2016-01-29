import java.io.*;
import java.util.*;
import java.nio.file.*;


public class csefsck {

	static String pathDir = "./FS";
	static String fileSystem = "fusedata";
	
	static final int startBlockNumber  = 0;
	static final int superBlockNumber = 0;

	static final int maxPointerPerBlock = 400;
	static final int blockSize = 4096;

	static int maxBlockNumber = -1;
	static int rootBlockNumber = -1;
	static int freeStart = -1;
	static int freeEnd = -1;

	static boolean usedBlockList[];
	static boolean freeBlockList[];

	public static void main(String args[]) {

		System.out.println("\n\n############################################################################");
		System.out.println("############################# CSE FSCK - START #############################");
		System.out.println("############################################################################\n\n");


		try
		{
			
			long currentUnixTime = System.currentTimeMillis() / 1000L; // UNIX EPOCH time

			
			// block 0 = fusedata.0 = super block
			// read super block first -> find free, root, maxblocknumber

			String superBlockPath = pathDir+"/"+fileSystem+"."+superBlockNumber;

			System.out.println("**** Processing fusedata at block number "+superBlockNumber+" (Super Block) ***** \n");

			for (String readData : Files.readAllLines(Paths.get(superBlockPath))) 
			{
				StringTokenizer splitAttributes = new StringTokenizer(readData, "{,}");
				while (splitAttributes.hasMoreElements())
				{
					StringTokenizer splitKeyValue = new StringTokenizer(splitAttributes.nextElement().toString(), ":");


					if (splitKeyValue.hasMoreElements())
					{

						String key = splitKeyValue.nextElement().toString().trim();
						String value = splitKeyValue.nextElement().toString().trim();

						if (key.equals("freeStart"))
						{
							freeStart = Integer.parseInt(value);
						}

						else if (key.equals("freeEnd"))
						{
							freeEnd = Integer.parseInt(value);
						}

						else if (key.equals("root"))
						{
							rootBlockNumber = Integer.parseInt(value);
						}

						else if (key.equals("maxBlocks"))
						{
							maxBlockNumber = Integer.parseInt(value);
						}


						/* (1) DeviceID check */
						else if (key.equals("devId"))
						{
							if(value.equals("20"))
							{
								System.out.println(fileSystem+" has a valid DeviceID of 20\n");
							}
							else
							{
								System.out.println("Error - "+fileSystem+" has an invalid DeviceID!\n");
							}
						}

						/* (2) All times are in the past, nothing in the future */	
						else if (key.equals("creationTime"))
						{
							long creationTime = Long.parseLong(value);

							if(currentUnixTime<creationTime)
							{
								System.out.println("Error - Creation Time cannot be in the future\n");
							}
						}



			

					}
				}
			}

			System.out.println("\n**** Finished Processing fusedata at block number "+superBlockNumber+" **** \n\n");

			System.out.println("############################################################################");
			System.out.println("############################################################################\n\n");



			usedBlockList = new boolean[maxBlockNumber];
			freeBlockList = new boolean[maxBlockNumber];


			LinkedList<Integer> processList = new LinkedList<Integer>();


			for(int i=startBlockNumber;i<maxBlockNumber;i++)
			{
				usedBlockList[i] = false;
				freeBlockList[i] = false;
			}


			//add super block to used block list
			usedBlockList[superBlockNumber] = true;

			//add free block list to used block list
			for(int j=freeStart;j<=freeEnd;j++)
			{
				usedBlockList[j] = true;

				// find all elements
				int noOfPointersIntThisFreeBlock = -1; 
				ArrayList<String> indexBlockElements = new ArrayList<String>();


				String freeBlockPath = pathDir+"/"+fileSystem+"."+j;

				for (String readFreeBlockPathData : Files.readAllLines(Paths.get(freeBlockPath))) 
				{
						//System.out.println(readFreeBlockPathData);
					StringTokenizer readFreeBlockStrTok = new StringTokenizer(readFreeBlockPathData, " ,");
					noOfPointersIntThisFreeBlock = readFreeBlockStrTok.countTokens();
						//System.out.println("count = " + noOfPointersIntThisFreeBlock);
					while (readFreeBlockStrTok.hasMoreElements())
					{
							//System.out.println(readFreeBlockStrTok.nextElement());
						String freeBlock = readFreeBlockStrTok.nextElement().toString().trim();

						freeBlockList[Integer.parseInt(freeBlock)] = true;
					}

				}

			}


			//add root block to used block list
			usedBlockList[rootBlockNumber] = true;


			//from root block traverse through all directories and files and add visited blocks to used block list

			int linkCount = -1;
			int size = -1;
			int workingBlockNumber = 26;

			processList.add(rootBlockNumber);

			while(processList.size()>0)
			{
				workingBlockNumber = processList.removeFirst();
				usedBlockList[workingBlockNumber] = true;


				System.out.println("**** Processing fusedata at block number "+workingBlockNumber+" ***** \n");


				String thisBlockPath = pathDir+"/"+fileSystem+"."+workingBlockNumber;

				for (String readData : Files.readAllLines(Paths.get(thisBlockPath))) 
				{

					System.out.println(readData+"\n");

					StringTokenizer splitAttributes = new StringTokenizer(readData, "{,}");
					int n=0;
					while (splitAttributes.hasMoreElements())
					{
						StringTokenizer splitKeyValue = new StringTokenizer(splitAttributes.nextElement().toString(), ":");


						if (splitKeyValue.hasMoreElements())
						{

							String key = splitKeyValue.nextElement().toString().trim();
							String value = splitKeyValue.nextElement().toString().trim();

							if(!key.equals("indirect"))
							{
								System.out.println("\n"+key+"\t"+value);
							}



			// ########################################################################################################################


							/* (2) All times are in the past, nothing in the future */	
							
							if (key.equals("atime"))
							{
								long aTime = Long.parseLong(value);

								if(currentUnixTime<aTime)
								{
									System.out.println("Error -  Access Time cannot be in the future");
								}
							}

							if (key.equals("ctime"))
							{
								long cTime = Long.parseLong(value);

								if(currentUnixTime<cTime)
								{
									System.out.println("Error -  Creation Time cannot be in the future");
								}
							}

							if (key.equals("mtime"))
							{
								long mTime = Long.parseLong(value);

								if(currentUnixTime<mTime)
								{
									System.out.println("Error -  Modification Time cannot be in the future");
								}
							}



			// ########################################################################################################################



							if (key.equals("linkcount"))
							{
								linkCount = Integer.parseInt(value);
							}

							if(key.equals("filename_to_inode_dict"))
							{

								if(linkCount<0) //linkCount has not yet been read //if ordering was changed
								{
									String linkCountData = readData.replaceAll("(.*)(linkcount)((.*:.*,.*)|(.*:.*}.*))","$3");
									//System.out.println("linkCountData="+linkCountData);
									StringTokenizer splitlinkCountData = new StringTokenizer(linkCountData, ": },");
									linkCountData = splitlinkCountData.nextElement().toString().trim();
									linkCount = Integer.parseInt(linkCountData);
									//System.out.println("linkCount="+linkCount);
								}

								String entityData = readData.replaceAll("(.*)(filename_to_inode_dict)(.*)(}.*}.*)","$3");
								StringTokenizer splitEntityAttr = new StringTokenizer(entityData, "{: },");

								
								

								/* (5)	Each directory’s link count matches the number of links in the filename_to_inode_dict */
								if(linkCount*3 != splitEntityAttr.countTokens())
								{
									System.out.println("Error - linkCount does not match with the number of links in the filename_to_inode_dict");
									
									//update the linkCount to its correct value based on the number of entitites filename_to_inode_dict
									linkCount = splitEntityAttr.countTokens()/3;
								}

								for(int i=0; i<linkCount; i++)
								{

									splitKeyValue = new StringTokenizer(splitAttributes.nextElement().toString(), ":");

									String entityType = splitKeyValue.nextElement().toString().trim();
									String entityName = splitKeyValue.nextElement().toString().trim();
									String entityBlockNum = splitKeyValue.nextElement().toString().trim();

									System.out.println("\n"+entityType+"\t"+entityName+"\t"+entityBlockNum);


									/* (4) Each directory contains . and .. and their block numbers are correct */

									if(entityName.equals("."))
									{
										if(Integer.parseInt(entityBlockNum)==workingBlockNumber)
										{
											System.out.println("Valid Case - Working block number "+workingBlockNumber+" is in correspondence with . block number "+entityBlockNum);
										}
										else
										{
											System.out.println("Error - Working block number "+workingBlockNumber+" is not in correspondence with . block number "+entityBlockNum);
										}

									}

									else if(entityName.equals(".."))
									{

										String parentBlockNumber = entityBlockNum;
										String blockPath = pathDir+"/"+fileSystem+"."+parentBlockNumber;
										
										boolean dotDotValidity = false;
										
										for (String readParentBlockData : Files.readAllLines(Paths.get(blockPath))) 
										{
											if (readParentBlockData.matches("(.*)(filename_to_inode_dict)(.*)(}.*}.*)"))
											{
												
												String parentEntityData = readParentBlockData.replaceAll("(.*)(filename_to_inode_dict)(.*)(}.*}.*)","$3");
												StringTokenizer splitParentEntityAttr = new StringTokenizer(parentEntityData, "{: },");

												while(splitParentEntityAttr.hasMoreElements())
												{
													String parentEntityType = splitParentEntityAttr.nextElement().toString().trim();
													String parentEntityName = splitParentEntityAttr.nextElement().toString().trim();
													String parentEntityBlockNum = splitParentEntityAttr.nextElement().toString().trim();

													if(Integer.parseInt(parentEntityBlockNum)==workingBlockNumber)
													{
														dotDotValidity = true;
													}

												}

											}
											else
											{
												dotDotValidity = false;
											}

											if(workingBlockNumber==rootBlockNumber) //special case for root
											{
												dotDotValidity = true;
											}
										
											if(dotDotValidity)
											{
												System.out.println("Valid Case - Parent block is in correspondence with .. block number "+entityBlockNum);
											}
											else
											{
												System.out.println("Error - Parent block is not in correspondence with .. block number "+entityBlockNum);
											}


										}

									}

									else //can be a file f, directory d, special s -> add them to process list
									{
										processList.add(Integer.parseInt(entityBlockNum));
									}


								}


							}

			// ########################################################################################################################


			/* (6) If the data contained in a location pointer is an array, that indirect is one */
				// if indirect is 1, we expect an array of block numbers to be present at the block pointed by the location
				// every element of the array will point to a new block of 4096 bytes

			/* (7) That the size is valid for the number of block pointers in the location array. The three possibilities are:
					a.	size<blocksize if  indirect=0 and size>0
					b.	size<blocksize*length of location array if indirect!=0
					c.	size>blocksize*(length of location array-1) if indirect !=0   	*/


					if(key.equals("size"))
					{
						size = Integer.parseInt(value);

						if(size<0)
						{
							System.out.println("Error - Size of the file cannot be less than zero");
						}
						else if(size>1638400)
						{
							System.out.println("Error - Size of the file is not in correspondence to the Single Indirect allocation technique");
						}
					}


					if(key.equals("indirect"))
					{
						String indirectValue = value.replaceAll("(.*)(location)(.*)","$1").trim();
						String locationValue = splitKeyValue.nextElement().toString().trim();

						int indirect = Integer.parseInt(indirectValue);
						int location = Integer.parseInt(locationValue);

						//add location to used block list
						usedBlockList[location]=true;

						System.out.println("\nindirect\t"+indirect+"\nlocation\t"+location);

						if(indirect==0)
						{

							System.out.println("Valid Case - Size of the file is "+size+" bytes which is less than block size of "+blockSize+" bytes");

							// read data of the file at location
							String blockPath = pathDir+"/"+fileSystem+"."+location;
							System.out.println("\nData present in the file at location "+location+" : ");
							for (String readBlockPathData : Files.readAllLines(Paths.get(blockPath))) 
							{
								System.out.println(readBlockPathData);
							}

						}
						else
						{

							int noOfPointersInIndexBlock = -1; 
							ArrayList<String> indexBlockElements = new ArrayList<String>();


							String blockPath = pathDir+"/"+fileSystem+"."+location;
							//System.out.println("Data present in the file at location "+location+" : ");
							for (String readBlockPathData : Files.readAllLines(Paths.get(blockPath))) 
							{
								StringTokenizer readBlockPathDataStrTok = new StringTokenizer(readBlockPathData, " ,");
								noOfPointersInIndexBlock = readBlockPathDataStrTok.countTokens();
								//System.out.println("count = " + noOfPointersInIndexBlock);
								
								while (readBlockPathDataStrTok.hasMoreElements())
								{
									indexBlockElements.add(readBlockPathDataStrTok.nextElement().toString().trim());
								}

							}



							if(size < blockSize*noOfPointersInIndexBlock && size > blockSize*(noOfPointersInIndexBlock-1))
							{
								System.out.println("Valid Case - Size of the file is "+size+" bytes which is more than "+blockSize*(noOfPointersInIndexBlock-1)+" bytes and less than "+blockSize*noOfPointersInIndexBlock+" bytes");

								for (int j=0;j<noOfPointersInIndexBlock;j++)
								{
									//read data at indexBlockElements[j]
									blockPath = pathDir+"/"+fileSystem+"."+indexBlockElements.get(j);
									System.out.println("\nData present in the file at location "+indexBlockElements.get(j)+" : ");
									
									for (String readBlockPathData : Files.readAllLines(Paths.get(blockPath))) 
									{
										System.out.print(readBlockPathData);
									}

									//add indexBlockElements to used block list
									usedBlockList[Integer.parseInt(indexBlockElements.get(j))]=true;

								}
							}
							else
							{
								System.out.println("Error - Size of the file is not in correspondence to the number of pointers present in the location array");
							}					


						}


					}

			// ########################################################################################################################






				}


			}

		}





		System.out.println("\n\n**** Finished Processing fusedata at block number "+workingBlockNumber+" **** \n\n");

		System.out.println("############################################################################");
		System.out.println("############################################################################\n\n");

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


	/* (3)	Validate that the free block list is accurate this includes
		a.	Making sure the free block list contains ALL of the free blocks
		b.	Make sure than there are no files/directories stored on items listed in the free block list */


		System.out.println("Comparing used block list with free block list...\n");

		for(int i=startBlockNumber;i<maxBlockNumber;i++)
		{
			if(usedBlockList[i]==freeBlockList[i])
			{
				System.out.println("Discrepancy found in block number "+i);
			}

		}

		System.out.println("\n\n############################################################################");
		System.out.println("############################## CSE FSCK - END ##############################");
		System.out.println("############################################################################\n\n\n");

	
	} // end of main


} // end of class

