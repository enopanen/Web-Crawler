package yelpbot;


public class Zip {
	private String zipNumber;
	private boolean deleted=false;
	
	public Zip(String zip)
	{
		zipNumber=zip;
	}//end Zip
	
	public void deleteZip()
	{
		deleted=true;
		
	}//end deleteZip()
	
	public boolean isDeleted()
	{
		return deleted;
	}//end isDeleted()
	
	
	@Override
	public String toString()
	{
		return zipNumber;
	}//end toString
}//end class Zip
